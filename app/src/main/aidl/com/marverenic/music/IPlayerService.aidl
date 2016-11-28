// IPlayerService.aidl
package com.marverenic.music;

import com.marverenic.music.instances.Song;
import com.marverenic.music.data.store.ImmutablePreferenceStore;
import com.marverenic.music.player.RemoteEqualizer;

interface IPlayerService {

    void stop();
    void skip();
    void previous();
    void togglePlay();
    void play();
    void pause();
    void setPreferences(in ImmutablePreferenceStore preferences);
    void setQueue(in List<Song> newQueue, int newPosition);
    void changeSong(int position);
    void editQueue(in List<Song> newQueue, int newPosition);
    void queueNext(in Song song);
    void queueNextList(in List<Song> songs);
    void queueLast(in Song song);
    void queueLastList(in List<Song> songs);
    void seekTo(int position);

    boolean isPlaying();
    Song getNowPlaying();
    List<Song> getQueue();
    int getQueuePosition();
    int getQueueSize();
    int getCurrentPosition();
    int getDuration();

    int getMultiRepeatCount();
    void setMultiRepeatCount(int count);

    long getSleepTimerEndTime();
    void setSleepTimerEndTime(long timestampInMillis);

}
