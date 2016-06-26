package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.instances.section.ArtistSection;
import com.marverenic.music.instances.section.LibraryEmptyState;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

import javax.inject.Inject;

public class ArtistFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private ArtistSection mArtistSection;
    private List<Artist> mArtists;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mMusicStore.getArtists()
                .compose(bindToLifecycle())
                .subscribe(
                        artists -> {
                            mArtists = artists;
                            setupAdapter();
                        },
                        Throwable::printStackTrace);
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
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mArtists == null) {
            return;
        }

        if (mArtistSection != null) {
            mArtistSection.setData(mArtists);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousAdapter();
            mRecyclerView.setAdapter(mAdapter);

            mArtistSection = new ArtistSection(this, mArtists);
            mAdapter.addSection(mArtistSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity(), mMusicStore));
        }
    }
}
