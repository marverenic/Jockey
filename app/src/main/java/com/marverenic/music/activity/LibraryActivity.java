package com.marverenic.music.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferencesStore;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Updater;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.FABMenu;

import javax.inject.Inject;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public class LibraryActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "LibraryActivity";
    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";

    @Inject MusicStore mMusicStore;
    @Inject PreferencesStore mPrefStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        JockeyApplication.getComponent(this).inject(this);

        // Setup the FAB
        FABMenu fab = (FABMenu) findViewById(R.id.fab);
        fab.addChild(R.drawable.ic_add_24dp, this, R.string.playlist);
        fab.addChild(R.drawable.ic_add_24dp, this, R.string.playlist_auto);

        ViewPager pager = (ViewPager) findViewById(R.id.pager);

        int page = mPrefStore.getDefaultPage();
        if (page != 0 || !hasRwPermission()) {
            fab.setVisibility(View.GONE);
        }

        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        adapter.setFloatingActionButton(fab);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        ((TabLayout) findViewById(R.id.tabs)).setupWithViewPager(pager);

        pager.setCurrentItem(page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        new Updater(this, findViewById(R.id.coordinator_layout)).execute();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasRwPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Util.hasPermissions(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_library, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Navigate.to(this, SettingsActivity.class);
                return true;
            case R.id.action_refresh_library:
                refreshLibrary();
                return true;
            case R.id.search:
                Navigate.to(this, SearchActivity.class);
                return true;
            case R.id.action_about:
                Navigate.to(this, AboutActivity.class);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshLibrary() {
        mMusicStore.refresh().subscribe(
                hasPermission -> {
                    if (hasPermission) {
                        View view = findViewById(R.id.list);
                        Snackbar.make(view, R.string.confirm_refresh_library, LENGTH_SHORT).show();
                    } else {
                        showPermissionSnackbar();
                    }
                },
                throwable -> {
                    Log.e(TAG, "Failed to refresh library", throwable);
                });
    }

    private void showPermissionSnackbar() {
        View view = findViewById(R.id.list);
        Snackbar.make(view, R.string.message_refresh_library_no_permission, LENGTH_LONG)
                .setAction(R.string.action_open_settings,
                        v -> {
                            Intent intent = new Intent();
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);

                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                .show();
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            if (v.getTag().equals("fab-" + getString(R.string.playlist))) {
                CreatePlaylistDialogFragment.newInstance()
                        .showSnackbarIn(R.id.list)
                        .show(getSupportFragmentManager(), TAG_MAKE_PLAYLIST);
            } else if (v.getTag().equals("fab-" + getString(R.string.playlist_auto))) {
                startActivity(AutoPlaylistEditActivity.newIntent(this));
            }
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {

        private PlaylistFragment playlistFragment;
        private SongFragment songFragment;
        private ArtistFragment artistFragment;
        private AlbumFragment albumFragment;
        private GenreFragment genreFragment;

        private FABMenu fab;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
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
                    return getResources().getString(R.string.header_playlists);
                case 1:
                    return getResources().getString(R.string.header_songs);
                case 2:
                    return getResources().getString(R.string.header_artists);
                case 3:
                    return getResources().getString(R.string.header_albums);
                case 4:
                    return getResources().getString(R.string.header_genres);
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
            if (!hasRwPermission()) return;

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
}
