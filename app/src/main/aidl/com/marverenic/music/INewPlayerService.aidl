// IPlayerService.aidl
package com.marverenic.music;

import android.graphics.Bitmap;
import com.marverenic.music.instances.Song;

interface INewPlayerService {
    Song getNowPlaying();
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
