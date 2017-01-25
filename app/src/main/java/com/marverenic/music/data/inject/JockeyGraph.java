package com.marverenic.music.data.inject;

import com.marverenic.music.activity.AboutActivity;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.activity.MainActivity;
import com.marverenic.music.activity.NowPlayingActivity;
import com.marverenic.music.activity.SearchActivity;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.activity.instance.ArtistActivity;
import com.marverenic.music.activity.instance.AutoPlaylistActivity;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.activity.instance.GenreActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.dialog.AppendPlaylistDialogFragment;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.dialog.PlaylistCollisionDialogFragment;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.DirectoryListFragment;
import com.marverenic.music.fragments.EqualizerFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.MiniplayerFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.PreferenceFragment;
import com.marverenic.music.fragments.QueueFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.ServicePlayerController;
import com.marverenic.music.viewmodel.AlbumViewModel;
import com.marverenic.music.viewmodel.ArtistViewModel;
import com.marverenic.music.viewmodel.GenreViewModel;
import com.marverenic.music.viewmodel.MiniplayerViewModel;
import com.marverenic.music.viewmodel.NowPlayingArtworkViewModel;
import com.marverenic.music.viewmodel.NowPlayingControllerViewModel;
import com.marverenic.music.viewmodel.PlaylistViewModel;
import com.marverenic.music.viewmodel.RuleHeaderViewModel;
import com.marverenic.music.viewmodel.RuleViewModel;
import com.marverenic.music.viewmodel.SongViewModel;
import com.marverenic.music.widget.BaseWidget;

public interface JockeyGraph {

    void injectBaseActivity(BaseActivity baseActivity);

    void inject(MainActivity activity);
    void inject(SearchActivity activity);
    void inject(AboutActivity activity);
    void inject(NowPlayingActivity activity);
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
    void inject(QueueFragment fragment);
    void inject(EqualizerFragment fragment);
    void inject(PreferenceFragment fragment);
    void inject(DirectoryListFragment fragment);
    void inject(MiniplayerFragment fragment);

    void inject(CreatePlaylistDialogFragment dialogFragment);
    void inject(AppendPlaylistDialogFragment dialogFragment);
    void inject(PlaylistCollisionDialogFragment dialogFragment);

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

    void inject(LibraryEmptyState emptyState);

    void inject(ServicePlayerController.Listener broadcastReceiver);
    void inject(MusicPlayer musicPlayer);

}
