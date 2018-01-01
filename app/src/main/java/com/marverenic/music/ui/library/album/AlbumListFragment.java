package com.marverenic.music.ui.library.album;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.view.HomogeneousFastScrollAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Album;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.GridSpacingDecoration;
import com.marverenic.music.view.ViewUtils;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class AlbumListFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    private FastScrollRecyclerView mRecyclerView;
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

        View view = inflater.inflate(R.layout.fragment_library_page, container, false);
        mRecyclerView = (FastScrollRecyclerView) view.findViewById(R.id.library_page_list);

        int numColumns = ViewUtils.getNumberOfGridColumns(getActivity(), R.dimen.grid_width);

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
            mAdapter = new HomogeneousFastScrollAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mAlbumSection = new AlbumSection(mAlbums, mMusicStore, mPlayerController, getFragmentManager());
            mAdapter.addSection(mAlbumSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity(), mMusicStore, mPlaylistStore));
        }
    }
}
