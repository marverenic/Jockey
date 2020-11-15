package com.marverenic.music.data.annotations;

import androidx.annotation.IntDef;

@IntDef(value = {StartPage.PLAYLISTS, StartPage.SONGS, StartPage.ARTISTS, StartPage.ALBUMS,
        StartPage.GENRES, StartPage.BROWSER, StartPage.RECENTLY_ADDED})
public @interface StartPage {
    int PLAYLISTS = 0;
    int SONGS = 1;
    int ARTISTS = 2;
    int ALBUMS = 3;
    int GENRES = 4;
    int BROWSER = 5;
    int RECENTLY_ADDED = 6;
}
