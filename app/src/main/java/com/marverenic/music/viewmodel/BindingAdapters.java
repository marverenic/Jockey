package com.marverenic.music.viewmodel;

import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.widget.ImageView;

public class BindingAdapters {

    @BindingAdapter("bind:bitmap")
    public static void bindBitmap(ImageView imageView, Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    @BindingAdapter("bind:tint")
    public static void bindImageViewTint(ImageView imageView, @ColorInt int color) {
        imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @BindingAdapter("bind:backgroundTint")
    public static void bindViewBackgroundTint(View view, @ColorInt int color) {
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    @BindingAdapter("bind:marginLeft")
    public static void bindLeftMargin(View view, int margin) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.leftMargin = margin;
        view.getParent().requestLayout();
    }

    @BindingAdapter("bind:animation")
    public static void bindAnimation(View view, @Nullable Animation animation) {
        if (animation == null) {
            return;
        }

        view.setAnimation(animation);
        animation.start();
    }

}
