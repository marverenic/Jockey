package com.marverenic.music.ui.library;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.utils.UriUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class LibraryActivity extends BaseLibraryActivity {

    private static final String ACTION_SHOW_NOW_PLAYING_PAGE = "LibraryActivity.ShowNowPlayingPage";

    @Inject PlayerController mPlayerController;

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

    @Override
    public boolean isToolbarCollapsing() {
        // The toolbar isn't actually collapsing here. This is just to ensure that the drawer menu
        // correctly handles drawing the window insets.
        return true;
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
