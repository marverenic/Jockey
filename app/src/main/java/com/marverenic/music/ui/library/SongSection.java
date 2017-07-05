package com.marverenic.music.ui.library;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.ui.BaseActivity;
import com.marverenic.music.databinding.InstanceSongBinding;
import com.marverenic.music.ui.BaseFragment;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.model.Song;
import com.marverenic.music.ui.library.SongViewModel;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

import java.util.List;

public class SongSection extends HeterogeneousAdapter.ListSection<Song>
        implements SectionedAdapter, MeasurableAdapter {

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
            mBinding.executePendingBindings();
        }
    }
}
