package com.marverenic.music.view;

import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.marverenic.music.R;

/**
 * GestureView is a {@link FrameLayout} that will respond to horizontal swipe gestures, display
 * a visual hint, and use a callback specified by {@link #setGestureListener(OnGestureListener)}. All
 * touch events that this view receives are consumed and may be used to start a swipe.
 *
 * When using this view in an xml layout file, you can specify additional attributes to define this
 * View's behavior including <code>leftIndicator</code> and <code>rightIndicator</code>, which are
 * equivalent to calling {@link #setLeftIndicator(Drawable)} and
 * {@link #setRightIndicator(Drawable)}.
 */
public class GestureView extends FrameLayout {

    private static final int ACTIVATION_THRESHOLD_DP = 96;
    private static final int INDICATOR_SIZE_DP = 36;
    private static final int DEFAULT_COLOR = Color.BLACK;
    private static final int TAP_DURATION_MS = 1000;
    private static final int MAX_TAP_MOVEMENT_DP = 10;

    private boolean mEnabled;

    private OnGestureListener mGestureListener;
    private Paint mOverlayPaint;
    private Point mOverlayOrigin;
    private Point mOverlayEdge;
    private long mGestureStartTime;
    private Drawable mLeftIndicator;
    private Drawable mRightIndicator;
    private Drawable mTapIndicator;
    private boolean mPreformingTap;
    private boolean mAbortedTap;
    private int mAlpha;
    private int mIndicatorSize;
    private int mActivationThreshold;

    public GestureView(Context context) {
        this(context, null);
    }

    public GestureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setOverlayAlpha(255);

        float densityMultiplier = getResources().getDisplayMetrics().density;
        mIndicatorSize = (int) (INDICATOR_SIZE_DP * densityMultiplier);
        mActivationThreshold = (int) (ACTIVATION_THRESHOLD_DP * densityMultiplier);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GestureView,
                0, 0);

        try {
            setLeftIndicator(a.getDrawable(R.styleable.GestureView_leftIndicator));
            setRightIndicator(a.getDrawable(R.styleable.GestureView_rightIndicator));
            setTapIndicator(a.getDrawable(R.styleable.GestureView_tapIndicator));
            setColor(a.getColor(R.styleable.GestureView_overlayColor, DEFAULT_COLOR));
        } finally {
            a.recycle();
        }
    }

    public void setGesturesEnabled(boolean enable) {
        mEnabled = enable;
    }

    public void setGestureListener(@Nullable OnGestureListener listener) {
        mGestureListener = listener;
    }

    public void setLeftIndicator(@Nullable Drawable icon) {
        mLeftIndicator = icon;
    }

    public void setRightIndicator(@Nullable Drawable icon) {
        mRightIndicator = icon;
    }

    public void setTapIndicator(@Nullable Drawable icon) {
        mTapIndicator = icon;
    }

    /**
     * Sets the color of the overlay background when the user is preforming a swipe gesture
     * @param color A color as an integer using the standard {@link android.graphics.Color} format
     */
    public void setColor(@ColorInt int color) {
        mOverlayPaint.setColor(color);
    }

    /**
     * This method will change the opacity of the overlay. This method is used by an
     * {@link ObjectAnimator} to animate completion events and usually shouldn't be used by
     * external classes because it will likely be overwritten.
     * (This method is public so that ObjectAnimator can find it)
     * @param alpha The new alpha of the overlay
     * @see #setColor(int) To change the overlay's ring color. If you need the overlay background to
     *                     be transparent, you can set the transparency bits like a normal
     *                     {@link android.graphics.Color} integer.
     */
    @SuppressWarnings("unused")
    public void setOverlayAlpha(int alpha) {
        mAlpha = alpha;
        invalidate();
    }

    /**
     * Sets the current radius of the overlay background. This method is used by an
     * {@link ObjectAnimator} to animate completion events and shouldn't be used by external
     * classes because it will be overwritten.
     * @param radius The new radius of the background overlay
     */
    @SuppressWarnings("unused")
    public void setRadius(int radius) {
        if (mOverlayEdge != null && mOverlayOrigin != null) {
            mOverlayEdge.x = mOverlayOrigin.x + radius;
            invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnabled) {
            return false;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mOverlayOrigin == null) {
                requestDisallowInterceptTouchEvent(true);
                mOverlayOrigin = new Point((int) event.getX(), (int) event.getY());
                mOverlayEdge = new Point(mOverlayOrigin);
                mGestureStartTime = System.currentTimeMillis();
                mPreformingTap = false;
                mAbortedTap = false;
                mAlpha = 255;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            complete();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            animateOutRadius(0);
            invalidate();
            return false;
        } else if (mOverlayOrigin != null) {
            mOverlayEdge.x = (int) event.getX();
            mOverlayEdge.y = (int) event.getY();
            invalidate();
            return true;
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mOverlayOrigin != null) {
            mOverlayPaint.setAlpha(mAlpha);

            int radius = radius();
            canvas.drawCircle(mOverlayOrigin.x, mOverlayOrigin.y, radius, mOverlayPaint);

            Drawable indicator = null;
            if (mPreformingTap || isTap()) {
                indicator = mTapIndicator;
            } else if (isLeft()) {
                indicator = mLeftIndicator;
                mAbortedTap = true;
            } else if (isRight()) {
                indicator = mRightIndicator;
                mAbortedTap = true;
            }

            if (indicator != null) {
                indicator.mutate();

                int indicatorSize = Math.min(radius, mIndicatorSize) / 2;
                indicator.setBounds(
                        mOverlayOrigin.x - indicatorSize,
                        mOverlayOrigin.y - indicatorSize,
                        mOverlayOrigin.x + indicatorSize,
                        mOverlayOrigin.y + indicatorSize);

                float alphaMultiplier =  Math.min(radius / (float) mActivationThreshold, 1);
                indicator.setAlpha((int) (mAlpha * alphaMultiplier));
                indicator.draw(canvas);

                /*
                    Because RotateDrawable does not respect .mutate() on API < 23, reset the alpha
                    to make sure that it doesn't change the transparency of any Drawables elsewhere
                    in the app
                 */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    indicator.setAlpha(255);
                }
            }
        }
    }

    /**
     * @return The radius of the circle that should be drawn when a gesture has been started
     */
    private int radius() {
        if (mOverlayOrigin == null || mOverlayEdge == null) {
            return 0;
        } else {
            return Math.abs(mOverlayOrigin.x - mOverlayEdge.x);
        }
    }

    /**
     * @return True if the current gesture is a tap. This is dependent on the gesture lasting
     *         less than a specific duration (set in {@link #TAP_DURATION_MS}) and that the gesture
     *         has not moved more than a specific distance (set in {@link #MAX_TAP_MOVEMENT_DP})
     */
    private boolean isTap() {
        return !(mOverlayEdge == null || mOverlayOrigin == null)
                && System.currentTimeMillis() - mGestureStartTime < TAP_DURATION_MS
                && radius() < MAX_TAP_MOVEMENT_DP * getResources().getDisplayMetrics().density
                && !mAbortedTap;
    }

    /**
     * @return True if the swipe gesture that's currently being handled is towards the left.
     *         If no swipe gesture's currently being handled, or the gesture doesn't have a
     *         direction, false will be returned.
     * @see #isRight()
     */
    private boolean isLeft() {
        return !(mOverlayEdge == null || mOverlayOrigin == null)
                && mOverlayEdge.x < mOverlayOrigin.x;
    }

    /**
     * @return True if the swipe gesture that's currently being handled is towards the right.
     *         If no swipe gesture's currently being handled, or the gesture doesn't have a
     *         direction, false will be returned.
     * @see #isLeft()
     */
    private boolean isRight() {
        return !(mOverlayEdge == null || mOverlayOrigin == null)
                && mOverlayEdge.x > mOverlayOrigin.x;
    }

    /**
     * @return Whether the current swipe gesture will trigger either the left or right action if
     *         it was to be released right now (or if it was released)
     *         Returns false if no swipe gesture is currently being handled
     */
    private boolean isComplete() {
        return !(mOverlayEdge == null || mOverlayOrigin == null)
                && radius() > mActivationThreshold;
    }

    /**
     * Called when a gesture completes. This method fires off the correct callback (if applicable),
     * animates the view overlay, and clears the current gesture so that another one may be started
     */
    private void complete() {
        if (isTap()) {
            mPreformingTap = true;
            if (mGestureListener != null) {
                mGestureListener.onTap();
            }
            animateOutRadius(getWidth());
        } else if (isComplete()) {
            if (isLeft()) {
                if (mGestureListener != null) {
                    mGestureListener.onLeftSwipe();
                }
                animateOutRadius(-1 * getWidth());
            } else {
                if (mGestureListener != null) {
                    mGestureListener.onRightSwipe();
                }
                animateOutRadius(getWidth());
            }
        } else {
            animateOutRadius(0);
        }
    }

    /**
     * Animates the overlay to a specified radius, fades it out, and clears the current gesture
     * @param targetRadius The radius to animate the circular overlay to
     */
    private void animateOutRadius(int targetRadius) {
        int distance = Math.abs(radius() - targetRadius);
        int time = (int) (200 / getResources().getDisplayMetrics().density / distance);
        animateOutRadius(targetRadius * 2, Math.max(time, 400), 300);
    }

    /**
     * Animates the overlay to a specified radius, fades it out, and clears the current gesture
     * @param targetRadius The radius to animate the circular overlay to
     * @param time The time for this animation to last
     * @param alphaDelay An optional delay to add before animating the transparency of the overlay
     */
    private void animateOutRadius(int targetRadius, int time, int alphaDelay) {
        ObjectAnimator alphaAnim = ObjectAnimator.ofObject(
                this, "overlayAlpha",
                new IntEvaluator(), mAlpha, 0);
        ObjectAnimator radiusAnim = ObjectAnimator.ofObject(
                this, "radius",
                new IntEvaluator(), (isRight() ? radius() : -radius()), targetRadius);

        radiusAnim
                .setDuration(time)
                .setInterpolator(AnimationUtils.loadInterpolator(getContext(),
                        android.R.interpolator.accelerate_quad));
        alphaAnim
                .setDuration(time)
                .setInterpolator(AnimationUtils.loadInterpolator(getContext(),
                        android.R.interpolator.accelerate_quad));

        radiusAnim.start();
        alphaAnim.setStartDelay(alphaDelay);
        alphaAnim.start();

        postDelayed(() -> {
            mOverlayEdge = null;
            mOverlayOrigin = null;
        }, time + alphaDelay);
    }

    public interface OnGestureListener {
        void onLeftSwipe();
        void onRightSwipe();
        void onTap();
    }
}
