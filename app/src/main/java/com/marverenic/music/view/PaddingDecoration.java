package com.marverenic.music.view;

import android.graphics.Rect;
import androidx.annotation.Dimension;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class PaddingDecoration extends RecyclerView.ItemDecoration {

    @Dimension
    private int mVerticalPadding;

    public PaddingDecoration(@Dimension int verticalPadding) {
        mVerticalPadding = verticalPadding;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        int indexInAdapter = parent.getChildAdapterPosition(view);
        if (indexInAdapter == 0) {
            outRect.top += mVerticalPadding;
        }
        if (indexInAdapter == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom += mVerticalPadding;
        }
    }
}
