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
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.*;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.*;

public class ObservableConcatTest {

    @Test
    public void testConcat() {
        Observer<String> observer = TestHelper.mockObserver();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);

        Observable<String> concat = Observable.concat(odds, even);
        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatWithList() {
        Observer<String> observer = TestHelper.mockObserver();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);
        final List<Observable<String>> list = new ArrayList<Observable<String>>();
        list.add(odds);
        list.add(even);
        Observable<String> concat = Observable.concat(Observable.fromIterable(list));
        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatObservableOfObservables() {
        Observer<String> observer = TestHelper.mockObserver();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);

        Observable<Observable<String>> observableOfObservables = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {

            @Override
            public void subscribe(Observer<? super Observable<String>> observer) {
                observer.onSubscribe(Disposables.empty());
                // simulate what would happen in an Observable
                observer.onNext(odds);
                observer.onNext(even);
                observer.onComplete();
            }

        });
        Observable<String> concat = Observable.concat(observableOfObservables);

        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    /**
     * Simple concat of 2 asynchronous observables ensuring it emits in correct order.
     */
    @Test
    public void testSimpleAsyncConcat() {
        Observer<String> observer = TestHelper.mockObserver();

        TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
        TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");

        Observable.concat(Observable.unsafeCreate(o1), Observable.unsafeCreate(o2)).subscribe(observer);

        try {
            // wait for async observables to complete
            o1.t.join();
            o2.t.join();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads");
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onNext("five");
        inOrder.verify(observer, times(1)).onNext("six");
    }

    @Test
    public void testNestedAsyncConcatLoop() throws Throwable {
        for (int i = 0; i < 500; i++) {
            if (i % 10 == 0) {
                System.out.println("testNestedAsyncConcat >> " + i);
            }
            testNestedAsyncConcat();
        }
    }

    /**
     * Test an async Observable that emits more async Observables.
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testNestedAsyncConcat() throws InterruptedException {
        Observer<String> observer = TestHelper.mockObserver();

        final TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");
        final TestObservable<String> o3 = new TestObservable<String>("seven", "eight", "nine");
        final CountDownLatch allowThird = new CountDownLatch(1);

        final AtomicReference<Thread> parent = new AtomicReference<Thread>();
        final CountDownLatch parentHasStarted = new CountDownLatch(1);
        final CountDownLatch parentHasFinished = new CountDownLatch(1);


        Observable<Observable<String>> observableOfObservables = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {

            @Override
            public void subscribe(final Observer<? super Observable<String>> observer) {
                final Disposable d = Disposables.empty();
                observer.onSubscribe(d);
                parent.set(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // emit first
                            if (!d.isDisposed()) {
                                System.out.println("Emit o1");
                                observer.onNext(Observable.unsafeCreate(o1));
                            }
                            // emit second
                            if (!d.isDisposed()) {
                                System.out.println("Emit o2");
                                observer.onNext(Observable.unsafeCreate(o2));
                            }

                            // wait until sometime later and emit third
                            try {
                                allowThird.await();
                            } catch (InterruptedException e) {
                                observer.onError(e);
                            }
                            if (!d.isDisposed()) {
                                System.out.println("Emit o3");
                                observer.onNext(Observable.unsafeCreate(o3));
                            }

                        } catch (Throwable e) {
                            observer.onError(e);
                        } finally {
                            System.out.println("Done parent Observable");
                            observer.onComplete();
                            parentHasFinished.countDown();
                        }
                    }
                }));
                parent.get().start();
                parentHasStarted.countDown();
            }
        });

        Observable.concat(observableOfObservables).subscribe(observer);

        // wait for parent to start
        parentHasStarted.await();

        try {
            // wait for first 2 async observables to complete
            System.out.println("Thread1 is starting ... waiting for it to complete ...");
            o1.waitForThreadDone();
            System.out.println("Thread2 is starting ... waiting for it to complete ...");
            o2.waitForThreadDone();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads", e);
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onNext("five");
        inOrder.verify(observer, times(1)).onNext("six");
        // we shouldn't have the following 3 yet
        inOrder.verify(observer, never()).onNext("seven");
        inOrder.verify(observer, never()).onNext("eight");
        inOrder.verify(observer, never()).onNext("nine");
        // we should not be completed yet
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));

        // now allow the third
        allowThird.countDown();

        try {
            // wait for 3rd to complete
            o3.waitForThreadDone();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads", e);
        }

        try {
            // wait for the parent to complete
            if (!parentHasFinished.await(5, TimeUnit.SECONDS)) {
                fail("Parent didn't finish within the time limit");
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads", e);
        }

        inOrder.verify(observer, times(1)).onNext("seven");
        inOrder.verify(observer, times(1)).onNext("eight");
        inOrder.verify(observer, times(1)).onNext("nine");

        verify(observer, never()).onError(any(Throwable.class));
        inOrder.verify(observer, times(1)).onComplete();
    }

    @Test
    public void testBlockedObservableOfObservables() {
        Observer<String> observer = TestHelper.mockObserver();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };
        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        @SuppressWarnings("unchecked")
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(callOnce, okToContinue, odds, even);
        Observable<String> concatF = Observable.concat(Observable.unsafeCreate(observableOfObservables));
        concatF.subscribe(observer);
        try {
            //Block main thread to allow observables to serve up o1.
            callOnce.await();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concated Observable should have served up all of the odds.
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("3");
        verify(observer, times(1)).onNext("5");
        verify(observer, times(1)).onNext("7");

        try {
            // unblock observables so it can serve up o2 and complete
            okToContinue.countDown();
            observableOfObservables.t.join();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concatenated Observable should now have served up all the evens.
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("4");
        verify(observer, times(1)).onNext("6");
    }

    @Test
    public void testConcatConcurrentWithInfinity() {
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        //This Observable will send "hello" MAX_VALUE time.
        final TestObservable<String> w2 = new TestObservable<String>("hello", Integer.MAX_VALUE);

        Observer<String> observer = TestHelper.mockObserver();

        @SuppressWarnings("unchecked")
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(Observable.unsafeCreate(w1), Observable.unsafeCreate(w2));
        Observable<String> concatF = Observable.concat(Observable.unsafeCreate(observableOfObservables));

        concatF.take(50).subscribe(observer);

        //Wait for the thread to start up.
        try {
            w1.waitForThreadDone();
            w2.waitForThreadDone();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(47)).onNext("hello");
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConcatNonBlockingObservables() {

        final CountDownLatch okToContinueW1 = new CountDownLatch(1);
        final CountDownLatch okToContinueW2 = new CountDownLatch(1);

        final TestObservable<String> w1 = new TestObservable<String>(null, okToContinueW1, "one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(null, okToContinueW2, "four", "five", "six");

        Observer<String> observer = TestHelper.mockObserver();

        Observable<Observable<String>> observableOfObservables = Observable.unsafeCreate(new ObservableSource<Observable<String>>() {

            @Override
            public void subscribe(Observer<? super Observable<String>> observer) {
                observer.onSubscribe(Disposables.empty());
                // simulate what would happen in an Observable
                observer.onNext(Observable.unsafeCreate(w1));
                observer.onNext(Observable.unsafeCreate(w2));
                observer.onComplete();
            }

        });
        Observable<String> concat = Observable.concat(observableOfObservables);
        concat.subscribe(observer);

        verify(observer, times(0)).onComplete();

        try {
            // release both threads
            okToContinueW1.countDown();
            okToContinueW2.countDown();
            // wait for both to finish
            w1.t.join();
            w2.t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onNext("five");
        inOrder.verify(observer, times(1)).onNext("six");
        verify(observer, times(1)).onComplete();

    }

    /**
     * Test unsubscribing the concatenated Observable in a single thread.
     */
    @Test
    public void testConcatUnsubscribe() {
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);

        final Observable<String> concat = Observable.concat(Observable.unsafeCreate(w1), Observable.unsafeCreate(w2));

        try {
            // Subscribe
            concat.subscribe(ts);
            //Block main thread to allow Observable "w1" to complete and Observable "w2" to call onNext once.
            callOnce.await();
            // Unsubcribe
            ts.dispose();
            //Unblock the Observable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, never()).onNext("five");
        inOrder.verify(observer, never()).onNext("six");
        inOrder.verify(observer, never()).onComplete();

    }

    /**
     * All observables will be running in different threads so subscribe() is unblocked. CountDownLatch is only used in order to call unsubscribe() in a predictable manner.
     */
    @Test
    public void testConcatUnsubscribeConcurrent() {
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

        Observer<String> observer = TestHelper.mockObserver();
        TestObserver<String> ts = new TestObserver<String>(observer);

        @SuppressWarnings("unchecked")
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(Observable.unsafeCreate(w1), Observable.unsafeCreate(w2));
        Observable<String> concatF = Observable.concat(Observable.unsafeCreate(observableOfObservables));

        concatF.subscribe(ts);

        try {
            //Block main thread to allow Observable "w1" to complete and Observable "w2" to call onNext exactly once.
            callOnce.await();
            //"four" from w2 has been processed by onNext()
            ts.dispose();
            //"five" and "six" will NOT be processed by onNext()
            //Unblock the Observable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, never()).onNext("five");
        inOrder.verify(observer, never()).onNext("six");
        verify(observer, never()).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    static class TestObservable<T> implements ObservableSource<T> {

        private final Disposable s = new Disposable() {
            @Override
            public void dispose() {
                    subscribed = false;
            }

            @Override
            public boolean isDisposed() {
                return subscribed;
            }
        };
        private final List<T> values;
        private Thread t;
        private int count;
        private volatile boolean subscribed = true;
        private final CountDownLatch once;
        private final CountDownLatch okToContinue;
        private final CountDownLatch threadHasStarted = new CountDownLatch(1);
        private final T seed;
        private final int size;

        TestObservable(T... values) {
            this(null, null, values);
        }

        TestObservable(CountDownLatch once, CountDownLatch okToContinue, T... values) {
            this.values = Arrays.asList(values);
            this.size = this.values.size();
            this.once = once;
            this.okToContinue = okToContinue;
            this.seed = null;
        }

        TestObservable(T seed, int size) {
            values = null;
            once = null;
            okToContinue = null;
            this.seed = seed;
            this.size = size;
        }

        @Override
        public void subscribe(final Observer<? super T> observer) {
            observer.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (count < size && subscribed) {
                            if (null != values) {
                                observer.onNext(values.get(count));
                            } else {
                                observer.onNext(seed);
                            }
                            count++;
                            //Unblock the main thread to call unsubscribe.
                            if (null != once) {
                                once.countDown();
                            }
                            //Block until the main thread has called unsubscribe.
                            if (null != okToContinue) {
                                okToContinue.await(5, TimeUnit.SECONDS);
                            }
                        }
                        if (subscribed) {
                            observer.onComplete();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }
                }

            });
            t.start();
            threadHasStarted.countDown();
        }

        void waitForThreadDone() throws InterruptedException {
            threadHasStarted.await();
            t.join();
        }
    }

    @Test
    public void testMultipleObservers() {
        Observer<Object> o1 = TestHelper.mockObserver();
        Observer<Object> o2 = TestHelper.mockObserver();

        TestScheduler s = new TestScheduler();

        Observable<Long> timer = Observable.interval(500, TimeUnit.MILLISECONDS, s).take(2);
        Observable<Long> o = Observable.concat(timer, timer);

        o.subscribe(o1);
        o.subscribe(o2);

        InOrder inOrder1 = inOrder(o1);
        InOrder inOrder2 = inOrder(o2);

        s.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        inOrder1.verify(o1, times(1)).onNext(0L);
        inOrder2.verify(o2, times(1)).onNext(0L);

        s.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        inOrder1.verify(o1, times(1)).onNext(1L);
        inOrder2.verify(o2, times(1)).onNext(1L);

        s.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        inOrder1.verify(o1, times(1)).onNext(0L);
        inOrder2.verify(o2, times(1)).onNext(0L);

        s.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        inOrder1.verify(o1, times(1)).onNext(1L);
        inOrder2.verify(o2, times(1)).onNext(1L);

        inOrder1.verify(o1, times(1)).onComplete();
        inOrder2.verify(o2, times(1)).onComplete();

        verify(o1, never()).onError(any(Throwable.class));
        verify(o2, never()).onError(any(Throwable.class));
    }

    @Test
    public void concatVeryLongObservableOfObservables() {
        final int n = 10000;
        Observable<Observable<Integer>> source = Observable.range(0, n).map(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.just(v);
            }
        });

        Single<List<Integer>> result = Observable.concat(source).toList();

        SingleObserver<List<Integer>> o = TestHelper.mockSingleObserver();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        List<Integer> list = new ArrayList<Integer>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        inOrder.verify(o).onSuccess(list);
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void concatVeryLongObservableOfObservablesTakeHalf() {
        final int n = 10000;
        Observable<Observable<Integer>> source = Observable.range(0, n).map(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) {
                return Observable.just(v);
            }
        });

        Single<List<Integer>> result = Observable.concat(source).take(n / 2).toList();

        SingleObserver<List<Integer>> o = TestHelper.mockSingleObserver();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        List<Integer> list = new ArrayList<Integer>(n);
        for (int i = 0; i < n / 2; i++) {
            list.add(i);
        }
        inOrder.verify(o).onSuccess(list);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConcatOuterBackpressure() {
        assertEquals(1,
                (int) Observable.<Integer> empty()
                        .concatWith(Observable.just(1))
                        .take(1)
                        .blockingSingle());
    }

    // https://github.com/ReactiveX/RxJava/issues/1818
    @Test
    public void testConcatWithNonCompliantSourceDoubleOnComplete() {
        Observable<String> o = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(Observer<? super String> s) {
                s.onSubscribe(Disposables.empty());
                s.onNext("hello");
                s.onComplete();
                s.onComplete();
            }

        });

        TestObserver<String> ts = new TestObserver<String>();
        Observable.concat(o, o).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("hello", "hello");
    }

    @Test(timeout = 30000)
    public void testIssue2890NoStackoverflow() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, Observable<Integer>> func = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                Observable<Integer> o = Observable.just(t)
                        .subscribeOn(sch)
                ;
                Subject<Integer> subject = UnicastSubject.create();
                o.subscribe(subject);
                return subject;
            }
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, n)
        .concatMap(func).subscribe(new DefaultObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                // Consume after sleep for 1 ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // ignored
                }
                if (counter.getAndIncrement() % 100 == 0) {
                    System.out.println("testIssue2890NoStackoverflow -> " + counter.get());
                };
            }

            @Override
            public void onComplete() {
                executor.shutdown();
            }

            @Override
            public void onError(Throwable e) {
                executor.shutdown();
            }
        });

        executor.awaitTermination(20000, TimeUnit.MILLISECONDS);

        assertEquals(n, counter.get());
    }

    @Test//(timeout = 100000)
    public void concatMapRangeAsyncLoopIssue2876() {
        final long durationSeconds = 2;
        final long startTime = System.currentTimeMillis();
        for (int i = 0;; i++) {
            //only run this for a max of ten seconds
            if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(durationSeconds)) {
                return;
            }
            if (i % 1000 == 0) {
                System.out.println("concatMapRangeAsyncLoop > " + i);
            }
            TestObserver<Integer> ts = new TestObserver<Integer>();
            Observable.range(0, 1000)
            .concatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.fromIterable(Arrays.asList(t));
                }
            })
            .observeOn(Schedulers.computation()).subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            ts.assertTerminated();
            ts.assertNoErrors();
            assertEquals(1000, ts.valueCount());
            assertEquals((Integer)999, ts.values().get(999));
        }
    }

    @Test
    public void concat3() {
        Observable.concat(Observable.just(1), Observable.just(2), Observable.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void concat4() {
        Observable.concat(Observable.just(1), Observable.just(2),
                Observable.just(3), Observable.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayError() {
        Observable.concatArrayDelayError(Observable.just(1), Observable.just(2),
                Observable.just(3), Observable.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayErrorWithError() {
        Observable.concatArrayDelayError(Observable.just(1), Observable.just(2),
                Observable.just(3).concatWith(Observable.<Integer>error(new TestException())),
                Observable.just(4))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayError() {
        Observable.concatDelayError(
                Arrays.asList(Observable.just(1), Observable.just(2),
                Observable.just(3), Observable.just(4)))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayErrorWithError() {
        Observable.concatDelayError(
                Arrays.asList(Observable.just(1), Observable.just(2),
                Observable.just(3).concatWith(Observable.<Integer>error(new TestException())),
                Observable.just(4)))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4);
    }

    @Test
    public void concatObservableDelayError() {
        Observable.concatDelayError(
                Observable.just(Observable.just(1), Observable.just(2),
                Observable.just(3), Observable.just(4)))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @Test
    public void concatObservableDelayErrorWithError() {
        Observable.concatDelayError(
                Observable.just(Observable.just(1), Observable.just(2),
                Observable.just(3).concatWith(Observable.<Integer>error(new TestException())),
                Observable.just(4)))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4);
    }

    @Test
    public void concatObservableDelayErrorBoundary() {
        Observable.concatDelayError(
                Observable.just(Observable.just(1), Observable.just(2),
                Observable.just(3).concatWith(Observable.<Integer>error(new TestException())),
                Observable.just(4)), 2, false)
        .test()
        .assertFailure(TestException.class, 1, 2, 3);
    }

    @Test
    public void concatObservableDelayErrorTillEnd() {
        Observable.concatDelayError(
                Observable.just(Observable.just(1), Observable.just(2),
                Observable.just(3).concatWith(Observable.<Integer>error(new TestException())),
                Observable.just(4)), 2, true)
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4);
    }

    @Test
    public void concatMapDelayError() {
        Observable.just(Observable.just(1), Observable.just(2))
        .concatMapDelayError(Functions.<Observable<Integer>>identity())
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatMapDelayErrorWithError() {
        Observable.just(Observable.just(1).concatWith(Observable.<Integer>error(new TestException())), Observable.just(2))
        .concatMapDelayError(Functions.<Observable<Integer>>identity())
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void concatMapIterableBufferSize() {

        Observable.just(1, 2).concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(1, 2, 3, 4, 5);
            }
        }, 1)
        .test()
        .assertResult(1, 2, 3, 4, 5, 1, 2, 3, 4, 5);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void emptyArray() {
        assertSame(Observable.empty(), Observable.concatArrayDelayError());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void singleElementArray() {
        assertSame(Observable.never(), Observable.concatArrayDelayError(Observable.never()));
    }

    @Test
    public void concatMapDelayErrorEmptySource() {
        assertSame(Observable.empty(), Observable.<Object>empty()
                .concatMapDelayError(new Function<Object, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Object v) throws Exception {
                        return Observable.just(1);
                    }
                }, 16, true));
    }

    @Test
    public void concatMapDelayErrorJustSource() {
        Observable.just(0)
        .concatMapDelayError(new Function<Object, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16, true)
        .test()
        .assertResult(1);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayEmpty() {
        assertSame(Observable.empty(), Observable.concatArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArraySingleElement() {
        assertSame(Observable.never(), Observable.concatArray(Observable.never()));
    }

    @Test
    public void concatMapErrorEmptySource() {
        assertSame(Observable.empty(), Observable.<Object>empty()
                .concatMap(new Function<Object, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Object v) throws Exception {
                        return Observable.just(1);
                    }
                }, 16));
    }

    @Test
    public void concatMapJustSource() {
        Observable.just(0)
        .concatMap(new Function<Object, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Object v) throws Exception {
                return Observable.just(1);
            }
        }, 16)
        .test()
        .assertResult(1);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscription() {
        final int[] calls = { 0 };

        Observable<Integer> source = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onNext(1);
                s.onComplete();
            }
        });

        Observable.concatArray(source, source).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscriptionDelayError() {
        final int[] calls = { 0 };

        Observable<Integer> source = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onNext(1);
                s.onComplete();
            }
        });

        Observable.concatArrayDelayError(source, source).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscriptionIterable() {
        final int[] calls = { 0 };

        Observable<Integer> source = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onNext(1);
                s.onComplete();
            }
        });

        Observable.concat(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noSubsequentSubscriptionDelayErrorIterable() {
        final int[] calls = { 0 };

        Observable<Integer> source = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> s) throws Exception {
                calls[0]++;
                s.onNext(1);
                s.onComplete();
            }
        });

        Observable.concatDelayError(Arrays.asList(source, source)).firstElement()
        .test()
        .assertResult(1);

        assertEquals(1, calls[0]);
    }

    @Test
    public void concatReportsDisposedOnComplete() {
        final Disposable[] disposable = { null };

        Observable.concat(Observable.just(1), Observable.just(2))
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void concatReportsDisposedOnCompleteDelayError() {
        final Disposable[] disposable = { null };

        Observable.concatArrayDelayError(Observable.just(1), Observable.just(2))
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }

    @Test
    public void concatReportsDisposedOnError() {
        final Disposable[] disposable = { null };

        Observable.concat(Observable.just(1), Observable.<Integer>error(new TestException()))
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void concatReportsDisposedOnErrorDelayError() {
        final Disposable[] disposable = { null };

        Observable.concatArrayDelayError(Observable.just(1), Observable.<Integer>error(new TestException()))
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {
                disposable[0] = d;
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(disposable[0].isDisposed());
    }
}
