package com.marverenic.music.ui;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.support.annotation.CheckResult;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.trello.rxlifecycle.FragmentEvent;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.components.support.RxFragment;

import rx.Observable;

public abstract class RxViewModel extends BaseObservable {

    private Observable<FragmentEvent> mFragmentLifecycle;
    private Context mContext;

    public RxViewModel(RxFragment fragment) {
        mFragmentLifecycle = fragment.lifecycle();
        mContext = fragment.getContext();
    }

    @NonNull
    @CheckResult
    protected final <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycle.bindUntilEvent(mFragmentLifecycle, FragmentEvent.DESTROY_VIEW);
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
