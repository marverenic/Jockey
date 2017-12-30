package com.marverenic.music.ui.library.genre;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.databinding.FragmentGenreBinding;
import com.marverenic.music.model.Genre;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseToolbarFragment;
import com.marverenic.music.ui.common.OnSongSelectedListener;

import javax.inject.Inject;

import timber.log.Timber;

public class GenreFragment extends BaseToolbarFragment {

    private static final String GENRE_ARG = "GenreFragment.GENRE";

    @Inject PlayerController mPlayerController;
    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PreferenceStore mPreferenceStore;

    private Genre mGenre;

    public static GenreFragment newInstance(Genre genre) {
        GenreFragment fragment = new GenreFragment();

        Bundle args = new Bundle();
        args.putParcelable(GENRE_ARG, genre);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        mGenre = getArguments().getParcelable(GENRE_ARG);
    }

    @Override
    protected String getFragmentTitle() {
        return mGenre.getGenreName();
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {

        FragmentGenreBinding binding = FragmentGenreBinding.inflate(inflater, container, false);
        GenreViewModel viewModel = new GenreViewModel(getContext(), getFragmentManager(),
                mPlayerController, mMusicStore, mPlaylistStore, mPreferenceStore,
                OnSongSelectedListener.defaultImplementation(getActivity(), mPreferenceStore));

        binding.setViewModel(viewModel);

        mMusicStore.getSongs(mGenre)
                .compose(bindToLifecycle())
                .subscribe(viewModel::setSongs, throwable -> {
                    Timber.e(throwable, "Failed to get song contents");
                });

        mPlayerController.getNowPlaying()
                .compose(bindToLifecycle())
                .subscribe(viewModel::setCurrentSong, throwable -> {
                    Timber.e(throwable, "Failed to update now playing");
                });

        return binding.getRoot();
    }

    @Override
    protected boolean canNavigateUp() {
        return true;
    }
}
