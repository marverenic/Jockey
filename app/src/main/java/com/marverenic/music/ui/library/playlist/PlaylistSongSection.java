package com.marverenic.music.ui.library.playlist;

import android.support.annotation.Nullable;
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
import com.marverenic.music.ui.common.OnSongSelectedListener;

import java.util.List;

import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class PlaylistSongSection extends EditableSongSection {

    private FragmentManager mFragmentManager;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;
    @Nullable private OnSongSelectedListener mSongListener;

    private BehaviorSubject<Song> mCurrentSong;
    private Playlist mReference;

    public PlaylistSongSection(List<Song> data, Playlist reference, FragmentManager fragmentManager,
                               MusicStore musicStore, PlaylistStore playlistStore,
                               PlayerController playerController,
                               @Nullable OnSongSelectedListener songSelectedListener) {
        super(data);
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
        mSongListener = songSelectedListener;

        mReference = reference;
        mCurrentSong = BehaviorSubject.create();
    }

    public void setCurrentSong(Song nowPlaying) {
        mCurrentSong.onNext(nowPlaying);
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
            PlaylistSongItemViewModel viewModel = new PlaylistSongItemViewModel(mBinding.getRoot().getContext(),
                    mFragmentManager, mMusicStore, mPlayerController, () -> {
                adapter.notifyDataSetChanged();
                mPlaylistStore.editPlaylist(mReference, getData());
            }, mSongListener);

            mCurrentSong.subscribe(viewModel::setCurrentlyPlayingSong, throwable -> {
                Timber.e(throwable, "Failed to update current song in view model");
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
