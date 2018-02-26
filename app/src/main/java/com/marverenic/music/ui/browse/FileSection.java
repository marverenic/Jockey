package com.marverenic.music.ui.browse;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.databinding.InstanceFileBinding;

import java.io.File;
import java.util.List;

public class FileSection extends HeterogeneousAdapter.ListSection<File> {

    private ThumbnailLoader mThumbnailLoader;
    private FileViewModel.OnFileSelectedListener mSelectionListener;

    public FileSection(@NonNull List<File> data,
                       @NonNull FileViewModel.OnFileSelectedListener selectionListener,
                       @NonNull ThumbnailLoader thumbnailLoader) {
        super(data);
        mSelectionListener = selectionListener;
        mThumbnailLoader = thumbnailLoader;
    }

    @Override
    public int getId(int position) {
        return get(position).hashCode();
    }

    @Override
    public EnhancedViewHolder<File> createViewHolder(HeterogeneousAdapter adapter, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(
                DataBindingUtil.inflate(inflater, R.layout.instance_file, parent, false));
    }

    private class ViewHolder extends EnhancedViewHolder<File> {

        private InstanceFileBinding mBinding;
        private FileViewModel mViewModel;

        public ViewHolder(InstanceFileBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mViewModel = new FileViewModel(itemView.getContext(), mThumbnailLoader);
            mViewModel.setFileSelectionListener(mSelectionListener);

            mBinding.setViewModel(mViewModel);
        }

        @Override
        public void onUpdate(File item, int position) {
            mViewModel.setFile(item);
            mBinding.executePendingBindings();
        }
    }

}
