package com.marverenic.music.instances.section;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceSongQueueBinding;
import com.marverenic.music.instances.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.QueueSongViewModel;

import java.util.List;

public class QueueSection extends EditableSongSection {

    public static final int ID = 721;

    public QueueSection(List<Song> data) {
        super(ID, data);
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        // Calculate where the current song index is moving to
        final int nowPlayingIndex = PlayerController.getQueuePosition();
        int futureNowPlayingIndex;

        if (from == nowPlayingIndex) {
            futureNowPlayingIndex = to;
        } else if (from < nowPlayingIndex && to >= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex - 1;
        } else if (from > nowPlayingIndex && to <= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex + 1;
        } else {
            futureNowPlayingIndex = nowPlayingIndex;
        }

        // Push the change to the service
        PlayerController.editQueue(mData, futureNowPlayingIndex);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        return ViewHolder.createViewHolder(adapter, parent, getData());
    }

    private static class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongQueueBinding mBinding;

        public static ViewHolder createViewHolder(HeterogeneousAdapter adapter, ViewGroup parent,
                                                  List<Song> songList) {
            InstanceSongQueueBinding binding = InstanceSongQueueBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding, songList, adapter);
        }

        public ViewHolder(InstanceSongQueueBinding binding, List<Song> songList,
                          HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(new QueueSongViewModel(itemView.getContext(), songList,
                    adapter::notifyDataSetChanged));
        }

        @Override
        public void update(Song s, int sectionPosition) {
            mBinding.getViewModel().setIndex(sectionPosition);
        }
    }
}
