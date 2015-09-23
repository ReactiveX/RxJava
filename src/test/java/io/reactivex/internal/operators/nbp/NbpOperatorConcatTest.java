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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.nbp.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorConcatTest {

    @Test
    public void testConcat() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final NbpObservable<String> odds = NbpObservable.fromArray(o);
        final NbpObservable<String> even = NbpObservable.fromArray(e);

        NbpObservable<String> concat = NbpObservable.concat(odds, even);
        concat.subscribe(NbpObserver);

        verify(NbpObserver, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatWithList() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final NbpObservable<String> odds = NbpObservable.fromArray(o);
        final NbpObservable<String> even = NbpObservable.fromArray(e);
        final List<NbpObservable<String>> list = new ArrayList<>();
        list.add(odds);
        list.add(even);
        NbpObservable<String> concat = NbpObservable.concat(NbpObservable.fromIterable(list));
        concat.subscribe(NbpObserver);

        verify(NbpObserver, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatObservableOfObservables() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final NbpObservable<String> odds = NbpObservable.fromArray(o);
        final NbpObservable<String> even = NbpObservable.fromArray(e);

        NbpObservable<NbpObservable<String>> observableOfObservables = NbpObservable.create(new NbpOnSubscribe<NbpObservable<String>>() {

            @Override
            public void accept(NbpSubscriber<? super NbpObservable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                // simulate what would happen in an NbpObservable
                NbpObserver.onNext(odds);
                NbpObserver.onNext(even);
                NbpObserver.onComplete();
            }

        });
        NbpObservable<String> concat = NbpObservable.concat(observableOfObservables);

        concat.subscribe(NbpObserver);

        verify(NbpObserver, times(7)).onNext(anyString());
    }

    /**
     * Simple concat of 2 asynchronous observables ensuring it emits in correct order.
     */
    @Test
    public void testSimpleAsyncConcat() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        TestObservable<String> o1 = new TestObservable<>("one", "two", "three");
        TestObservable<String> o2 = new TestObservable<>("four", "five", "six");

        NbpObservable.concat(NbpObservable.create(o1), NbpObservable.create(o2)).subscribe(NbpObserver);

        try {
            // wait for async observables to complete
            o1.t.join();
            o2.t.join();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads");
        }

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, times(1)).onNext("five");
        inOrder.verify(NbpObserver, times(1)).onNext("six");
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
     * Test an async NbpObservable that emits more async Observables
     */
    @Test
    public void testNestedAsyncConcat() throws Throwable {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        final TestObservable<String> o1 = new TestObservable<>("one", "two", "three");
        final TestObservable<String> o2 = new TestObservable<>("four", "five", "six");
        final TestObservable<String> o3 = new TestObservable<>("seven", "eight", "nine");
        final CountDownLatch allowThird = new CountDownLatch(1);

        final AtomicReference<Thread> parent = new AtomicReference<>();
        final CountDownLatch parentHasStarted = new CountDownLatch(1);
        final CountDownLatch parentHasFinished = new CountDownLatch(1);
        
        
        NbpObservable<NbpObservable<String>> observableOfObservables = NbpObservable.create(new NbpOnSubscribe<NbpObservable<String>>() {

            @Override
            public void accept(final NbpSubscriber<? super NbpObservable<String>> NbpObserver) {
                final BooleanDisposable s = new BooleanDisposable();
                NbpObserver.onSubscribe(s);
                parent.set(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // emit first
                            if (!s.isDisposed()) {
                                System.out.println("Emit o1");
                                NbpObserver.onNext(NbpObservable.create(o1));
                            }
                            // emit second
                            if (!s.isDisposed()) {
                                System.out.println("Emit o2");
                                NbpObserver.onNext(NbpObservable.create(o2));
                            }

                            // wait until sometime later and emit third
                            try {
                                allowThird.await();
                            } catch (InterruptedException e) {
                                NbpObserver.onError(e);
                            }
                            if (!s.isDisposed()) {
                                System.out.println("Emit o3");
                                NbpObserver.onNext(NbpObservable.create(o3));
                            }

                        } catch (Throwable e) {
                            NbpObserver.onError(e);
                        } finally {
                            System.out.println("Done parent NbpObservable");
                            NbpObserver.onComplete();
                            parentHasFinished.countDown();
                        }
                    }
                }));
                parent.get().start();
                parentHasStarted.countDown();
            }
        });

        NbpObservable.concat(observableOfObservables).subscribe(NbpObserver);

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

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, times(1)).onNext("five");
        inOrder.verify(NbpObserver, times(1)).onNext("six");
        // we shouldn't have the following 3 yet
        inOrder.verify(NbpObserver, never()).onNext("seven");
        inOrder.verify(NbpObserver, never()).onNext("eight");
        inOrder.verify(NbpObserver, never()).onNext("nine");
        // we should not be completed yet
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

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
        
        inOrder.verify(NbpObserver, times(1)).onNext("seven");
        inOrder.verify(NbpObserver, times(1)).onNext("eight");
        inOrder.verify(NbpObserver, times(1)).onNext("nine");

        verify(NbpObserver, never()).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testBlockedObservableOfObservables() {
        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };
        final NbpObservable<String> odds = NbpObservable.fromArray(o);
        final NbpObservable<String> even = NbpObservable.fromArray(e);
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        TestObservable<NbpObservable<String>> observableOfObservables = new TestObservable<>(callOnce, okToContinue, odds, even);
        NbpObservable<String> concatF = NbpObservable.concat(NbpObservable.create(observableOfObservables));
        concatF.subscribe(NbpObserver);
        try {
            //Block main thread to allow observables to serve up o1.
            callOnce.await();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concated NbpObservable should have served up all of the odds.
        verify(NbpObserver, times(1)).onNext("1");
        verify(NbpObserver, times(1)).onNext("3");
        verify(NbpObserver, times(1)).onNext("5");
        verify(NbpObserver, times(1)).onNext("7");

        try {
            // unblock observables so it can serve up o2 and complete
            okToContinue.countDown();
            observableOfObservables.t.join();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concatenated NbpObservable should now have served up all the evens.
        verify(NbpObserver, times(1)).onNext("2");
        verify(NbpObserver, times(1)).onNext("4");
        verify(NbpObserver, times(1)).onNext("6");
    }

    @Test
    public void testConcatConcurrentWithInfinity() {
        final TestObservable<String> w1 = new TestObservable<>("one", "two", "three");
        //This NbpObservable will send "hello" MAX_VALUE time.
        final TestObservable<String> w2 = new TestObservable<>("hello", Integer.MAX_VALUE);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
        TestObservable<NbpObservable<String>> observableOfObservables = new TestObservable<>(NbpObservable.create(w1), NbpObservable.create(w2));
        NbpObservable<String> concatF = NbpObservable.concat(NbpObservable.create(observableOfObservables));

        concatF.take(50).subscribe(NbpObserver);

        //Wait for the thread to start up.
        try {
            w1.waitForThreadDone();
            w2.waitForThreadDone();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(47)).onNext("hello");
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConcatNonBlockingObservables() {

        final CountDownLatch okToContinueW1 = new CountDownLatch(1);
        final CountDownLatch okToContinueW2 = new CountDownLatch(1);

        final TestObservable<String> w1 = new TestObservable<>(null, okToContinueW1, "one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<>(null, okToContinueW2, "four", "five", "six");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable<NbpObservable<String>> observableOfObservables = NbpObservable.create(new NbpOnSubscribe<NbpObservable<String>>() {

            @Override
            public void accept(NbpSubscriber<? super NbpObservable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                // simulate what would happen in an NbpObservable
                NbpObserver.onNext(NbpObservable.create(w1));
                NbpObserver.onNext(NbpObservable.create(w2));
                NbpObserver.onComplete();
            }

        });
        NbpObservable<String> concat = NbpObservable.concat(observableOfObservables);
        concat.subscribe(NbpObserver);

        verify(NbpObserver, times(0)).onComplete();

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

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, times(1)).onNext("five");
        inOrder.verify(NbpObserver, times(1)).onNext("six");
        verify(NbpObserver, times(1)).onComplete();

    }

    /**
     * Test unsubscribing the concatenated NbpObservable in a single thread.
     */
    @Test
    public void testConcatUnsubscribe() {
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        final TestObservable<String> w1 = new TestObservable<>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<>(callOnce, okToContinue, "four", "five", "six");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);

        final NbpObservable<String> concat = NbpObservable.concat(NbpObservable.create(w1), NbpObservable.create(w2));

        try {
            // Subscribe
            concat.subscribe(ts);
            //Block main thread to allow NbpObservable "w1" to complete and NbpObservable "w2" to call onNext once.
            callOnce.await();
            // Unsubcribe
            ts.dispose();
            //Unblock the NbpObservable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, never()).onNext("five");
        inOrder.verify(NbpObserver, never()).onNext("six");
        inOrder.verify(NbpObserver, never()).onComplete();

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

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);
        
        TestObservable<NbpObservable<String>> observableOfObservables = new TestObservable<>(NbpObservable.create(w1), NbpObservable.create(w2));
        NbpObservable<String> concatF = NbpObservable.concat(NbpObservable.create(observableOfObservables));

        concatF.subscribe(ts);

        try {
            //Block main thread to allow NbpObservable "w1" to complete and NbpObservable "w2" to call onNext exactly once.
            callOnce.await();
            //"four" from w2 has been processed by onNext()
            ts.dispose();
            //"five" and "six" will NOT be processed by onNext()
            //Unblock the NbpObservable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext("one");
        inOrder.verify(NbpObserver, times(1)).onNext("two");
        inOrder.verify(NbpObserver, times(1)).onNext("three");
        inOrder.verify(NbpObserver, times(1)).onNext("four");
        inOrder.verify(NbpObserver, never()).onNext("five");
        inOrder.verify(NbpObserver, never()).onNext("six");
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    private static class TestObservable<T> implements NbpOnSubscribe<T> {

        private final Disposable s = () -> {
                subscribed = false;
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
        public void accept(final NbpSubscriber<? super T> NbpObserver) {
            NbpObserver.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (count < size && subscribed) {
                            if (null != values)
                                NbpObserver.onNext(values.get(count));
                            else
                                NbpObserver.onNext(seed);
                            count++;
                            //Unblock the main thread to call unsubscribe.
                            if (null != once)
                                once.countDown();
                            //Block until the main thread has called unsubscribe.
                            if (null != okToContinue)
                                okToContinue.await(5, TimeUnit.SECONDS);
                        }
                        if (subscribed)
                            NbpObserver.onComplete();
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
        NbpSubscriber<Object> o1 = TestHelper.mockNbpSubscriber();
        NbpSubscriber<Object> o2 = TestHelper.mockNbpSubscriber();

        TestScheduler s = new TestScheduler();

        NbpObservable<Long> timer = NbpObservable.interval(500, TimeUnit.MILLISECONDS, s).take(2);
        NbpObservable<Long> o = NbpObservable.concat(timer, timer);

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
        NbpObservable<NbpObservable<Integer>> source = NbpObservable.range(0, n).map(NbpObservable::just);
        
        NbpObservable<List<Integer>> result = NbpObservable.concat(source).toList();
        
        NbpSubscriber<List<Integer>> o = TestHelper.mockNbpSubscriber();
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
        NbpObservable<NbpObservable<Integer>> source = NbpObservable.range(0, n).map(NbpObservable::just);
        
        NbpObservable<List<Integer>> result = NbpObservable.concat(source).take(n / 2).toList();
        
        NbpSubscriber<List<Integer>> o = TestHelper.mockNbpSubscriber();
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
                (int) NbpObservable.<Integer> empty()
                        .concatWith(NbpObservable.just(1))
                        .take(1)
                        .toBlocking().single());
    }
    
    // https://github.com/ReactiveX/RxJava/issues/1818
    @Test
    public void testConcatWithNonCompliantSourceDoubleOnComplete() {
        NbpObservable<String> o = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onNext("hello");
                s.onComplete();
                s.onComplete();
            }
            
        });
        
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.concat(o, o).subscribe(ts);
        ts.awaitTerminalEvent(500, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertValues("hello", "hello");
    }

    @Test(timeout = 10000)
    public void testIssue2890NoStackoverflow() throws InterruptedException {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final Scheduler sch = Schedulers.from(executor);

        Function<Integer, NbpObservable<Integer>> func = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t) {
                NbpObservable<Integer> o = NbpObservable.just(t)
                        .subscribeOn(sch)
                ;
                NbpSubject<Integer, Integer> subject = NbpUnicastSubject.create();
                o.subscribe(subject);
                return subject;
            }
        };

        int n = 5000;
        final AtomicInteger counter = new AtomicInteger();

        NbpObservable.range(1, n)
        .concatMap(func).subscribe(new NbpObserver<Integer>() {
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

        executor.awaitTermination(12000, TimeUnit.MILLISECONDS);
        
        assertEquals(n, counter.get());
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
            NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
            NbpObservable.range(0, 1000)
            .concatMap(new Function<Integer, NbpObservable<Integer>>() {
                @Override
                public NbpObservable<Integer> apply(Integer t) {
                    return NbpObservable.fromIterable(Arrays.asList(t));
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