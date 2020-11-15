package com.marverenic.music.view;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;

public class InsetDecoration extends RecyclerView.ItemDecoration {

    private Drawable mInset;
    private int mHeight;
    private int mGravity;

    public InsetDecoration(Drawable inset, int height, int gravity) {
        mInset = inset;
        mHeight = height;
        mGravity = gravity;
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int top = 0;
        int left = 0;
        int right = parent.getWidth();
        int bottom = parent.getHeight();

        if (mGravity == Gravity.TOP) {
            mInset.setBounds(left, top, right, mHeight);
        } else if (mGravity == Gravity.BOTTOM) {
            mInset.setBounds(left, bottom - mHeight, right, bottom);
        }

        mInset.draw(c);
    }
}
