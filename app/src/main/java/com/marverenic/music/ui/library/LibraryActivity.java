package com.marverenic.music.ui.library;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.databinding.ActivityLibraryBinding;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.BaseLibraryActivityViewModel.OnBottomSheetStateChangeListener.BottomSheetState;
import com.marverenic.music.ui.about.AboutActivity;
import com.marverenic.music.ui.library.browse.MusicBrowserFragment;
import com.marverenic.music.ui.settings.SettingsActivity;
import com.marverenic.music.utils.UriUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class LibraryActivity extends BaseLibraryActivity {

    private static final String ACTION_SHOW_NOW_PLAYING_PAGE = "LibraryActivity.ShowNowPlayingPage";
    private static final String EXTRA_SAVED_PAGE_ID = "LibraryActivity.selectedPageId";

    private ActivityLibraryBinding mBinding;

    @Inject PlayerController mPlayerController;

    public static Intent newNowPlayingIntent(Context context) {
        Intent intent = new Intent(context, LibraryActivity.class);
        intent.setAction(ACTION_SHOW_NOW_PLAYING_PAGE);
        return intent;
    }

    @Override
    protected Fragment onCreateFragment(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return LibraryFragment.newInstance();
        } else {
            int savedPage = savedInstanceState.getInt(EXTRA_SAVED_PAGE_ID, R.id.menu_library_home);
            return createFragmentForSelectedPage(savedPage);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        JockeyApplication.getComponent(this).inject(this);
        onNewIntent(getIntent());

        if (savedInstanceState == null) {
            setSelectedPage(R.id.menu_library_home);
        } else {
            int savedPage = savedInstanceState.getInt(EXTRA_SAVED_PAGE_ID, R.id.menu_library_home);
            setSelectedPage(savedPage);
        }
    }

    @Override
    protected void onCreateLayout(@Nullable Bundle savedInstanceState) {
        super.onCreateLayout(savedInstanceState);
        ViewGroup contentViewContainer = findViewById(android.R.id.content);
        View root = contentViewContainer.getChildAt(0);
        contentViewContainer.removeAllViews();

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_library);
        ViewGroup contentContainer = findViewById(R.id.library_content_container);
        contentContainer.addView(root);

        mBinding.libraryDrawerNavigationView.setNavigationItemSelectedListener(item -> {
            mBinding.libraryDrawerLayout.closeDrawers();
            onNavigationItemSelected(item.getItemId());
            return true;
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().equals(ACTION_SHOW_NOW_PLAYING_PAGE)) {
            expandBottomSheet();
            mBinding.libraryDrawerLayout.closeDrawers();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SAVED_PAGE_ID, getSelectedDrawerItem());
    }

    @IdRes
    private int getSelectedDrawerItem() {
        Menu menu = mBinding.libraryDrawerNavigationView.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isChecked()) {
                return item.getItemId();
            }
        }
        return R.id.menu_library_home;
    }

    @Override
    public boolean onSupportNavigateUp() {
        mBinding.libraryDrawerLayout.openDrawer(Gravity.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mBinding.libraryDrawerLayout.isDrawerOpen(Gravity.START)) {
            mBinding.libraryDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_nav_menu_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onBottomSheetStateChange(BottomSheetState newState) {
        boolean collapsed = (newState == BottomSheetState.COLLAPSED)
                || (newState == BottomSheetState.HIDDEN);

        mBinding.libraryDrawerLayout.setDrawerLockMode((collapsed)
                        ? DrawerLayout.LOCK_MODE_UNLOCKED
                        : DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                Gravity.START);
    }

    private void onNavigationItemSelected(@IdRes int itemId) {
        switch (itemId) {
            case R.id.menu_library_settings:
                startActivity(SettingsActivity.newIntent(this));
                return;
            case R.id.menu_library_about:
                startActivity(AboutActivity.newIntent(this));
                return;
        }

        setSelectedPage(itemId);
        replaceFragment(createFragmentForSelectedPage(itemId));
    }

    private void setSelectedPage(@IdRes int itemId) {
        Menu navMenu = mBinding.libraryDrawerNavigationView.getMenu();
        for (int i = 0; i < navMenu.size(); i++) {
            MenuItem menuItem = navMenu.getItem(i);
            if (menuItem.getItemId() == itemId && menuItem.isChecked()) {
                // If the item id hasn't changed, then return early
                return;
            }

            menuItem.setChecked(navMenu.getItem(i).getItemId() == itemId);
        }
    }

    private Fragment createFragmentForSelectedPage(@IdRes int itemId) {
        switch (itemId) {
            case R.id.menu_library_home:
                return LibraryFragment.newInstance();
            case R.id.menu_library_browse:
                return MusicBrowserFragment.newInstance();
            default:
                throw new UnsupportedOperationException("Failed to switch to fragment with menu" +
                        " item id " + getResources().getResourceName(itemId));
        }
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

    @Override
    protected int getSnackbarContainerViewId() {
        return R.id.library_pager;
    }

}
