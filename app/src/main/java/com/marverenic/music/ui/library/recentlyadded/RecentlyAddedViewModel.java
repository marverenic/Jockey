package com.marverenic.music.ui.library.recentlyadded;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.marverenic.music.ui.library.song.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.Collections;
import java.util.List;

public class RecentlyAddedViewModel extends BaseViewModel {

    private HeterogeneousAdapter mAdapter;
    private SongSection mSongSection;

    public RecentlyAddedViewModel(Context context, PlayerController playerController,
                                  MusicStore musicStore, PlaylistStore playlistStore,
                                  FragmentManager fragmentManager,
                                  OnSongSelectedListener songSelectedListener) {
        super(context);

        mAdapter = new HeterogeneousAdapter();
        mSongSection = new SongSection(Collections.emptyList(), playerController, musicStore,
                fragmentManager, songSelectedListener);

        mAdapter.addSection(mSongSection);
        mAdapter.setEmptyState(new LibraryEmptyState(context, musicStore, playlistStore) {
            @Override
            public String getEmptyMessageDetail() {
                return getString(R.string.empty_recently_added_detail);
            }

            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });
    }

    public RecyclerView.Adapter<?> getAdapter() {
        return mAdapter;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    public RecyclerView.ItemDecoration[] getItemDecorations() {
        return new RecyclerView.ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext(), R.id.empty_layout)
        };
    }

}
