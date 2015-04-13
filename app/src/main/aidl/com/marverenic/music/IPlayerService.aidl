// IPlayerService.aidl
package com.marverenic.music;

import android.graphics.Bitmap;
import com.marverenic.music.instances.Song;

interface IPlayerService {

    void setQueue(in List<Song> newQueue, int newPosition);
    void changeQueue(in List<Song> newQueue, int newPosition);
    void queueNext(in Song song);
    void queueLast(in Song song);
    void queueNextList(in List<Song> songs);
    void queueLastList(in List<Song> songs);

    void begin();
    Song getNowPlaying();
    void togglePlay();
    void play();
    void pause();
    void stop();
    void previous();
    void skip();
    void seek(int position);
    void changeSong(int newPosition);
    void toggleShuffle();
    void toggleRepeat();
    Bitmap getArt();
    Bitmap getFullArt();
    boolean isPlaying();
    boolean isPreparing();
    int getCurrentPosition();
    int getDuration();
    boolean isShuffle();
    boolean isRepeat();
    boolean isRepeatOne();
    List<Song> getQueue();
    int getPosition();
}
