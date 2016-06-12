package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstanceAlbumBinding;
import com.marverenic.music.instances.Album;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.AlbumViewModel;

import java.util.List;

public class AlbumSection extends HeterogeneousAdapter.ListSection<Album> {

    public static final int ID = 7804;

    public AlbumSection(@NonNull List<Album> data) {
        super(ID, data);
    }

    @Override
    public EnhancedViewHolder<Album> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        return ViewHolder.create(parent);
    }

    public static class ViewHolder extends EnhancedViewHolder<Album> {

        private InstanceAlbumBinding mBinding;

        public static ViewHolder create(ViewGroup parent) {
            InstanceAlbumBinding binding = InstanceAlbumBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding);
        }

        public ViewHolder(InstanceAlbumBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new AlbumViewModel(itemView.getContext()));
        }

        @Override
        public void update(Album item, int sectionPosition) {
            mBinding.getViewModel().setAlbum(item);
        }
    }
}
