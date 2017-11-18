package android.support.design.widget;

import android.content.Context;
import android.support.annotation.Keep;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import static android.support.v4.view.ViewCompat.TYPE_TOUCH;

@Keep
public class FixedSnappingAppBarLayoutBehavior extends AppBarLayout.Behavior {

    public FixedSnappingAppBarLayoutBehavior() {}

    public FixedSnappingAppBarLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed, int type) {
        if (type == TYPE_TOUCH) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        if (type == TYPE_TOUCH) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes, int type) {
        return type == TYPE_TOUCH && super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type);
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl,
                                   View target, int type) {
        if (type == ViewCompat.TYPE_TOUCH) {
            super.onStopNestedScroll(coordinatorLayout, abl, target, type);
        }
    }

}