package com.marverenic.music.view;

import android.annotation.TargetApi;
import android.content.Context;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.WindowInsets;

/**
 * This is kind of disgusting.
 *
 * So basically, Coordinator Layout uses Window Insets to figure out how to draw the status bar and
 * offset children and whatnot when the status bar is transparent, which is great. What's less great
 * is that these Window Insets can be consumed by views, so if two views need to have a Window Inset
 * applied to them, only the first one will get it (if it consumes the insets).
 *
 * This is particularly bad with the sliding now playing panel in Jockey on pages with collapsing
 * toolbars. The reason is because on view hierarchies like this, there are actually TWO
 * CollapsingToolbarLayouts and two CoordinatorLayouts (one nested inside the other). When Window
 * Insets are dispatched to the children in the Activity's view hierarchy, the first Collapsing
 * Toolbar will consume the insets, and prevent the second Collapsing Toolbar from getting them.
 * This causes the status bar padding to not be applied to the now playing page when there's another
 * CollapsingToolbarLayout in the activity's main view.
 *
 * This class is an extension of CoordinatorLayout that's used on the outer CoordinatorLayout. When
 * it dispatches window insets, it does NOT allow its children to consume them. This means that
 * every view in the hierarchy will be given these insets (although views may not choose to use
 * them). This prevents the two CollapsingToolbarLayout and nested CoordinatorLayouts from
 * interfering with each other.
 */
public class NestedCoordinatorLayout extends CoordinatorLayout {

    public NestedCoordinatorLayout(Context context) {
        super(context);
    }

    public NestedCoordinatorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NestedCoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        super.dispatchApplyWindowInsets(insets);

        for (int i = 0; i < getChildCount(); i++) {
            // Do NOT allow children to consume these insets
            getChildAt(i).dispatchApplyWindowInsets(insets);
        }

        return insets;
    }
}
