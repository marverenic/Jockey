package com.marverenic.music.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AppendPlaylistDialogFragment extends DialogFragment {

    private static final String TAG = "AppendPlaylistDialogFragment";

    private static final String TAG_MAKE_PLAYLIST = "CreateNewPlaylistDialog";
    private static final String SAVED_TITLE = "AppendPlaylistDialogFragment.Title";
    private static final String SAVED_SONG = "AppendPlaylistDialogFragment.Song";
    private static final String SAVED_SONGS = "AppendPlaylistDialogFragment.Songs";
    private static final String SAVED_SNACKBAR_VIEW = "AppendPlaylistDialogFragment.Snackbar";

    @Inject PlaylistStore mPlaylistStore;

    private Dialog mDialog;
    private String mTitle;
    private String mCollectionName;
    private Playlist[] mChoices;
    private String[] mChoiceNames;
    private Song mSong;
    private List<Song> mSongs;
    private boolean mSingle;
    @IdRes private int mSnackbarView;

    public static AppendPlaylistDialogFragment newInstance() {
        return new AppendPlaylistDialogFragment();
    }

    public AppendPlaylistDialogFragment setCollectionName(String name) {
        mCollectionName = name;
        return this;
    }

    public AppendPlaylistDialogFragment setTitle(String title) {
        mTitle = title;
        return this;
    }

    public AppendPlaylistDialogFragment setSong(@NonNull Song song) {
        mSong = song;
        mSingle = true;
        return this;
    }

    public AppendPlaylistDialogFragment setSongs(@NonNull List<Song> songs) {
        mSongs = songs;
        mSingle = false;
        return this;
    }

    public AppendPlaylistDialogFragment showSnackbarIn(@IdRes int viewId) {
        mSnackbarView = viewId;
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(SAVED_TITLE);
            mSnackbarView = savedInstanceState.getInt(SAVED_SNACKBAR_VIEW);

            if (savedInstanceState.containsKey(SAVED_SONG)) {
                mSong = savedInstanceState.getParcelable(SAVED_SONG);
                mSingle = true;
            } else if (savedInstanceState.containsKey(SAVED_SONGS)) {
                mSongs = savedInstanceState.getParcelableArrayList(SAVED_SONGS);
                mSingle = false;
            }
        }

        mPlaylistStore.getPlaylists()
                .take(1)
                .subscribe(playlists -> {
                    List<Playlist> choices = new ArrayList<>(playlists.size());
                    List<String> choiceNames = new ArrayList<>(playlists.size());

                    choiceNames.add(getString(R.string.action_make_new_playlist));

                    for (Playlist playlist : playlists) {
                        if (!(playlist instanceof AutoPlaylist)) {
                            choices.add(playlist);
                            choiceNames.add(playlist.getPlaylistName());
                        }
                    }

                    mChoices = new Playlist[choices.size()];
                    mChoiceNames = new String[choiceNames.size()];

                    choices.toArray(mChoices);
                    choiceNames.toArray(mChoiceNames);

                    showDialog();
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_TITLE, mTitle);
        outState.putInt(SAVED_SNACKBAR_VIEW, mSnackbarView);

        if (mSingle) {
            outState.putParcelable(SAVED_SONG, mSong);
        } else {
            outState.putParcelableArrayList(SAVED_SONGS, new ArrayList<>(mSongs));
        }
    }

    private void showDialog() {
        if (getDialog() != null) {
            getDialog().hide();
        }

        if (mTitle == null) {
            if (mSingle) {
                setTitle(getString(R.string.header_add_song_name_to_playlist, mSong));
            } else if (mCollectionName != null) {
                setTitle(getString(R.string.header_add_song_name_to_playlist, mCollectionName));
            }
        }

        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(mTitle)
                .setItems(mChoiceNames, (dialog, which) -> {onPlaylistSelected(which);})
                .setNegativeButton(R.string.action_cancel, null)
                .setOnDismissListener(dialog -> getDialog().dismiss())
                .show();
    }

    private void onPlaylistSelected(int which) {
        if (which == 0) {
            CreatePlaylistDialogFragment.newInstance()
                    .setSongs((mSingle) ? Collections.singletonList(mSong) : mSongs)
                    .showSnackbarIn(mSnackbarView)
                    .show(getFragmentManager(), TAG_MAKE_PLAYLIST);
        } else {
            addToPlaylist(mChoices[which]);
        }
    }

    private void addToPlaylist(Playlist playlist) {
        mPlaylistStore.getSongs(playlist)
                .take(1)
                .subscribe(
                        oldEntries -> {
                            if (mSingle) {
                                mPlaylistStore.addToPlaylist(playlist, mSong);
                            } else {
                                mPlaylistStore.addToPlaylist(playlist, mSongs);
                            }
                            showSnackbar(playlist, oldEntries);
                        },
                        throwable -> {
                            Log.e(TAG, "Failed to get old entries");
                        });
    }

    private void showSnackbar(Playlist editedPlaylist, List<Song> previousSongs) {
        String message;
        if (mSingle) {
            message = getString(R.string.message_added_song, mSong, editedPlaylist);
        } else {
            message = getString(R.string.confirm_add_songs, mSongs.size(), editedPlaylist);
        }

        showSnackbar(message, editedPlaylist, previousSongs);
    }

    private void showSnackbar(String message, Playlist editedPlaylist, List<Song> previousSongs) {
        View container = getActivity().findViewById(mSnackbarView);

        if (container != null) {
            Snackbar.make(container, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, view -> {
                        mPlaylistStore.editPlaylist(editedPlaylist, previousSongs);
                    })
                    .show();
        }
    }
}
