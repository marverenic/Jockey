package com.marverenic.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;

import com.marverenic.music.IPlayerService;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.transaction.ListTransaction;
import com.marverenic.music.utils.ObservableQueue;
import com.marverenic.music.utils.Optional;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
    private long mServiceStartRequestTime;

    private PublishSubject<String> mErrorStream = PublishSubject.create();
    private PublishSubject<String> mInfoStream = PublishSubject.create();

    private final Prop<Boolean> mPlaying = new Prop<>("playing");
    private final Prop<Song> mNowPlaying = new Prop<>("now playing");
    private final Prop<List<Song>> mQueue = new Prop<>("queue", Collections.emptyList());
    private final Prop<Integer> mQueuePosition = new Prop<>("queue index");
    private final Prop<Integer> mCurrentPosition = new Prop<>("seek position");
    private final Prop<Integer> mDuration = new Prop<>("duration");
    private final Prop<Integer> mMultiRepeatCount = new Prop<>("multi-repeat");
    private final Prop<Long> mSleepTimerEndTime = new Prop<>("sleep timer");

    private BehaviorSubject<Boolean> mShuffled;
    private BehaviorSubject<Integer> mRepeatMode;
    private BehaviorSubject<Bitmap> mArtwork;
    private Subscription mCurrentPositionClock;

    private Random mShuffleSeedGenerator;
    private Handler mMainHandler;
    private HandlerThread mRequestThread;
    private ObservableQueue<Runnable> mRequestQueue;
    private Subscription mRequestQueueSubscription;

    public ServicePlayerController(Context context, PreferenceStore preferenceStore) {
        mContext = context;
        mRequestThread = new HandlerThread("ServiceExecutor");
        mShuffled = BehaviorSubject.create(preferenceStore.isShuffled());
        mRepeatMode = BehaviorSubject.create(preferenceStore.getRepeatMode());
        mRequestQueue = new ObservableQueue<>();

        mShuffleSeedGenerator = new Random();
        mMainHandler = new Handler(Looper.getMainLooper());
        mRequestThread.start();
        startService();

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

        Intent serviceIntent = PlayerService.newIntent(mContext, true);

        // Manually start the service to ensure that it is associated with this task and can
        // appropriately set its dismiss behavior
        mContext.startService(serviceIntent);
        mServiceStartRequestTime = SystemClock.uptimeMillis();

        mContext.bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBinding = IPlayerService.Stub.asInterface(service);
                initAllProperties();
                bindRequestQueue();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mContext.unbindService(this);
                releaseAllProperties();
                mServiceStartRequestTime = 0;
                mBinding = null;
                if (mRequestQueueSubscription != null) {
                    mRequestQueueSubscription.unsubscribe();
                    mRequestQueueSubscription = null;
                }
            }
        }, Context.BIND_WAIVE_PRIORITY);
    }

    private void ensureServiceStarted() {
        if (mBinding == null) {
            startService();
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

    private void execute(Runnable command) {
        ensureServiceStarted();
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
        execute(() -> {
            try {
                mBinding.setPreferences(new ImmutablePreferenceStore(preferenceStore), seed);
                runOnMainThread(() -> {
                    mShuffled.onNext(preferenceStore.isShuffled());
                    mRepeatMode.onNext(preferenceStore.getRepeatMode());
                });
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
            boolean shuffled = mShuffled.getValue();

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
        ensureServiceStarted();
        return mPlaying.getObservable();
    }

    @Override
    public Observable<Song> getNowPlaying() {
        ensureServiceStarted();
        return mNowPlaying.getObservable();
    }

    @Override
    public Observable<List<Song>> getQueue() {
        ensureServiceStarted();
        return mQueue.getObservable();
    }

    @Override
    public Observable<Integer> getQueuePosition() {
        ensureServiceStarted();
        return mQueuePosition.getObservable();
    }

    @Override
    public Observable<Integer> getCurrentPosition() {
        ensureServiceStarted();
        startCurrentPositionClock();
        return mCurrentPosition.getObservable();
    }

    @Override
    public Observable<Integer> getDuration() {
        ensureServiceStarted();
        return mDuration.getObservable();
    }

    @Override
    public Observable<Boolean> isShuffleEnabled() {
        ensureServiceStarted();
        return mShuffled.asObservable().distinctUntilChanged();
    }

    @Override
    public Observable<Integer> getRepeatMode() {
        ensureServiceStarted();
        return mMultiRepeatCount.getObservable()
                .flatMap(multiRepeatCount -> {
                    if (multiRepeatCount > 1) {
                        return Observable.just(multiRepeatCount);
                    } else {
                        return mRepeatMode.asObservable();
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
        ensureServiceStarted();
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
            mArtwork = BehaviorSubject.create();

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

    private static final class Prop<T> {

        private final String mName;
        private final T mNullValue;
        private final BehaviorSubject<Optional<T>> mSubject;
        private final Observable<T> mObservable;

        private Retriever<T> mRetriever;

        public Prop(String propertyName) {
            this(propertyName, null);
        }

        public Prop(String propertyName, T nullValue) {
            mName = propertyName;
            mNullValue = nullValue;
            mSubject = BehaviorSubject.create();

            mObservable = mSubject.filter(Optional::isPresent)
                    .map(Optional::getValue)
                    .distinctUntilChanged();
        }

        public void setFunction(Retriever<T> retriever) {
            mRetriever = retriever;
        }

        public void invalidate() {
            mSubject.onNext(Optional.empty());

            if (mRetriever != null) {
                Observable.fromCallable(mRetriever::retrieve)
                        .subscribeOn(Schedulers.computation())
                        .map(data -> (data == null) ? mNullValue : data)
                        .map(Optional::ofNullable)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mSubject::onNext, throwable -> {
                            Timber.e(throwable, "Failed to fetch " + mName + " property.");
                        });
            }
        }

        public boolean isSubscribedTo() {
            return mSubject.hasObservers();
        }

        public void setValue(T value) {
            mSubject.onNext(Optional.ofNullable(value));
        }

        public boolean hasValue() {
            return mSubject.getValue() != null && mSubject.getValue().isPresent();
        }

        public T lastValue() {
            return mSubject.getValue().getValue();
        }

        public Observable<T> getObservable() {
            return mObservable;
        }

        interface Retriever<T> {
            T retrieve() throws Exception;
        }

    }
}
