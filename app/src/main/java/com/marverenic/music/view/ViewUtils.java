package com.marverenic.music.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.R;

public final class ViewUtils {

    /**
     * This class is never instantiated
     */
    private ViewUtils() {

    }

    public static int getNumberOfGridColumns(Context context, @DimenRes int minWidthDimen) {
        Resources res = context.getResources();

        // Calculate the number of columns that can fit on the screen
        short screenWidth = (short) res.getConfiguration().screenWidthDp;
        float density = res.getDisplayMetrics().density;

        short globalPadding = (short) (res.getDimension(R.dimen.global_padding) / density);
        short minWidth = (short) (res.getDimension(minWidthDimen) / density);
        short gridPadding = (short) (res.getDimension(R.dimen.grid_margin) / density);

        short availableWidth = (short) (screenWidth - 2 * globalPadding);
        return (availableWidth) / (minWidth + 2 * gridPadding);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static boolean isRtl(Context context) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN
                && context.getResources().getConfiguration().getLayoutDirection()
                == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public static <V extends View> V findViewByClass(ViewGroup rootView, Class<V> clazz) {
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);

            if (child.getClass().equals(clazz)) {
                //noinspection unchecked
                return (V) child;
            } else if (child instanceof ViewGroup) {
                V found = findViewByClass((ViewGroup) child, clazz);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
