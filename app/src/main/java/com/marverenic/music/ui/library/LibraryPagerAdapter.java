package com.marverenic.music.ui.library;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.marverenic.music.R;
import com.marverenic.music.ui.library.album.AlbumListFragment;
import com.marverenic.music.ui.library.artist.ArtistListFragment;
import com.marverenic.music.ui.library.genre.GenreListFragment;
import com.marverenic.music.ui.library.playlist.PlaylistListFragment;
import com.marverenic.music.ui.library.song.SongFragment;

class LibraryPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;

    private PlaylistListFragment playlistFragment;
    private SongFragment songFragment;
    private ArtistListFragment artistFragment;
    private AlbumListFragment albumFragment;
    private GenreListFragment genreFragment;

    public LibraryPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (playlistFragment == null) {
                    playlistFragment = new PlaylistListFragment();
                }
                return playlistFragment;
            case 1:
                if (songFragment == null) {
                    songFragment = new SongFragment();
                }
                return songFragment;
            case 2:
                if (artistFragment == null) {
                    artistFragment = new ArtistListFragment();
                }
                return artistFragment;
            case 3:
                if (albumFragment == null) {
                    albumFragment = new AlbumListFragment();
                }
                return albumFragment;
            case 4:
                if (genreFragment == null) {
                    genreFragment = new GenreListFragment();
                }
                return genreFragment;
        }
        return new Fragment();
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mContext.getResources().getString(R.string.header_playlists);
            case 1:
                return mContext.getResources().getString(R.string.header_songs);
            case 2:
                return mContext.getResources().getString(R.string.header_artists);
            case 3:
                return mContext.getResources().getString(R.string.header_albums);
            case 4:
                return mContext.getResources().getString(R.string.header_genres);
            default:
                return "Page " + position;
        }
    }

}
