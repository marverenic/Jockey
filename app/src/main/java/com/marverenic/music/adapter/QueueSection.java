package com.marverenic.music.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.databinding.InstanceSongQueueBinding;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.viewmodel.QueueSongViewModel;

import java.util.List;

import timber.log.Timber;

public class QueueSection extends EditableSongSection {

    private FragmentManager mFragmentManager;
    private PlayerController mPlayerController;

    public QueueSection(Fragment fragment, PlayerController playerController, List<Song> data) {
        this(fragment.getFragmentManager(), playerController, data);
    }

    public QueueSection(FragmentManager fragmentManager, PlayerController playerController,
                        List<Song> data) {
        super(data);
        mPlayerController = playerController;
        mFragmentManager = fragmentManager;
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        // Calculate where the current song index is moving to
        mPlayerController.getQueuePosition().take(1).subscribe(nowPlayingIndex -> {
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
            mPlayerController.editQueue(mData, futureNowPlayingIndex);
        }, throwable -> {
            Timber.e(throwable, "Failed to drop queue item");
        });
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
