package com.marverenic.music.ui.common;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.adapter.EnhancedViewHolder;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.marverenic.music.R;
import com.marverenic.music.data.store.PreferenceStore;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;

import java.util.List;

public class ShuffleAllSection extends HeterogeneousAdapter.SingletonSection<List<Song>>
        implements MeasurableAdapter {

    private PlayerController mPlayerController;
    private PreferenceStore mPrefStore;

    @Nullable
    private OnSongSelectedListener mSongListener;

    /**
     * @param data The item to show in this Section
     */
    public ShuffleAllSection(List<Song> data, PreferenceStore preferenceStore,
                             PlayerController playerController,
                             @Nullable OnSongSelectedListener songSelectedListener) {
        super(data);
        mPlayerController = playerController;
        mPrefStore = preferenceStore;
        mSongListener = songSelectedListener;
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

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, int viewType) {
        return recyclerView.getResources().getDimensionPixelSize(R.dimen.list_height_small)
                + recyclerView.getResources().getDimensionPixelSize(R.dimen.divider_height);
    }

    private class ViewHolder extends EnhancedViewHolder<List<Song>> {

        /**
         * @param itemView The view that this ViewHolder will manage
         */
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void onUpdate(List<Song> songs, int position) {
            itemView.setOnClickListener(v -> {
                int firstSong = (int) (Math.random() * songs.size());
                mPrefStore.setShuffle(true);
                mPlayerController.updatePlayerPreferences(mPrefStore);
                mPlayerController.setQueue(songs, firstSong);
                mPlayerController.play();

                if (mSongListener != null) {
                    mSongListener.onSongSelected();
                }
            });
        }
    }
}
