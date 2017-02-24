package com.marverenic.music.adapter;

import android.support.v4.util.ArrayMap;
import android.view.ViewGroup;

import com.marverenic.adapter.DragDropAdapter;
import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class EditableSongSection extends DragDropAdapter.DragSection<Song> {

    protected List<Song> mData;
    private final List<Integer> mIds;

    public EditableSongSection(List<Song> data) {
        mIds = new ArrayList<>();
        setData(data);
    }

    public void setData(List<Song> data) {
        mData = data;
        buildIdMap();
    }

    private void buildIdMap() {
        mIds.clear();
        Map<Song, Integer> occurrences = new ArrayMap<>();
        for (Song song : mData) {
            Integer count = occurrences.get(song);
            if (count == null) {
                count = 0;
            }

            int id = (int) (song.getSongId() * Math.pow(7, count));
            mIds.add(id);
            occurrences.put(song, ++count);
        }
    }

    public List<Song> getData() {
        return mData;
    }

    @Override
    public int getId(int position) {
        return mIds.get(position);
    }

    @Override
    public int getDragHandleId() {
        return R.id.handle;
    }

    @Override
    protected void onDrag(int from, int to) {
        // Avoid the array copy in ArrayList by swapping the items manually
        // Since the difference between from and to is never more than 1, we don't have
        // to worry about shifting other items in the List's backing array
        Song fromObject = mData.get(from);
        Song toObject = mData.get(to);

        mData.set(to, fromObject);
        mData.set(from, toObject);

        Integer fromId = mIds.get(from);
        Integer toId = mIds.get(to);

        mIds.set(to, fromId);
        mIds.set(from, toId);
    }

    @Override
    protected abstract void onDrop(int from, int to);

    @Override
    public abstract EnhancedViewHolder<Song> createViewHolder(HeterogeneousAdapter adapter,
                                                              ViewGroup parent);

    @Override
    public int getItemCount(HeterogeneousAdapter adapter) {
        return mData.size();
    }

    @Override
    public Song get(int position) {
        return mData.get(position);
    }
}
