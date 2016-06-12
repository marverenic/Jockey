package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceArtistBinding;
import com.marverenic.music.instances.Artist;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.ArtistViewModel;

import java.util.List;

public class ArtistSection extends HeterogeneousAdapter.ListSection<Artist> {

    public static final int ID = 3401;

    public ArtistSection(@NonNull List<Artist> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Artist> createViewHolder(HeterogeneousAdapter adapter,
                                                                    ViewGroup parent) {
        return ViewHolder.createViewHolder(parent);
    }

    public static class ViewHolder extends EnhancedViewHolder<Artist> {

        private InstanceArtistBinding mBinding;

        public static ViewHolder createViewHolder(ViewGroup parent) {
            InstanceArtistBinding binding = InstanceArtistBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding);
        }

        public ViewHolder(InstanceArtistBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new ArtistViewModel(itemView.getContext()));
        }

        @Override
        public void update(Artist item, int sectionPosition) {
            mBinding.getViewModel().setArtist(item);
        }
    }
}
