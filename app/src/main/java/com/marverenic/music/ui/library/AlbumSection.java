package com.marverenic.music.ui.library;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.databinding.InstanceAlbumBinding;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.ui.library.AlbumViewModel;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

public class AlbumSection extends HeterogeneousAdapter.ListSection<Album>
        implements FastScrollRecyclerView.SectionedAdapter {

    private FragmentManager mFragmentManager;

    public AlbumSection(AppCompatActivity activity, @NonNull List<Album> data) {
        this(activity.getSupportFragmentManager(), data);
    }

    public AlbumSection(Fragment fragment, @NonNull List<Album> data) {
        this(fragment.getFragmentManager(), data);
    }

    public AlbumSection(FragmentManager fragmentManager, @NonNull List<Album> data) {
        super(data);
        mFragmentManager = fragmentManager;
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getAlbumId();
    }

    @Override
    public EnhancedViewHolder<Album> createViewHolder(HeterogeneousAdapter adapter,
                                                      ViewGroup parent) {
        InstanceAlbumBinding binding = InstanceAlbumBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        char firstChar = ModelUtil.sortableTitle(get(position).getAlbumName()).charAt(0);
        return Character.toString(firstChar).toUpperCase();
    }

    private class ViewHolder extends EnhancedViewHolder<Album> {

        private InstanceAlbumBinding mBinding;

        public ViewHolder(InstanceAlbumBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new AlbumViewModel(itemView.getContext(), mFragmentManager));
        }

        @Override
        public void onUpdate(Album item, int sectionPosition) {
            mBinding.getViewModel().setAlbum(item);
            mBinding.executePendingBindings();
        }
    }
}
