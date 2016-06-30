package com.marverenic.music.instances.section;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.instances.Song;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.SongViewModel;

import java.util.List;

public class SongSection extends HeterogeneousAdapter.ListSection<Song> {

    private FragmentManager mFragmentManager;

    public SongSection(AppCompatActivity activity, @NonNull List<Song> data) {
        this(activity.getSupportFragmentManager(), data);
    }

    public SongSection(Fragment fragment, @NonNull List<Song> data) {
        this(fragment.getFragmentManager(), data);
    }

    public SongSection(FragmentManager fragmentManager, @NonNull List<Song> data) {
        super(data);
        mFragmentManager = fragmentManager;
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

            Context context = itemView.getContext();
            binding.setViewModel(new SongViewModel(context, mFragmentManager, songList));
        }

        @Override
        public void onUpdate(Song s, int sectionPosition) {
            mBinding.getViewModel().setSong(getData(), sectionPosition);
        }
    }
}
