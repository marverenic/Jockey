package com.marverenic.music.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.marverenic.music.R;

public class ViewUtils {

    public static int getNumberOfGridColumns(Context context){
        // Calculate the number of columns that can fit on the screen
        final short screenWidth = (short) context.getResources().getConfiguration().screenWidthDp;
        final float density = context.getResources().getDisplayMetrics().density;
        final short globalPadding = (short) (context.getResources().getDimension(R.dimen.global_padding) / density);
        final short minWidth = (short) (context.getResources().getDimension(R.dimen.grid_width) / density);
        final short gridPadding = (short) (context.getResources().getDimension(R.dimen.grid_margin) / density);

        short availableWidth = (short) (screenWidth - 2 * globalPadding);
        return (availableWidth) / (minWidth + 2 * gridPadding);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
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
}
