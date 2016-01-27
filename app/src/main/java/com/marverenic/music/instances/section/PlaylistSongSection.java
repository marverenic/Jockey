package com.marverenic.music.instances.section;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.Playlist;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.DragDropSongViewHolder;
import com.marverenic.music.instances.viewholder.PlaylistSongViewHolder;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

public class PlaylistSongSection extends EditableSongSection {

    public static final int ID = 720;

    private Context mContext;
    private DragDropSongViewHolder.OnRemovedListener mRemovedListener;
    private Playlist mReference;

    public PlaylistSongSection(List<Song> data, Context context,
                               DragDropSongViewHolder.OnRemovedListener onRemovedListener,
                               Playlist reference) {
        super(ID, data);
        mContext = context;
        mRemovedListener = onRemovedListener;
        mReference = reference;
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        Library.editPlaylist(mContext, mReference, mData);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(final HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        return new PlaylistSongViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_song_drag, parent, false),
                mData,
                mReference,
                mRemovedListener);
    }
}
