package com.marverenic.music.player;

import android.graphics.Bitmap;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.subjects.BehaviorSubject;

public class MockPlayerController implements PlayerController {

    // TODO implement stubbed methods when a test requires interactions with a PlayerController
    private BehaviorSubject<List<Song>> mQueue;
    private BehaviorSubject<Integer> mQueueIndex;

    public MockPlayerController() {
        mQueue = BehaviorSubject.create(Collections.emptyList());
        mQueueIndex = BehaviorSubject.create(0);
    }

    @Override
    public Observable<String> getError() {
        return Observable.never();
    }

    @Override
    public Observable<String> getInfo() {
        return Observable.never();
    }

    @Override
    public Single<PlayerState> getPlayerState() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void restorePlayerState(PlayerState restoreState) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void skip() {
        mQueueIndex.onNext(mQueueIndex.getValue() + 1);
    }

    @Override
    public void previous() {
        mQueueIndex.onNext(mQueueIndex.getValue() - 1);
    }

    @Override
    public void togglePlay() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void play() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void updatePlayerPreferences(ReadOnlyPreferenceStore preferenceStore) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void setQueue(List<Song> newQueue, int newPosition) {
        mQueue.onNext(newQueue);
        mQueueIndex.onNext(newPosition);
    }

    @Override
    public void clearQueue() {
        setQueue(Collections.emptyList(), 0);
    }

    @Override
    public void changeSong(int newPosition) {
        mQueueIndex.onNext(newPosition);
    }

    @Override
    public void editQueue(List<Song> queue, int newPosition) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void queueNext(Song song) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void queueNext(List<Song> songs) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void queueLast(Song song) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void queueLast(List<Song> songs) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void seek(int position) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Boolean> isPlaying() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Song> getNowPlaying() {
        return Observable.combineLatest(mQueue, mQueueIndex, (queue, index) -> {
            if (index >= queue.size()) {
                return null;
            } else {
                return queue.get(index);
            }
        });
    }

    @Override
    public Observable<List<Song>> getQueue() {
        return mQueue;
    }

    @Override
    public Observable<Integer> getQueuePosition() {
        return mQueueIndex;
    }

    @Override
    public Observable<Integer> getCurrentPosition() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Integer> getDuration() {
        return getNowPlaying().map(Song::getSongDuration).cast(Integer.class);
    }

    @Override
    public Observable<Boolean> isShuffleEnabled() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Integer> getMultiRepeatCount() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void setMultiRepeatCount(int count) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Long> getSleepTimerEndTime() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void setSleepTimerEndTime(long timestampInMillis) {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void disableSleepTimer() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Bitmap> getArtwork() {
        throw new UnsupportedOperationException("Stub!");
    }
}
