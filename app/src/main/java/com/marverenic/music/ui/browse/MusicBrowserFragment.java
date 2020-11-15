package com.marverenic.music.ui.browse;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentMusicBrowserBinding;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.utils.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class MusicBrowserFragment extends BaseFragment {

    private static final String KEY_SAVED_LIST_STATE = "RecentlyAddedFragment.RecyclerViewState";
    private static final String EXTRA_SAVED_HISTORY = "MusicBrowserFragment.History";
    private static final String EXTRA_SAVED_DIRECTORY = "MusicBrowserFragment.CurrentDirectory";
    private static final String ARG_STARTING_DIRECTORY = "MusicBrowserFragment.StartingDirectory";
    private static final String ARG_CONFIRM_EXIT = "MusicBrowserFragment.ConfirmExit";

    @Inject PreferenceStore mPrefStore;
    @Inject MusicStore mMusicStore;
    @Inject PlayerController mPlayerController;

    private FragmentMusicBrowserBinding mBinding;
    private MusicBrowserViewModel mViewModel;
    private boolean mExitConfirmed;
    private boolean mDisableExitConfirmation;

    public static MusicBrowserFragment newInstance() {
        return new MusicBrowserFragment();
    }

    public static MusicBrowserFragment newInstance(File startingDirectory, boolean confirmExit) {
        Bundle args = new Bundle();
        args.putString(ARG_STARTING_DIRECTORY, startingDirectory.getAbsolutePath());
        args.putBoolean(ARG_CONFIRM_EXIT, confirmExit);
        MusicBrowserFragment fragment = new MusicBrowserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_music_browser,
                container, false);

        File startingDirectory;

        if (getArguments() != null) {
            String startingPath = getArguments().getString(ARG_STARTING_DIRECTORY);
            mDisableExitConfirmation = !getArguments().getBoolean(ARG_CONFIRM_EXIT, true);

            if (startingPath != null) {
                startingDirectory = new File(startingPath);
            } else {
                startingDirectory = null;
            }
        } else {
            startingDirectory = resolveStartingDirectory();
            mDisableExitConfirmation = false;
        }

        mViewModel = new MusicBrowserViewModel(getContext(), startingDirectory, this::playFile);

        mBinding.setViewModel(mViewModel);
        mBinding.executePendingBindings();
        setupToolbar(mBinding.toolbar);

        if (savedInstanceState != null) {
            mBinding.musicBrowserRecyclerView.getLayoutManager()
                    .onRestoreInstanceState(savedInstanceState.getParcelable(KEY_SAVED_LIST_STATE));
        }

        mViewModel.getObservableDirectory()
                .subscribe(directory -> {
                    mExitConfirmed = false;
                }, throwable -> {
                    Timber.e(throwable, "Failed to update exit confirmation state");
                });

        if (savedInstanceState != null) {
            String[] savedHistory = savedInstanceState.getStringArray(EXTRA_SAVED_HISTORY);
            String savedDirectory = savedInstanceState.getString(EXTRA_SAVED_DIRECTORY);

            if (savedDirectory != null) {
                mViewModel.setDirectory(new File(savedDirectory));
                if (savedHistory != null) {
                    mViewModel.setHistory(savedHistory);
                }
            }
        }

        return mBinding.getRoot();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mViewModel.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_SAVED_DIRECTORY, mViewModel.getDirectory().getAbsolutePath());
        outState.putStringArray(EXTRA_SAVED_HISTORY, mViewModel.getHistory());

        outState.putParcelable(KEY_SAVED_LIST_STATE,
                mBinding.musicBrowserRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    private File resolveStartingDirectory() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (!musicDir.canRead() || !musicDir.exists()) {
            return Environment.getExternalStorageDirectory();
        } else {
            return musicDir;
        }
    }

    private void setupToolbar(Toolbar toolbar) {
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
        }
    }

    @Override
    protected boolean onBackPressed() {
        return mViewModel.goBack() || confirmBackPressed() || super.onBackPressed();
    }

    private boolean confirmBackPressed() {
        if (mExitConfirmed || mDisableExitConfirmation) {
            return false;
        }

        Snackbar.make(mBinding.getRoot(), R.string.confirm_application_exit, Snackbar.LENGTH_SHORT)
                .show();
        mExitConfirmed = true;
        return true;
    }

    private void playFile(File song) {
        List<File> songFiles = getNeighboringSongs(song);
        int startIndex = songFiles.indexOf(song);

        mMusicStore.getSongsFromFiles(songFiles)
                .observeOn(AndroidSchedulers.mainThread())
                .compose(bindToLifecycle())
                .subscribe(songs -> {
                    startPlayback(songs, startIndex);
                }, throwable -> {
                    Timber.e(throwable, "Failed to begin playback from '" + song + "'");
                    String errorMessage = getString(R.string.error_playback_from_files_failed, song.getName());
                    Snackbar.make(mBinding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
                });
    }

    private void startPlayback(List<Song> playlist, int startIndex) {
        mPlayerController.setQueue(playlist, startIndex);
        mPlayerController.play();

        // Expand now playing page if necessary
        if (getActivity() instanceof BaseLibraryActivity) {
            BaseLibraryActivity activity = (BaseLibraryActivity) getActivity();
            if (mPrefStore.openNowPlayingOnNewQueue()) {
                activity.expandBottomSheet();
            }
        }
    }

    private List<File> getNeighboringSongs(File song) {
        List<File> files = new ArrayList<>();

        for (File neighbor : song.getParentFile().listFiles()) {
            if (Util.isFileMusic(neighbor)) {
                files.add(neighbor);
            }
        }

        Collections.sort(files);
        return files;
    }

}
