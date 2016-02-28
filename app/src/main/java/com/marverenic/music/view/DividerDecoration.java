package com.marverenic.music.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.marverenic.music.R;

/**
 * An {@link android.support.v7.widget.RecyclerView.ItemDecoration} that draws horizontal dividers
 * between entries in a {@link RecyclerView}
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private static final int DIVIDER_HEIGHT_DP = 1;

    private Drawable dividerDrawable;
    private static int measuredDividerHeight;
    private int[] excludedIDs;
    private boolean drawOnLastItem;

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param context A context held temporarily to get colors and display metrics
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a divider to
     *                          none to add a divider to each entry in the RecyclerView
     */
    public DividerDecoration(Context context, @IdRes int... excludedLayoutIDs) {
        this(context, false, excludedLayoutIDs);
    }

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param context A context held temporarily to get colors and display metrics
     * @param drawOnLastItem Whether or not to draw a divider under the last item in the list
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a divider to
     *                          none to add a divider to each entry in the RecyclerView
     */
    public DividerDecoration(Context context, boolean drawOnLastItem,
                             @IdRes int... excludedLayoutIDs) {
        //noinspection deprecation
        dividerDrawable = new ColorDrawable(context.getResources().getColor(R.color.divider));
        measuredDividerHeight = (int) Math.ceil(
                DIVIDER_HEIGHT_DP * context.getResources().getDisplayMetrics().density);
        excludedIDs = excludedLayoutIDs;
        this.drawOnLastItem = drawOnLastItem;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int endIndex = parent.getChildCount();
        for (int i = 0; i < endIndex; i++) {
            final View child = parent.getChildAt(i);
            if (excludedIDs == null || includeView(child)) {

                final RecyclerView.LayoutParams params =
                        (RecyclerView.LayoutParams) child.getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin
                        + (int) child.getTranslationY();
                final int bottom = top + measuredDividerHeight;

                dividerDrawable.setBounds(left, top, right, bottom);

                // Don't draw separators under the last item in a section unless we've been told to
                View nextChild = parent.getChildAt(i + 1);
                if (drawOnLastItem || (nextChild != null && includeView(nextChild))) {
                    dividerDrawable.draw(c);
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (includeView(view)) {
            outRect.bottom = measuredDividerHeight;
        }
    }

    protected boolean includeView(View view) {
        int viewId = view.getId();
        for (int i : excludedIDs) {
            if (viewId == i) {
                return false;
            }
        }
        return true;
    }
}
