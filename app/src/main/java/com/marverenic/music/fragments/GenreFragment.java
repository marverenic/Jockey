package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.section.GenreSection;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

import javax.inject.Inject;

public class GenreFragment extends Fragment {

    @Inject MusicStore mMusicStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private GenreSection mGenreSection;
    private List<Genre> mGenres;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mMusicStore.getGenres().subscribe(
                genres -> {
                    mGenres = genres;
                    setupAdapter();
                },
                Throwable::printStackTrace);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        setupAdapter();

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        return view;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mGenres == null) {
            return;
        }

        if (mGenreSection != null) {
            mGenreSection.setData(mGenres);
            mAdapter.notifyDataSetChanged();
        } else {

            mAdapter = new HeterogeneousAdapter();
            mAdapter.addSection(new GenreSection(Library.getGenres()));
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()));

            mRecyclerView.addItemDecoration(
                    new BackgroundDecoration(Themes.getBackgroundElevated()));
            mRecyclerView.addItemDecoration(new DividerDecoration(getContext(), R.id.empty_layout));
            mRecyclerView.setAdapter(mAdapter);

            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(layoutManager);
        }
    }
}
