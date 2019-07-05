package com.marverenic.music.view.bindingadapters;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;

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

    @BindingAdapter("scrollPosition")
    public static void setScrollY(RecyclerView recyclerView, int scrollPosition) {
        if (getScrollPosition(recyclerView) != scrollPosition) {
            recyclerView.scrollToPosition(scrollPosition);
        }
    }

    @InverseBindingAdapter(attribute = "scrollPosition")
    public static int getScrollPosition(RecyclerView recyclerView) {
        if (recyclerView.getChildCount() == 0) {
            return 0;
        }

        return recyclerView.getChildAdapterPosition(recyclerView.getChildAt(0));
    }

    @BindingAdapter("scrollPositionAttrChanged")
    public static void setScrollListener(RecyclerView recyclerView, InverseBindingListener listener) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                listener.onChange();
            }
        });
    }

}
