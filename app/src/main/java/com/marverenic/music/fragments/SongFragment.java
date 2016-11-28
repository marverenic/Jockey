package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.adapter.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class SongFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;
    private List<Song> mSongs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mMusicStore.getSongs()
                .compose(bindToLifecycle())
                .subscribe(
                        songs -> {
                            mSongs = songs;
                            setupAdapter();
                        },
                        throwable -> Timber.e(throwable, "Failed to get new songs"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);
        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new DividerDecoration(getContext(), R.id.empty_layout));

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        if (mAdapter == null) {
            setupAdapter();
        } else {
            mRecyclerView.setAdapter(mAdapter);
        }

        int paddingH = (int) getActivity().getResources().getDimension(R.dimen.global_padding);
        view.setPadding(paddingH, 0, paddingH, 0);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView = null;
        mAdapter = null;
        mSongSection = null;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mSongs == null) {
            return;
        }

        if (mSongSection != null) {
            mSongSection.setData(mSongs);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mSongSection = new SongSection(this, mSongs);
            mAdapter.addSection(mSongSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()));
        }
    }
}
