package com.marverenic.music.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.fragments.BaseFragment;
import com.marverenic.music.model.Song;
import com.marverenic.music.viewmodel.SongViewModel;

import java.util.List;

public class SongSection extends HeterogeneousAdapter.ListSection<Song> {

    private BaseActivity mActivity;
    private BaseFragment mFragment;

    public SongSection(BaseActivity activity, @NonNull List<Song> data) {
        super(data);
        mActivity = activity;
    }

    public SongSection(BaseFragment fragment, @NonNull List<Song> data) {
        super(data);
        mFragment = fragment;
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

        return new ViewHolder(binding, getData());
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongBinding mBinding;

        public ViewHolder(InstanceSongBinding binding, List<Song> songList) {
            super(binding.getRoot());
            mBinding = binding;

            if (mFragment != null) {
                binding.setViewModel(new SongViewModel(mFragment, songList));
            } else if (mActivity != null) {
                binding.setViewModel(new SongViewModel(mActivity, songList));
            } else {
                throw new RuntimeException("Unable to create view model. This SongSection has not "
                        + "been created with a valid activity or fragment");
            }
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
        }
    }
}
