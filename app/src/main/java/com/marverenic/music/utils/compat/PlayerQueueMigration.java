package com.marverenic.music.utils.compat;

import android.content.Context;
import android.net.Uri;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.persistence.PlaybackPersistenceManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.inject.Inject;

import timber.log.Timber;

public class PlayerQueueMigration {

    private static final String QUEUE_FILE = ".queue";

    private Context mContext;

    @Inject PlaybackPersistenceManager mPersistenceManager;

    public PlayerQueueMigration(Context context) {
        mContext = context;
        JockeyApplication.getComponent(context).inject(this);
    }

    public void migrateLegacyQueueFile() {
        File save = new File(mContext.getExternalFilesDir(null), QUEUE_FILE);
        if (!save.exists() || mPersistenceManager.hasState()) return;

        Scanner scanner = null;
        try {
            scanner = new Scanner(save);
            int seekPosition = scanner.nextInt();
            int queuePosition = scanner.nextInt();
            int queueLength = scanner.nextInt();
            long[] queueIDs = new long[queueLength];
            for (int i = 0; i < queueLength; i++) {
                queueIDs[i] = scanner.nextInt();
            }
            List<Song> queue = MediaStoreUtil.buildSongListFromIds(queueIDs, mContext);
            List<Song> shuffledQueue = Collections.emptyList();
            long[] shuffleQueueIDs;
            if (scanner.hasNextInt()) {
                shuffleQueueIDs = new long[queueLength];
                for (int i = 0; i < queueLength; i++) {
                    shuffleQueueIDs[i] = scanner.nextInt();
                }
                shuffledQueue = MediaStoreUtil.buildSongListFromIds(shuffleQueueIDs, mContext);
            }

            mPersistenceManager.setState(
                    new PlaybackPersistenceManager.State(
                            seekPosition, queuePosition,
                            songsToUris(queue),
                            songsToUris(shuffledQueue)
                    ));
        } catch (IOException|RuntimeException e) {
            Timber.i(e, "Failed to parse previous state. Removing old state...");
        } finally {
            if (scanner != null) {
                scanner.close();
            }

            if (!save.delete()) {
                Timber.w("Failed to delete old queue save at " + save);
            }
        }
    }

    private List<Uri> songsToUris(List<Song> songs) {
        List<Uri> uris = new ArrayList<>();
        for (Song song : songs) {
            uris.add(song.getLocation());
        }

        return uris;
    }

}
