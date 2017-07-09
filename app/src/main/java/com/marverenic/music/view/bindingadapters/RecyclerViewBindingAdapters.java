package com.marverenic.music.view.bindingadapters;

import android.databinding.BindingAdapter;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;

public class RecyclerViewBindingAdapters {

    @BindingAdapter("itemDecorations")
    public static void setItemDecorations(RecyclerView recyclerView, ItemDecoration... decor) {
        for (ItemDecoration decoration : decor) {
            recyclerView.addItemDecoration(decoration);
        }
    }

}
