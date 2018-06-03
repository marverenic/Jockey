package com.marverenic.music.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * Intended for use with overflow buttons on list items. This class fixes an issue where opening
 * an overflow menu in a list can cause the list to scroll to a different location.
 *
 * See https://stackoverflow.com/q/29473977 for more info
 */
public class AnchoredImageView extends AppCompatImageView {

    public AnchoredImageView(Context context) {
        super(context);
    }

    public AnchoredImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnchoredImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate) {
        return false;
    }
}
