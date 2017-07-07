package com.marverenic.music.ui.library;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.marverenic.music.R;
import com.marverenic.music.ui.nowplaying.GenreFragment;

class LibraryPagerAdapter extends FragmentPagerAdapter {

    private Context mContext;

    private PlaylistFragment playlistFragment;
    private SongFragment songFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private GenreFragment genreFragment;

    public LibraryPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (playlistFragment == null) {
                    playlistFragment = new PlaylistFragment();
                }
                return playlistFragment;
            case 1:
                if (songFragment == null) {
                    songFragment = new SongFragment();
                }
                return songFragment;
            case 2:
                if (artistFragment == null) {
                    artistFragment = new ArtistFragment();
                }
                return artistFragment;
            case 3:
                if (albumFragment == null) {
                    albumFragment = new AlbumFragment();
                }
                return albumFragment;
            case 4:
                if (genreFragment == null) {
                    genreFragment = new GenreFragment();
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
