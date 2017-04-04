package com.marverenic.music.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.model.Song;

import java.util.List;

public class ShuffleAllSection extends HeterogeneousAdapter.SingletonSection<List<Song>> {

    public ShuffleAllSection(List<Song> data) {
        super(data);
    }

    @Override
    public EnhancedViewHolder<List<Song>> createViewHolder(HeterogeneousAdapter adapter,
                                                           ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.instance_shuffle_all, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public boolean showSection(HeterogeneousAdapter adapter) {
        return !get(0).isEmpty();
    }

    private class ViewHolder extends EnhancedViewHolder<List<Song>> {

        /**
         * @param itemView The view that this ViewHolder will manage
         */
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onUpdate(List<Song> item, int position) {

        }
    }
}
