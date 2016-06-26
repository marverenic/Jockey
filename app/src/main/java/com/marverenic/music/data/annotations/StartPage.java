package com.marverenic.music.data.annotations;

import android.support.annotation.IntDef;

@IntDef(value = {StartPage.PLAYLISTS, StartPage.SONGS, StartPage.ARTISTS, StartPage.ALBUMS,
        StartPage.GENRES})
public @interface StartPage {
    int PLAYLISTS = 0;
    int SONGS = 1;
    int ARTISTS = 2;
    int ALBUMS = 3;
    int GENRES = 4;
}
