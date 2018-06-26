package com.marverenic.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;

import com.marverenic.music.IPlayerService;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.transaction.ListTransaction;
import com.marverenic.music.utils.ObservableQueue;
import com.marverenic.music.utils.RxProperty;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * An implementation of {@link PlayerController} used in release builds to communicate with the
 * media player throughout the application. This implementation uses AIDL to send commands through
 * IPC to the remote player service, and gets information with a combination of AIDL to fetch data
 * and BroadcastReceivers to be notified of automatic changes to the player state.
 *
 * This class is responsible for all communication to the remote service, including starting,
 * binding, unbinding and restarting the service if it crashes.
 */
public class ServicePlayerController implements PlayerController {

    private static final int POSITION_TICK_MS = 200;
    private static final int SERVICE_RESTART_THRESHOLD_MS = 500;

    private Context mContext;
    private IPlayerService mBinding;
    private PlayerServiceConnection mConnection;
    private long mServiceStartRequestTime;

    private PublishSubject<String> mErrorStream = PublishSubject.create();
    private PublishSubject<String> mInfoStream = PublishSubject.create();

    private final RxProperty<Boolean> mPlaying = new RxProperty<>("playing");
    private final RxProperty<Song> mNowPlaying = new RxProperty<>("now playing");
    private final RxProperty<List<Song>> mQueue = new RxProperty<>("queue", Collections.emptyList());
    private final RxProperty<Integer> mQueuePosition = new RxProperty<>("queue index");
    private final RxProperty<Integer> mCurrentPosition = new RxProperty<>("seek position");
    private final RxProperty<Integer> mDuration = new RxProperty<>("duration");
    private final RxProperty<Integer> mMultiRepeatCount = new RxProperty<>("multi-repeat");
    private final RxProperty<Long> mSleepTimerEndTime = new RxProperty<>("sleep timer");
    private final RxProperty<Boolean> mShuffleMode = new RxProperty<>("shuffle-mode");
    private final RxProperty<Integer> mRepeatMode = new RxProperty<>("repeat-mode");

    private BehaviorSubject<MediaSessionCompat.Token> mMediaSessionToken;

    private BehaviorSubject<Bitmap> mArtwork;
    private Subscription mCurrentPositionClock;

    private Random mShuffleSeedGenerator;
    private Handler mMainHandler;
    private HandlerThread mRequestThread;
    private ObservableQueue<Runnable> mRequestQueue;
    private Subscription mRequestQueueSubscription;

    private Set<ServiceBinding> mActiveBindings;

    public ServicePlayerController(Context context, PreferenceStore preferenceStore) {
        mContext = context;
        mConnection = new PlayerServiceConnection();
        mRequestThread = new HandlerThread("ServiceExecutor");
        mMediaSessionToken = BehaviorSubject.create();
        mRequestQueue = new ObservableQueue<>();
        mActiveBindings = new HashSet<>();

        mShuffleMode.setValue(preferenceStore.isShuffled());
        mRepeatMode.setValue(preferenceStore.getRepeatMode());

        mShuffleSeedGenerator = new Random();
        mMainHandler = new Handler(Looper.getMainLooper());
        mRequestThread.start();

        isPlaying().subscribe(
                isPlaying -> {
                    if (isPlaying) {
                        startCurrentPositionClock();
                    } else {
                        stopCurrentPositionClock();
                    }
                }, throwable -> {
                    Timber.e(throwable, "Failed to update current position clock");
                });
    }

    @Override
    public Binding bind() {
        ServiceBinding binding = new ServiceBinding();
        mActiveBindings.add(binding);
        startService();
        return binding;
    }

    @Override
    public void unbind(Binding binding) {
        if (!(binding instanceof ServiceBinding)) {
            throw new IllegalArgumentException(binding + " is not a valid binding");
        }

        if (!mActiveBindings.remove(binding)) {
            Timber.w("Binding with UID %s was already unbound", ((ServiceBinding) binding).uid);
        } else if (mActiveBindings.isEmpty()) {
            unbindService();
        }
    }

    private void startService() {
        MediaStoreUtil.getPermission(mContext)
                .subscribe(this::bindService, t -> Timber.i(t, "Failed to get Storage permission"));
    }

    private void bindService(boolean hasMediaStorePermission) {
        long timeSinceLastStartRequest = SystemClock.uptimeMillis() - mServiceStartRequestTime;

        if (!hasMediaStorePermission || mBinding != null
                || timeSinceLastStartRequest < SERVICE_RESTART_THRESHOLD_MS) {
            return;
        }

        mServiceStartRequestTime = SystemClock.uptimeMillis();
        Timber.i("Starting service at time %dl", mServiceStartRequestTime);

        Intent serviceIntent = PlayerService.newIntent(mContext, true);
        int bindFlags = Context.BIND_WAIVE_PRIORITY | Context.BIND_AUTO_CREATE;
        mContext.bindService(serviceIntent, mConnection, bindFlags);
    }

    private void unbindService() {
        disconnectService();
        mContext.unbindService(mConnection);
    }

    private void disconnectService() {
        releaseAllProperties();
        mBinding = null;
        mServiceStartRequestTime = 0;
        if (mRequestQueueSubscription != null) {
            mRequestQueueSubscription.unsubscribe();
            mRequestQueueSubscription = null;
        }
    }

    private void bindRequestQueue() {
        mRequestQueueSubscription = mRequestQueue.toObservable()
                .observeOn(AndroidSchedulers.from(mRequestThread.getLooper()))
                .subscribe(Runnable::run, throwable -> {
                    Timber.e(throwable, "Failed to process request");
                    // Make sure to restart the request queue, otherwise all future commands will
                    // be dropped
                    bindRequestQueue();
                });
    }

    // region RxProperty management

    private void execute(Runnable command) {
        mRequestQueue.enqueue(command);
    }

    private void startCurrentPositionClock() {
        if (mCurrentPositionClock != null && !mCurrentPositionClock.isUnsubscribed()) {
            return;
        }

        mCurrentPositionClock = Observable.interval(POSITION_TICK_MS, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .subscribe(tick -> {
                    if (!mCurrentPosition.isSubscribedTo()) {
                        stopCurrentPositionClock();
                    } else {
                        mCurrentPosition.invalidate();
                    }
                }, throwable -> {
                    Timber.e(throwable, "Failed to perform position tick");
                });
    }

    private void stopCurrentPositionClock() {
        if (mCurrentPositionClock != null) {
            mCurrentPositionClock.unsubscribe();
            mCurrentPositionClock = null;
        }
    }

    private void releaseAllProperties() {
        mPlaying.setFunction(null);
        mNowPlaying.setFunction(null);
        mQueue.setFunction(null);
        mQueuePosition.setFunction(null);
        mCurrentPosition.setFunction(null);
        mDuration.setFunction(null);
        mMultiRepeatCount.setFunction(null);
        mSleepTimerEndTime.setFunction(null);
        mShuffleMode.setFunction(null);
        mRepeatMode.setFunction(null);
        mMediaSessionToken.onNext(null);
    }

    private void initAllProperties() {
        mPlaying.setFunction(mBinding::isPlaying);
        mNowPlaying.setFunction(mBinding::getNowPlaying);
        mQueue.setFunction(this::getBigQueue);
        mQueuePosition.setFunction(mBinding::getQueuePosition);
        mCurrentPosition.setFunction(mBinding::getCurrentPosition);
        mDuration.setFunction(mBinding::getDuration);
        mMultiRepeatCount.setFunction(mBinding::getMultiRepeatCount);
        mSleepTimerEndTime.setFunction(mBinding::getSleepTimerEndTime);
        mShuffleMode.setFunction(mBinding::getShuffleMode);
        mRepeatMode.setFunction(mBinding::getRepeatMode);

        invalidateAll();
    }

    private void runOnMainThread(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mMainHandler.post(action);
        }
    }

    private void invalidateAll() {
        runOnMainThread(() -> {
            mPlaying.invalidate();
            mNowPlaying.invalidate();
            mQueue.invalidate();
            mQueuePosition.invalidate();
            mCurrentPosition.invalidate();
            mDuration.invalidate();
            mMultiRepeatCount.invalidate();
            mSleepTimerEndTime.invalidate();
            mShuffleMode.invalidate();
            mRepeatMode.invalidate();

            fetchMediaSessionToken();
        });
    }

    @Override
    public Observable<String> getError() {
        return mErrorStream.asObservable();
    }

    @Override
    public Observable<String> getInfo() {
        return mInfoStream.asObservable();
    }

    // endregion RxProperty management

    // region Binder delegates

    private void fetchMediaSessionToken() {
        if (mBinding == null) {
            return;
        }

        if (!mMediaSessionToken.hasValue() || mMediaSessionToken.getValue() == null) {
            MediaSessionCompat.Token token = null;

            try {
                token = mBinding.getMediaSessionToken();
            } catch (RemoteException e) {
                Timber.e(e, "Failed to get session token");
            }

            if (token != null) {
                mMediaSessionToken.onNext(token);
            }
        }
    }

    @Override
    public Single<PlayerState> getPlayerState() {
        return Observable.fromCallable(mBinding::getPlayerState).toSingle();
    }

    @Override
    public void restorePlayerState(PlayerState restoreState) {
        execute(() -> {
            try {
                mBinding.restorePlayerState(restoreState);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to restore player state");
            }
            invalidateAll();
        });
    }

    @Override
    public void stop() {
        mPlaying.setValue(false);

        execute(() -> {
            try {
                mBinding.stop();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to stop service");
            }
            invalidateAll();
        });
    }

    @Override
    public void skip() {
        execute(() -> {
            try {
                mBinding.skip();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip current track");
            }
            invalidateAll();
        });
    }

    @Override
    public void previous() {
        execute(() -> {
            try {
                mBinding.previous();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip backward");
            }
            invalidateAll();
        });
    }

    @Override
    public void togglePlay() {
        if (mPlaying.hasValue()) {
            mPlaying.setValue(!mPlaying.lastValue());
        }

        execute(() -> {
            try {
                mBinding.togglePlay();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to toggle playback");
            }
            invalidateAll();
        });
    }

    @Override
    public void play() {
        mPlaying.setValue(true);

        execute(() -> {
            try {
                mBinding.play();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to resume playback");
            }
            invalidateAll();
        });
    }

    @Override
    public void pause() {
        mPlaying.setValue(false);

        execute(() -> {
            try {
                mBinding.pause();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to pause playback");
            }
            invalidateAll();
        });
    }

    @Override
    public void updatePlayerPreferences(ReadOnlyPreferenceStore preferenceStore) {
        long seed = mShuffleSeedGenerator.nextLong();
        mShuffleMode.setValue(preferenceStore.isShuffled());
        mRepeatMode.setValue(preferenceStore.getRepeatMode());

        execute(() -> {
            try {
                mBinding.setPreferences(new ImmutablePreferenceStore(preferenceStore), seed);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to update remote player preferences");
            }
            invalidateAll();
        });
    }

    @Override
    public void setQueue(List<Song> newQueue, int newPosition) {
        long seed = mShuffleSeedGenerator.nextLong();

        if (newPosition < newQueue.size()) {
            boolean shuffled = mShuffleMode.lastValue();

            mNowPlaying.setValue(newQueue.get(newPosition));
            mQueuePosition.setValue(shuffled ? 0 : newPosition);
            mCurrentPosition.setValue(0);

            if (shuffled) {
                mQueue.setValue(MusicPlayer.generateShuffledQueue(newQueue, newPosition, seed));
            } else {
                mQueue.setValue(newQueue);
            }
        }

        execute(() -> {
            try {
                if (newQueue.size() > MAXIMUM_CHUNK_ENTRIES) {
                    ListTransaction.<Song, RemoteException>send(newQueue).transmit(
                            token -> mBinding.beginLargeQueueTransaction(token),
                            (header, data) -> mBinding.sendQueueChunk(header, data),
                            () -> mBinding.endLargeQueueTransaction(newPosition, seed));
                } else {
                    mBinding.setQueue(newQueue, newPosition, seed);
                }
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to set queue");
            }
            invalidateAll();
        });
    }

    /**
     * This method allow read queue from remote service by chunks, but if the queue size is less
     * than {@link #MAXIMUM_CHUNK_ENTRIES}, this method will return {@link IPlayerService#getQueue()}
     * directly.
     *
     * @return Now playing queue.
     */
    private List<Song> getBigQueue() throws RemoteException {
        int queueSize = mBinding.getQueueSize();
        if (queueSize <= MAXIMUM_CHUNK_ENTRIES) {
            return mBinding.getQueue();
        } else {
            List<Song> queue = new ArrayList<>();
            try {
                int offset;
                for (offset = 0; offset + MAXIMUM_CHUNK_ENTRIES <= queueSize; offset += MAXIMUM_CHUNK_ENTRIES) {
                    queue.addAll(mBinding.getQueueChunk(offset, MAXIMUM_CHUNK_ENTRIES));
                }
                if (offset < queueSize) queue.addAll(mBinding.getQueueChunk(offset, queueSize - offset));
            } catch (IllegalArgumentException ex) {
                // maybe queue is modified --> just return what was read
                Timber.d(ex, "Can't get queue chunk, return current queue with size: %d", queue.size());
            }
            return queue;
        }
    }

    @Override
    public void clearQueue() {
        setQueue(Collections.emptyList(), 0);
    }

    @Override
    public void changeSong(int newPosition) {
        execute(() -> {
            try {
                mBinding.changeSong(newPosition);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to change song");
            }
            invalidateAll();
        });
    }

    @Override
    public void editQueue(List<Song> queue, int newPosition) {
        execute(() -> {
            try {
                if (queue.size() > MAXIMUM_CHUNK_ENTRIES) {
                    ListTransaction.<Song, RemoteException>send(queue).transmit(
                            token -> mBinding.beginLargeQueueTransaction(token),
                            (header, data) -> mBinding.sendQueueChunk(header, data),
                            () -> mBinding.endLargeQueueEdit(newPosition));
                } else {
                    mBinding.editQueue(queue, newPosition);
                }
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to edit queue");
            }
            invalidateAll();
        });
    }

    @Override
    public void queueNext(Song song) {
        execute(() -> {
            try {
                mBinding.queueNext(song);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue next song");
            }
            invalidateAll();
        });
    }

    @Override
    public void queueNext(List<Song> songs) {
        execute(() -> {
            try {
                mBinding.queueNextList(songs);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue next songs");
            }
            invalidateAll();
        });
    }

    @Override
    public void queueLast(Song song) {
        execute(() -> {
            try {
                mBinding.queueLast(song);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue last song");
            }
            invalidateAll();
        });
    }

    @Override
    public void queueLast(List<Song> songs) {
        execute(() -> {
            try {
                mBinding.queueLastList(songs);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to queue last songs");
            }
            invalidateAll();
        });
    }

    @Override
    public void seek(int position) {
        mCurrentPosition.setValue(position);

        execute(() -> {
            try {
                mBinding.seekTo(position);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to seek");
            }
            invalidateAll();
        });
    }

    @Override
    public Observable<Boolean> isPlaying() {
        return mPlaying.getObservable();
    }

    @Override
    public Observable<Song> getNowPlaying() {
        return mNowPlaying.getObservable();
    }

    @Override
    public Observable<List<Song>> getQueue() {
        return mQueue.getObservable();
    }

    @Override
    public Observable<Integer> getQueuePosition() {
        return mQueuePosition.getObservable();
    }

    @Override
    public Observable<Integer> getCurrentPosition() {
        startCurrentPositionClock();
        return mCurrentPosition.getObservable();
    }

    @Override
    public Observable<Integer> getDuration() {
        return mDuration.getObservable();
    }

    @Override
    public Observable<Boolean> isShuffleEnabled() {
        return mShuffleMode.getObservable().distinctUntilChanged();
    }

    @Override
    public Observable<Integer> getRepeatMode() {
        return mMultiRepeatCount.getObservable()
                .flatMap(multiRepeatCount -> {
                    if (multiRepeatCount > 1) {
                        return Observable.just(multiRepeatCount);
                    } else {
                        return mRepeatMode.getObservable();
                    }
                })
                .distinctUntilChanged();
    }

    @Override
    public void setMultiRepeatCount(int count) {
        execute(() -> {
            try {
                mBinding.setMultiRepeatCount(count);
                runOnMainThread(() -> {
                    mMultiRepeatCount.setValue(count);
                });
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to set multi-repeat count");
                invalidateAll();
            }
        });
    }

    @Override
    public Observable<Long> getSleepTimerEndTime() {
        return mSleepTimerEndTime.getObservable();
    }

    @Override
    public void setSleepTimerEndTime(long timestampInMillis) {
        execute(() -> {
            try {
                mBinding.setSleepTimerEndTime(timestampInMillis);
                runOnMainThread(() -> {
                    mSleepTimerEndTime.setValue(timestampInMillis);
                });
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to set sleep-timer end time");
                invalidateAll();
            }
        });
    }

    @Override
    public void disableSleepTimer() {
        setSleepTimerEndTime(0L);
    }

    @Override
    public Observable<Bitmap> getArtwork() {
        if (mArtwork == null) {
            mArtwork = BehaviorSubject.create(BitmapFactory.decodeResource(
                    mContext.getResources(), R.drawable.art_default_xl));

            getNowPlaying()
                    .observeOn(Schedulers.io())
                    .flatMap((Song song) -> {
                        return Util.fetchArtwork(mContext, song);
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mArtwork::onNext, throwable -> {
                        Timber.e(throwable, "Failed to fetch artwork");
                        mArtwork.onNext(null);
                    });
        }

        return mArtwork;
    }

    @Override
    public Observable<MediaSessionCompat.Token> getMediaSessionToken() {
        return mMediaSessionToken.filter(token -> token != null);
    }

    // endregion Binder delegates

    private class PlayerServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Timber.i("Service connected");
            mBinding = IPlayerService.Stub.asInterface(service);
            initAllProperties();
            bindRequestQueue();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Timber.i("Service disconnected");
            disconnectService();
        }
    }

    private class ServiceBinding implements Binding {
        private final UUID uid = UUID.randomUUID();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceBinding that = (ServiceBinding) o;
            return uid.equals(that.uid);
        }

        @Override
        public int hashCode() {
            return uid.hashCode();
        }
    }

    /**
     * A {@link BroadcastReceiver} class listening for intents with an
     * {@link MusicPlayer#UPDATE_BROADCAST} action. This broadcast must be sent ordered with this
     * receiver being the highest priority so that the UI can access this class for accurate
     * information from the player service
     */
    public static class Listener extends BroadcastReceiver {

        @Inject
        PlayerController mController;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(MusicPlayer.UPDATE_EXTRA_MINOR, false)) {
                // Ignore minor updates – we already handle them without being notified
                return;
            }

            if (mController == null) {
                JockeyApplication.getComponent(context).inject(this);
            }

            if (mController instanceof ServicePlayerController) {
                ServicePlayerController playerController = (ServicePlayerController) mController;

                if (intent.getAction().equals(MusicPlayer.UPDATE_BROADCAST)) {
                    playerController.invalidateAll();
                } else if (intent.getAction().equals(MusicPlayer.INFO_BROADCAST)) {
                    String error = intent.getExtras().getString(MusicPlayer.INFO_EXTRA_MESSAGE);
                    playerController.mInfoStream.onNext(error);

                } else if (intent.getAction().equals(MusicPlayer.ERROR_BROADCAST)) {
                    String info = intent.getExtras().getString(MusicPlayer.ERROR_EXTRA_MSG);
                    playerController.mErrorStream.onNext(info);
                }
            }
        }
    }

}
