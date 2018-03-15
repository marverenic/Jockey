package com.marverenic.music.ui.library.album;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceAlbumBinding;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.player.PlayerController;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.List;

public class AlbumSection extends HeterogeneousAdapter.ListSection<Album>
        implements FastScrollRecyclerView.SectionedAdapter {

    private Context mContext;
    private FragmentManager mFragmentManager;
    private MusicStore mMusicStore;
    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    public AlbumSection(@NonNull List<Album> data, Context context, MusicStore musicStore,
                        PlaylistStore playlistStore, PlayerController playerController,
                        FragmentManager fragmentManager) {
        super(data);
        mContext = context;
        mFragmentManager = fragmentManager;
        mMusicStore = musicStore;
        mPlaylistStore = playlistStore;
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
        String title = ModelUtil.sortableTitle(get(position).getAlbumName(), mContext.getResources());
        return Character.toString(title.charAt(0)).toUpperCase();
    }

    private class ViewHolder extends EnhancedViewHolder<Album> {

        private InstanceAlbumBinding mBinding;

        public ViewHolder(InstanceAlbumBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new AlbumItemViewModel(itemView.getContext(), mFragmentManager,
                    mMusicStore, mPlaylistStore, mPlayerController));
        }

        @Override
        public void onUpdate(Album item, int sectionPosition) {
            mBinding.getViewModel().setAlbum(item);
            mBinding.executePendingBindings();
        }
    }
}
