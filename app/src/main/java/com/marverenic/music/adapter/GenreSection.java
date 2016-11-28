package com.marverenic.music.adapter;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceGenreBinding;
import com.marverenic.music.instances.Genre;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.GenreViewModel;

import java.util.List;

public class GenreSection extends HeterogeneousAdapter.ListSection<Genre> {

    private FragmentManager mFragmentManager;

    public GenreSection(AppCompatActivity activity, @NonNull List<Genre> data) {
        this(activity.getSupportFragmentManager(), data);
    }

    public GenreSection(Fragment fragment, @NonNull List<Genre> data) {
        this(fragment.getFragmentManager(), data);
    }

    public GenreSection(FragmentManager fragmentManager, @NonNull List<Genre> data) {
        super(data);
        mFragmentManager = fragmentManager;
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getGenreId();
    }

    @Override
    public EnhancedViewHolder<Genre> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        InstanceGenreBinding binding = InstanceGenreBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    private class ViewHolder extends EnhancedViewHolder<Genre> {

        private InstanceGenreBinding mBinding;

        public ViewHolder(InstanceGenreBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new GenreViewModel(itemView.getContext(), mFragmentManager));
        }

        @Override
        public void onUpdate(Genre item, int sectionPosition) {
            mBinding.getViewModel().setGenre(item);
        }
    }
}
