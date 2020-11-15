package com.marverenic.music.view;

import android.content.Context;
import androidx.annotation.IdRes;
import android.view.View;

import com.marverenic.adapter.DragDropAdapter;

import androidx.recyclerview.widget.RecyclerView;

/**
 * An extension of {@link DividerDecoration} designed for use with a
 * {@link RecyclerView} with a {@link DragDropAdapter} attached. This
 * class acts as an {@link RecyclerView.ItemDecoration} that draws
 * dividers between views, excluding any view that's currently being dragged by the user.
 */
public class DragDividerDecoration extends DividerDecoration {

    @IdRes
    private int mDragViewId;

    /**
     * Create an ItemDecorator for use with a RecyclerView
     *
     * @param context           A context held temporarily to get colors and display metrics
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a divider to
     */
    public DragDividerDecoration(@IdRes int dragViewId, Context context,
                                 @IdRes int... excludedLayoutIDs) {
        super(context, excludedLayoutIDs);
        mDragViewId = dragViewId;
    }

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param context A context held temporarily to get colors and display metrics
     * @param drawOnLastItem Whether or not to draw a divider under the last item in the list
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a divider to
     *                          none to add a divider to each entry in the RecyclerView
     */
    public DragDividerDecoration(Context context, boolean drawOnLastItem,
                                 @IdRes int... excludedLayoutIDs) {
        super(context, drawOnLastItem, excludedLayoutIDs);
    }

    @Override
    protected boolean includeView(View view) {
        return (mDragViewId != view.getId() || view.getTag() == null) && super.includeView(view);
    }
}
