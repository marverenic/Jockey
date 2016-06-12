package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.instances.Song;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.SongViewModel;

import java.util.List;

public class SongSection extends HeterogeneousAdapter.ListSection<Song> {

    public static final int ID = 9149;

    public SongSection(@NonNull List<Song> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                                  ViewGroup parent) {
        return ViewHolder.createViewHolder(parent, getData());
    }

    public static class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongBinding mBinding;

        public static ViewHolder createViewHolder(ViewGroup parent, List<Song> songList) {
            InstanceSongBinding binding = InstanceSongBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding, songList);
        }

        public ViewHolder(InstanceSongBinding binding, List<Song> songList) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(new SongViewModel(itemView.getContext(), songList));
        }

        @Override
        public void update(Song s, int sectionPosition) {
            mBinding.getViewModel().setIndex(sectionPosition);
        }
    }
}
