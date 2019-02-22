package com.marverenic.music.utils;

import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.Toolbar;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ImageView;

import com.marverenic.music.view.ViewUtils;

import timber.log.Timber;

public class BindingAdapters {

    @BindingAdapter("height")
    public static void bindHeight(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height;
        view.setLayoutParams(params);
    }

    @BindingAdapter(value = { "bitmap", "cornerRadius" })
    public static void bindRoundedBitmap(ImageView imageView, Bitmap bitmap, float cornerRadius) {
        ViewUtils.whenLaidOut(imageView, () -> {
            if (imageView.getWidth() == 0 || imageView.getHeight() == 0) {
                Timber.e("ImageView has a dimension of 0", new RuntimeException());
                imageView.setImageBitmap(bitmap);
            } else {
                RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory
                        .create(imageView.getResources(), bitmap);

                float scaleX = bitmap.getWidth() / imageView.getWidth();
                float scaleY = bitmap.getHeight() / imageView.getHeight();
                drawable.setCornerRadius(Math.min(scaleX, scaleY) * cornerRadius);

                imageView.setImageDrawable(drawable);
            }
        });
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
        callback.onStateChanged(view, behavior.getState());
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

    @BindingAdapter("toolbar_alpha")
    public static void bindToolbarAlpha(ViewGroup toolbarContainer, float alpha) {
        Toolbar toolbar = ViewUtils.findViewByClass(toolbarContainer, Toolbar.class);
        toolbar.setAlpha(alpha);
    }

    @BindingAdapter("toolbar_expanded")
    public static void bindToolbarExpanded(ViewGroup container, boolean expanded) {
        if (expanded) {
            AppBarLayout appBarLayout = ViewUtils.findViewByClass(container, AppBarLayout.class);
            appBarLayout.setExpanded(true);
        }
    }

    @BindingAdapter("android:translationYPercent")
    public static void bindTranslationYPercent(View view, float percent) {
        View parent = (View) view.getParent();
        MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();
        int remainingHeight = parent.getMeasuredHeight() - layoutParams.bottomMargin;
        view.setTranslationY(-1 * percent * remainingHeight);
    }

}
