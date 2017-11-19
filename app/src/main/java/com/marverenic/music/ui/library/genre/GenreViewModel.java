package com.marverenic.music.ui.library.genre;

import android.content.Context;
import android.databinding.Bindable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutManager;

import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.SongSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import java.util.List;

public class GenreViewModel extends BaseViewModel {

    private FragmentManager mFragmentManager;
    private PlayerController mPlayerController;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PreferenceStore mPreferenceStore;

    private HeterogeneousAdapter mAdapter;
    private ShuffleAllSection mShuffleAllSection;
    private SongSection mSongSection;

    public GenreViewModel(Context context, FragmentManager fragmentManager,
                          PlayerController playerController, MusicStore musicStore,
                          PlaylistStore playlistStore, PreferenceStore preferenceStore) {
        super(context);
        mFragmentManager = fragmentManager;
        mPlayerController = playerController;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPreferenceStore = preferenceStore;

        createAdapter();
    }

    private void createAdapter() {
        mAdapter = new HeterogeneousAdapter();
        mAdapter.setEmptyState(new LibraryEmptyState(getContext(), mMusicStore, mPlaylistStore) {
            @Override
            public String getEmptyAction1Label() {
                return "";
            }
        });
    }

    public void setSongs(List<Song> genreContents) {
        if (mSongSection != null && mShuffleAllSection != null) {
            mSongSection.setData(genreContents);
            mShuffleAllSection.setData(genreContents);
            mAdapter.notifyDataSetChanged();
        } else {
            mSongSection = new SongSection(genreContents, mPlayerController, mMusicStore, mFragmentManager);
            mShuffleAllSection = new ShuffleAllSection(genreContents, mPreferenceStore, mPlayerController);
            mAdapter.addSection(mShuffleAllSection);
            mAdapter.addSection(mSongSection);
        }
    }

    @Bindable
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Bindable
    public ItemDecoration[] getItemDecorations() {
        return new ItemDecoration[] {
                new BackgroundDecoration(),
                new DividerDecoration(getContext(), R.id.empty_layout)
        };
    }

}
