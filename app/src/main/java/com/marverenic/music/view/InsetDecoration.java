package com.marverenic.music.view;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;

public class InsetDecoration extends RecyclerView.ItemDecoration {

    private Drawable mInset;
    private int mHeight;

    /**
     * @param inset The drawable to overlay at the top of the RecyclerView this decoration is bound
     *              to
     * @param height The height of the inset
     */
    public InsetDecoration(Drawable inset, int height) {
        mInset = inset;
        mHeight = height;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        mInset.setBounds(parent.getLeft(), 0, parent.getRight(), mHeight);
        mInset.draw(c);
    }
}
