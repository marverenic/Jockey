package com.marverenic.music.ui.common.playlist;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class CreatePlaylistDialogFragment extends DialogFragment implements TextWatcher {

    private static final String KEY_TITLE = "CreatePlaylistDialogFragment.Name";
    private static final String KEY_SNACKBAR_VIEW = "CreatePlaylistDialogFragment.Snackbar";
    private static final String KEY_SONGS = "CreatePlaylistDialogFragment.Songs";
    private static final String SAVED_INPUT_STATE = "CreatePlaylistDialogFragment.EditTextState";

    @Inject PlaylistStore mPlaylistStore;
    @Inject ThemeStore mThemeStore;

    private AlertDialog mDialog;
    private TextInputLayout mInputLayout;
    private AppCompatEditText mEditText;

    private List<Song> mSongs;
    @IdRes private int mSnackbarView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mSnackbarView = getArguments().getInt(KEY_SNACKBAR_VIEW);
        mSongs = getArguments().getParcelableArrayList(KEY_SONGS);
        onCreateDialogLayout(getArguments().getString(KEY_TITLE));

        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.header_create_playlist)
                .setView(mInputLayout)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {createPlaylist();})
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mDialog.show();

        updateDialogButtons(true);

        int padding = (int) getResources().getDimension(R.dimen.alert_padding);
        ((View) mInputLayout.getParent()).setPadding(
                padding - mInputLayout.getPaddingLeft(),
                padding,
                padding - mInputLayout.getPaddingRight(),
                mInputLayout.getPaddingBottom());

        if (savedInstanceState != null) {
            mEditText.onRestoreInstanceState(savedInstanceState.getParcelable(SAVED_INPUT_STATE));
        }

        return mDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_INPUT_STATE, mEditText.onSaveInstanceState());
    }

    private void onCreateDialogLayout(@Nullable String restoredName) {
        mInputLayout = new TextInputLayout(getContext());
        mEditText = new AppCompatEditText(getContext());

        mEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        mEditText.setHint(R.string.hint_playlist_name);
        mEditText.setText(restoredName);

        mInputLayout.addView(mEditText);
        mInputLayout.setErrorEnabled(true);

        mEditText.addTextChangedListener(this);
    }

    private void createPlaylist() {
        String name = mEditText.getText().toString();

        Playlist created = mPlaylistStore.makePlaylist(name, mSongs);
        showSnackbar(created);
    }

    private void showSnackbar(Playlist created) {
        View container = getActivity().findViewById(mSnackbarView);

        if (container != null) {
            String name = created.getPlaylistName();
            String message = getString(R.string.message_created_playlist, name);

            Snackbar.make(container, message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo, view -> {
                        mPlaylistStore.removePlaylist(created);
                    })
                    .show();
        }
    }

    private void updateDialogButtons(boolean error) {
        Button button = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        button.setEnabled(!error);

        if (error) {
            button.setTextColor(ResourcesCompat.getColor(getResources(),
                    R.color.secondary_text_disabled, getActivity().getTheme()));
        } else {
            button.setTextColor(mThemeStore.getAccentColor());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String error = mPlaylistStore.verifyPlaylistName(s.toString());

        mInputLayout.setError(error);
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == null && s.length() > 0);

        updateDialogButtons(error != null || s.length() == 0);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    public static class Builder {

        private FragmentManager mFragmentManager;
        private Bundle mArgs;

        public Builder(FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
            mArgs = new Bundle();
        }

        public Builder setTitle(String title) {
            mArgs.putString(KEY_TITLE, title);
            return this;
        }

        public Builder setSongs(Song song) {
            return setSongs(Collections.singletonList(song));
        }

        public Builder setSongs(List<Song> songs) {
            mArgs.putParcelableArrayList(KEY_SONGS, copyLocalSongs(songs));
            return this;
        }

        private ArrayList<Song> copyLocalSongs(List<Song> from) {
            ArrayList<Song> copy = new ArrayList<>();
            for (Song song : from) {
                if (song.isInLibrary()) copy.add(song);
            }
            return copy;
        }

        public Builder showSnackbarIn(@IdRes int snackbarContainerId) {
            mArgs.putInt(KEY_SNACKBAR_VIEW, snackbarContainerId);
            return this;
        }

        public void show(String tag) {
            CreatePlaylistDialogFragment dialogFragment = new CreatePlaylistDialogFragment();
            dialogFragment.setArguments(mArgs);
            dialogFragment.show(mFragmentManager, tag);
        }

    }
}
