package com.marverenic.music.ui.library.playlist.contents.edit;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.adapter.HeterogeneousAdapter.SingletonSection;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceRulesHeaderBinding;
import com.marverenic.music.model.AutoPlaylist;

public class RuleHeaderSingleton extends SingletonSection<AutoPlaylist.Builder> {

    private AutoPlaylist mOriginalPlaylist;
    private PlaylistStore mPlaylistStore;

    public RuleHeaderSingleton(AutoPlaylist playlist, AutoPlaylist.Builder editor,
                               PlaylistStore playlistStore) {
        super(editor);
        mOriginalPlaylist = playlist;
        mPlaylistStore = playlistStore;
    }

    @Override
    public EnhancedViewHolder<AutoPlaylist.Builder> createViewHolder(HeterogeneousAdapter adapter,
                                                                     ViewGroup parent) {
        InstanceRulesHeaderBinding binding = InstanceRulesHeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    private class ViewHolder extends EnhancedViewHolder<AutoPlaylist.Builder> {

        private InstanceRulesHeaderBinding mBinding;
        private RuleHeaderViewModel mViewModel;

        public ViewHolder(InstanceRulesHeaderBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            mViewModel = new RuleHeaderViewModel(mPlaylistStore);
            mViewModel.setOriginalReference(mOriginalPlaylist);

            mBinding.setViewModel(mViewModel);
        }

        @Override
        public void onUpdate(AutoPlaylist.Builder item, int position) {
            mViewModel.setBuilder(item);
        }
    }
}
