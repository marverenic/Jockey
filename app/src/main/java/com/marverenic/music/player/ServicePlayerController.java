package com.marverenic.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;

import com.marverenic.music.IPlayerService;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class ServicePlayerController implements PlayerController {

    private Context mContext;
    private IPlayerService mBinding;

    private PublishSubject<String> mErrorStream = PublishSubject.create();
    private PublishSubject<String> mInfoStream = PublishSubject.create();

    private final Prop<Boolean> mPlaying = new Prop<>("playing");
    private final Prop<Song> mNowPlaying = new Prop<>("now playing");
    private final Prop<List<Song>> mQueue = new Prop<>("queue");
    private final Prop<Integer> mQueuePosition = new Prop<>("queue index");
    private final Prop<Integer> mCurrentPosition = new Prop<>("seek position");
    private final Prop<Integer> mDuration = new Prop<>("duration");
    private final Prop<Integer> mMultiRepeatCount = new Prop<>("multi-repeat");
    private final Prop<Long> mSleepTimerEndTime = new Prop<>("sleep timer");

    private BehaviorSubject<Bitmap> mArtwork;

    public ServicePlayerController(Context context) {
        mContext = context;
        startService();
    }

    private void startService() {
        Intent serviceIntent = new Intent(mContext, PlayerService.class);

        // Manually start the service to ensure that it is associated with this task and can
        // appropriately set its dismiss behavior
        mContext.startService(serviceIntent);

        mContext.bindService(serviceIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mBinding = IPlayerService.Stub.asInterface(service);
                initAllProperties();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mBinding = null;
            }
        }, Context.BIND_WAIVE_PRIORITY);
    }

    private void initAllProperties() {
        mPlaying.setFunction(mBinding::isPlaying);
        mNowPlaying.setFunction(mBinding::getNowPlaying);
        mQueue.setFunction(mBinding::getQueue);
        mQueuePosition.setFunction(mBinding::getQueuePosition);
        mCurrentPosition.setFunction(mBinding::getCurrentPosition);
        mDuration.setFunction(mBinding::getDuration);
        mMultiRepeatCount.setFunction(mBinding::getMultiRepeatCount);
        mSleepTimerEndTime.setFunction(mBinding::getSleepTimerEndTime);

        invalidateAll();
    }

    private void invalidateAll() {
        mPlaying.invalidate();
        mNowPlaying.invalidate();
        mQueue.invalidate();
        mQueuePosition.invalidate();
        mCurrentPosition.invalidate();
        mDuration.invalidate();
        mMultiRepeatCount.invalidate();
        mSleepTimerEndTime.invalidate();
    }

    private void invalidateSong() {
        mNowPlaying.invalidate();
        mCurrentPosition.invalidate();
        mDuration.invalidate();
        if (mMultiRepeatCount.getValue(0) > 0) mMultiRepeatCount.invalidate();
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
    public Observable<PlayerState> getPlayerState() {
        return isPlaying()
                .map(isPlaying -> {
                    PlayerState.Builder builder = new PlayerState.Builder();
                    builder.setPlaying(isPlaying);
                    return builder;
                })
                .concatMap(builder -> getQueue().map(builder::setQueue))
                .concatMap(builder -> getQueuePosition().map(builder::setQueuePosition))
                .concatMap(builder -> getCurrentPosition().map(builder::setSeekPosition))
                .map(PlayerState.Builder::build);
    }

    @Override
    public void restorePlayerState(PlayerState restoreState) {
        editQueue(restoreState.getQueue(), restoreState.getQueuePosition());
        seek(restoreState.getSeekPosition());

        if (restoreState.isPlaying()) play();
    }

    @Override
    public void stop() {
        // TODO post to service
        mPlaying.setValue(false);
    }

    @Override
    public void skip() {
        if (mBinding != null) {
            try {
                mBinding.skip();
                invalidateSong();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip current track");
            }
        }
    }

    @Override
    public void previous() {
        if (mBinding != null) {
            try {
                mBinding.previous();
                invalidateSong();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip backward");
            }
        }
    }

    @Override
    public void togglePlay() {
        if (mBinding != null) {
            try {
                mBinding.togglePlay();
                mPlaying.setValue(!mPlaying.getValue(false));
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to toggle playback");
            }
        }
    }

    @Override
    public void play() {
        // TODO post to service
        mPlaying.setValue(true);
    }

    @Override
    public void pause() {
        // TODO post to service
        mPlaying.setValue(false);
    }

    @Override
    public void updatePlayerPreferences(ReadOnlyPreferenceStore preferenceStore) {
        // TODO post to service
    }

    @Override
    public void setQueue(List<Song> newQueue, int newPosition) {
        // TODO post to service
        mQueue.setValue(Collections.unmodifiableList(new ArrayList<>(newQueue)));
    }

    @Override
    public void clearQueue() {
        // TODO post to service
        mQueue.setValue(Collections.emptyList());
        mQueuePosition.setValue(0);
    }

    @Override
    public void changeSong(int newPosition) {
        if (mBinding != null) {
            try {
                mBinding.changeSong(newPosition);
                invalidateSong();
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to skip current track");
            }
        }
    }

    @Override
    public void editQueue(List<Song> queue, int newPosition) {
        // TODO post to service

    }

    @Override
    public void queueNext(Song song) {
        // TODO post to service
    }

    @Override
    public void queueNext(List<Song> songs) {
        // TODO post to service
    }

    @Override
    public void queueLast(Song song) {
        // TODO post to service
    }

    @Override
    public void queueLast(List<Song> songs) {
        // TODO post to service
    }

    @Override
    public void seek(int position) {
        if (mBinding != null) {
            try {
                mBinding.seekTo(position);
                mCurrentPosition.setValue(position);
            } catch (RemoteException exception) {
                Timber.e(exception, "Failed to seek");
            }
        }
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
        return mCurrentPosition.getObservable();
    }

    @Override
    public Observable<Integer> getDuration() {
        return mDuration.getObservable();
    }

    @Override
    public Observable<Integer> getMultiRepeatCount() {
        return mMultiRepeatCount.getObservable();
    }

    @Override
    public void setMultiRepeatCount(int count) {
        // TODO post to remote service
        mMultiRepeatCount.setValue(count);
    }

    @Override
    public Observable<Long> getSleepTimerEndTime() {
        return mSleepTimerEndTime.getObservable();
    }

    @Override
    public void setSleepTimerEndTime(long timestampInMillis) {
        // TODO post to remote service
        mSleepTimerEndTime.setValue(timestampInMillis);
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
                    .map((Song song) -> Util.fetchFullArt(mContext, song))
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
            if (mController == null) {
                JockeyApplication.getComponent(context).inject(this);
            }

            if (mController instanceof ServicePlayerController) {
                ServicePlayerController playerController = (ServicePlayerController) mController;

                if (intent.getAction().equals(MusicPlayer.UPDATE_BROADCAST)) {
                    playerController.invalidateAll();
                } else if (intent.getAction().equals(MusicPlayer.INFO_BROADCAST)) {
                    String error = intent.getExtras().getString(MusicPlayer.INFO_EXTRA_MESSAGE);
                    playerController.mErrorStream.onNext(error);

                } else if (intent.getAction().equals(MusicPlayer.ERROR_BROADCAST)) {
                    String info = intent.getExtras().getString(MusicPlayer.ERROR_EXTRA_MSG);
                    playerController.mInfoStream.onNext(info);
                }
            }
        }

    }

    private static final class Prop<T> {

        private final String mName;
        private final BehaviorSubject<T> mSubject;

        private Retriever<T> mRetriever;

        public Prop(String propertyName) {
            mName = propertyName;
            mSubject = BehaviorSubject.create();
        }

        public void setFunction(Retriever<T> retriever) {
            mRetriever = retriever;
        }

        public void invalidate() {
            if (mRetriever != null) {
                Observable.fromCallable(mRetriever::retrieve)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mSubject::onNext, (Throwable t) -> {
                            Timber.e(t, "Failed to fetch " + mName + " property.");
                        });
            }
        }

        public void setValue(T value) {
            mSubject.onNext(value);
        }

        public T getValue(T defaultValue) {
            T value = mSubject.getValue();

            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        }

        public Observable<T> getObservable() {
            return mSubject.asObservable();
        }

        interface Retriever<T> {
            T retrieve() throws Exception;
        }

    }
}
