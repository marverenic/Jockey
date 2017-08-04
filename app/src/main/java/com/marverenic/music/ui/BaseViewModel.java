package com.marverenic.music.ui;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.DimenRes;
import android.support.annotation.StringRes;

import com.trello.rxlifecycle.components.support.RxFragment;

public abstract class BaseViewModel extends RxViewModel {

    private Context mContext;

    public BaseViewModel(RxFragment fragment) {
        super(fragment);
        mContext = fragment.getContext();
    }

    protected Context getContext() {
        return mContext;
    }

    protected Resources getResources() {
        return mContext.getResources();
    }

    protected String getString(@StringRes int stringRes) {
        return mContext.getString(stringRes);
    }

    protected float getDimension(@DimenRes int dimenRes) {
        return mContext.getResources().getDimension(dimenRes);
    }

    protected int getDimensionPixelSize(@DimenRes int dimenRes) {
        return mContext.getResources().getDimensionPixelSize(dimenRes);
    }

}
