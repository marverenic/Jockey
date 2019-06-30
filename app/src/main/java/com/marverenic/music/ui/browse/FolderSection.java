package com.marverenic.music.ui.browse;

import androidx.databinding.DataBindingUtil;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.databinding.InstanceFolderBinding;

import java.io.File;
import java.util.List;

public class FolderSection extends HeterogeneousAdapter.ListSection<File> {

    @Nullable
    private FolderViewModel.OnFolderSelectedListener mSelectionListener;

    public FolderSection(@NonNull List<File> data,
                         @Nullable FolderViewModel.OnFolderSelectedListener selectionListener) {
        super(data);
        mSelectionListener = selectionListener;
    }

    @Override
    public int getId(int position) {
        return get(position).hashCode();
    }

    @Override
    public EnhancedViewHolder<File> createViewHolder(HeterogeneousAdapter adapter, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(
                DataBindingUtil.inflate(inflater, R.layout.instance_folder, parent, false));
    }

    private class ViewHolder extends EnhancedViewHolder<File> {

        private InstanceFolderBinding mBinding;
        private FolderViewModel mViewModel;

        public ViewHolder(InstanceFolderBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mViewModel = new FolderViewModel();
            mViewModel.setSelectionListener(mSelectionListener);
            mBinding.setViewModel(mViewModel);
        }

        @Override
        public void onUpdate(File item, int position) {
            mViewModel.setFolder(item);
            mBinding.executePendingBindings();
        }
    }
}
