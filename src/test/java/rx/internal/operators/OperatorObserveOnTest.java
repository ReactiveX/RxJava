/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.*;
import rx.subjects.PublishSubject;

public class OperatorObserveOnTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testObserveOn() {
        Observer<Integer> observer = mock(Observer.class);
        Observable.just(1, 2, 3).observeOn(Schedulers.immediate()).subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOrdering() throws InterruptedException {
        Observable<String> obs = Observable.just("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);

        InOrder inOrder = inOrder(observer);
        TestSubscriber<String> ts = new TestSubscriber<String>(observer);

        obs.observeOn(Schedulers.computation()).subscribe(ts);

        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        if (ts.getOnErrorEvents().size() > 0) {
            for (Throwable t : ts.getOnErrorEvents()) {
                t.printStackTrace();
            }
            fail("failed with exception");
        }

        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext(null);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testThreadName() throws InterruptedException {
        System.out.println("Main Thread: " + Thread.currentThread().getName());
        Observable<String> obs = Observable.just("one", null, "two", "three", "four");

        Observer<String> observer = mock(Observer.class);
        final String parentThreadName = Thread.currentThread().getName();

        final CountDownLatch completedLatch = new CountDownLatch(1);

        // assert subscribe is on main thread
        obs = obs.doOnNext(new Action1<String>() {

            @Override
            public void call(String s) {
                String threadName = Thread.currentThread().getName();
                System.out.println("Source ThreadName: " + threadName + "  Expected => " + parentThreadName);
                assertEquals(parentThreadName, threadName);
            }

        });

        // assert observe is on new thread
        obs.observeOn(Schedulers.newThread()).doOnNext(new Action1<String>() {

            @Override
            public void call(String t1) {
                String threadName = Thread.currentThread().getName();
                boolean correctThreadName = threadName.startsWith("RxNewThreadScheduler");
                System.out.println("ObserveOn ThreadName: " + threadName + "  Correct => " + correctThreadName);
                assertTrue(correctThreadName);
            }

        }).doAfterTerminate(new Action0() {

            @Override
            public void call() {
                completedLatch.countDown();

            }
        }).subscribe(observer);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(5)).onNext(any(String.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void observeOnTheSameSchedulerTwice() {
        Scheduler scheduler = Schedulers.immediate();

        Observable<Integer> o = Observable.just(1, 2, 3);
        Observable<Integer> o2 = o.observeOn(scheduler);

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o2.subscribe(observer1);
        o2.subscribe(observer2);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    @Test
    public void observeSameOnMultipleSchedulers() {
        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        Observable<Integer> o = Observable.just(1, 2, 3);
        Observable<Integer> o1 = o.observeOn(scheduler1);
        Observable<Integer> o2 = o.observeOn(scheduler2);

        @SuppressWarnings("unchecked")
        Observer<Object> observer1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<Object> observer2 = mock(Observer.class);

        InOrder inOrder1 = inOrder(observer1);
        InOrder inOrder2 = inOrder(observer2);

        o1.subscribe(observer1);
        o2.subscribe(observer2);

        scheduler1.advanceTimeBy(1, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder1.verify(observer1, times(1)).onNext(1);
        inOrder1.verify(observer1, times(1)).onNext(2);
        inOrder1.verify(observer1, times(1)).onNext(3);
        inOrder1.verify(observer1, times(1)).onCompleted();
        verify(observer1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(observer2, times(1)).onNext(1);
        inOrder2.verify(observer2, times(1)).onNext(2);
        inOrder2.verify(observer2, times(1)).onNext(3);
        inOrder2.verify(observer2, times(1)).onCompleted();
        verify(observer2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    /**
     * Confirm that running on a NewThreadScheduler uses the same thread for the entire stream
     */
    @Test
    public void testObserveOnWithNewThreadScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 100000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.newThread())
                .toBlocking().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxNewThreadScheduler"));
                    }

                });

    }

    /**
     * Confirm that running on a ThreadPoolScheduler allows multiple threads but is still ordered.
     */
    @Test
    public void testObserveOnWithComputationScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 100000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
                .toBlocking().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxComputationScheduler"));
                    }

                });
    }

    /**
     * Attempts to confirm that when pauses exist between events, the ScheduledObserver
     * does not lose or reorder any events since the scheduler will not block, but will
     * be re-scheduled when it receives new events after each pause.
     * 
     * 
     * This is non-deterministic in proving success, but if it ever fails (non-deterministically)
     * it is a sign of potential issues as thread-races and scheduling should not affect output.
     */
    @Test
    public void testObserveOnOrderingConcurrency() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Observable.range(1, 10000).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                if (randomIntFrom0to100() > 98) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
                .toBlocking().forEach(new Action1<Integer>() {

                    @Override
                    public void call(Integer t1) {
                        assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                        assertTrue(Thread.currentThread().getName().startsWith("RxComputationScheduler"));
                    }

                });
    }

    @Test
    public void testNonBlockingOuterWhileBlockingOnNext() throws InterruptedException {

        final CountDownLatch completedLatch = new CountDownLatch(1);
        final CountDownLatch nextLatch = new CountDownLatch(1);
        final AtomicLong completeTime = new AtomicLong();
        // use subscribeOn to make async, observeOn to move
        Observable.range(1, 2).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(new Observer<Integer>() {

            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
                completeTime.set(System.nanoTime());
                completedLatch.countDown();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // don't let this thing finish yet
                try {
                    if (!nextLatch.await(1000, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("it shouldn't have timed out");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("it shouldn't have failed");
                }
            }

        });

        long afterSubscribeTime = System.nanoTime();
        System.out.println("After subscribe: " + completedLatch.getCount());
        assertEquals(1, completedLatch.getCount());
        nextLatch.countDown();
        completedLatch.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(completeTime.get() > afterSubscribeTime);
        System.out.println("onComplete nanos after subscribe: " + (completeTime.get() - afterSubscribeTime));
    }

    private static int randomIntFrom0to100() {
        // XORShift instead of Math.random http://javamex.com/tutorials/random_numbers/xorshift.shtml
        long x = System.nanoTime();
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return Math.abs((int) x % 100);
    }

    @Test
    public void testDelayedErrorDeliveryWhenSafeSubscriberUnsubscribes() {
        TestScheduler testScheduler = new TestScheduler();

        Observable<Integer> source = Observable.concat(Observable.<Integer> error(new TestException()), Observable.just(1));

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.observeOn(testScheduler).subscribe(o);

        inOrder.verify(o, never()).onError(any(TestException.class));

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onCompleted();
    }

    @Test
    public void testAfterUnsubscribeCalledThenObserverOnNextNeverCalled() {
        final TestScheduler testScheduler = new TestScheduler();
        @SuppressWarnings("unchecked")
        final Observer<Integer> observer = mock(Observer.class);
        final Subscription subscription = Observable.just(1, 2, 3)
                .observeOn(testScheduler)
                .subscribe(observer);
        subscription.unsubscribe();
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        final InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, never()).onNext(anyInt());
        inOrder.verify(observer, never()).onError(any(Exception.class));
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    public void testBackpressureWithTakeAfter() {
        final AtomicInteger generated = new AtomicInteger();
        Observable<Integer> observable = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                System.err.println("c t = " + t + " thread " + Thread.currentThread());
                super.onNext(t);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        };

        observable
                .observeOn(Schedulers.newThread())
                .take(3)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        System.err.println(testSubscriber.getOnNextEvents());
        testSubscriber.assertReceivedOnNext(Arrays.asList(0, 1, 2));
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated: " + generated.get());
        assertTrue(generated.get() >= 3 && generated.get() <= RxRingBuffer.SIZE);
    }

    @Test
    public void testBackpressureWithTakeAfterAndMultipleBatches() {
        int numForBatches = RxRingBuffer.SIZE * 3 + 1; // should be 4 batches == ((3*n)+1) items
        final AtomicInteger generated = new AtomicInteger();
        Observable<Integer> observable = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                //                System.err.println("c t = " + t + " thread " + Thread.currentThread());
                super.onNext(t);
            }
        };

        observable
                .observeOn(Schedulers.newThread())
                .take(numForBatches)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        System.err.println(testSubscriber.getOnNextEvents());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated: " + generated.get());
        assertTrue(generated.get() >= numForBatches && generated.get() <= numForBatches + RxRingBuffer.SIZE);
    }

    @Test
    public void testBackpressureWithTakeBefore() {
        final AtomicInteger generated = new AtomicInteger();
        Observable<Integer> observable = Observable.from(new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    @Override
                    public void remove() {
                    }

                    @Override
                    public Integer next() {
                        return generated.getAndIncrement();
                    }

                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                };
            }
        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>();
        observable
                .take(7)
                .observeOn(Schedulers.newThread())
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertReceivedOnNext(Arrays.asList(0, 1, 2, 3, 4, 5, 6));
        assertEquals(7, generated.get());
    }

    @Test
    public void testQueueFullEmitsError() {
        final CountDownLatch latch = new CountDownLatch(1);
        Observable<Integer> observable = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> o) {
                for (int i = 0; i < RxRingBuffer.SIZE + 10; i++) {
                    o.onNext(i);
                }
                latch.countDown();
                o.onCompleted();
            }

        });

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<Integer>(new Observer<Integer>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                // force it to be slow and wait until we have queued everything
                try {
                    latch.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        });
        observable.observeOn(Schedulers.newThread()).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        List<Throwable> errors = testSubscriber.getOnErrorEvents();
        assertEquals(1, errors.size());
        System.out.println("Errors: " + errors);
        Throwable t = errors.get(0);
        if (t instanceof MissingBackpressureException) {
            // success, we expect this
        } else {
            if (t.getCause() instanceof MissingBackpressureException) {
                // this is also okay
            } else {
                fail("Expecting MissingBackpressureException");
            }
        }
    }

    @Test
    public void testQueueFullEmitsErrorWithVaryingBufferSize() {
        for (int i = 1; i <= 1024; i = i * 2) {
            final int capacity = i;
            System.out.println(">> testQueueFullEmitsErrorWithVaryingBufferSize @ " + i);
            
            PublishSubject<Integer> ps = PublishSubject.create();
            
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
            
            TestScheduler test = Schedulers.test();
            
            ps.observeOn(test, capacity).subscribe(ts);
            
            for (int j = 0; j < capacity + 10; j++) {
                ps.onNext(j);
            }
            ps.onCompleted();
            
            test.advanceTimeBy(1, TimeUnit.SECONDS);
            
            ts.assertNoValues();
            ts.assertError(MissingBackpressureException.class);
            ts.assertNotCompleted();
        }
    }

    @Test
    public void testAsyncChild() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.range(0, 100000).observeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void testOnErrorCutsAheadOfOnNext() {
        for (int i = 0; i < 50; i++) {
            final PublishSubject<Long> subject = PublishSubject.create();
    
            final AtomicLong counter = new AtomicLong();
            TestSubscriber<Long> ts = new TestSubscriber<Long>(new Observer<Long>() {
    
                @Override
                public void onCompleted() {
    
                }
    
                @Override
                public void onError(Throwable e) {
    
                }
    
                @Override
                public void onNext(Long t) {
                    // simulate slow consumer to force backpressure failure
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
    
            });
            subject.observeOn(Schedulers.computation()).subscribe(ts);
    
            // this will blow up with backpressure
            while (counter.get() < 102400) {
                subject.onNext(counter.get());
                counter.incrementAndGet();
            }
    
            ts.awaitTerminalEvent();
            assertEquals(1, ts.getOnErrorEvents().size());
            assertTrue(ts.getOnErrorEvents().get(0) instanceof MissingBackpressureException);
            // assert that the values are sequential, that cutting in didn't allow skipping some but emitting others.
            // example [0, 1, 2] not [0, 1, 4]
            List<Long> onNextEvents = ts.getOnNextEvents();
            assertTrue(onNextEvents.isEmpty() || onNextEvents.size() == onNextEvents.get(onNextEvents.size() - 1) + 1);
            // we should emit the error without emitting the full buffer size
            assertTrue(onNextEvents.size() < RxRingBuffer.SIZE);
        }
    }

    /**
     * Make sure we get a MissingBackpressureException propagated through when we have a fast temporal (hot) producer.
     */
    @Test
    public void testHotOperatorBackpressure() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Observable.interval(0, 1, TimeUnit.MICROSECONDS)
                .observeOn(Schedulers.computation())
                .map(new Func1<Long, String>() {

                    @Override
                    public String call(Long t1) {
                        System.out.println(t1);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        return t1 + " slow value";
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("Errors: " + ts.getOnErrorEvents());
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals(MissingBackpressureException.class, ts.getOnErrorEvents().get(0).getClass());
    }

    @Test
    public void testErrorPropagatesWhenNoOutstandingRequests() {
        Observable<Long> timer = Observable.interval(0, 1, TimeUnit.MICROSECONDS)
                .doOnEach(new Action1<Notification<? super Long>>() {

                    @Override
                    public void call(Notification<? super Long> n) {
                        //                                                System.out.println("BEFORE " + n);
                    }

                })
                .observeOn(Schedulers.newThread())
                .doOnEach(new Action1<Notification<? super Long>>() {

                    @Override
                    public void call(Notification<? super Long> n) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        //                                                System.out.println("AFTER " + n);
                    }

                });

        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Observable.combineLatest(timer, Observable.<Integer> never(), new Func2<Long, Integer, Long>() {

            @Override
            public Long call(Long t1, Integer t2) {
                return t1;
            }

        }).take(RxRingBuffer.SIZE * 2).subscribe(ts);

        ts.awaitTerminalEvent();
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals(MissingBackpressureException.class, ts.getOnErrorEvents().get(0).getClass());
    }

    @Test
    public void testRequestOverflow() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, 100).observeOn(Schedulers.computation())
                .subscribe(new Subscriber<Integer>() {

                    boolean first = true;
                    
                    @Override
                    public void onStart() {
                        request(2);
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        count.incrementAndGet();
                        if (first) {
                            request(Long.MAX_VALUE - 1);
                            request(Long.MAX_VALUE - 1);
                            request(10);
                            first = false;
                        }
                    }
                });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(100, count.get());

    }
    
    @Test
    public void testNoMoreRequestsAfterUnsubscribe() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Long> requests = Collections.synchronizedList(new ArrayList<Long>());
        Observable.range(1, 1000000)
                .doOnRequest(new Action1<Long>() {

                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Integer t) {
                        unsubscribe();
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(1, requests.size());
    }

    @Test
    public void testErrorDelayed() {
        TestScheduler s = Schedulers.test();
        
        Observable<Integer> source = Observable.just(1, 2 ,3)
                .concatWith(Observable.<Integer>error(new TestException()));
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        source.observeOn(s, true).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        s.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);
        s.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(3); // requesting 2 doesn't switch to the error() source for some reason in concat.
        s.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertValues(1, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void testErrorDelayedAsync() {
        Observable<Integer> source = Observable.just(1, 2 ,3)
                .concatWith(Observable.<Integer>error(new TestException()));
        
        TestSubscriber<Integer> ts = TestSubscriber.create();

        source.observeOn(Schedulers.computation(), true).subscribe(ts);
        
        ts.awaitTerminalEvent(2, TimeUnit.SECONDS);
        ts.assertValues(1, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void requestExactCompletesImmediately() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        TestScheduler test = Schedulers.test();

        Observable.range(1, 10).observeOn(test).subscribe(ts);

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(10);

        test.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertValueCount(10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void fixedReplenishPattern() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        TestScheduler test = Schedulers.test();
        
        final List<Long> requests = new ArrayList<Long>();
        
        Observable.range(1, 100)
        .doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long v) {
                requests.add(v);
            }
        })
        .observeOn(test, 16).subscribe(ts);
        
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.requestMore(20);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.requestMore(10);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.requestMore(50);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.requestMore(35);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertValueCount(100);
        ts.assertCompleted();
        ts.assertNoErrors();
        
        assertEquals(Arrays.asList(16L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L), requests);
    }
    
    @Test
    public void bufferSizesWork() {
        for (int i = 1; i <= 1024; i = i * 2) {
            TestSubscriber<Integer> ts = TestSubscriber.create();
            
            Observable.range(1, 1000 * 1000).observeOn(Schedulers.computation(), i)
            .subscribe(ts);
            
            ts.awaitTerminalEvent();
            ts.assertValueCount(1000 * 1000);
            ts.assertCompleted();
            ts.assertNoErrors();
        }
    }
    
    @Test
    public void synchronousRebatching() {
        final List<Long> requests = new ArrayList<Long>();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            
        Observable.range(1, 50)
        .doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long r) {
                requests.add(r);
            }
        })
       .rebatchRequests(20)
       .subscribe(ts);
       
       ts.assertValueCount(50);
       ts.assertNoErrors();
       ts.assertCompleted();
       
       assertEquals(Arrays.asList(20L, 15L, 15L, 15L), requests);
    }
    
    @Test
    public void rebatchRequestsArgumentCheck() {
        try {
            Observable.never().rebatchRequests(-99);
            fail("Didn't throw IAE");
        } catch (IllegalArgumentException ex) {
            assertEquals("n > 0 required but it was -99", ex.getMessage());
        }
    }
}
