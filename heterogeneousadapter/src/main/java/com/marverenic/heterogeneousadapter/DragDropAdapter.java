package com.marverenic.heterogeneousadapter;

import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class DragDropAdapter extends HeterogeneousAdapter {

    private DragSection mDragSection;
    private ItemTouchHelper mTouchHelper;

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
        final EnhancedViewHolder viewHolder = super.onCreateViewHolder(parent, viewType);

        if (mDragSection != null && viewType == mDragSection.getTypeId()) {
            View handle = viewHolder.itemView.findViewById(mDragSection.getDragHandleId());
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
        }
        return viewHolder;
    }

    public DragDropAdapter setDragSection(DragSection section) {
        return setDragSection(section, getSectionCount());
    }

    public DragDropAdapter setDragSection(DragSection section, int index) {
        if (mDragSection != null) {
            throw new IllegalStateException("A DragSection has already been attached. "
                    + "You must remove it before setting a new drag section.");
        }

        super.addSection(section, index);
        mDragSection = section;
        return this;
    }

    private void drag(int from, int to) {
        int leadingViews = getLeadingViewCount(mDragSection.getTypeId());
        mDragSection.onDrag(from - leadingViews, to - leadingViews);
        notifyItemMoved(from, to);
    }

    private void drop(RecyclerView.ViewHolder viewHolder) {
        int leadingViews = getLeadingViewCount(mDragSection.getTypeId());
        mDragSection.onDrop(
                ((DragMarker) viewHolder.itemView.getTag()).from - leadingViews,
                viewHolder.getAdapterPosition() - leadingViews);
        viewHolder.itemView.setTag(null);
    }

    public static abstract class DragSection<Type> extends Section<Type> {

        /**
         * @return The view ID of that may be touched to begin a drag gesture. If the entire view
         *         may be used, return its ID, otherwise return the ID of a specific view in
         *         the itemView hierarchy
         */
        @IdRes
        public abstract int getDragHandleId();

        /**
         * Called when a drag and drop operation has caused items in the Section to move.
         * Implementors are only responsible for updating the backing data set.
         * {@link android.support.v7.widget.RecyclerView.Adapter#notifyItemMoved(int, int)} will be
         * called automatically. Because this method may be called many times throughout the
         * lifecycle of a drag gesture, it should not be used to apply the change of this operation
         * to the data's source.
         *
         * To implement behavior when the user has finished a drag and drop gesture, implement
         * {@link #onDrop(int, int)} instead.
         * @param from The index that an item was moved from
         * @param to The index that an item was moved to
         */
        protected abstract void onDrag(int from, int to);

        /**
         * Called when a drag and drop operation has finished. This method isn't responsible for
         * reordering the items in the data set, but {@link #onDrag(int, int)} is.
         * This method should be used to apply any changes caused by rearranging items in the list
         * @param from The index that an item was moved from. May be equal to <code>to</code>.
         * @param to The index that an item was moved to. May be equal to <code>from</code>.
         */
        protected abstract void onDrop(int from, int to);
    }

    public static abstract class ListDragSection<Type> extends DragSection<Type> {

        private List<Type> mData;

        public ListDragSection(List<Type> data) {
            mData = data;
        }

        @Override
        protected void onDrag(int from, int to) {
            mData.add(to, mData.remove(from));
        }

        @Override
        public int getItemCount(HeterogeneousAdapter adapter) {
            return mData.size();
        }

        @Override
        public Type get(int position) {
            return mData.get(position);
        }
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
            if (viewHolder.getItemViewType() == target.getItemViewType()) {
                mAdapter.drag(
                        viewHolder.getAdapterPosition(),
                        target.getAdapterPosition());
                return true;
            }
            return false;
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
            mAdapter.drop(viewHolder);
        }
    }

    /**
     * Used as a tag on a ViewHolder's itemView to mark that it is currently being dragged. This
     * tag also encapsulates its starting position so that the attached
     * {@link DragDropAdapter.Section} can be informed about the net result of a transaction
     * instead of having to update the original data set with every minor change
     */
    protected final static class DragMarker {
        private final int from;

        private DragMarker(int from) {
            this.from = from;
        }
    }

}
