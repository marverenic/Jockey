package com.marverenic.music.ui.library.artist;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.view.HeterogeneousFastScrollAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.model.Artist;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class ArtistListFragment extends BaseFragment {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    private FastScrollRecyclerView mRecyclerView;
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
                        }, throwable -> {
                            Timber.e(throwable, "Failed to get all artists from MusicStore");
                        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_library_page, container, false);
        mRecyclerView = view.findViewById(R.id.library_page_list);
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
        mArtistSection = null;
    }

    private void setupAdapter() {
        if (mRecyclerView == null || mArtists == null) {
            return;
        }

        if (mArtistSection != null) {
            mArtistSection.setData(mArtists);
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new HeterogeneousFastScrollAdapter();
            mAdapter.setHasStableIds(true);
            mRecyclerView.setAdapter(mAdapter);

            mArtistSection = new ArtistSection(mArtists, getContext(),
                    getFragmentManager(), mMusicStore, mPlaylistStore, mPlayerController);
            mAdapter.addSection(mArtistSection);
            mAdapter.setEmptyState(new LibraryEmptyState(getActivity(), mMusicStore, mPlaylistStore));
        }
    }
}
