package com.marverenic.music.ui.library;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.about.AboutActivity;
import com.marverenic.music.ui.common.playlist.CreatePlaylistDialogFragment;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.ui.search.SearchActivity;
import com.marverenic.music.ui.settings.SettingsActivity;
import com.marverenic.music.utils.UriUtils;
import com.marverenic.music.utils.Util;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import timber.log.Timber;

public class LibraryActivity extends BaseLibraryActivity implements View.OnClickListener {

    private static final String TAG_MAKE_PLAYLIST = "CreatePlaylistDialog";
    private static final String ACTION_SHOW_NOW_PLAYING_PAGE = "LibraryActivity.ShowNowPlayingPage";

    @Inject ThemeStore mThemeStore;
    @Inject MusicStore mMusicStore;
    @Inject PlayerController mPlayerController;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PreferenceStore mPrefStore;

    private SwipeRefreshLayout mRefreshLayout;

    public static Intent newNowPlayingIntent(Context context) {
        Intent intent = new Intent(context, LibraryActivity.class);
        intent.setAction(ACTION_SHOW_NOW_PLAYING_PAGE);
        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        return LibraryFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JockeyApplication.getComponent(this).inject(this);
        onNewIntent(getIntent());

        /*
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

        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this, getSupportFragmentManager());
        adapter.setFloatingActionButton(fab);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(adapter);
        ((TabLayout) findViewById(R.id.library_tabs)).setupWithViewPager(pager);

        pager.setCurrentItem(page);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }*/
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals(ACTION_SHOW_NOW_PLAYING_PAGE)) {
            expandBottomSheet();
            // Don't try to process this intent again
            setIntent(new Intent(this, LibraryActivity.class));
            return;
        }

        // Handle incoming requests to play media from other applications
        if (intent.getData() == null) {
            return;
        }

        // If this intent is a music intent, process it
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            MediaStoreUtil.promptPermission(this)
                    .subscribe(hasPermission -> {
                        if (hasPermission) {
                            startPlaybackFromUri(intent.getData());
                        }
                    }, throwable -> {
                        Timber.e(throwable, "Failed to start playback from URI %s",
                                intent.getData());
                    });
        }

        // Don't try to process this intent again
        setIntent(new Intent(this, LibraryActivity.class));
    }

    private void startPlaybackFromUri(Uri songUri) {
        String songName = UriUtils.getDisplayName(this, songUri);

        List<Song> queue = buildQueueFromFileUri(songUri);
        int position;

        if (queue == null || queue.isEmpty()) {
            queue = buildQueueFromUri(songUri);
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

        expandBottomSheet();
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

    @Override
    public void onClick(View v) {
        if (v.getTag() != null) {
            if (v.getTag().equals("fab-" + getString(R.string.playlist))) {
                new CreatePlaylistDialogFragment.Builder(getSupportFragmentManager())
                        .showSnackbarIn(R.id.library_pager)
                        .show(TAG_MAKE_PLAYLIST);
            } else if (v.getTag().equals("fab-" + getString(R.string.playlist_auto))) {
                startActivity(AutoPlaylistEditActivity.newIntent(this));
            }
        }
    }

    @Override
    protected int getSnackbarContainerViewId() {
        return R.id.library_pager;
    }

}
