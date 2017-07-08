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

    private Observable<FragmentEvent> fragmentLifecycle;

    public RxViewModel(RxFragment fragment) {
        fragmentLifecycle = fragment.lifecycle();
    }

    @NonNull
    @CheckResult
    protected final <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycle.bindUntilEvent(fragmentLifecycle, FragmentEvent.DESTROY_VIEW);
    }

}
