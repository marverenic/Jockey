package com.marverenic.music.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class AppendPlaylistDialogFragment extends DialogFragment {

    private static final String TAG_MAKE_PLAYLIST = "CreateNewPlaylistDialog";
    private static final String SAVED_TITLE = "AppendPlaylistDialogFragment.Title";
    private static final String SAVED_SONG = "AppendPlaylistDialogFragment.Song";
    private static final String SAVED_SONGS = "AppendPlaylistDialogFragment.Songs";

    @Inject PlaylistStore mPlaylistStore;

    private Dialog mDialog;
    private String mTitle;
    private Playlist[] mChoices;
    private String[] mChoiceNames;
    private Song mSong;
    private List<Song> mSongs;
    private boolean mSingle;

    public static AppendPlaylistDialogFragment newInstance() {
        return new AppendPlaylistDialogFragment();
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString(SAVED_TITLE);

            if (savedInstanceState.containsKey(SAVED_SONG)) {
                mSong = savedInstanceState.getParcelable(SAVED_SONG);
                mSingle = true;
            } else if (savedInstanceState.containsKey(SAVED_SONGS)) {
                mSongs = savedInstanceState.getParcelableArrayList(SAVED_SONGS);
                mSingle = false;
            }
        }

        mPlaylistStore.getPlaylists().subscribe(playlists -> {
            mChoices = new Playlist[playlists.size() + 1];
            mChoiceNames = new String[playlists.size() + 1];

            mChoiceNames[0] = getString(R.string.action_make_new_playlist);

            for (int i = 0; i < playlists.size(); i++) {
                mChoices[i + 1] = playlists.get(i);
                mChoiceNames[i + 1] = playlists.get(i).getPlaylistName();
            }

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

        if (mTitle == null && mSingle) {
            setTitle(getString(R.string.header_add_song_name_to_playlist, mSong));
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
                    .show(getFragmentManager(), TAG_MAKE_PLAYLIST);
        } else if (mSingle) {
            mPlaylistStore.addToPlaylist(mChoices[which], mSong);
        } else {
            mPlaylistStore.addToPlaylist(mChoices[which], mSongs);
        }
    }
}
