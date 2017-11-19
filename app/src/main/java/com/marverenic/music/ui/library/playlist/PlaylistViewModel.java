package com.marverenic.music.ui.library.playlist;

import android.content.Context;
import android.content.Intent;
import android.databinding.Bindable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.view.View;

import com.marverenic.adapter.DragDropAdapter;
import com.marverenic.adapter.DragDropDecoration;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseViewModel;
import com.marverenic.music.ui.common.LibraryEmptyState;
import com.marverenic.music.ui.common.ShuffleAllSection;
import com.marverenic.music.ui.library.playlist.edit.AutoPlaylistEditActivity;
import com.marverenic.music.view.DragBackgroundDecoration;
import com.marverenic.music.view.DragDividerDecoration;

import java.util.List;

public class PlaylistViewModel extends BaseViewModel {

    private FragmentManager mFragmentManager;

    private PlayerController mPlayerController;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PreferenceStore mPreferenceStore;

    private Playlist mPlaylist;

    private DragDropAdapter mAdapter;
    private PlaylistSongSection mSongSection;
    private ShuffleAllSection mShuffleAllSection;

    public PlaylistViewModel(Context context, FragmentManager fragmentManager,
                             PlayerController playerController, MusicStore musicStore,
                             PlaylistStore playlistStore, PreferenceStore preferenceStore,
                             Playlist playlist) {
        super(context);
        mFragmentManager = fragmentManager;
        mPlayerController = playerController;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPreferenceStore = preferenceStore;
        mPlaylist = playlist;

        createAdapter();
    }

    private void createAdapter() {
        mAdapter = new DragDropAdapter();
        mAdapter.setHasStableIds(true);

        mAdapter.setEmptyState(new LibraryEmptyState(getContext(), mMusicStore, mPlaylistStore) {
            @Override
            public String getEmptyMessage() {
                if (mPlaylist instanceof AutoPlaylist) {
                    return getString(R.string.empty_auto_playlist);
                } else {
                    return getString(R.string.empty_playlist);
                }
            }

            @Override
            public String getEmptyMessageDetail() {
                if (mPlaylist instanceof AutoPlaylist) {
                    return getString(R.string.empty_auto_playlist_detail);
                } else {
                    return getString(R.string.empty_playlist_detail);
                }
            }

            @Override
            public String getEmptyAction1Label() {
                if (mPlaylist instanceof AutoPlaylist) {
                    return getString(R.string.action_edit_playlist_rules);
                } else {
                    return "";
                }
            }

            @Override
            public void onAction1(View button) {
                if (mPlaylist instanceof AutoPlaylist) {
                    AutoPlaylist playlist = (AutoPlaylist) mPlaylist;
                    Intent intent = AutoPlaylistEditActivity.newIntent(getContext(), playlist);

                    getContext().startActivity(intent);
                }
            }
        });
    }

    public void setSongs(List<Song> playlistSongs) {
        if (mSongSection == null || mShuffleAllSection == null) {
            mSongSection = new PlaylistSongSection(playlistSongs, mPlaylist, mFragmentManager,
                    mMusicStore, mPlaylistStore, mPlayerController);
            mShuffleAllSection = new ShuffleAllSection(playlistSongs, mPreferenceStore, mPlayerController);
            mAdapter.addSection(mShuffleAllSection);
            mAdapter.setDragSection(mSongSection);
        } else {
            mShuffleAllSection.setData(playlistSongs);
            mSongSection.setData(playlistSongs);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Bindable
    public DragDropAdapter getAdapter() {
        return mAdapter;
    }

    @Bindable
    public LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Bindable
    public ItemDecoration[] getItemDecorations() {
        NinePatchDrawable dragShadow = (NinePatchDrawable) ContextCompat.getDrawable(
                getContext(), R.drawable.list_drag_shadow);

        return new ItemDecoration[] {
                new DragBackgroundDecoration(R.id.song_drag_root),
                new DragDividerDecoration(R.id.song_drag_root, getContext(), R.id.empty_layout),
                new DragDropDecoration(dragShadow)
        };
    }

}
