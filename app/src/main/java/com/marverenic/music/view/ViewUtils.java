package com.marverenic.music.view;

import android.content.Context;

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
}
