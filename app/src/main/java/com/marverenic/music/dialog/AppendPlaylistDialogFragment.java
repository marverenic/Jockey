package com.marverenic.music.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import timber.log.Timber;

public class AppendPlaylistDialogFragment extends DialogFragment {

    private static final String TAG_MAKE_PLAYLIST = "CreateNewPlaylistDialog";
    private static final String KEY_TITLE = "AppendPlaylistDialogFragment.Title";
    private static final String KEY_SONG = "AppendPlaylistDialogFragment.Song";
    private static final String KEY_SONGS = "AppendPlaylistDialogFragment.Songs";
    private static final String KEY_SNACKBAR_VIEW = "AppendPlaylistDialogFragment.Snackbar";

    @Inject PlaylistStore mPlaylistStore;

    private Dialog mDialog;
    private String mTitle;
    private Playlist[] mChoices;
    private String[] mChoiceNames;
    private Song mSong;
    private List<Song> mSongs;
    private boolean mSingle;
    @IdRes private int mSnackbarView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mTitle = getArguments().getString(KEY_TITLE);
        mSnackbarView = getArguments().getInt(KEY_SNACKBAR_VIEW);

        if (getArguments().containsKey(KEY_SONG)) {
            mSong = getArguments().getParcelable(KEY_SONG);
            mSingle = true;
        } else if (getArguments().containsKey(KEY_SONGS)) {
            mSongs = getArguments().getParcelableArrayList(KEY_SONGS);
            mSingle = false;
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
            addToPlaylist(mChoices[which - 1]);
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
                            Timber.e(throwable, "Failed to get old entries");
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

    public static class Builder {

        private Bundle mArgs;
        private FragmentManager mFragmentManager;
        private Context mContext;

        public Builder(AppCompatActivity activity) {
            this(activity, activity.getSupportFragmentManager());
        }

        public Builder(Context context, FragmentManager fragmentManager) {
            mContext = context;
            mFragmentManager = fragmentManager;
            mArgs = new Bundle();
        }

        public Builder setTitle(String title) {
            mArgs.putString(KEY_TITLE, title);
            return this;
        }

        public Builder setSongs(Song song) {
            mArgs.putParcelable(KEY_SONG, song);
            mArgs.remove(KEY_SONGS);

            if (!mArgs.containsKey(KEY_TITLE)) {
                String name = song.getSongName();
                String title = mContext.getString(R.string.header_add_song_name_to_playlist, name);
                setTitle(title);
            }
            return this;
        }

        public Builder setSongs(List<Song> songs) {
            mArgs.putParcelableArrayList(KEY_SONGS, new ArrayList<>(songs));
            mArgs.remove(KEY_SONG);
            return this;
        }

        public Builder setSongs(List<Song> songs, String name) {
            mArgs.putParcelableArrayList(KEY_SONGS, new ArrayList<>(songs));
            mArgs.remove(KEY_SONG);
            String title = mContext.getString(R.string.header_add_song_name_to_playlist, name);
            setTitle(title);
            return this;
        }

        public Builder showSnackbarIn(@IdRes int snackbarContainerId) {
            mArgs.putInt(KEY_SNACKBAR_VIEW, snackbarContainerId);
            return this;
        }

        public void show(String tag) {
            AppendPlaylistDialogFragment dialogFragment = new AppendPlaylistDialogFragment();
            dialogFragment.setArguments(mArgs);

            dialogFragment.show(mFragmentManager, tag);
        }

    }
}
