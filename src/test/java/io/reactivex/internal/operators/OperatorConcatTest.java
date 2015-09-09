/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.BooleanDisposable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.*;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorConcatTest {

    @Test
    public void testConcat() {
        Subscriber<String> observer = TestHelper.mockSubscriber();

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
        Subscriber<String> observer = TestHelper.mockSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);
        final List<Observable<String>> list = new ArrayList<>();
        list.add(odds);
        list.add(even);
        Observable<String> concat = Observable.concat(Observable.fromIterable(list));
        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatObservableOfObservables() {
        Subscriber<String> observer = TestHelper.mockSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);

        Observable<Observable<String>> observableOfObservables = Observable.create(new Publisher<Observable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                // simulate what would happen in an observable
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
        Subscriber<String> observer = TestHelper.mockSubscriber();

        TestObservable<String> o1 = new TestObservable<>("one", "two", "three");
        TestObservable<String> o2 = new TestObservable<>("four", "five", "six");

        Observable.concat(Observable.create(o1), Observable.create(o2)).subscribe(observer);

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
     * Test an async Observable that emits more async Observables
     */
    @Test
    public void testNestedAsyncConcat() throws Throwable {
        Subscriber<String> observer = TestHelper.mockSubscriber();

        final TestObservable<String> o1 = new TestObservable<>("one", "two", "three");
        final TestObservable<String> o2 = new TestObservable<>("four", "five", "six");
        final TestObservable<String> o3 = new TestObservable<>("seven", "eight", "nine");
        final CountDownLatch allowThird = new CountDownLatch(1);

        final AtomicReference<Thread> parent = new AtomicReference<>();
        final CountDownLatch parentHasStarted = new CountDownLatch(1);
        final CountDownLatch parentHasFinished = new CountDownLatch(1);
        
        
        Observable<Observable<String>> observableOfObservables = Observable.create(new Publisher<Observable<String>>() {

            @Override
            public void subscribe(final Subscriber<? super Observable<String>> observer) {
                final BooleanDisposable s = new BooleanDisposable();
                observer.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                        
                    }
                    @Override
                    public void cancel() {
                        s.dispose();
                    }
                });
                parent.set(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // emit first
                            if (!s.isDisposed()) {
                                System.out.println("Emit o1");
                                observer.onNext(Observable.create(o1));
                            }
                            // emit second
                            if (!s.isDisposed()) {
                                System.out.println("Emit o2");
                                observer.onNext(Observable.create(o2));
                            }

                            // wait until sometime later and emit third
                            try {
                                allowThird.await();
                            } catch (InterruptedException e) {
                                observer.onError(e);
                            }
                            if (!s.isDisposed()) {
                                System.out.println("Emit o3");
                                observer.onNext(Observable.create(o3));
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
        Subscriber<String> observer = TestHelper.mockSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };
        final Observable<String> odds = Observable.fromArray(o);
        final Observable<String> even = Observable.fromArray(e);
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<>(callOnce, okToContinue, odds, even);
        Observable<String> concatF = Observable.concat(Observable.create(observableOfObservables));
        concatF.subscribe(observer);
        try {
            //Block main thread to allow observables to serve up o1.
            callOnce.await();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concated observable should have served up all of the odds.
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
        // The concatenated observable should now have served up all the evens.
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("4");
        verify(observer, times(1)).onNext("6");
    }

    @Test
    public void testConcatConcurrentWithInfinity() {
        final TestObservable<String> w1 = new TestObservable<>("one", "two", "three");
        //This observable will send "hello" MAX_VALUE time.
        final TestObservable<String> w2 = new TestObservable<>("hello", Integer.MAX_VALUE);

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<>(Observable.create(w1), Observable.create(w2));
        Observable<String> concatF = Observable.concat(Observable.create(observableOfObservables));

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

        final TestObservable<String> w1 = new TestObservable<>(null, okToContinueW1, "one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<>(null, okToContinueW2, "four", "five", "six");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        
        Observable<Observable<String>> observableOfObservables = Observable.create(new Publisher<Observable<String>>() {

            @Override
            public void subscribe(Subscriber<? super Observable<String>> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                // simulate what would happen in an observable
                observer.onNext(Observable.create(w1));
                observer.onNext(Observable.create(w2));
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
        final TestObservable<String> w1 = new TestObservable<>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<>(callOnce, okToContinue, "four", "five", "six");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<>(observer, null);

        final Observable<String> concat = Observable.concat(Observable.create(w1), Observable.create(w2));

        try {
            // Subscribe
            concat.subscribe(ts);
            //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
            callOnce.await();
            // Unsubcribe
            ts.dispose();
            //Unblock the observable to continue.
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
        final TestObservable<String> w1 = new TestObservable<>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<>(callOnce, okToContinue, "four", "five", "six");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<>(observer, null);
        
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<>(Observable.create(w1), Observable.create(w2));
        Observable<String> concatF = Observable.concat(Observable.create(observableOfObservables));

        concatF.subscribe(ts);

        try {
            //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext exactly once.
            callOnce.await();
            //"four" from w2 has been processed by onNext()
            ts.dispose();
            //"five" and "six" will NOT be processed by onNext()
            //Unblock the observable to continue.
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

    private static class TestObservable<T> implements Publisher<T> {

        private final Subscription s = new Subscription() {

            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                subscribed = false;
            }
        };
        private final List<T> values;
        private Thread t = null;
        private int count = 0;
        private volatile boolean subscribed = true;
        private final CountDownLatch once;
        private final CountDownLatch okToContinue;
        private final CountDownLatch threadHasStarted = new CountDownLatch(1);
        private final T seed;
        private final int size;

        @SafeVarargs
        public TestObservable(T... values) {
            this(null, null, values);
        }

        @SafeVarargs
        public TestObservable(CountDownLatch once, CountDownLatch okToContinue, T... values) {
            this.values = Arrays.asList(values);
            this.size = this.values.size();
            this.once = once;
            this.okToContinue = okToContinue;
            this.seed = null;
        }

        public TestObservable(T seed, int size) {
            values = null;
            once = null;
            okToContinue = null;
            this.seed = seed;
            this.size = size;
        }

        @Override
        public void subscribe(final Subscriber<? super T> observer) {
            observer.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (count < size && subscribed) {
                            if (null != values)
                                observer.onNext(values.get(count));
                            else
                                observer.onNext(seed);
                            count++;
                            //Unblock the main thread to call unsubscribe.
                            if (null != once)
                                once.countDown();
                            //Block until the main thread has called unsubscribe.
                            if (null != okToContinue)
                                okToContinue.await(5, TimeUnit.SECONDS);
                        }
                        if (subscribed)
                            observer.onComplete();
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
        Subscriber<Object> o1 = TestHelper.mockSubscriber();
        Subscriber<Object> o2 = TestHelper.mockSubscriber();

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
        Observable<Observable<Integer>> source = Observable.range(0, n).map(Observable::just);
        
        Observable<List<Integer>> result = Observable.concat(source).toList();
        
        Subscriber<List<Integer>> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);

        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        inOrder.verify(o).onNext(list);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void concatVeryLongObservableOfObservablesTakeHalf() {
        final int n = 10000;
        Observable<Observable<Integer>> source = Observable.range(0, n).map(Observable::just);
        
        Observable<List<Integer>> result = Observable.concat(source).take(n / 2).toList();
        
        Subscriber<List<Integer>> o = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(o);
        
        result.subscribe(o);

        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n / 2; i++) {
            list.add(i);
        }
        inOrder.verify(o).onNext(list);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testConcatOuterBackpressure() {
        assertEquals(1,
                (int) Observable.<Integer> empty()
                        .concatWith(Observable.just(1))
                        .take(1)
                        .toBlocking().single());
    }
    
    @Test
    public void testInnerBackpressureWithAlignedBoundaries() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(0, Observable.bufferSize() * 2)
                .concatWith(Observable.range(0, Observable.bufferSize() * 2))
                .observeOn(Schedulers.computation()) // observeOn has a backpressured RxRingBuffer
                .subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 4, ts.valueCount());
    }

    /*
     * Testing without counts aligned with buffer sizes because concat must prevent the subscription
     * to the next Observable if request == 0 which can happen at the end of a subscription
     * if the request size == emitted size. It needs to delay subscription until the next request when aligned, 
     * when not aligned, it just subscribesNext with the outstanding request amount.
     */
    @Test
    public void testInnerBackpressureWithoutAlignedBoundaries() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Observable.range(0, (Observable.bufferSize() * 2) + 10)
                .concatWith(Observable.range(0, (Observable.bufferSize() * 2) + 10))
                .observeOn(Schedulers.computation()) // observeOn has a backpressured RxRingBuffer
                .subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals((Observable.bufferSize() * 4) + 20, ts.valueCount());
    }
    
    // https://github.com/ReactiveX/RxJava/issues/1818
    @Test
    public void testConcatWithNonCompliantSourceDoubleOnComplete() {
        Observable<String> o = Observable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(EmptySubscription.INSTANCE);
                s.onNext("hello");
                s.onComplete();
                s.onComplete();
            }
            
        });
        
        TestSubscriber<String> ts = new TestSubscriber<>();
        Observable.concat(o, o).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("hello", "hello");
    }

    @Test(timeout = 10000)
    public void testIssue2890NoStackoverflow() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, Observable<Integer>> func = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t) {
                Observable<Integer> observable = Observable.just(t)
                        .subscribeOn(sch)
                ;
                Subject<Integer, Integer> subject = UnicastSubject.create();
                observable.subscribe(subject);
                return subject;
            }
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        Observable.range(1, n).concatMap(func).subscribe(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
                // Consume after sleep for 1 ms
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // ignored
                }
                if (counter.getAndIncrement() % 100 == 0) {
                    System.out.print("testIssue2890NoStackoverflow -> ");
                    System.out.println(counter.get());
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

        executor.awaitTermination(12000, TimeUnit.MILLISECONDS);
        
        assertEquals(n, counter.get());
    }
    
    @Test
    public void testRequestOverflowDoesNotStallStream() {
        Observable<Integer> o1 = Observable.just(1,2,3);
        Observable<Integer> o2 = Observable.just(4,5,6);
        final AtomicBoolean completed = new AtomicBoolean(false);
        o1.concatWith(o2).subscribe(new Observer<Integer>() {

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Integer t) {
                request(2);
            }});
        
        assertTrue(completed.get());
    }
    
    @Test//(timeout = 100000)
    public void concatMapRangeAsyncLoopIssue2876() {
        final long durationSeconds = 2;
        final long startTime = System.currentTimeMillis();
        for (int i = 0;; i++) {
            //only run this for a max of ten seconds
            if (System.currentTimeMillis()-startTime > TimeUnit.SECONDS.toMillis(durationSeconds))
                return;
            if (i % 1000 == 0) {
                System.out.println("concatMapRangeAsyncLoop > " + i);
            }
            TestSubscriber<Integer> ts = new TestSubscriber<>();
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
    
}