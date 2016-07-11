package com.marverenic.music.viewmodel;

import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;

public class BindingAdapters {

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

    @BindingAdapter("animation")
    public static void bindAnimation(View view, @Nullable Animation animation) {
        if (animation == null) {
            return;
        }

        view.setAnimation(animation);
        animation.start();
    }

    @BindingAdapter("textChangedListener")
    public static void bindTextChangedListener(EditText editText, TextWatcher watcher) {
        editText.addTextChangedListener(watcher);
    }

}
