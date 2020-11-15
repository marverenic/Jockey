package com.marverenic.music.view;

import android.content.Context;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.marverenic.music.R;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class FABMenu extends FloatingActionButton implements View.OnClickListener {

    private static final int SIZE_L_DP = 56;
    private static final int SIZE_S_DP = 40;

    private FrameLayout screen;
    private final List<FloatingActionButton> children = new ArrayList<>();
    private final List<TextView> labels = new ArrayList<>();
    private boolean childrenVisible = false;
    private Runnable delayedRunnable;

    public FABMenu(Context context) {
        super(context);
        setOnClickListener(this);
        buildScreen(context);
    }

    public FABMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        buildScreen(context);
    }

    public FABMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnClickListener(this);
        buildScreen(context);
    }

    private void buildScreen(Context context) {
        screen = new FrameLayout(context);
        screen.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        //noinspection deprecation
        screen.setBackgroundColor(context.getResources().getColor(R.color.screen_overlay));
        screen.setOnClickListener(this);
    }

    public void setChildren(MenuItem... menuItems) {
        for (FloatingActionButton child : children) {
            ViewGroup childParent = (ViewGroup) child.getParent();
            childParent.removeView(child);
        }

        children.clear();

        for (MenuItem item : menuItems) {
            addChild(item);
        }
    }

    public void addChild(MenuItem menuItem) {
        if (menuItem.labelRes == 0) {
            addChild(menuItem.iconRes, menuItem.onClickListener, menuItem.label);
        } else {
            addChild(menuItem.iconRes, menuItem.onClickListener, menuItem.labelRes);
        }
    }

    public void addChild(@DrawableRes int icon, OnClickListener onClickListener, String label) {
        children.add(buildChild(icon, onClickListener, label));
        labels.add(buildChildLabel(label));
    }

    public void addChild(@DrawableRes int icon, OnClickListener onClickListener,
                         @StringRes int label) {
        addChild(icon, onClickListener, getResources().getString(label));
    }

    private FloatingActionButton buildChild(@DrawableRes int icon,
                                            final OnClickListener onClickListener, String label) {
        FloatingActionButton button = LayoutInflater.from(getContext())
                .inflate(R.layout.mini_fab, (ViewGroup) getParent(), true)
                .findViewWithTag("fab-null");

        button.setTag("fab-" + label);
        button.setImageResource(icon);
        button.setVisibility(GONE);
        button.setOnClickListener(v -> {
            onClickListener.onClick(v);
            hideChildren();
        });

        if (getParent() instanceof CoordinatorLayout) {
            final float padding = getResources().getDimension(R.dimen.fab_margin);
            final float dpScale = getResources().getDisplayMetrics().density;

            CoordinatorLayout.LayoutParams params =
                    (CoordinatorLayout.LayoutParams) button.getLayoutParams();
            if (ViewUtils.isRtl(getContext())) {
                params.leftMargin += padding;
            } else {
                params.rightMargin += padding;
            }
            params.bottomMargin = (int) (SIZE_L_DP * dpScale + padding * (2 + children.size())
                    + SIZE_S_DP * dpScale * children.size());

            // For some reason, the children are 12dp higher and 18dp further to the left on pre-L
            // devices than on L+ devices. I don't know for sure what causes this (I suspect it's
            // the drop shadow or elevation compatibility code), but this takes care of it.
            //
            // There's probably a better way to fix this, but this was the easiest. If for some
            // reason this changes in an update to one of the support libraries, just remeasure
            // these offsets and update them here.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                if (ViewUtils.isRtl(getContext())) {
                    params.leftMargin -= 12 * dpScale;
                } else {
                    params.rightMargin -= 12 * dpScale;
                }
                params.bottomMargin -= 18 * dpScale;
            }

            button.setLayoutParams(params);
        } else {
            Timber.e("Parent must be a CoordinatorLayout to properly set margin");
        }

        // When children aren't visible on screen, remove them from the view hierarchy completely
        // If we don't do this, then the FloatingActionButton Behaviors conflict for some reason
        // and Snackbars won't slide the FAB up which is kind of an important detail.
        //
        // FABMenu.Behavior takes care of some of the left over discrepancies like overlapping FAB's
        //
        // Additionally, the screen is functionally important because it prevents the user
        // from doing something that could generate a Snackbar when the FAB's are visible
        // which can cause the main FAB to be overlapped.
        ((ViewGroup) button.getParent()).removeView(button);
        return button;
    }

    private TextView buildChildLabel(String name) {
        TextView label = LayoutInflater.from(getContext())
                .inflate(R.layout.mini_fab_label, (ViewGroup) getParent(), true)
                .findViewWithTag("fab-label-null");

        label.setTag("fab-label-" + label);
        label.setText(name);
        label.setVisibility(GONE);

        if (getParent() instanceof CoordinatorLayout) {
            final float padding = getResources().getDimension(R.dimen.fab_margin);
            final float dpScale = getResources().getDisplayMetrics().density;

            CoordinatorLayout.LayoutParams params =
                    (CoordinatorLayout.LayoutParams) label.getLayoutParams();

            if (ViewUtils.isRtl(getContext())) {
                params.leftMargin += padding + 40 * dpScale;
            } else {
                params.rightMargin += padding + 40 * dpScale;
            }
            params.bottomMargin = (int) (SIZE_L_DP * dpScale + 4 * dpScale
                    + padding * (2 + labels.size()) + SIZE_S_DP * dpScale * labels.size());

            label.setLayoutParams(params);
        } else {
            Timber.e("Parent must be a CoordinatorLayout to properly set margin");
        }

        ((ViewGroup) label.getParent()).removeView(label);
        return label;
    }

    public void setShown(boolean visible) {
        if ((visible ? View.VISIBLE : View.GONE) == getVisibility()) {
            return;
        }

        if (visible) {
            show();
        } else {
            hide();
        }
    }

    public void show() {
        Animation fabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_in);
        fabAnim.setDuration(300);
        fabAnim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);

        startAnimation(fabAnim);

        // Make sure the FAB is visible when the animation starts
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        if (!ViewCompat.isLaidOut(this)) {
            setVisibility(GONE);
            return;
        }

        if (childrenVisible) {
            hideChildren();
            childrenVisible = false;
        }

        Animation fabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_out);
        fabAnim.setDuration(300);
        fabAnim.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);
        fabAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Make sure to hide the FAB after the animation finishes and reset its rotation
                setVisibility(View.GONE);
                setRotation(0f);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        startAnimation(fabAnim);
    }

    public void showChildren() {
        if (childrenVisible || delayedRunnable != null) {
            return;
        }
        childrenVisible = true;

        // Start a sliding animation on each child
        for (int i = 0; i < children.size(); i++) {
            final FloatingActionButton child = children.get(i);
            ((ViewGroup) getParent()).addView(child);

            final float padding = getResources().getDimension(R.dimen.fab_margin);
            final float dpScale = getResources().getDisplayMetrics().density;

            final float dY = 28 * dpScale + padding + (padding + 40 * dpScale) * i;

            TranslateAnimation translateAnim = new TranslateAnimation(0, 0, dY, 0);
            AlphaAnimation fadeAnim = new AlphaAnimation(0, 1);

            AnimationSet slideFadeAnim = new AnimationSet(true);
            slideFadeAnim.addAnimation(translateAnim);
            slideFadeAnim.addAnimation(fadeAnim);
            slideFadeAnim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);
            slideFadeAnim.setDuration(300 + 25 * i);

            child.startAnimation(slideFadeAnim);

            // Make sure the FABs are visible when the animation starts
            child.setVisibility(VISIBLE);
        }

        //Delay the label animation
        delayedRunnable = () -> {
            final AlphaAnimation fadeAnim = new AlphaAnimation(0, 1);
            fadeAnim.setDuration(400);
            fadeAnim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);

            for (TextView l : labels) {
                ((ViewGroup) getParent()).addView(l);
                l.setVisibility(VISIBLE);
                l.startAnimation(fadeAnim);
            }
            delayedRunnable = null;
        };

        postDelayed(delayedRunnable, 300);

        // Rotate the main FAB icon by 45 degrees to form a close button
        Animation rotateAnim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                setRotation(45 * interpolatedTime);
            }
        };
        rotateAnim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);
        rotateAnim.setDuration(300);

        startAnimation(rotateAnim);

        // Make the list inactive by showing a screen over it
        ((ViewGroup) getParent()).addView(screen, ((ViewGroup) getParent()).indexOfChild(this));
        AlphaAnimation fadeAnimation = new AlphaAnimation(0, 1);
        fadeAnimation.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);
        fadeAnimation.setDuration(300);

        screen.startAnimation(fadeAnimation);
    }

    public void hideChildren() {
        if (!childrenVisible || delayedRunnable != null) {
            return;
        }
        childrenVisible = false;

        Animation fabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fab_out);
        fabAnim.setDuration(300);
        fabAnim.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);

        Animation labelAnim = AnimationUtils.loadAnimation(getContext(), R.anim.abc_fade_out);
        labelAnim.setDuration(300);
        labelAnim.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);

        for (FloatingActionButton c : children) {
            c.startAnimation(fabAnim);
        }
        for (TextView l : labels) {
            l.startAnimation(labelAnim);
        }

        // Make sure to hide the FABs and screen after the animation finishes
        delayedRunnable = () -> {
            for (FloatingActionButton c : children) {
                c.setVisibility(GONE);
                ((ViewGroup) c.getParent()).removeView(c);
            }
            for (TextView l : labels) {
                l.setVisibility(GONE);
                ((ViewGroup) l.getParent()).removeView(l);
            }

            ((ViewGroup) screen.getParent()).removeView(screen);

            delayedRunnable = null;
        };
        postDelayed(delayedRunnable, 300);

        // Rotate the main FAB icon by 45 degrees to invert the original rotation
        Animation rotateAnim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                setRotation(45 + 45 * interpolatedTime);
            }
        };
        rotateAnim.setInterpolator(getContext(), android.R.interpolator.decelerate_quint);
        rotateAnim.setDuration(300);

        startAnimation(rotateAnim);

        // Make the list active again by removing the screen over it
        AlphaAnimation fadeAnimation = new AlphaAnimation(1, 0);
        fadeAnimation.setInterpolator(getContext(), android.R.interpolator.accelerate_quint);
        fadeAnimation.setDuration(300);

        screen.startAnimation(fadeAnimation);
    }

    @Override
    public void onClick(View v) {
        if (v == this) {
            if (childrenVisible) {
                hideChildren();
            } else {
                showChildren();
            }
        } else if (v == screen) {
            hideChildren();
        }
    }

    public static class MenuItem {

        @DrawableRes final int iconRes;
        @StringRes final int labelRes;
        final String label;
        final OnClickListener onClickListener;

        public MenuItem(@DrawableRes int iconRes, OnClickListener onClickListener,
                        @StringRes int labelRes) {
            this.iconRes = iconRes;
            this.labelRes = labelRes;
            this.onClickListener = onClickListener;
            label = null;
        }

        public MenuItem(@DrawableRes int iconRes, OnClickListener onClickListener,
                        String label) {
            this.iconRes = iconRes;
            this.label = label;
            this.onClickListener = onClickListener;
            labelRes = 0;
        }

    }

    // A lot of code here is copied from FloatingActionButton.Behavior because I can't override the
    // methods since Google made them private. The only code that's actually functionally different
    // is in updateFabTranslationForSnackbar( ... )
    @SuppressWarnings("unused")
    public static class Behavior extends FloatingActionButton.Behavior {

        public Behavior() {
            super();
        }

        public Behavior(Context context, AttributeSet attrs) {
            super();
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent,
                                              FloatingActionButton child, View dependency) {
            if (dependency instanceof Snackbar.SnackbarLayout) {
                updateFabTranslationForSnackbar(parent, child, dependency);
                return false;
            } else {
                return super.onDependentViewChanged(parent, child, dependency);
            }
        }

        private void updateFabTranslationForSnackbar(CoordinatorLayout parent,
                                                     FloatingActionButton fab, View snackbar) {
            float translationY = this.getFabTranslationYForSnackbar(parent, fab);
            fab.setTranslationY(translationY);

            for (FloatingActionButton child : ((FABMenu) fab).children) {
                child.setTranslationY(translationY);
            }

            for (TextView label : ((FABMenu) fab).labels) {
                label.setTranslationY(translationY);
            }
        }

        private float getFabTranslationYForSnackbar(CoordinatorLayout parent,
                                                    FloatingActionButton fab) {
            float minOffset = 0.0F;
            List dependencies = parent.getDependencies(fab);
            int i = 0;

            for (int z = dependencies.size(); i < z; ++i) {
                View view = (View) dependencies.get(i);
                if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                    minOffset = Math.min(
                            minOffset, view.getTranslationY() - (float) view.getHeight());
                }
            }

            return minOffset;
        }
    }
}
