package com.marverenic.music.utils;

import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.observers.TestSubscriber;

public class ObservableQueueTest {

    private ObservableQueue<String> mSubject;

    @Before
    public void setUp() {
        mSubject = new ObservableQueue<>();
    }

    @Test
    public void testEnqueueOnce() {
        TestSubscriber<String> subscriber = new TestSubscriber<>();

        mSubject.toObservable().subscribe(subscriber);
        subscriber.assertNoTerminalEvent();
        subscriber.assertNoValues();

        mSubject.enqueue("Hello world!");
        subscriber.assertNoTerminalEvent();
        subscriber.assertValue("Hello world!");
    }

    @Test
    public void testEnqueueSeveralTimes() {
        TestSubscriber<String> subscriber = new TestSubscriber<>();

        mSubject.toObservable().subscribe(subscriber);
        subscriber.assertNoTerminalEvent();
        subscriber.assertNoValues();

        mSubject.enqueue("Hello world!");
        mSubject.enqueue("My hands are typing words!");
        mSubject.enqueue("HAAAAAANDS!");
        subscriber.assertNoTerminalEvent();
        subscriber.assertValues("Hello world!", "My hands are typing words!", "HAAAAAANDS!");
    }

    @Test
    public void testValuesPostedAfterLateSubscribe() {
        TestSubscriber<String> subscriber = new TestSubscriber<>();

        mSubject.enqueue("Hello world!");
        mSubject.enqueue("My hands are typing words!");
        mSubject.enqueue("HAAAAAANDS!");

        mSubject.toObservable().subscribe(subscriber);
        subscriber.assertNoTerminalEvent();
        subscriber.assertValues("Hello world!", "My hands are typing words!", "HAAAAAANDS!");
    }

    @Test
    public void testSubscribeTwice() {
        TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        TestSubscriber<String> subscriber2 = new TestSubscriber<>();

        mSubject.toObservable().subscribe(subscriber1);
        mSubject.enqueue("sub-1 obj-1");
        mSubject.enqueue("sub-1 obj-2");

        subscriber1.assertNoTerminalEvent();
        subscriber1.assertValues("sub-1 obj-1", "sub-1 obj-2");

        mSubject.toObservable().subscribe(subscriber2);
        subscriber1.assertCompleted();

        mSubject.enqueue("sub-2 obj-1");
        mSubject.enqueue("sub-2 obj-2");

        subscriber2.assertNoTerminalEvent();
        subscriber2.assertValues("sub-2 obj-1", "sub-2 obj-2");
    }

    @Test
    public void testEarlyUnsubscribeDoesNotDestroyQueue() {
        TestSubscriber<String> subscriber1 = new TestSubscriber<>();
        TestSubscriber<String> subscriber2 = new TestSubscriber<>();

        mSubject.enqueue("Thing 1");
        mSubject.enqueue("Thing 2");
        mSubject.enqueue("Thing 3");
        mSubject.enqueue("Thing 4");

        mSubject.toObservable().take(1).subscribe(subscriber1);

        subscriber1.assertTerminalEvent();
        subscriber1.assertValue("Thing 1");

        mSubject.toObservable().subscribe(subscriber2);
        subscriber2.assertNoTerminalEvent();
        subscriber2.assertValues("Thing 2", "Thing 3", "Thing 4");
    }

    @Test
    public void testConsistencyOfEnqueueBeforeSubscribe() {
        TestSubscriber<String> subscriber = new TestSubscriber<>();

        mSubject.enqueue("Hello world!");
        mSubject.enqueue("My hands are typing words!");

        Observable<String> observable = mSubject.toObservable();

        mSubject.enqueue("HAAAAAANDS!");
        observable.subscribe(subscriber);

        subscriber.assertNoTerminalEvent();
        subscriber.assertValues("Hello world!", "My hands are typing words!", "HAAAAAANDS!");
    }

}
