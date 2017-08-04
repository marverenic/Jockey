package com.marverenic.music.ui.library.genre;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.databinding.FragmentGenreBinding;
import com.marverenic.music.model.Genre;
import com.marverenic.music.ui.BaseToolbarFragment;

import javax.inject.Inject;

public class GenreFragment extends BaseToolbarFragment {

    private static final String GENRE_ARG = "GenreFragment.GENRE";

    @Inject MusicStore mMusicStore;

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
        binding.setViewModel(new GenreViewModel(this, mMusicStore, mGenre));

        return binding.getRoot();
    }

    @Override
    protected boolean canNavigateUp() {
        return true;
    }
}
