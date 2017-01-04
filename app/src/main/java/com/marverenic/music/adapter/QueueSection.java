package com.marverenic.music.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceSongQueueBinding;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.OldPlayerController;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.QueueSongViewModel;

import java.util.List;

public class QueueSection extends EditableSongSection {

    private FragmentManager mFragmentManager;

    public QueueSection(AppCompatActivity activity, List<Song> data) {
        this(activity.getSupportFragmentManager(), data);
    }

    public QueueSection(Fragment fragment, List<Song> data) {
        this(fragment.getFragmentManager(), data);
    }

    public QueueSection(FragmentManager fragmentManager, List<Song> data) {
        super(data);
        mFragmentManager = fragmentManager;
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        // Calculate where the current song index is moving to
        final int nowPlayingIndex = OldPlayerController.getQueuePosition();
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
        OldPlayerController.editQueue(mData, futureNowPlayingIndex);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        InstanceSongQueueBinding binding = InstanceSongQueueBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, getData(), adapter);
    }

    public class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongQueueBinding mBinding;

        public ViewHolder(InstanceSongQueueBinding binding, List<Song> songList,
                          HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(new QueueSongViewModel(itemView.getContext(), mFragmentManager,
                    songList, adapter::notifyDataSetChanged));
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
        }
    }
}
