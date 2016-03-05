// IPlayerService.aidl
package com.marverenic.music;

import com.marverenic.music.instances.Song;

interface IPlayerService {

    void stop();
    void skip();
    void previous();
    void begin();
    void togglePlay();
    void play();
    void pause();
    void setShuffle(boolean shuffle);
    void setRepeat(int repeat);
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
    int getAudioSessionId();

}
