package com.marverenic.music.view;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
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
     *                   {@link android.support.v7.widget.GridLayoutManager#setSpanCount(int)}
     */
    public GridSpacingDecoration(int spacing, int numColumns){
        this(spacing, numColumns, ANY_VIEW);
    }

    /**
     * Create a new ItemDecorator for use with a RecyclerView
     * @param spacing The padding around elements in a grid
     * @param numColumns The number of columns in this grid. Use the same value as set by
     *                   {@link android.support.v7.widget.GridLayoutManager#setSpanCount(int)}
     * @param viewType The type of view that should be padded in a grid. Use the same value as
     *                 {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     */
    public GridSpacingDecoration(int spacing, int numColumns, int viewType){
        this.spacing = spacing;
        this.numColumns = numColumns;
        this.viewType = viewType;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        RecyclerView.Adapter adapter = parent.getAdapter();
        int adapterPosition = parent.getChildAdapterPosition(view);

        if (viewType == ANY_VIEW || adapter.getItemViewType(adapterPosition) == viewType) {
            int halfSpacing = spacing / 2;

            int sectionPosition;
            int childCount;
            int column;

            if (viewType != ANY_VIEW){
                int leadingViews = 0;
                while (adapter.getItemViewType(leadingViews) != viewType) ++leadingViews;
                sectionPosition = adapterPosition - leadingViews;
                column = sectionPosition % numColumns;

                childCount = 0;
                while(adapter.getItemViewType(childCount + leadingViews) == viewType) ++childCount;
            }
            else{
                sectionPosition = adapterPosition;
                childCount = parent.getAdapter().getItemCount();
                column = adapterPosition % numColumns;
            }

            outRect.top = halfSpacing;
            outRect.bottom = halfSpacing;
            outRect.left = halfSpacing;
            outRect.right = halfSpacing;

            if (sectionPosition < numColumns) // Items in the first row
                outRect.top = spacing;
            if (column == 0) // Items on the far left column
                outRect.left = spacing;
            if (column == numColumns - 1) // Items in the far right column
                outRect.right = spacing;
            if (sectionPosition >= childCount - (childCount % numColumns)) // Items in the last row
                outRect.bottom = spacing;
        }
    }
}