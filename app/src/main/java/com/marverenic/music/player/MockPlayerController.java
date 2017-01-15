package com.marverenic.music.player;

import android.graphics.Bitmap;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Observable;
import rx.Single;

public class MockPlayerController implements PlayerController {

    // TODO implement stubbed methods when a test requires interactions with a PlayerController

    @Override
    public Observable<String> getError() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<String> getInfo() {
        throw new UnsupportedOperationException("Stub!");
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
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void previous() {
        throw new UnsupportedOperationException("Stub!");
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
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void clearQueue() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public void changeSong(int newPosition) {
        throw new UnsupportedOperationException("Stub!");
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
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<List<Song>> getQueue() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Integer> getQueuePosition() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Integer> getCurrentPosition() {
        throw new UnsupportedOperationException("Stub!");
    }

    @Override
    public Observable<Integer> getDuration() {
        throw new UnsupportedOperationException("Stub!");
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
