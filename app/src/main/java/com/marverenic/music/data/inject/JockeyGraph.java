package com.marverenic.music.data.inject;

import com.marverenic.music.activity.LibraryActivity;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;

public interface JockeyGraph {

    void inject(LibraryActivity activity);

    void inject(SongFragment fragment);
    void inject(AlbumFragment fragment);
    void inject(ArtistFragment fragment);
    void inject(PlaylistFragment fragment);
    void inject(GenreFragment fragment);

}
