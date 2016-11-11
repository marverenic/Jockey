package com.marverenic.music.view;

import android.content.Context;
import android.support.v7.widget.LinearSmoothScroller;
import android.util.DisplayMetrics;

public class SnappingScroller extends LinearSmoothScroller {

    private static final int MIN_SCROLL_TIME_MS = 75;
    private static final float MILLISECONDS_PER_INCH = 50f;

    public static final int SNAP_TO_START = LinearSmoothScroller.SNAP_TO_START;
    public static final int SNAP_TO_END = LinearSmoothScroller.SNAP_TO_END;

    private int mSnapPreference;

    public SnappingScroller(Context context, int snap) {
        super(context);
        mSnapPreference = snap;
    }

    @Override
    protected int getHorizontalSnapPreference() {
        return mSnapPreference;
    }

    @Override
    protected int getVerticalSnapPreference() {
        return mSnapPreference;
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return Math.max(MIN_SCROLL_TIME_MS, super.calculateTimeForScrolling(dx));
    }
}
