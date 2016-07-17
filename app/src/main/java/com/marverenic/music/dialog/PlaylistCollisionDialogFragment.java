package com.marverenic.music.dialog;

import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

public class PlaylistCollisionDialogFragment extends DialogFragment {

    private static final String KEY_PLAYLIST = "PlaylistCollisionDialogFragment.Playlist";
    private static final String KEY_SONG = "PlaylistCollisionDialogFragment.Song";
    private static final String KEY_SONGS = "PlaylistCollisionDialogFragment.Songs";
    private static final String KEY_SNACKBAR_VIEW = "PlaylistCollisionDialogFragment.Snackbar";

    @Inject PlaylistStore mPlaylistStore;

    private Dialog mDialog;
    private Playlist mPlaylist;
    private List<Song> mPlaylistContents;
    private Song mSong;
    private List<Song> mSongs;
    private boolean mSingle;
    @IdRes private int mSnackbarView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mSnackbarView = getArguments().getInt(KEY_SNACKBAR_VIEW);
        mPlaylist = getArguments().getParcelable(KEY_PLAYLIST);

        if (getArguments().containsKey(KEY_SONG)) {
            mSong = getArguments().getParcelable(KEY_SONG);
            mSingle = true;
        } else if (getArguments().containsKey(KEY_SONGS)) {
            mSongs = getArguments().getParcelableArrayList(KEY_SONGS);
            mSingle = false;
        }

        mPlaylistStore.getSongs(mPlaylist)
                .take(1)
                .subscribe(songs -> {
                    mPlaylistContents = songs;
                    showDialog();
                }, throwable -> {
                    Timber.e(throwable, "Failed to get playlists");
                });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (mDialog == null) {
            return super.onCreateDialog(savedInstanceState);
        } else {
            return mDialog;
        }
    }

    private void showDialog() {
        if (getDialog() != null) {
            getDialog().hide();
        }

        if (mSingle) {
            showSingleSongDialog();
        } else {
            showMultiSongDialog();
        }
    }

    private void showSingleSongDialog() {
        String title = getResources().getQuantityString(R.plurals.alert_confirm_duplicates, 1);
        String songName = mSong.getSongName();
        String playlistName = mPlaylist.getPlaylistName();
        String message = getString(R.string.playlist_confirm_duplicate, playlistName, songName);

        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_add, (dialogInterface, which) -> {
                    addAllSongs();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .setOnDismissListener(dialog -> getDialog().dismiss())
                .show();
    }

    private int getDuplicateSongCount() {
        Set<Song> originalEntrySet = new HashSet<>(mPlaylistContents);
        originalEntrySet.retainAll(mSongs);
        return originalEntrySet.size();
    }

    private void showMultiSongDialog() {
        int count = getDuplicateSongCount();

        Resources res = getResources();
        String title = res.getQuantityString(R.plurals.alert_confirm_duplicates, count);
        String addAll = res.getQuantityString(R.plurals.playlist_positive_add_duplicates, count);
        String addNew = getString(R.string.action_add_new);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setOnDismissListener(dialog -> getDialog().dismiss());

        if (mSongs.size() == count) {
            String message = getString(R.string.playlist_confirm_all_duplicates, count);

            builder.setMessage(message)
                    .setPositiveButton(addAll, (dialogInterface, which) -> {
                        addAllSongs();
                    })
                    .setNegativeButton(R.string.action_cancel, null);
        } else {
            String message = res.getQuantityString(R.plurals.playlist_confirm_some_duplicates,
                    count, count);

            builder.setMessage(message)
                    .setPositiveButton(addAll, (dialogInterface, which) -> {
                        addAllSongs();
                    })
                    .setNegativeButton(addNew, (dialogInterface, which) -> {
                        addNewSongs();
                    })
                    .setNeutralButton(R.string.action_cancel, null);
        }

        mDialog = builder.show();
    }

    private void addAllSongs() {
        List<Song> updatedContents = new ArrayList<>(mPlaylistContents);

        String message;
        if (mSingle) {
            updatedContents.add(mSong);

            String songName = mSong.getSongName();
            String playlistName = mPlaylist.getPlaylistName();
            message = getString(R.string.message_added_song, songName, playlistName);
        } else {
            updatedContents.addAll(mSongs);

            int count = mSongs.size();
            String playlistName = mPlaylist.getPlaylistName();
            message = getString(R.string.confirm_add_songs, count, playlistName);
        }

        mPlaylistStore.editPlaylist(mPlaylist, updatedContents);
        showSnackbar(message);
    }

    private void addNewSongs() {
        List<Song> updatedContents = new ArrayList<>(mPlaylistContents);

        int count = 0;
        for (Song s : mSongs) {
            if (!mPlaylistContents.contains(s)) {
                count++;
                updatedContents.add(s);
            }
        }

        mPlaylistStore.editPlaylist(mPlaylist, updatedContents);
        String message = getString(R.string.confirm_add_songs, count, mPlaylist.getPlaylistName());
        showSnackbar(message);
    }

    private void showSnackbar(String message) {
        View container = getActivity().findViewById(mSnackbarView);

        if (container != null) {
            Snackbar.make(container, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, view -> {
                        mPlaylistStore.editPlaylist(mPlaylist, mPlaylistContents);
                    })
                    .show();
        }
    }

    public static class Builder {

        private Bundle mArgs;
        private FragmentManager mFragmentManager;

        public Builder(FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
            mArgs = new Bundle();
        }

        public Builder setPlaylist(Playlist playlist) {
            mArgs.putParcelable(KEY_PLAYLIST, playlist);
            return this;
        }

        public Builder setSongs(Song song) {
            mArgs.putParcelable(KEY_SONG, song);
            mArgs.remove(KEY_SONGS);
            return this;
        }

        public Builder setSongs(List<Song> songs) {
            mArgs.putParcelableArrayList(KEY_SONGS, new ArrayList<>(songs));
            mArgs.remove(KEY_SONG);
            return this;
        }

        public Builder showSnackbarIn(@IdRes int snackbarContainerId) {
            mArgs.putInt(KEY_SNACKBAR_VIEW, snackbarContainerId);
            return this;
        }

        public void show(String tag) {
            PlaylistCollisionDialogFragment dialogFragment = new PlaylistCollisionDialogFragment();
            dialogFragment.setArguments(mArgs);

            dialogFragment.show(mFragmentManager, tag);
        }
    }
}
