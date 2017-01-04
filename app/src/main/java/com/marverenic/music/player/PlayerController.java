package com.marverenic.music.player;

import android.graphics.Bitmap;

import com.marverenic.music.data.store.ReadOnlyPreferenceStore;
import com.marverenic.music.model.Song;

import java.util.List;

import rx.Observable;

public interface PlayerController {

    Observable<String> getError();
    Observable<String> getInfo();

    void stop();
    void skip();
    void previous();
    void togglePlay();
    void play();
    void pause();

    void updatePlayerPreferences(ReadOnlyPreferenceStore preferenceStore);

    void setQueue(List<Song> newQueue, int newPosition);
    void clearQueue();
    void changeSong(int newPosition);
    void editQueue(List<Song> queue, int newPosition);

    void queueNext(Song song);
    void queueNext(List<Song> songs);

    void queueLast(Song song);
    void queueLast(List<Song> songs);

    void seek(int position);

    Observable<Boolean> isPlaying();
    Observable<Song> getNowPlaying();
    Observable<List<Song>> getQueue();
    Observable<Integer> getQueuePosition();

    Observable<Integer> getCurrentPosition();
    Observable<Integer> getDuration();

    Observable<Integer> getMultiRepeatCount();
    void setMultiRepeatCount(int count);

    Observable<Long> getSleepTimerEndTime();
    void setSleepTimerEndTime(long timestampInMillis);
    void disableSleepTimer();

    Observable<Bitmap> getArtwork();

}
