package com.marverenic.music.ui.library;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.ui.nowplaying.GenreFragment;
import com.marverenic.music.view.FABMenu;

class LibraryPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

    private Context mContext;

    private PlaylistFragment playlistFragment;
    private SongFragment songFragment;
    private ArtistFragment artistFragment;
    private AlbumFragment albumFragment;
    private GenreFragment genreFragment;

    private FABMenu fab;

    public LibraryPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    public void setFloatingActionButton(FABMenu view) {
        fab = view;
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

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        // Hide the fab when outside of the Playlist fragment

        // Don't show the FAB if we can't write to the library
        if (!MediaStoreUtil.hasPermission(mContext)) return;

        // If the fab isn't supposed to change states, don't animate anything
        if (position != 0 && fab.getVisibility() == View.GONE) return;

        if (position == 0) {
            fab.show();
        } else {
            fab.hide();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
