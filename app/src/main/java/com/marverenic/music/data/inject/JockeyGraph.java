package com.marverenic.music.data.inject;

import com.marverenic.music.ui.about.AboutActivity;
import com.marverenic.music.ui.BaseActivity;
import com.marverenic.music.ui.library.LibraryActivity;
import com.marverenic.music.ui.search.SearchActivity;
import com.marverenic.music.ui.library.album.AlbumActivity;
import com.marverenic.music.ui.library.artist.ArtistActivity;
import com.marverenic.music.ui.library.playlist.AutoPlaylistActivity;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.ui.library.genre.GenreActivity;
import com.marverenic.music.ui.library.playlist.PlaylistActivity;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.common.playlist.PlaylistCollisionDialogFragment;
import com.marverenic.music.ui.library.AlbumFragment;
import com.marverenic.music.ui.library.ArtistFragment;
import com.marverenic.music.ui.settings.DirectoryListFragment;
import com.marverenic.music.ui.settings.EqualizerFragment;
import com.marverenic.music.ui.nowplaying.GenreFragment;
import com.marverenic.music.ui.nowplaying.MiniplayerFragment;
import com.marverenic.music.ui.nowplaying.NowPlayingFragment;
import com.marverenic.music.ui.library.PlaylistFragment;
import com.marverenic.music.ui.settings.PreferenceFragment;
import com.marverenic.music.ui.nowplaying.QueueFragment;
import com.marverenic.music.ui.library.SongFragment;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.ServicePlayerController;
import com.marverenic.music.ui.library.AlbumViewModel;
import com.marverenic.music.ui.library.ArtistViewModel;
import com.marverenic.music.ui.BaseLibraryActivityViewModel;
import com.marverenic.music.ui.library.GenreViewModel;
import com.marverenic.music.ui.nowplaying.MiniplayerViewModel;
import com.marverenic.music.ui.nowplaying.NowPlayingArtworkViewModel;
import com.marverenic.music.ui.nowplaying.NowPlayingControllerViewModel;
import com.marverenic.music.ui.library.PlaylistViewModel;
import com.marverenic.music.ui.library.playlist.edit.RuleHeaderViewModel;
import com.marverenic.music.ui.library.playlist.edit.RuleViewModel;
import com.marverenic.music.ui.library.SongViewModel;
import com.marverenic.music.widget.BaseWidget;

public interface JockeyGraph {

    void injectBaseActivity(BaseActivity baseActivity);

    void inject(LibraryActivity activity);
    void inject(SearchActivity activity);
    void inject(AboutActivity activity);
    void inject(AlbumActivity activity);
    void inject(ArtistActivity activity);
    void inject(GenreActivity activity);
    void inject(PlaylistActivity activity);
    void inject(AutoPlaylistActivity activity);
    void inject(AutoPlaylistEditActivity activity);

    void inject(BaseWidget widget);

    void inject(SongFragment fragment);
    void inject(AlbumFragment fragment);
    void inject(ArtistFragment fragment);
    void inject(PlaylistFragment fragment);
    void inject(GenreFragment fragment);
    void inject(NowPlayingFragment fragment);
    void inject(QueueFragment fragment);
    void inject(EqualizerFragment fragment);
    void inject(PreferenceFragment fragment);
    void inject(DirectoryListFragment fragment);
    void inject(MiniplayerFragment fragment);

    void inject(CreatePlaylistDialogFragment dialogFragment);
    void inject(AppendPlaylistDialogFragment dialogFragment);
    void inject(PlaylistCollisionDialogFragment dialogFragment);

    void inject(BaseLibraryActivityViewModel viewModel);
    void inject(MiniplayerViewModel viewModel);
    void inject(NowPlayingControllerViewModel viewModel);
    void inject(NowPlayingArtworkViewModel viewModel);
    void inject(SongViewModel viewModel);
    void inject(AlbumViewModel viewModel);
    void inject(ArtistViewModel viewModel);
    void inject(GenreViewModel viewModel);
    void inject(PlaylistViewModel viewModel);
    void inject(RuleHeaderViewModel viewModel);
    void inject(RuleViewModel viewModel);

    void inject(ShuffleAllSection section);

    void inject(LibraryEmptyState emptyState);

    void inject(ServicePlayerController.Listener broadcastReceiver);
    void inject(MusicPlayer musicPlayer);

}
