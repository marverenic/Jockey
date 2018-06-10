package com.marverenic.music.ui.library.playlist.contents.edit;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceRuleBinding;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;

import java.util.List;

public class RuleSection extends HeterogeneousAdapter.ListSection<AutoPlaylistRule> {

    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;

    public RuleSection(@NonNull List<AutoPlaylistRule> data, MusicStore musicStore,
                       PlaylistStore playlistStore) {
        super(data);
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
    }

    @Override
    public EnhancedViewHolder<AutoPlaylistRule> createViewHolder(HeterogeneousAdapter adapter,
                                                                 ViewGroup parent) {
        InstanceRuleBinding binding = InstanceRuleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, adapter);
    }

    @Override
    public int getId(int position) {
        return get(position).hashCode();
    }

    public class ViewHolder extends EnhancedViewHolder<AutoPlaylistRule> {

        private InstanceRuleBinding mBinding;

        public ViewHolder(InstanceRuleBinding binding, HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            RuleViewModel viewModel = new RuleViewModel(itemView.getContext(), mMusicStore, mPlaylistStore);
            viewModel.setOnRemovalListener(index -> {
                AutoPlaylistRule removed = getData().remove(index);
                adapter.notifyDataSetChanged();

                Snackbar.make(itemView, R.string.confirm_removed_rule, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.action_undo, v -> {
                            getData().add(index, removed);
                            adapter.notifyDataSetChanged();
                        })
                        .show();
            });

            mBinding.setViewModel(viewModel);
        }

        @Override
        public void onUpdate(AutoPlaylistRule rule, int position) {
            mBinding.getViewModel().setRule(getData(), position);
        }
    }

}
