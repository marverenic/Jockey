package com.marverenic.music.instances.section;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.instances.Song;
import com.marverenic.music.instances.viewholder.DragDropSongViewHolder;
import com.marverenic.music.instances.viewholder.QueueSongViewHolder;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.List;

public class QueueSection extends EditableSongSection {

    public static final int ID = 721;

    public QueueSection(List<Song> data) {
        super(ID, data);
    }

    @Override
    protected void onDrop(int from, int to) {
        if (from == to) return;

        // Calculate where the current song index is moving to
        final int nowPlayingIndex = PlayerController.getQueuePosition();
        int futureNowPlayingIndex;

        if (from == nowPlayingIndex) {
            futureNowPlayingIndex = to;
        } else if (from < nowPlayingIndex && to >= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex - 1;
        } else if (from > nowPlayingIndex && to <= nowPlayingIndex) {
            futureNowPlayingIndex = nowPlayingIndex + 1;
        } else {
            futureNowPlayingIndex = nowPlayingIndex;
        }

        // Push the change to the service
        PlayerController.editQueue(mData, futureNowPlayingIndex);
    }

    @Override
    public EnhancedViewHolder<Song> createViewHolder(final HeterogeneousAdapter adapter, ViewGroup parent) {
        return new QueueSongViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.instance_song_queue, parent, false),
                new DragDropSongViewHolder.OnRemovedListener() {
                    @Override
                    public void onItemRemoved(int index) {
                        mData.remove(index);

                        // Calculate the new song index of the track that's currently playing
                        int playingIndex = PlayerController.getQueuePosition();
                        if (index < playingIndex) {
                            playingIndex--;
                        }

                        // push the change to the service
                        PlayerController.editQueue(mData, playingIndex);

                        adapter.notifyItemRemoved(index);
                    }
                });
    }
}
