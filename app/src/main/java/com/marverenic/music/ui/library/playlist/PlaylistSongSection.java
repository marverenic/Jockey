package com.marverenic.music.ui.library.playlist;

import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceSongDragBinding;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.EditableSongSection;

import java.util.List;

public class PlaylistSongSection extends EditableSongSection {

    private FragmentManager mFragmentManager;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    private Playlist mReference;

    public PlaylistSongSection(List<Song> data, Playlist reference, FragmentManager fragmentManager,
                               MusicStore musicStore, PlaylistStore playlistStore,
                               PlayerController playerController) {
        super(data);
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;

        mReference = reference;
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        mPlaylistStore.editPlaylist(mReference, mData);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(final HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {

        InstanceSongDragBinding binding = InstanceSongDragBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, adapter);
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongDragBinding mBinding;

        public ViewHolder(InstanceSongDragBinding binding, HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(
                    new PlaylistSongViewModel(mBinding.getRoot().getContext(), mFragmentManager,
                            mMusicStore, mPlayerController, () -> {
                                adapter.notifyDataSetChanged();
                                mPlaylistStore.editPlaylist(mReference, getData());
                            }));
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
        }
    }
}
