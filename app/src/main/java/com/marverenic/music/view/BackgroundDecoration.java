package com.marverenic.music.view;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * An {@link android.support.v7.widget.RecyclerView.ItemDecoration} that draws a solid color behind
 * a {@link RecyclerView} and its children
 */
public class BackgroundDecoration extends RecyclerView.ItemDecoration {

    private Drawable mBackground;
    private int[] excludedIDs;

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param color the color of the background
     */
    public BackgroundDecoration(int color) {
        this(color, null);
    }

    /**
     * Create an ItemDecorator for use with a RecyclerView
     * @param color the color of the background
     * @param excludedLayoutIDs an array of layoutIDs to exclude adding a background color to
     *                          null to add a background to the entire RecyclerView
     */
    public BackgroundDecoration(int color, int[] excludedLayoutIDs){
        mBackground = new ColorDrawable(color);
        excludedIDs = excludedLayoutIDs;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        if (excludedIDs == null) {
            int top = 0;
            int bottom = c.getHeight();

            mBackground.setBounds(left, top, right, bottom);
            mBackground.draw(c);
        }
        else{
            int layoutCount = parent.getChildCount();
            for (int i = 0; i < layoutCount; i++){
                View topView = parent.getChildAt(i);
                if (includeView(topView.getId())) {
                    // Find the last view in this section that will receive a background
                    View bottomView = topView;
                    while(++i < layoutCount && includeView(bottomView.getId())){
                        bottomView = parent.getChildAt(i);
                    }

                    RecyclerView.LayoutParams topParams = (RecyclerView.LayoutParams) topView.getLayoutParams();
                    RecyclerView.LayoutParams bottomParams = (RecyclerView.LayoutParams) bottomView.getLayoutParams();

                    final int top = topView.getTop() - topParams.topMargin;
                    final int bottom = (i == layoutCount - 1 || parent.getChildAdapterPosition(bottomView) == parent.getAdapter().getItemCount() - 1)
                            ? parent.getBottom() // If this is the last item in the adapter or last visible view, fill the parent
                            : bottomView.getBottom() + bottomParams.bottomMargin; // Otherwise, fill to the bottom of the last item in the section

                    mBackground.setBounds(left, top, right, bottom);
                    mBackground.draw(c);
                }
            }
        }
    }

    private boolean includeView(int viewId){
        for (int i : excludedIDs){
            if (viewId == i) return false;
        }
        return true;
    }
}
