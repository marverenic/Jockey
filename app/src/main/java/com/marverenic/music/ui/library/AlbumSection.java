package com.marverenic.music.ui.library;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.databinding.InstanceAlbumBinding;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.player.PlayerController;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

public class AlbumSection extends HeterogeneousAdapter.ListSection<Album>
        implements FastScrollRecyclerView.SectionedAdapter {

    private FragmentManager mFragmentManager;
    private MusicStore mMusicStore;
    private PlayerController mPlayerController;

    public AlbumSection(@NonNull List<Album> data, MusicStore musicStore,
                        PlayerController playerController, FragmentManager fragmentManager) {
        super(data);
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlayerController = playerController;
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
            mBinding.setViewModel(new AlbumViewModel(itemView.getContext(), mFragmentManager,
                    mMusicStore, mPlayerController));
        }

        @Override
        public void onUpdate(Album item, int sectionPosition) {
            mBinding.getViewModel().setAlbum(item);
            mBinding.executePendingBindings();
        }
    }
}
