package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.databinding.InstanceRuleBinding;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.viewmodel.RuleViewModel;

import java.util.List;

public class RuleSection extends HeterogeneousAdapter.ListSection<AutoPlaylistRule> {

    public RuleSection(@NonNull List<AutoPlaylistRule> data) {
        super(data);
    }

    @Override
    public EnhancedViewHolder<AutoPlaylistRule> createViewHolder(HeterogeneousAdapter adapter,
                                                                 ViewGroup parent) {
        InstanceRuleBinding binding = InstanceRuleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, adapter);
    }

    public class ViewHolder extends EnhancedViewHolder<AutoPlaylistRule> {

        private InstanceRuleBinding mBinding;

        public ViewHolder(InstanceRuleBinding binding, RecyclerView.Adapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            RuleViewModel viewModel = new RuleViewModel(itemView.getContext());
            viewModel.setOnRemovalListener(adapter::notifyItemRemoved);

            mBinding.setViewModel(viewModel);
        }

        @Override
        public void onUpdate(AutoPlaylistRule rule, int position) {
            mBinding.getViewModel().setRule(getData(), position);
        }
    }

}
