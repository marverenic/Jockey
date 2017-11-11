package com.marverenic.music.view;

import android.content.Context;
import android.support.design.widget.CollapsingToolbarLayout;
import android.util.AttributeSet;
import android.view.View;

/**
 * Fixes a bug in the Android Support Library described in this issue:
 * https://issuetracker.google.com/issues/64065383
 *
 * This bug was introduced in version 26 of the support library.
 *
 * TODO: Delete this class when issue 64065383 is resolved in a future version of the support lib
 */
public class FixedCollapsingToolbarLayout extends CollapsingToolbarLayout {

    public FixedCollapsingToolbarLayout(Context context) {
        super(context);
    }

    public FixedCollapsingToolbarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedCollapsingToolbarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View content = getChildAt(0);
        if (heightMeasureSpec == 0) {
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(
                    content.getMeasuredHeight(), View.MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
