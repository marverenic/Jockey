package com.marverenic.music.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.marverenic.music.R;

/**
 * An {@link android.support.v7.widget.RecyclerView.ItemDecoration} that draws a solid color behind
 * a {@link RecyclerView} and its children
 */
public class BackgroundDecoration extends RecyclerView.ItemDecoration {

    private Drawable mBackground;
    private NinePatchDrawable mShadow;
    private int[] excludedIDs;

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param color the color of the background
     * @param excludedLayoutIDs A list of layoutIDs to exclude adding a background color to
     *                          empty to add a background to the entire RecyclerView
     */
    public BackgroundDecoration(int color, int... excludedLayoutIDs) {
        mBackground = new ColorDrawable(color);
        excludedIDs = excludedLayoutIDs;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        if (mShadow == null) {
            //noinspection deprecation
            mShadow = (NinePatchDrawable) parent.getContext().getResources()
                    .getDrawable(R.drawable.list_shadow);
        }

        Rect shadowPadding = new Rect();
        mShadow.getPadding(shadowPadding);

        if (excludedIDs == null) {
            int top = 0;
            int bottom = c.getHeight();

            mBackground.setBounds(left, top, right, bottom);
            mBackground.draw(c);

            mShadow.setBounds(left - shadowPadding.left, top - shadowPadding.top,
                    right + shadowPadding.right, bottom + shadowPadding.bottom);
            mShadow.draw(c);
        } else {
            int layoutCount = parent.getChildCount();
            for (int i = 0; i < layoutCount; i++) {
                View topView = parent.getChildAt(i);
                if (includeView(topView.getId())) {

                    //noinspection StatementWithEmptyBody
                    while (++i < layoutCount && includeView(parent.getChildAt(i).getId())) {
                        // Find the last view in this section that will receive a background
                        // This loop is intentionally left empty
                    }

                    View bottomView = parent.getChildAt(--i);

                    RecyclerView.LayoutParams topParams =
                            (RecyclerView.LayoutParams) topView.getLayoutParams();
                    RecyclerView.LayoutParams bottomParams =
                            (RecyclerView.LayoutParams) bottomView.getLayoutParams();

                    int top = topView.getTop() - topParams.topMargin;
                    int bottom;

                    if (i == layoutCount - 1 || parent.getChildAdapterPosition(bottomView)
                            == parent.getAdapter().getItemCount() - 1) {
                        // If this is the last item in the adapter or last visible view,
                        // fill the parent
                        bottom = parent.getBottom();
                    } else {
                        // Otherwise, fill to the bottom of the last item in the section
                        bottom = bottomView.getBottom() + bottomParams.bottomMargin;
                    }

                    mBackground.setBounds(left, top, right, bottom);
                    mBackground.draw(c);

                    mShadow.setBounds(left - shadowPadding.left, top - shadowPadding.top,
                            right + shadowPadding.right, bottom + shadowPadding.bottom);
                    mShadow.draw(c);
                }
            }
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
