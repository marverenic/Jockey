package com.marverenic.music.view.EnhancedAdapters;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * A {@link android.support.v7.widget.RecyclerView.Adapter} designed for homogeneous data sets with
 * drag and drop support.
 *
 * Because this class is final, {@link android.support.v7.widget.RecyclerView.ViewHolder}s are
 * supplied with the {@link com.marverenic.music.view.EnhancedAdapters.DragDropAdapter.ViewSupplier}
 * interface. Instead of overriding this entire adapter, implement the interface where convenient
 * and return a valid {@link EnhancedViewHolder}.
 *
 * When the adapter is setup, make sure to call {@link #attach(RecyclerView)} to link it to your
 * RecyclerView. Failing to do so will prevent drag and drop from working and will make you sad.
 *
 * @param <Type> The type of data that this adapter will manage
 */
public final class DragDropAdapter<Type> extends EnhancedAdapter {

    private static final int EMPTY_VIEW = 0;
    private static final int DATA_VIEW = 1;

    private List<Type> mData;
    private ViewSupplier<Type> mSupplier;
    private ItemTouchHelper mTouchHelper;
    private OnItemMovedListener mMovedListener;
    private EmptyState mEmptyState;

    /**
     * @param data Your data set. If you need to change this data later, keep a reference to this
     *             List and make modifications to it when your data changes. (Don't forget to call
     *             {@link #notifyDataSetChanged()} if you do this, though)
     * @param supplier A ViewSupplier that will be used to build the ViewHolders shown in the
     *                 RecyclerView this adapter is attached to
     * @param moveListener A callback when items are moved around in the list
     */
    public DragDropAdapter(@NonNull List<Type> data, @NonNull ViewSupplier<Type> supplier,
                           @Nullable OnItemMovedListener moveListener) {
        mData = data;
        mSupplier = supplier;
        mMovedListener = moveListener;
    }

    @Override
    public void setEmptyState(EmptyState emptyState) {
        mEmptyState = emptyState;
    }

    /**
     * Attach this adapter to a View. You MUST call this method if you want drag and drop to work.
     * This method will call {@link RecyclerView#setAdapter(RecyclerView.Adapter)} in addition to
     * setting up drag and drop.
     * @param recyclerView The RecyclerView to bind this adapter to
     */
    public void attach(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        mTouchHelper = new ItemTouchHelper(new TouchCallback(this));
        mTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public EnhancedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == DATA_VIEW) {
            final EnhancedViewHolder viewHolder = mSupplier.createViewHolder(parent);

            View handle = viewHolder.itemView.findViewById(mSupplier.getHandleId());
            handle.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mTouchHelper.startDrag(viewHolder);
                        viewHolder.itemView.setTag(new DragMarker(viewHolder.getAdapterPosition()));
                    }
                    return false;
                }
            });
            return viewHolder;
        } else if (viewType == EMPTY_VIEW) {
            return mEmptyState.createViewHolder(this, parent);
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(EnhancedViewHolder holder, int position) {
        if (holder instanceof EmptyState.EmptyViewHolder) {
            ((EmptyState.EmptyViewHolder) holder).update(null, position);
        } else {
            holder.update(mData.get(position), position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mData.isEmpty() && mEmptyState != null) {
            return EMPTY_VIEW;
        } else {
            return DATA_VIEW;
        }
    }

    @Override
    public int getItemCount() {
        if (mData.isEmpty() && mEmptyState != null) {
            return 1;
        } else {
            return mData.size();
        }
    }

    private void move(int from, int to) {
        mData.add(to, mData.remove(from));
        notifyItemMoved(from, to);
    }

    private void finishMove(RecyclerView.ViewHolder viewHolder) {
        if (mMovedListener != null) {
            mMovedListener.onItemMoved(
                    ((DragMarker) viewHolder.itemView.getTag()).from,
                    viewHolder.getAdapterPosition());
        }
        viewHolder.itemView.setTag(null);
    }

    /**
     * Interface to supply views to this adapter
     * @param <T>
     */
    public interface ViewSupplier<T> {
        /**
         * Called with the attached adapter needs another ViewHolder
         * @param parent The ViewGroup that this ViewHolder will be bound to
         * @return A new ViewHolder
         * @see android.support.v7.widget.RecyclerView.Adapter#createViewHolder(ViewGroup, int)
         */
        EnhancedViewHolder<T> createViewHolder(ViewGroup parent);

        /**
         * Gets of the id region that can be touched to initiate a drag and drop gesture
         * @return The ViewId of the drag handle. If the entire view can start a drag, return the
         *         id of the itemView's parent.
         */
        @IdRes int getHandleId();
    }

    /**
     * A Callback used to inform a listener of drag and drop operations
     */
    public interface OnItemMovedListener {
        /**
         * Called when a drag and drop operation has finished. The adapter this listener is attached
         * to reorders data entries on its own. Because of this, implementations should NOT reorder
         * their entries when this is called. Instead, they should update the data set that the
         * adapter is populated from with this change.
         * @param from The index that an item was moved from. May be equal to <code>to</code>.
         * @param to The index that an item was moved to. May be equal to <code>from</code>.
         */
        void onItemMoved(int from, int to);
    }

    /**
     * Helper class used to specify drag behavior and initiate appropriate callbacks
     */
    private static class TouchCallback extends ItemTouchHelper.Callback {

        private DragDropAdapter mAdapter;

        TouchCallback(DragDropAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            mAdapter.move(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            mAdapter.finishMove(viewHolder);
        }
    }

    /**
     * Used as a tag on a ViewHolder's itemView to mark that it is currently being dragged. This
     * tag also encapsulates its starting position so that the attached
     * {@link DragDropAdapter.OnItemMovedListener} doesn't have to be informed about every single
     * transaction that the adapter handles when dragging an item, only the resulting modification.
     */
    protected final static class DragMarker {
        private final int from;

        private DragMarker(int from) {
            this.from = from;
        }
    }
}
