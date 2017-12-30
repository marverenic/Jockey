package com.marverenic.music.ui.library;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.common.OnSongSelectedListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

import java.util.List;

import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class SongSection extends HeterogeneousAdapter.ListSection<Song>
        implements SectionedAdapter, MeasurableAdapter {

    private PlayerController mPlayerController;
    private MusicStore mMusicStore;
    private FragmentManager mFragmentManager;
    @Nullable private OnSongSelectedListener mSongListener;

    private BehaviorSubject<Song> mCurrentSong;

    public SongSection(@NonNull List<Song> data, PlayerController playerController,
                       MusicStore musicStore, FragmentManager fragmentManager,
                       @Nullable OnSongSelectedListener songSelectedListener) {
        super(data);
        mMusicStore = musicStore;
        mPlayerController = playerController;
        mFragmentManager = fragmentManager;
        mSongListener = songSelectedListener;

        mCurrentSong = BehaviorSubject.create();
    }

    public void setCurrentSong(Song nowPlaying) {
        mCurrentSong.onNext(nowPlaying);
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getSongId();
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        InstanceSongBinding binding = InstanceSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        char firstChar = ModelUtil.sortableTitle(get(position).getSongName()).charAt(0);
        return Character.toString(firstChar).toUpperCase();
    }

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, int viewType) {
        return recyclerView.getResources().getDimensionPixelSize(R.dimen.list_height)
                + recyclerView.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongBinding mBinding;

        public ViewHolder(InstanceSongBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            SongViewModel viewModel = new SongViewModel(binding.getRoot().getContext(),
                    mFragmentManager, mMusicStore, mPlayerController, mSongListener);
            binding.setViewModel(viewModel);

            mCurrentSong.subscribe(viewModel::setCurrentlyPlayingSong, throwable -> {
                Timber.e("Failed to set current song", throwable);
            });
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
            mBinding.executePendingBindings();
        }
    }
}
