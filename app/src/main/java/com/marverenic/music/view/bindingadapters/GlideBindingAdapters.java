package com.marverenic.music.view.bindingadapters;

import androidx.databinding.BindingAdapter;
import android.widget.ImageView;

import com.bumptech.glide.GenericRequestBuilder;

public class GlideBindingAdapters {

    @BindingAdapter("android:src")
    public static void bindImage(ImageView imageView, GenericRequestBuilder request) {
        if (request != null) {
            request.into(imageView);
        }
    }

}
