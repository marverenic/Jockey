package com.marverenic.music.ui.library.browse;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    @Inject PreferenceStore mPrefStore;
    @Inject MusicStore mMusicStore;
    @Inject PlayerController mPlayerController;

    private FragmentMusicBrowserBinding mBinding;
    private MusicBrowserViewModel mViewModel;

    public static MusicBrowserFragment newInstance() {
        return new MusicBrowserFragment();
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

        mViewModel = new MusicBrowserViewModel(getContext(),
                resolveStartingDirectory(), this::playFile);
        mBinding.setViewModel(mViewModel);
        setupToolbar(mBinding.toolbar);

        return mBinding.getRoot();
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
        return mViewModel.goBack() || super.onBackPressed();
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
