package com.marverenic.music.instances.section;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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

    private FragmentManager mFragmentManager;

    public AlbumSection(AppCompatActivity activity, @NonNull List<Album> data) {
        this(activity.getSupportFragmentManager(), data);
    }

    public AlbumSection(Fragment fragment, @NonNull List<Album> data) {
        this(fragment.getFragmentManager(), data);
    }

    public AlbumSection(FragmentManager fragmentManager, @NonNull List<Album> data) {
        super(ID, data);
        mFragmentManager = fragmentManager;
    }

    @Override
    public EnhancedViewHolder<Album> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        InstanceAlbumBinding binding = InstanceAlbumBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    private class ViewHolder extends EnhancedViewHolder<Album> {

        private InstanceAlbumBinding mBinding;

        public ViewHolder(InstanceAlbumBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new AlbumViewModel(itemView.getContext(), mFragmentManager));
        }

        @Override
        public void update(Album item, int sectionPosition) {
            mBinding.getViewModel().setAlbum(item);
        }
    }
}
