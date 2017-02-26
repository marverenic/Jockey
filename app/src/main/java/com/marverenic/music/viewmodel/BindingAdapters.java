package com.marverenic.music.viewmodel;

import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.widget.Toolbar;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;

import com.marverenic.music.view.ViewUtils;

public class BindingAdapters {

    @BindingAdapter("height")
    public static void bindHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    @BindingAdapter("bitmap")
    public static void bindBitmap(ImageView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    @BindingAdapter("tint")
    public static void bindImageViewTint(ImageView imageView, @ColorInt int color) {
        imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @BindingAdapter("backgroundTint")
    public static void bindViewBackgroundTint(View view, @ColorInt int color) {
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @BindingAdapter("marginLeft")
    public static void bindLeftMargin(View view, int margin) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.leftMargin = margin;
        view.getParent().requestLayout();
    }

    @BindingAdapter("android:layout_marginBottom")
    public static void bindBottomMargin(View view, int margin) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.bottomMargin = margin;
        view.getParent().requestLayout();
    }

    @BindingAdapter("android:layout_marginTop")
    public static void bindTopMargin(View view, int margin) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.topMargin = margin;
        view.getParent().requestLayout();
    }

    @BindingAdapter("animation")
    public static void bindAnimation(View view, @Nullable Animation animation) {
        if (animation == null) {
            return;
        }

        view.setAnimation(animation);
        animation.start();
    }

    @BindingAdapter("behavior_bottomSheetCallback")
    public static void bindBottomSheetCallback(View view,
                                               BottomSheetBehavior.BottomSheetCallback callback) {

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
        behavior.setBottomSheetCallback(callback);
    }

    @BindingAdapter("behavior_peekHeight")
    public static void bindPeekHeight(View view, int peekHeight) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(view);
        behavior.setPeekHeight(peekHeight);
    }

    @BindingAdapter("textChangedListener")
    public static void bindTextChangedListener(EditText editText, TextWatcher watcher) {
        editText.addTextChangedListener(watcher);
    }

    @BindingAdapter("toolbar_marginTop")
    public static void bindToolbarMarginTop(ViewGroup toolbarContainer, int marginTop) {
        Toolbar toolbar = ViewUtils.findViewByClass(toolbarContainer, Toolbar.class);
        bindTopMargin(toolbar, marginTop);
    }

}
