package com.marverenic.music.view.EnhancedAdapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * An extension of {@link android.support.v7.widget.RecyclerView.ViewHolder} used by
 * {@link HeterogeneousAdapter}s to add functionality to ViewHolders
 * @param <Type> The type of data that this ViewHolder will be used to display. Void may be used
 *              if this ViewHolder doesn't have a data type and doesn't need to be updated.
 */
public abstract class EnhancedViewHolder<Type> extends RecyclerView.ViewHolder {

    /**
     * @param itemView The view that this ViewHolder will manage
     */
    public EnhancedViewHolder(View itemView) {
        super(itemView);
    }

    /**
     * Called when this ViewHolder has been recycled and needs to be populated with new data
     * @param item The item to show in this ViewHolder
     * @param position The index of this item in the adapter's data set
     */
    public abstract void update(Type item, int position);

}
