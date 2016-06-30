package com.marverenic.music.instances.section;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.databinding.InstanceSongDragBinding;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.viewmodel.PlaylistSongViewModel;

import java.util.List;

public class PlaylistSongSection extends EditableSongSection {

    public static final int ID = 720;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private PlaylistStore mPlaylistStore;
    private Playlist mReference;

    public PlaylistSongSection(AppCompatActivity activity, PlaylistStore playlistStore,
                               List<Song> data, Playlist reference) {
        this(activity, activity.getSupportFragmentManager(), playlistStore, data, reference);
    }

    public PlaylistSongSection(Fragment fragment, PlaylistStore playlistStore, List<Song> data,
                               Playlist reference) {
        this(fragment.getContext(), fragment.getFragmentManager(), playlistStore, data, reference);
    }

    public PlaylistSongSection(Context context, FragmentManager fragmentManager,
                               PlaylistStore playlistStore, List<Song> data,
                               Playlist reference) {
        super(ID, data);
        mContext = context;
        mFragmentManager = fragmentManager;
        mPlaylistStore = playlistStore;
        mReference = reference;
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        mPlaylistStore.editPlaylist(mReference, mData);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(final HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {

        InstanceSongDragBinding binding = InstanceSongDragBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);

        return new ViewHolder(binding, getData(), adapter);
    }

    private class ViewHolder extends EnhancedViewHolder<Song> {

        private InstanceSongDragBinding mBinding;

        public ViewHolder(InstanceSongDragBinding binding, List<Song> songList,
                          HeterogeneousAdapter adapter) {
            super(binding.getRoot());
            mBinding = binding;

            binding.setViewModel(
                    new PlaylistSongViewModel(itemView.getContext(), mFragmentManager, songList,
                            () -> {
                                adapter.notifyDataSetChanged();
                                mPlaylistStore.editPlaylist(mReference, getData());
                            }));
        }

        @Override
        public void update(Song s, int sectionPosition) {
            mBinding.getViewModel().setIndex(sectionPosition);
        }
    }
}
