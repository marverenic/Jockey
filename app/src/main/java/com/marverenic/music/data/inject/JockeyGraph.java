package com.marverenic.music.data.inject;

import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.SongFragment;

public interface JockeyGraph {

    void inject(SongFragment fragment);
    void inject(AlbumFragment fragment);

}
