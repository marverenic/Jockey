package com.marverenic.music.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Themes;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class CreatePlaylistDialogFragment extends DialogFragment implements TextWatcher {

    private static final String SAVED_TITLE = "CreatePlaylistDialogFragment.Name";
    private static final String SAVED_SNACKBAR_VIEW = "AppendPlaylistDialogFragment.Snackbar";

    @Inject PlaylistStore mPlaylistStore;

    private AlertDialog mDialog;
    private TextInputLayout mInputLayout;
    private AppCompatEditText mEditText;

    private BehaviorSubject<Playlist> mSubject = BehaviorSubject.create();
    private List<Song> mSongs;
    @IdRes private int mSnackbarView;

    public static CreatePlaylistDialogFragment newInstance() {
        return new CreatePlaylistDialogFragment();
    }

    public CreatePlaylistDialogFragment setSongs(List<Song> songs) {
        mSongs = songs;
        return this;
    }

    public CreatePlaylistDialogFragment showSnackbarIn(@IdRes int viewId) {
        mSnackbarView = viewId;
        return this;
    }

    public Observable<Playlist> getCreated() {
        return mSubject;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            onCreateDialogLayout(null);
        } else {
            mSnackbarView = savedInstanceState.getInt(SAVED_SNACKBAR_VIEW);
            onCreateDialogLayout(savedInstanceState.getString(SAVED_TITLE));
        }

        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.header_create_playlist)
                .setView(mInputLayout)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {createPlaylist();})
                .setNegativeButton(R.string.action_cancel, null)
                .show();

        updateDialogButtons(true);

        int padding = (int) getResources().getDimension(R.dimen.alert_padding);
        ((View) mInputLayout.getParent()).setPadding(
                padding - mInputLayout.getPaddingLeft(),
                padding,
                padding - mInputLayout.getPaddingRight(),
                mInputLayout.getPaddingBottom());

        return mDialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_TITLE, mEditText.getText().toString());
        outState.putInt(SAVED_SNACKBAR_VIEW, mSnackbarView);
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

        mSubject.onNext(created);
        mSubject.onCompleted();

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
            button.setTextColor(Themes.getAccent());
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
}
