package com.marverenic.music.ui;

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

    public RxViewModel(RxFragment fragment) {
        mFragmentLifecycle = fragment.lifecycle();
    }

    @NonNull
    @CheckResult
    protected final <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycle.bindUntilEvent(mFragmentLifecycle, FragmentEvent.DESTROY_VIEW);
    }

}
