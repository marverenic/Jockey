package com.marverenic.music.player.browser;

import com.marverenic.music.model.Song;

import java.util.List;

public class MediaList {

    public final List<Song> songs;
    public final int startIndex;
    public final boolean enableShuffle;
    public final boolean keepCurrentQueue;

    MediaList(List<Song> songs, int startingPosition, boolean enableShuffle) {
        this.songs = songs;
        this.startIndex = startingPosition;
        this.enableShuffle = enableShuffle;
        keepCurrentQueue = false;
    }

    MediaList(int startIndex, boolean enableShuffle) {
        this.songs = null;
        this.startIndex = startIndex;
        this.enableShuffle = enableShuffle;
        this.keepCurrentQueue = true;
    }

}
