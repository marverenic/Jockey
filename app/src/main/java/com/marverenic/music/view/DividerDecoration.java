package com.marverenic.music.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.marverenic.music.utils.Themes;

/**
 * An {@link android.support.v7.widget.RecyclerView.ItemDecoration} that draws horizontal dividers
 * between entries in a {@link RecyclerView}
 */
public class DividerDecoration extends RecyclerView.ItemDecoration {

    private static final int DIVIDER_HEIGHT_DP = 1;

    private Drawable dividerDrawable;
    private static int measuredDividerHeight;
    private int[] excludedIDs;

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param context A context held temporarily to get colors and display metrics
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a divider to
     *                          none to add a divider to each entry in the RecyclerView
     */
    public DividerDecoration(Context context, @IdRes int... excludedLayoutIDs) {
        dividerDrawable = new ColorDrawable(Themes.isLight(context) ? 0xFFE0E0E0 : 0xFF1F1F1F);
        measuredDividerHeight = (int) Math.ceil(
                DIVIDER_HEIGHT_DP * context.getResources().getDisplayMetrics().density);
        excludedIDs = excludedLayoutIDs;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int endIndex = parent.getChildCount();
        for (int i = 0; i < endIndex; i++) {
            final View child = parent.getChildAt(i);
            if (excludedIDs == null || includeView(child.getId())) {

                final RecyclerView.LayoutParams params =
                        (RecyclerView.LayoutParams) child.getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + measuredDividerHeight;

                dividerDrawable.setBounds(left, top, right, bottom);

                // Don't draw separators under the last item in a section unless it's at the end
                // of the list and it has a divider above it
                View nextChild = parent.getChildAt(i + 1);
                if ((nextChild == null && includeView(child.getId()))
                        || (nextChild != null && includeView(nextChild.getId()))) {
                    dividerDrawable.draw(c);
                }
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (includeView(view.getId())) {
            outRect.bottom = measuredDividerHeight;
        }
    }

    private boolean includeView(int viewId) {
        for (int i : excludedIDs) {
            if (viewId == i) {
                return false;
            }
        }
        return true;
    }
}
