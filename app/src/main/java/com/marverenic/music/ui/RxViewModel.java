package com.marverenic.music.ui;

import android.content.Context;
import android.databinding.BaseObservable;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

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

}
