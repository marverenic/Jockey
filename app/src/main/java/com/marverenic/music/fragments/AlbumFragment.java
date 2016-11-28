package com.marverenic.music.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.instances.Album;
import com.marverenic.music.adapter.AlbumSection;
import com.marverenic.music.adapter.LibraryEmptyState;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class AlbumFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;

    private RecyclerView mRecyclerView;
    private HeterogeneousAdapter mAdapter;
    private AlbumSection mAlbumSection;
    private List<Album> mAlbums;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);
        mMusicStore.getAlbums()
                .compose(bindToLifecycle())
                .subscribe(
                        albums -> {
                            mAlbums = albums;
                            setupAdapter();
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get all albums from MusicStore");
                        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.list, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.list);

        final int numColumns = ViewUtils.getNumberOfGridColumns(getActivity());

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), numColumns);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAlbums.isEmpty() ? numColumns : 1;
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.addItemDecoration(new BackgroundDecoration());
        mRecyclerView.addItemDecoration(new GridSpacingDecoration(
                (int) getResources().getDimension(R.dimen.grid_margin), numColumns));

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
        mAlbumSection = null;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mAlbums == null) {
            return;
        }

        if (mAlbumSection != null) {
            mAlbumSection.setData(mAlbums);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mAlbumSection = new AlbumSection(this, mAlbums);
            mAdapter.addSection(mAlbumSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity()));
        }
    }
}
