package com.marverenic.music.ui.nowplaying;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceSongQueueBinding;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.EditableSongSection;
import com.marverenic.music.ui.common.OnSongSelectedListener;

import java.util.List;

import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class QueueSection extends EditableSongSection {

    private FragmentManager mFragmentManager;

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;
    @Nullable private OnSongSelectedListener mSongListener;

    private BehaviorSubject<Integer> mCurrentIndex;

    public QueueSection(List<Song> data, FragmentManager fragmentManager,
                        MusicStore musicStore, PlaylistStore playlistStore,
                        PlayerController playerController,
                        @Nullable OnSongSelectedListener songSelectedListener) {
        super(data);
        mFragmentManager = fragmentManager;

        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
        mSongListener = songSelectedListener;
        mCurrentIndex = BehaviorSubject.create();
    }

    public void setCurrentSongIndex(int nowPlayingIndex) {
        mCurrentIndex.onNext(nowPlayingIndex);
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

        return new ViewHolder(binding, adapter);
    }

    public class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongQueueBinding mBinding;

        public ViewHolder(InstanceSongQueueBinding binding, HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;
            QueueSongItemViewModel viewModel = new QueueSongItemViewModel(mBinding.getRoot().getContext(),
                    mFragmentManager, mMusicStore, mPlaylistStore, mPlayerController,
                    adapter::notifyDataSetChanged, mSongListener);

            mCurrentIndex.subscribe(viewModel::setCurrentlyPlayingSongIndex, throwable -> {
                Timber.e(throwable, "Failed to update current song index in view model");
            });
            binding.setViewModel(viewModel);
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
            mBinding.executePendingBindings();
        }
    }
}
