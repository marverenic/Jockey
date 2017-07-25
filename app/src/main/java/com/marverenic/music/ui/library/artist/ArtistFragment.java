package com.marverenic.music.ui.library.artist;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.FragmentArtistBinding;
import com.marverenic.music.lastfm.data.store.LastFmStore;
import com.marverenic.music.model.Artist;
import com.marverenic.music.ui.BaseFragment;

import javax.inject.Inject;

public class ArtistFragment extends BaseFragment {

    private static final String ARTIST_ARG = "ArtistFragment.ARTIST";

    @Inject MusicStore mMusicStore;
    @Inject LastFmStore mLfmStore;
    @Inject PreferenceStore mPrefStore;
    @Inject ThemeStore mThemeStore;

    private Artist mArtist;

    public static ArtistFragment newInstance(Artist artist) {
        ArtistFragment fragment = new ArtistFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARTIST_ARG, artist);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mArtist = getArguments().getParcelable(ARTIST_ARG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        FragmentArtistBinding binding = FragmentArtistBinding.inflate(inflater, container, false);
        ArtistViewModel viewModel = new ArtistViewModel(this, mArtist, mMusicStore, mLfmStore,
                mPrefStore, mThemeStore);

        setUpToolbar(binding.toolbar);

        binding.setViewModel(viewModel);
        return binding.getRoot();
    }

    private void setUpToolbar(Toolbar toolbar) {
        toolbar.setTitle(mArtist.getArtistName());

        setActivitySupportActionBar(toolbar);
        ActionBar actionBar = getActivitySupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }
}
