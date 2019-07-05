package com.marverenic.music.utils;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Queue;

import rx.Observable;
import rx.subjects.ReplaySubject;

/**
 * A data structure that combines a queue with an observable stream. Objects can be enqueued to the
 * queue, and will be sent in order to the observable returned  by {@link #toObservable()}. Objects
 * are removed from the queue immediately before they are delivered to the subscriber.
 * @param <T>
 */
public class ObservableQueue<T> {

    private final Object mLock = new Object();

    private Queue<T> mQueue;
    private ReplaySubject<T> mSubject;

    public ObservableQueue() {
        mQueue = new ArrayDeque<>();
    }

    /**
     * If this queue is subscribed to, {@code value} will immediately be sent to the observer.
     * Otherwise, this will be added to the end of the queue.
     * @param value The value to be enqueued. Must not be null.
     */
    public void enqueue(@NonNull T value) {
        synchronized (mLock) {
            mQueue.add(value);
            if (mSubject != null) {
                mSubject.onNext(value);
            }
        }
    }

    /**
     * Creates an Observable stream from this queue. There should only ever be one subscriber to
     * this method. Calling this method twice will complete any previously opened observable
     * (leaving unprocessed elements in the queue).
     * @return An observable containing the contents of the queue in order
     */
    public Observable<T> toObservable() {
        synchronized (mLock) {
            if (mSubject != null) {
                mSubject.onCompleted();
            }

            if (mQueue.isEmpty()) {
                mSubject = ReplaySubject.create();
            } else {
                mSubject = ReplaySubject.create(mQueue.size());

                for (T data : mQueue) {
                    mSubject.onNext(data);
                }
            }
        }

        return mSubject.map(item -> {
            mQueue.remove();
            return item;
        }).asObservable();
    }

}
