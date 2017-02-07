/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.annotations.NonNull;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Action;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

public class ObservableUnsubscribeOnTest {

    @Test(timeout = 5000)
    public void unsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnSameThread() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.unsafeCreate(new ObservableSource<Integer>() {

                @Override
                public void subscribe(Observer<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(UI_EVENT_LOOP).observeOn(Schedulers.computation())
            .unsubscribeOn(UI_EVENT_LOOP)
            .take(2)
            .subscribe(observer);

            observer.awaitTerminalEvent(5, TimeUnit.SECONDS);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertTrue(unsubscribeThread.toString(), unsubscribeThread == UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    @Test(timeout = 5000)
    public void unsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnDifferentThreads() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.unsafeCreate(new ObservableSource<Integer>() {

                @Override
                public void subscribe(Observer<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.computation())
            .unsubscribeOn(UI_EVENT_LOOP)
            .take(2)
            .subscribe(observer);

            observer.awaitTerminalEvent(1, TimeUnit.SECONDS);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("UI Thread: " + UI_EVENT_LOOP.getThread());
            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertSame(unsubscribeThread, UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    private static class ThreadSubscription extends AtomicBoolean implements Disposable {

        private static final long serialVersionUID = -5011338112974328771L;

        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void dispose() {
            set(true);
            System.out.println("unsubscribe invoked: " + Thread.currentThread());
            thread = Thread.currentThread();
            latch.countDown();
        }

        @Override public boolean isDisposed() {
            return get();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
    }

    public static class UIEventLoopScheduler extends Scheduler {

        private final Scheduler eventLoop;
        private volatile Thread t;

        public UIEventLoopScheduler() {

            eventLoop = Schedulers.single();

            /*
             * DON'T DO THIS IN PRODUCTION CODE
             */
            final CountDownLatch latch = new CountDownLatch(1);
            eventLoop.scheduleDirect(new Runnable() {

                @Override
                public void run() {
                    t = Thread.currentThread();
                    latch.countDown();
                }

            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("failed to initialize and get inner thread");
            }
        }

        @NonNull
        @Override
        public Worker createWorker() {
            return eventLoop.createWorker();
        }

        public Thread getThread() {
            return t;
        }

    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).unsubscribeOn(Schedulers.single()));
    }

    @Test
    public void normal() {
        final int[] calls = { 0 };

        Observable.just(1)
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                calls[0]++;
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertResult(1);

        assertEquals(0, calls[0]);
    }

    @Test
    public void error() {
        final int[] calls = { 0 };

        Observable.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                calls[0]++;
            }
        })
        .unsubscribeOn(Schedulers.single())
        .test()
        .assertFailure(TestException.class);

        assertEquals(0, calls[0]);
    }

    @Test
    public void signalAfterDispose() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());
                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .unsubscribeOn(Schedulers.single())
            .take(1)
            .test()
            .assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
