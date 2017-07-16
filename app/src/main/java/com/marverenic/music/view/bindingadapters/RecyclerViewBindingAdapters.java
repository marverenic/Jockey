package com.marverenic.music.view.bindingadapters;

import android.databinding.BindingAdapter;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;

import com.marverenic.adapter.DragDropAdapter;

public class RecyclerViewBindingAdapters {

    @BindingAdapter("itemDecorations")
    public static void setItemDecorations(RecyclerView recyclerView, ItemDecoration... decor) {
        for (ItemDecoration decoration : decor) {
            recyclerView.addItemDecoration(decoration);
        }
    }

    @BindingAdapter("dragDropAdapter")
    public static void setDragDropAdapter(RecyclerView recyclerView, DragDropAdapter adapter) {
        adapter.attach(recyclerView);
    }

}
