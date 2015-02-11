package com.marverenic.music.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;

import com.marverenic.music.R;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;

public class LibraryPagerAdapter extends FragmentPagerAdapter {
    private final static int NUM_ITEMS = 5;
    private PlaylistFragment playlistFragment;
    private SongFragment songFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private GenreFragment genreFragment;
    private FragmentActivity activity;

    public LibraryPagerAdapter(FragmentActivity activity) {
        super(activity.getSupportFragmentManager());
        this.activity = activity;
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
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return activity.getResources().getString(R.string.header_playlists);
            case 1:
                return activity.getResources().getString(R.string.header_songs);
            case 2:
                return activity.getResources().getString(R.string.header_artists);
            case 3:
                return activity.getResources().getString(R.string.header_albums);
            case 4:
                return activity.getResources().getString(R.string.header_genres);
            default:
                return "Page " + position;
        }
    }

    public void refreshLibrary() {
        playlistFragment = null;
        songFragment = null;
        artistFragment = null;
        albumFragment = null;
        genreFragment = null;
    }
}
