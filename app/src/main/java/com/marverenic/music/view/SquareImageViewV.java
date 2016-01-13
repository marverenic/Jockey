package com.marverenic.music.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SquareImageViewV extends ImageView {

    public SquareImageViewV(Context context) {
        super(context);
    }

    public SquareImageViewV(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageViewV(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SquareImageViewV(Context context, AttributeSet attrs, int defStyleAttr,
                            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //noinspection SuspiciousNameCombination
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }

}
