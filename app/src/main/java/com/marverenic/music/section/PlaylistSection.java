package com.marverenic.music.section;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.databinding.InstancePlaylistBinding;
import com.marverenic.music.instances.Playlist;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.PlaylistViewModel;

import java.util.List;

public class PlaylistSection extends HeterogeneousAdapter.ListSection<Playlist> {

    public PlaylistSection(@NonNull List<Playlist> data) {
        super(data);
    }

    @Override
    public EnhancedViewHolder<Playlist> createViewHolder(HeterogeneousAdapter adapter,
                                                         ViewGroup parent) {
        return ViewHolder.createViewHolder(parent);
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getPlaylistId();
    }

    public static class ViewHolder extends EnhancedViewHolder<Playlist> {

        private InstancePlaylistBinding mBinding;

        public static ViewHolder createViewHolder(ViewGroup parent) {
            InstancePlaylistBinding binding = InstancePlaylistBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);

            return new ViewHolder(binding);
        }

        public ViewHolder(InstancePlaylistBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new PlaylistViewModel(itemView.getContext()));
        }

        @Override
        public void onUpdate(Playlist item, int sectionPosition) {
            mBinding.getViewModel().setPlaylist(item);
        }
    }
}
