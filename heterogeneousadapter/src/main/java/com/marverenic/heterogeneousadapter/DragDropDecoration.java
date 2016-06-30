package com.marverenic.heterogeneousadapter;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * An {@link android.support.v7.widget.RecyclerView.ItemDecoration} used to draw an elevated
 * background behind items in a {@link RecyclerView} with a {@link DragDropAdapter} that are
 * currently being repositioned by the user
 */
public class DragDropDecoration extends RecyclerView.ItemDecoration {

    private NinePatchDrawable mDecoration;
    private Rect mDecorationPadding;

    /**
     * @param decoration The background to draw behind list elements that are being dragged
     */
    public DragDropDecoration(NinePatchDrawable decoration) {
        mDecoration = decoration;
        mDecorationPadding = new Rect();
        mDecoration.getPadding(mDecorationPadding);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int childCount = parent.getChildCount();

        if (childCount == 0) {
            return;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);

            if (!(child.getTag() instanceof DragDropAdapter.DragMarker)) {
                continue;
            }

            final int tx = (int) (ViewCompat.getTranslationX(child) + 0.5f);
            final int ty = (int) (ViewCompat.getTranslationY(child) + 0.5f);

            final int left = child.getLeft() - mDecorationPadding.left;
            final int right = child.getRight() + mDecorationPadding.right;
            final int top = child.getTop() - mDecorationPadding.top;
            final int bottom = child.getBottom() + mDecorationPadding.bottom;

            mDecoration.setBounds(left + tx, top + ty, right + tx, bottom + ty);
            mDecoration.draw(c);

            c.translate(child.getLeft() + child.getTranslationX(),
                    child.getTop() + child.getTranslationY());
            child.draw(c);

            // Reset the translation matrix to avoid affecting other decorations
            c.translate(-child.getLeft() - child.getTranslationX(),
                    -child.getTop() - child.getTranslationY());
        }
    }
}
