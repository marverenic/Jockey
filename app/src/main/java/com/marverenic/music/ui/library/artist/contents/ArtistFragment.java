package com.marverenic.music.ui.library.artist.contents;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.data.store.ThemeStore;
import com.marverenic.music.databinding.FragmentArtistBinding;
import com.marverenic.music.lastfm.data.store.LastFmStore;
import com.marverenic.music.model.Artist;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.utils.Util;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

public class ArtistFragment extends BaseFragment {

    private static final String ARTIST_ARG = "ArtistListFragment.ARTIST";

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        FragmentArtistBinding binding = FragmentArtistBinding.inflate(inflater, container, false);
        ArtistViewModel viewModel = new ArtistViewModel(getContext(), getFragmentManager(), mArtist,
                mPlayerController, mMusicStore, mPlaylistStore, mPrefStore, mThemeStore,
                OnSongSelectedListener.defaultImplementation(getActivity(), mPrefStore));

        mMusicStore.getSongs(mArtist)
                .compose(bindToLifecycle())
                .subscribe(viewModel::setArtistSongs, throwable -> {
                    Timber.e(throwable, "Failed to get song contents");
                });

        mMusicStore.getAlbums(mArtist)
                .compose(bindToLifecycle())
                .subscribe(viewModel::setArtistAlbums, throwable -> {
                    Timber.e(throwable, "Failed to get album contents");
                });

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setCurrentSong, throwable -> {
                    Timber.e(throwable, "Failed to set now playing song");
                });

        if (Util.canAccessInternet(getContext(), mPrefStore.useMobileNetwork())) {
            viewModel.setLoadingLastFmData(true);

            mLfmStore.getArtistInfo(mArtist.getArtistName())
                    .compose(bindToLifecycle())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(viewModel::setLastFmData,
                            throwable -> {
                                Timber.e(throwable, "Failed to get Last.fm artist info");
                                viewModel.setLoadingLastFmData(false);
                            });
        }

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
