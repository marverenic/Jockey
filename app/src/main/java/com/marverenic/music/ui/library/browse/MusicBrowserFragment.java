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

import com.marverenic.music.R;
import com.marverenic.music.databinding.FragmentMusicBrowserBinding;
import com.marverenic.music.ui.BaseFragment;

import java.io.File;

public class MusicBrowserFragment extends BaseFragment {

    private FragmentMusicBrowserBinding mBinding;
    private MusicBrowserViewModel mViewModel;

    public static MusicBrowserFragment newInstance() {
        return new MusicBrowserFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_music_browser,
                container, false);

        mViewModel = new MusicBrowserViewModel(getContext(), resolveStartingDirectory());
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
}
