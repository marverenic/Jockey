package com.marverenic.music.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

public abstract class BaseViewModel extends BaseObservable {

    private Context mContext;

    public BaseViewModel(Context context) {
        mContext = context;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Resources getResources() {
        return mContext.getResources();
    }

    @ColorInt
    protected int getColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(mContext, colorRes);
    }

    protected Drawable getDrawable(@DrawableRes int drawableRes) {
        return ContextCompat.getDrawable(mContext, drawableRes);
    }

    protected String getString(@StringRes int stringRes) {
        return mContext.getString(stringRes);
    }

    protected String getString(@StringRes int stringRes, Object... formatArgs) {
        return mContext.getString(stringRes, formatArgs);
    }

    protected float getDimension(@DimenRes int dimenRes) {
        return mContext.getResources().getDimension(dimenRes);
    }

    protected int getDimensionPixelSize(@DimenRes int dimenRes) {
        return mContext.getResources().getDimensionPixelSize(dimenRes);
    }

    protected void startActivity(Intent intent) {
        mContext.startActivity(intent);
    }

}
