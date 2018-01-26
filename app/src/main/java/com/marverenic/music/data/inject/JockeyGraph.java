package com.marverenic.music.data.inject;

import com.marverenic.music.player.MusicPlayer;
import com.marverenic.music.player.ServicePlayerController;
import com.marverenic.music.ui.BaseActivity;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.about.AboutActivity;
import com.marverenic.music.ui.common.playlist.AppendPlaylistDialogFragment;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.common.playlist.PlaylistCollisionDialogFragment;
import com.marverenic.music.ui.library.album.AlbumListFragment;
import com.marverenic.music.ui.library.artist.ArtistListFragment;
import com.marverenic.music.ui.library.LibraryActivity;
import com.marverenic.music.ui.library.LibraryFragment;
import com.marverenic.music.ui.library.browse.MusicBrowserFragment;
import com.marverenic.music.ui.library.playlist.PlaylistListFragment;
import com.marverenic.music.ui.library.recentlyadded.RecentlyAddedFragment;
import com.marverenic.music.ui.library.song.SongFragment;
import com.marverenic.music.ui.library.album.contents.AlbumFragment;
import com.marverenic.music.ui.library.artist.contents.ArtistFragment;
import com.marverenic.music.ui.library.genre.contents.GenreFragment;
import com.marverenic.music.ui.library.playlist.contents.PlaylistFragment;
import com.marverenic.music.ui.library.playlist.contents.edit.AutoPlaylistEditFragment;
import com.marverenic.music.ui.library.genre.GenreListFragment;
import com.marverenic.music.ui.nowplaying.MiniplayerFragment;
import com.marverenic.music.ui.nowplaying.NowPlayingFragment;
import com.marverenic.music.ui.nowplaying.PlayerControllerFragment;
import com.marverenic.music.ui.nowplaying.QueueFragment;
import com.marverenic.music.ui.search.SearchActivity;
import com.marverenic.music.ui.search.SearchFragment;
import com.marverenic.music.ui.settings.DirectoryListFragment;
import com.marverenic.music.ui.settings.EqualizerFragment;
import com.marverenic.music.ui.settings.PreferenceFragment;
import com.marverenic.music.widget.BaseWidget;

public interface JockeyGraph {

    void injectBaseActivity(BaseActivity baseActivity);
    void injectBaseLibraryActivity(BaseLibraryActivity baseLibraryActivity);

    void inject(LibraryActivity activity);
    void inject(SearchActivity activity);
    void inject(AboutActivity activity);

    void inject(BaseWidget widget);

    void inject(LibraryFragment fragment);
    void inject(MusicBrowserFragment fragment);
    void inject(RecentlyAddedFragment fragment);
    void inject(SearchFragment fragment);
    void inject(SongFragment fragment);
    void inject(AlbumListFragment fragment);
    void inject(AlbumFragment fragment);
    void inject(ArtistListFragment fragment);
    void inject(PlaylistListFragment fragment);
    void inject(GenreListFragment fragment);
    void inject(NowPlayingFragment fragment);
    void inject(PlayerControllerFragment fragment);
    void inject(QueueFragment fragment);
    void inject(EqualizerFragment fragment);
    void inject(PreferenceFragment fragment);
    void inject(DirectoryListFragment fragment);
    void inject(ArtistFragment fragment);
    void inject(PlaylistFragment playlistFragment);
    void inject(GenreFragment fragment);
    void inject(AutoPlaylistEditFragment fragment);

    void inject(MiniplayerFragment fragment);

    void inject(CreatePlaylistDialogFragment dialogFragment);
    void inject(AppendPlaylistDialogFragment dialogFragment);
    void inject(PlaylistCollisionDialogFragment dialogFragment);

    void inject(ServicePlayerController.Listener broadcastReceiver);
    void inject(MusicPlayer musicPlayer);

}
