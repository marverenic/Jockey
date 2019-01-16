package com.marverenic.music.ui.library.playlist.contents.edit;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.FragmentAutoPlaylistEditBinding;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.ui.BaseToolbarFragment;

import javax.inject.Inject;

public class AutoPlaylistEditFragment extends BaseToolbarFragment {

    private static final String ARG_PLAYLIST = "AutoPlaylistEditFragment.PLAYLIST";
    private static final String EXTRA_MODIFIED_PLAYLIST = "AutoPlaylistEditFragment.MODIFIED";

    @Inject PlaylistStore mPlaylistStore;
    @Inject MusicStore mMusicStore;

    private FragmentAutoPlaylistEditBinding mBinding;
    private AutoPlaylistEditViewModel mViewModel;

    private AutoPlaylist mOriginalPlaylist;
    private AutoPlaylist.Builder mBuilder;

    public static AutoPlaylistEditFragment newInstance(Context context, AutoPlaylist toEdit) {
        AutoPlaylistEditFragment fragment = new AutoPlaylistEditFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PLAYLIST,
                (toEdit == null) ? AutoPlaylist.emptyPlaylist(context) : toEdit);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mOriginalPlaylist = getArguments().getParcelable(ARG_PLAYLIST);

        if (savedInstanceState == null) {
            mBuilder = new AutoPlaylist.Builder(mOriginalPlaylist);
        } else {
            mBuilder = savedInstanceState.getParcelable(EXTRA_MODIFIED_PLAYLIST);
        }
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {

        mBinding = FragmentAutoPlaylistEditBinding.inflate(inflater, container, false);
        mViewModel = new AutoPlaylistEditViewModel(getContext(), mOriginalPlaylist, mBuilder,
                mPlaylistStore, mMusicStore);
        mBinding.setViewModel(mViewModel);

        setHasOptionsMenu(true);
        return mBinding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_auto_playlist_editor, menu);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EXTRA_MODIFIED_PLAYLIST, mBuilder);
    }

    @Override
    protected String getFragmentTitle() {
        if (mOriginalPlaylist.getPlaylistName().isEmpty() && mBuilder.getName().isEmpty()) {
            return getString(R.string.playlist_auto_new);
        } else {
            return mBuilder.getName();
        }
    }

    @Override
    protected Drawable getUpButtonDrawable() {
        return ContextCompat.getDrawable(getContext(), R.drawable.ic_done_24dp);
    }

    @Override
    protected boolean canNavigateUp() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_auto_playlist_discard:
                if (shouldPromptUnsavedChanges()) {
                    promptDiscardChanges();
                } else {
                    getActivity().finish();
                }
                break;
            case R.id.menu_auto_playlist_edit_add:
                mViewModel.addRule();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean onBackPressed() {
        if (shouldPromptUnsavedChanges()) {
            promptSaveChanges();
            return true;
        }
        return false;
    }

    @Override
    protected void onNavigateUp() {
        if (isPlaylistNameValid()) {
            savePlaylist();
            getActivity().finish();
        } else {
            mViewModel.focusPlaylistName();
        }
    }

    private boolean shouldPromptUnsavedChanges() {
        return !mBuilder.isEqual(mOriginalPlaylist)
                || !mBuilder.getRules().equals(mOriginalPlaylist.getRules());
    }

    private boolean isPlaylistNameValid() {
        String originalName = mOriginalPlaylist.getPlaylistName().trim();
        String editedName = mBuilder.getName().trim();

        boolean equal = !mOriginalPlaylist.getPlaylistName().trim().isEmpty()
                && originalName.equalsIgnoreCase(editedName);

        return equal || mPlaylistStore.verifyPlaylistName(editedName) == null;
    }

    private void promptSaveChanges() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.prompt_save_changes)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    if (isPlaylistNameValid()) {
                        savePlaylist();
                        getActivity().finish();
                    } else {
                        mViewModel.focusPlaylistName();
                    }
                })
                .setNegativeButton(R.string.action_discard, (dialog, which) -> getActivity().finish())
                .setNeutralButton(R.string.action_cancel, null)
                .show();
    }

    private void promptDiscardChanges() {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.prompt_discard_changes)
                .setPositiveButton(R.string.action_discard, (dialog, which) -> getActivity().finish())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void savePlaylist() {
        if (mOriginalPlaylist.getPlaylistId() == AutoPlaylist.Builder.NO_ID) {
            mPlaylistStore.makePlaylist(mBuilder.build(getContext()));
        } else {
            mPlaylistStore.editPlaylist(mBuilder.build(getContext()));
        }
    }
}
