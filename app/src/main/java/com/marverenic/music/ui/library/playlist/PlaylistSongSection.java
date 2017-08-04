package com.marverenic.music.ui.library.playlist;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceSongDragBinding;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.ui.common.EditableSongSection;

import java.util.List;

public class PlaylistSongSection extends EditableSongSection {

    private BaseFragment mFragment;
    private PlaylistStore mPlaylistStore;
    private Playlist mReference;

    public PlaylistSongSection(BaseFragment fragment, PlaylistStore playlistStore,
                               List<Song> data, Playlist reference) {
        super(data);
        mFragment = fragment;
        mPlaylistStore = playlistStore;
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

        return new ViewHolder(binding, getData(), adapter);
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongDragBinding mBinding;

        public ViewHolder(InstanceSongDragBinding binding, List<Song> songList,
                          HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(
                    new PlaylistSongViewModel(mFragment, songList,
                            () -> {
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
