package com.marverenic.music.utils;

import android.support.annotation.Nullable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public final class RxProperty<T> {

    private final String mName;
    private final T mNullValue;
    private final BehaviorSubject<Optional<T>> mSubject;
    private final Observable<T> mObservable;

    @Nullable
    private Retriever<T> mFallbackRetriever;

    @Nullable
    private Retriever<T> mRetriever;

    public RxProperty(String propertyName) {
        this(propertyName, null);
    }

    public RxProperty(String propertyName, T nullValue) {
        mName = propertyName;
        mNullValue = nullValue;
        mSubject = BehaviorSubject.create();

        mObservable = mSubject.filter(Optional::isPresent)
                .map(Optional::getValue)
                .distinctUntilChanged();
    }

    public void setFunction(Retriever<T> retriever) {
        mRetriever = retriever;
    }

    public void setFallbackFunction(@Nullable Retriever<T> retriever) {
        mFallbackRetriever = retriever;
    }

    public void invalidate() {
        mSubject.onNext(Optional.empty());
        Observable<T> retriever;

        if (mRetriever != null) {
            retriever = Observable.fromCallable(mRetriever::retrieve);
        } else if (mFallbackRetriever != null) {
            retriever = Observable.fromCallable(mFallbackRetriever::retrieve);
        } else {
            return;
        }

        retriever.subscribeOn(Schedulers.computation())
                .map(data -> (data == null) ? mNullValue : data)
                .map(Optional::ofNullable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mSubject::onNext, throwable -> {
                    Timber.e(throwable, "Failed to fetch " + mName + " property.");
                });
    }

    public boolean isSubscribedTo() {
        return mSubject.hasObservers();
    }

    public void setValue(T value) {
        mSubject.onNext(Optional.ofNullable(value));
    }

    public boolean hasValue() {
        return mSubject.getValue() != null && mSubject.getValue().isPresent();
    }

    public T lastValue() {
        return mSubject.getValue().getValue();
    }

    public Observable<T> getObservable() {
        return mObservable;
    }

    public interface Retriever<T> {
        T retrieve() throws Exception;
    }

}
