package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceGenreBinding;
import com.marverenic.music.instances.Genre;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.GenreViewModel;

import java.util.List;

public class GenreSection extends HeterogeneousAdapter.ListSection<Genre> {

    public static final int ID = 9267;

    public GenreSection(@NonNull List<Genre> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Genre> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        return ViewHolder.createViewHolder(parent);
    }

    public static class ViewHolder extends EnhancedViewHolder<Genre> {

        private InstanceGenreBinding mBinding;

        public static ViewHolder createViewHolder(ViewGroup parent) {
            InstanceGenreBinding binding = InstanceGenreBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding);
        }

        public ViewHolder(InstanceGenreBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new GenreViewModel(itemView.getContext()));
        }

        @Override
        public void update(Genre item, int sectionPosition) {
            mBinding.getViewModel().setGenre(item);
        }
    }
}
