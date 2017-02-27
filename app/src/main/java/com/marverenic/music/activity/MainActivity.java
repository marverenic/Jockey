package com.marverenic.music.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.instance.AutoPlaylistEditActivity;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.dialog.CreatePlaylistDialogFragment;
import com.marverenic.music.fragments.AlbumFragment;
import com.marverenic.music.fragments.ArtistFragment;
import com.marverenic.music.fragments.GenreFragment;
import com.marverenic.music.fragments.PlaylistFragment;
import com.marverenic.music.fragments.SongFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.utils.UriUtils;
import com.marverenic.music.utils.Util;
import com.marverenic.music.view.FABMenu;
import com.trello.rxlifecycle.ActivityEvent;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.support.design.widget.Snackbar.LENGTH_SHORT;

public class MainActivity extends BaseLibraryActivity implements View.OnClickListener {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";

    @Inject ThemeStore mThemeStore;
    @Inject MusicStore mMusicStore;
    @Inject PlayerController mPlayerController;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PreferenceStore mPrefStore;

    private SwipeRefreshLayout mRefreshLayout;

    public static Intent newNowPlayingIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JockeyApplication.getComponent(this).inject(this);
        onNewIntent(getIntent());

        initRefreshLayout();
        mMusicStore.loadAll();
        mPlaylistStore.loadPlaylists();

        // Setup the FAB
        FABMenu fab = (FABMenu) findViewById(R.id.fab);
        fab.addChild(R.drawable.ic_add_24dp, this, R.string.playlist);
        fab.addChild(R.drawable.ic_add_24dp, this, R.string.playlist_auto);

        ViewPager pager = (ViewPager) findViewById(R.id.library_pager);

        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.library_app_bar_layout);
        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) ->
                pager.setPadding(pager.getPaddingLeft(),
                        pager.getPaddingTop(),
                        pager.getPaddingRight(),
                        layout.getTotalScrollRange() + verticalOffset));

        int page = mPrefStore.getDefaultPage();
        if (page != 0 || !hasRwPermission()) {
            fab.setVisibility(View.GONE);
        }

        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        adapter.setFloatingActionButton(fab);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        ((TabLayout) findViewById(R.id.library_tabs)).setupWithViewPager(pager);

        pager.setCurrentItem(page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.activity_library;
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Handle incoming requests to play media from other applications
        if (intent.getData() == null) return;

        // If this intent is a music intent, process it
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri songUri = intent.getData();
            String songName = UriUtils.getDisplayName(this, songUri);

            List<Song> queue = buildQueueFromFileUri(songUri);
            int position;

            if (queue == null || queue.isEmpty()) {
                queue = buildQueueFromUri(intent.getData());
                position = findStartingPositionInQueue(songUri, queue);
            } else {
                String path = UriUtils.getPathFromUri(this, songUri);
                //noinspection ConstantConditions This won't be null, because we found data from it
                Uri fileUri = Uri.fromFile(new File(path));
                position = findStartingPositionInQueue(fileUri, queue);
            }

            if (queue.isEmpty()) {
                showSnackbar(getString(R.string.message_play_error_not_found, songName));
            } else {
                startIntentQueue(queue, position);
            }
        }

        // Don't try to process this intent again
        setIntent(new Intent(this, this.getClass()));
    }

    private List<Song> buildQueueFromFileUri(Uri fileUri) {
        // URI is not a file URI
        String path = UriUtils.getPathFromUri(this, fileUri);
        if (path == null || path.trim().isEmpty()) {
            return Collections.emptyList();
        }

        File file = new File(path);
        String mimeType = getContentResolver().getType(fileUri);
        return MediaStoreUtil.buildSongListFromFile(this, file, mimeType);
    }

    private List<Song> buildQueueFromUri(Uri uri) {
        return Collections.singletonList(Song.fromUri(this, uri));
    }

    private int findStartingPositionInQueue(Uri originalUri, List<Song> queue) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getLocation().equals(originalUri)) {
                return i;
            }
        }

        return 0;
    }

    private void startIntentQueue(List<Song> queue, int position) {
        mPlayerController.setQueue(queue, position);
        mPlayerController.play();
    }

    private void initRefreshLayout() {
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.library_refresh_layout);
        mRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
        mRefreshLayout.setColorSchemeColors(mThemeStore.getPrimaryColor(),
                mThemeStore.getAccentColor());
        mRefreshLayout.setEnabled(false);

        Observable.combineLatest(mMusicStore.isLoading(), mPlaylistStore.isLoading(),
                (musicLoading, playlistLoading) -> {
                    return musicLoading || playlistLoading;
                })
                .compose(bindToLifecycle())
                .subscribe(
                        refreshing -> {
                            mRefreshLayout.setEnabled(refreshing);
                            mRefreshLayout.setRefreshing(refreshing);
                        }, throwable -> {
                            Timber.e(throwable, "Failed to update refresh indicator");
                        });
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
            case R.id.menu_library_settings:
                startActivity(SettingsActivity.newIntent(this));
                return true;
            case R.id.menu_library_refresh:
                refreshLibrary();
                return true;
            case R.id.menu_library_search:
                startActivity(SearchActivity.newIntent(this));
                return true;
            case R.id.menu_library_about:
                startActivity(AboutActivity.newIntent(this));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshLibrary() {
        Observable<Boolean> musicStoreResult = mMusicStore.refresh();
        Observable<Boolean> playlistStoreResult = mPlaylistStore.refresh();

        Observable<Boolean> combinedResult = Observable.combineLatest(
                musicStoreResult, playlistStoreResult, (result1, result2) -> result1 && result2);

        combinedResult.take(1)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(successful -> {
                    if (successful) {
                        View view = findViewById(R.id.library_pager);
                        Snackbar.make(view, R.string.confirm_refresh_library, LENGTH_SHORT).show();
                    } else {
                        showPermissionSnackbar();
                    }
                }, throwable -> {
                    Timber.e(throwable, "Failed to refresh library");
                });
    }

    private void showPermissionSnackbar() {
        View view = findViewById(R.id.library_pager);
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
                new CreatePlaylistDialogFragment.Builder(getSupportFragmentManager())
                        .showSnackbarIn(R.id.list)
                        .show(TAG_MAKE_PLAYLIST);
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
