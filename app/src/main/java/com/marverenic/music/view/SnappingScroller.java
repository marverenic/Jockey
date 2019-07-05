package com.marverenic.music.view;

import android.content.Context;
import androidx.recyclerview.widget.LinearSmoothScroller;
import android.util.DisplayMetrics;
import android.view.View;

public class SnappingScroller extends LinearSmoothScroller {

    private static final float MILLISECONDS_PER_INCH = 100f;

    public static final int SNAP_TO_START = LinearSmoothScroller.SNAP_TO_START;
    public static final int SNAP_TO_END = LinearSmoothScroller.SNAP_TO_END;

    private Context mContext;
    private int mSnapPreference;
    private float mMillisecondsPerPixel;

    public SnappingScroller(Context context, int snap) {
        super(context);
        mContext = context;
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
    protected void onStart() {
        super.onStart();

        View firstView = getLayoutManager().getChildAt(0);
        int firstViewPosition = getChildPosition(firstView);
        int intermediateViewCount = Math.abs(firstViewPosition - getTargetPosition());

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        mMillisecondsPerPixel = getSpeedPerPixel(displayMetrics, intermediateViewCount);
    }

    private float getSpeedPerPixel(DisplayMetrics displayMetrics, int intermediateViewCount) {
        int dpi = displayMetrics.densityDpi;
        return MILLISECONDS_PER_INCH / (float) Math.sqrt(intermediateViewCount) / dpi;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return (int) Math.ceil(Math.abs(dx) * mMillisecondsPerPixel);
    }
}
