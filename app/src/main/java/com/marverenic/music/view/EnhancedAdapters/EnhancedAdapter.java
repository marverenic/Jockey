package com.marverenic.music.view.EnhancedAdapters;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

/**
 * An extension of {@link android.support.v7.widget.RecyclerView.Adapter} designed to remove
 * ugly, boilerplate, spaghetti code. On its own, this class does basically nothing except aid
 * with polymorphism.
 *
 * @see HeterogeneousAdapter
 * @see DragDropAdapter
 */
public abstract class EnhancedAdapter extends RecyclerView.Adapter<EnhancedViewHolder> {

    /**
     * Set an optional empty state to be shown when this adapter has no data to display
     * @param emptyState The empty state to use when there is no available data, or null to remove
     *                   an empty state and show a completely blank page when there is no data
     */
    public abstract void setEmptyState(@Nullable EmptyState emptyState);

}
