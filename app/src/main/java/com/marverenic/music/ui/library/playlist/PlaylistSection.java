package com.marverenic.music.ui.library.playlist;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstancePlaylistBinding;
import com.marverenic.music.model.ModelUtil;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.player.PlayerController;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

import java.util.List;

public class PlaylistSection extends HeterogeneousAdapter.ListSection<Playlist>
        implements SectionedAdapter, MeasurableAdapter {

    private PlaylistStore mPlaylistStore;
    private PlayerController mPlayerController;

    public PlaylistSection(@NonNull List<Playlist> data, PlaylistStore playlistStore,
                           PlayerController playerController) {
        super(data);
        mPlaylistStore = playlistStore;
        mPlayerController = playerController;
    }

    @Override
    public EnhancedViewHolder<Playlist> createViewHolder(HeterogeneousAdapter adapter,
                                                         ViewGroup parent) {
        InstancePlaylistBinding binding = InstancePlaylistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public int getId(int position) {
        return (int) get(position).getPlaylistId();
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        char firstChar = ModelUtil.sortableTitle(get(position).getPlaylistName()).charAt(0);
        return Character.toString(firstChar).toUpperCase();
    }

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, int viewType) {
        return recyclerView.getResources().getDimensionPixelSize(R.dimen.list_height)
                + recyclerView.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    private class ViewHolder extends EnhancedViewHolder<Playlist> {

        private InstancePlaylistBinding mBinding;

        public ViewHolder(InstancePlaylistBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setViewModel(new PlaylistItemViewModel(itemView.getContext(),
                    mPlaylistStore, mPlayerController));
        }

        @Override
        public void onUpdate(Playlist item, int sectionPosition) {
            mBinding.getViewModel().setPlaylist(item);
            mBinding.executePendingBindings();
        }
    }
}
