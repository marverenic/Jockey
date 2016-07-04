package com.marverenic.music.instances.section;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter.SingletonSection;
import com.marverenic.music.databinding.InstanceRulesHeaderBinding;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.viewmodel.RuleHeaderViewModel;

public class RuleHeaderSingleton extends SingletonSection<AutoPlaylist.Builder> {

    public RuleHeaderSingleton(AutoPlaylist.Builder editor) {
        super(editor);
    }

    @Override
    public EnhancedViewHolder<AutoPlaylist.Builder> createViewHolder(HeterogeneousAdapter adapter,
                                                                     ViewGroup parent) {
        InstanceRulesHeaderBinding binding = InstanceRulesHeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    private static class ViewHolder extends EnhancedViewHolder<AutoPlaylist.Builder> {

        private InstanceRulesHeaderBinding mBinding;
        private RuleHeaderViewModel mViewModel;

        public ViewHolder(InstanceRulesHeaderBinding binding) {
            super(binding.getRoot());
            mBinding = binding;

            mViewModel = new RuleHeaderViewModel(itemView.getContext());
            mBinding.setViewModel(mViewModel);
        }

        @Override
        public void onUpdate(AutoPlaylist.Builder item, int position) {
            mViewModel.setBuilder(item);
        }
    }
}
