package com.marverenic.music.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;

import com.marverenic.music.R;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;

public class SearchPagerAdapter extends FragmentPagerAdapter {
    private final static int NUM_ITEMS = 5;
    private PlaylistFragment playlistFragment;
    private SongFragment songFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private GenreFragment genreFragment;
    private FragmentActivity activity;

    public static final String DATA_KEY = "DATA";

    ArrayList<Playlist> playlistResults;
    ArrayList<Song> songResults;
    ArrayList<Artist> artistResults;
    ArrayList<Album> albumResults;
    ArrayList<Genre> genreResults;

    public SearchPagerAdapter(FragmentActivity activity, ArrayList<Playlist> playlistResults,
                              ArrayList<Song> songResults, ArrayList<Artist> artistResults,
                              ArrayList<Album> albumResults, ArrayList<Genre> genreResults) {
        super(activity.getSupportFragmentManager());
        this.activity = activity;

        this.playlistResults = playlistResults;
        this.songResults = songResults;
        this.artistResults = artistResults;
        this.albumResults = albumResults;
        this.genreResults = genreResults;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (playlistFragment == null) {
                    playlistFragment = new PlaylistFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(DATA_KEY, playlistResults);
                    playlistFragment.setArguments(bundle);
                }
                return playlistFragment;
            case 1:
                if (songFragment == null) {
                    songFragment = new SongFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(DATA_KEY, songResults);
                    songFragment.setArguments(bundle);
                }
                return songFragment;
            case 2:
                if (artistFragment == null) {
                    artistFragment = new ArtistFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(DATA_KEY, artistResults);
                    artistFragment.setArguments(bundle);
                }
                return artistFragment;
            case 3:
                if (albumFragment == null) {
                    albumFragment = new AlbumFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(DATA_KEY, albumResults);
                    albumFragment.setArguments(bundle);
                }
                return albumFragment;
            case 4:
                if (genreFragment == null) {
                    genreFragment = new GenreFragment();

                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(DATA_KEY, genreResults);
                    genreFragment.setArguments(bundle);
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
}
