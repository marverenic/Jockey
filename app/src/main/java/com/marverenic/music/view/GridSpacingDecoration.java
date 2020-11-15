package com.marverenic.music.view;

import android.graphics.Rect;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {

    private static final int ANY_VIEW = -1;

    private int spacing;
    private int numColumns;
    private int viewType;

    /**
     * Create a new ItemDecorator for use with a RecyclerView
     * @param spacing The padding around elements in a grid
     * @param numColumns The number of columns in this grid. Use the same value as set by
     *                   {@link GridLayoutManager#setSpanCount(int)}
     */
    public GridSpacingDecoration(int spacing, int numColumns) {
        this(spacing, numColumns, ANY_VIEW);
    }

    /**
     * Create a new ItemDecorator for use with a RecyclerView
     * @param spacing The padding around elements in a grid
     * @param numColumns The number of columns in this grid. Use the same value as set by
     *                   {@link GridLayoutManager#setSpanCount(int)}
     * @param viewType The type of view that should be padded in a grid. Use the same value as
     *                 {@link RecyclerView.Adapter#getItemViewType(int)}
     */
    public GridSpacingDecoration(int spacing, int numColumns, int viewType) {
        this.spacing = spacing;
        this.numColumns = numColumns;
        this.viewType = viewType;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        RecyclerView.Adapter adapter = parent.getAdapter();
        int adapterPosition = parent.getChildAdapterPosition(view);

        if (viewType == ANY_VIEW || adapter.getItemViewType(adapterPosition) == viewType) {
            int halfSpacing = spacing / 2;

            int sectionPosition;
            int childCount;
            int column;

            if (viewType != ANY_VIEW) {
                int leadingViews = 0;
                while (adapter.getItemViewType(leadingViews) != viewType) {
                    ++leadingViews;
                }
                sectionPosition = adapterPosition - leadingViews;
                column = sectionPosition % numColumns;

                childCount = 0;
                while (childCount + leadingViews < adapter.getItemCount()
                        && adapter.getItemViewType(childCount + leadingViews) == viewType) {
                    ++childCount;
                }
            } else {
                sectionPosition = adapterPosition;
                childCount = parent.getAdapter().getItemCount();
                column = adapterPosition % numColumns;
            }

            outRect.top = halfSpacing;
            outRect.bottom = halfSpacing;
            if (ViewUtils.isRtl(view.getContext())) {
                outRect.right = spacing * (numColumns - column) / numColumns;
                outRect.left = spacing * (column + 1) / numColumns;
            } else {
                outRect.left = spacing * (numColumns - column) / numColumns;
                outRect.right = spacing * (column + 1) / numColumns;
            }

            // Items in the first row
            if (sectionPosition < numColumns) {
                outRect.top = spacing;
            }

            // Items in the last row
            int lastRowItemCount = childCount % numColumns;
            // If the last row is completely filled, the mod operation will suggest that the
            // last row is empty
            if (lastRowItemCount == 0) {
                lastRowItemCount = numColumns;
            }
            if (sectionPosition >= childCount - lastRowItemCount) {
                outRect.bottom = spacing;
            }
        }
    }
}
