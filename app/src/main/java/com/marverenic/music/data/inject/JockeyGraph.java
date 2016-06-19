package com.marverenic.music.data.inject;

import com.marverenic.music.activity.LibraryActivity;
import com.marverenic.music.activity.SearchActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.activity.instance.AutoPlaylistActivity;
import com.marverenic.music.activity.instance.GenreActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.viewmodel.AlbumViewModel;
import com.marverenic.music.viewmodel.ArtistViewModel;
import com.marverenic.music.viewmodel.GenreViewModel;
import com.marverenic.music.viewmodel.NowPlayingControllerViewModel;
import com.marverenic.music.viewmodel.PlaylistViewModel;
import com.marverenic.music.viewmodel.SongViewModel;

public interface JockeyGraph {

    void inject(LibraryActivity activity);
    void inject(SearchActivity activity);
    void inject(AlbumActivity activity);
    void inject(ArtistActivity activity);
    void inject(GenreActivity activity);
    void inject(PlaylistActivity activity);
    void inject(AutoPlaylistActivity activity);

    void inject(SongFragment fragment);
    void inject(AlbumFragment fragment);
    void inject(ArtistFragment fragment);
    void inject(PlaylistFragment fragment);
    void inject(GenreFragment fragment);

    void inject(CreatePlaylistDialogFragment dialogFragment);
    void inject(AppendPlaylistDialogFragment dialogFragment);

    void inject(NowPlayingControllerViewModel viewModel);
    void inject(SongViewModel viewModel);
    void inject(AlbumViewModel viewModel);
    void inject(ArtistViewModel viewModel);
    void inject(GenreViewModel viewModel);
    void inject(PlaylistViewModel viewModel);

}
