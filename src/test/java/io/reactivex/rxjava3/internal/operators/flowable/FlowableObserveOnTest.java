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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableObserveOn.BaseObserveOnSubscriber;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableObserveOnTest extends RxJavaTest {

    /**
     * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
     */
    @Test
    public void observeOn() {
        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        Flowable.just(1, 2, 3).observeOn(ImmediateThinScheduler.INSTANCE).subscribe(subscriber);

        verify(subscriber, times(1)).onNext(1);
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onNext(3);
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void ordering() throws InterruptedException {
        Flowable<String> obs = Flowable.just("one", "null", "two", "three", "four");

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        InOrder inOrder = inOrder(subscriber);
        TestSubscriberEx<String> ts = new TestSubscriberEx<>(subscriber);

        obs.observeOn(Schedulers.computation()).subscribe(ts);

        ts.awaitDone(1000, TimeUnit.MILLISECONDS);
        if (ts.errors().size() > 0) {
            for (Throwable t : ts.errors()) {
                t.printStackTrace();
            }
            fail("failed with exception");
        }

        inOrder.verify(subscriber, times(1)).onNext("one");
        inOrder.verify(subscriber, times(1)).onNext("null");
        inOrder.verify(subscriber, times(1)).onNext("two");
        inOrder.verify(subscriber, times(1)).onNext("three");
        inOrder.verify(subscriber, times(1)).onNext("four");
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void threadName() throws InterruptedException {
        System.out.println("Main Thread: " + Thread.currentThread().getName());
        Flowable<String> obs = Flowable.just("one", "null", "two", "three", "four");

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        final String parentThreadName = Thread.currentThread().getName();

        final CountDownLatch completedLatch = new CountDownLatch(1);

        // assert subscribe is on main thread
        obs = obs.doOnNext(new Consumer<String>() {

            @Override
            public void accept(String s) {
                String threadName = Thread.currentThread().getName();
                System.out.println("Source ThreadName: " + threadName + "  Expected => " + parentThreadName);
                assertEquals(parentThreadName, threadName);
            }

        });

        // assert observe is on new thread
        obs.observeOn(Schedulers.newThread()).doOnNext(new Consumer<String>() {

            @Override
            public void accept(String t1) {
                String threadName = Thread.currentThread().getName();
                boolean correctThreadName = threadName.startsWith("RxNewThreadScheduler");
                System.out.println("ObserveOn ThreadName: " + threadName + "  Correct => " + correctThreadName);
                assertTrue(correctThreadName);
            }

        }).doAfterTerminate(new Action() {

            @Override
            public void run() {
                completedLatch.countDown();

            }
        }).subscribe(subscriber);

        if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting");
        }

        verify(subscriber, never()).onError(any(Throwable.class));
        verify(subscriber, times(5)).onNext(any(String.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void observeOnTheSameSchedulerTwice() {
        Scheduler scheduler = ImmediateThinScheduler.INSTANCE;

        Flowable<Integer> f = Flowable.just(1, 2, 3);
        Flowable<Integer> f2 = f.observeOn(scheduler);

        Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
        Subscriber<Object> subscriber2 = TestHelper.mockSubscriber();

        InOrder inOrder1 = inOrder(subscriber1);
        InOrder inOrder2 = inOrder(subscriber2);

        f2.subscribe(subscriber1);
        f2.subscribe(subscriber2);

        inOrder1.verify(subscriber1, times(1)).onNext(1);
        inOrder1.verify(subscriber1, times(1)).onNext(2);
        inOrder1.verify(subscriber1, times(1)).onNext(3);
        inOrder1.verify(subscriber1, times(1)).onComplete();
        verify(subscriber1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(subscriber2, times(1)).onNext(1);
        inOrder2.verify(subscriber2, times(1)).onNext(2);
        inOrder2.verify(subscriber2, times(1)).onNext(3);
        inOrder2.verify(subscriber2, times(1)).onComplete();
        verify(subscriber2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    @Test
    public void observeSameOnMultipleSchedulers() {
        TestScheduler scheduler1 = new TestScheduler();
        TestScheduler scheduler2 = new TestScheduler();

        Flowable<Integer> f = Flowable.just(1, 2, 3);
        Flowable<Integer> f1 = f.observeOn(scheduler1);
        Flowable<Integer> f2 = f.observeOn(scheduler2);

        Subscriber<Object> subscriber1 = TestHelper.mockSubscriber();
        Subscriber<Object> subscriber2 = TestHelper.mockSubscriber();

        InOrder inOrder1 = inOrder(subscriber1);
        InOrder inOrder2 = inOrder(subscriber2);

        f1.subscribe(subscriber1);
        f2.subscribe(subscriber2);

        scheduler1.advanceTimeBy(1, TimeUnit.SECONDS);
        scheduler2.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder1.verify(subscriber1, times(1)).onNext(1);
        inOrder1.verify(subscriber1, times(1)).onNext(2);
        inOrder1.verify(subscriber1, times(1)).onNext(3);
        inOrder1.verify(subscriber1, times(1)).onComplete();
        verify(subscriber1, never()).onError(any(Throwable.class));
        inOrder1.verifyNoMoreInteractions();

        inOrder2.verify(subscriber2, times(1)).onNext(1);
        inOrder2.verify(subscriber2, times(1)).onNext(2);
        inOrder2.verify(subscriber2, times(1)).onNext(3);
        inOrder2.verify(subscriber2, times(1)).onComplete();
        verify(subscriber2, never()).onError(any(Throwable.class));
        inOrder2.verifyNoMoreInteractions();
    }

    /**
     * Confirm that running on a NewThreadScheduler uses the same thread for the entire stream.
     */
    @Test
    public void observeOnWithNewThreadScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Flowable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.newThread())
        .blockingForEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                // FIXME toBlocking methods run on the current thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
            }

        });

    }

    /**
     * Confirm that running on a ThreadPoolScheduler allows multiple threads but is still ordered.
     */
    @Test
    public void observeOnWithThreadPoolScheduler() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Flowable.range(1, 100000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                return t1 * _multiple;
            }

        }).observeOn(Schedulers.computation())
        .blockingForEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
                // FIXME toBlocking methods run on the caller's thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
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
    public void observeOnOrderingConcurrency() {
        final AtomicInteger count = new AtomicInteger();
        final int _multiple = 99;

        Flowable.range(1, 10000).map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
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
        .blockingForEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                assertEquals(count.incrementAndGet() * _multiple, t1.intValue());
//                assertTrue(name.startsWith("RxComputationThreadPool"));
                // FIXME toBlocking now runs its methods on the caller thread
                String name = Thread.currentThread().getName();
                assertFalse("Wrong thread name: " + name, name.startsWith("Rx"));
            }

        });
    }

    @Test
    public void nonBlockingOuterWhileBlockingOnNext() throws InterruptedException {

        final CountDownLatch completedLatch = new CountDownLatch(1);
        final CountDownLatch nextLatch = new CountDownLatch(1);
        final AtomicLong completeTime = new AtomicLong();
        // use subscribeOn to make async, observeOn to move
        Flowable.range(1, 2).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {
                System.out.println("onComplete");
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
    public void delayedErrorDeliveryWhenSafeSubscriberUnsubscribes() {
        TestScheduler testScheduler = new TestScheduler();

        Flowable<Integer> source = Flowable.concat(Flowable.<Integer> error(new TestException()), Flowable.just(1));

        Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        InOrder inOrder = inOrder(subscriber);

        source.observeOn(testScheduler).subscribe(subscriber);

        inOrder.verify(subscriber, never()).onError(any(TestException.class));

        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder.verify(subscriber).onError(any(TestException.class));
        inOrder.verify(subscriber, never()).onNext(anyInt());
        inOrder.verify(subscriber, never()).onComplete();
    }

    @Test
    public void afterUnsubscribeCalledThenObserverOnNextNeverCalled() {
        final TestScheduler testScheduler = new TestScheduler();

        final Subscriber<Integer> subscriber = TestHelper.mockSubscriber();
        TestSubscriber<Integer> ts = new TestSubscriber<>(subscriber);

        Flowable.just(1, 2, 3)
                .observeOn(testScheduler)
                .subscribe(ts);

        ts.cancel();
        testScheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        final InOrder inOrder = inOrder(subscriber);

        inOrder.verify(subscriber, never()).onNext(anyInt());
        inOrder.verify(subscriber, never()).onError(any(Exception.class));
        inOrder.verify(subscriber, never()).onComplete();
    }

    @Test
    public void backpressureWithTakeAfter() {
        final AtomicInteger generated = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.fromIterable(new Iterable<Integer>() {
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

        flowable
                .observeOn(Schedulers.newThread())
                .take(3)
                .subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        System.err.println(testSubscriber.values());
        testSubscriber.assertValues(0, 1, 2);
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated: " + generated.get());
        assertTrue(generated.get() >= 3 && generated.get() <= Flowable.bufferSize());
    }

    @Test
    public void backpressureWithTakeAfterAndMultipleBatches() {
        int numForBatches = Flowable.bufferSize() * 3 + 1; // should be 4 batches == ((3*n)+1) items
        final AtomicInteger generated = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.fromIterable(new Iterable<Integer>() {
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

        flowable
                .observeOn(Schedulers.newThread())
                .take(numForBatches)
                .subscribe(testSubscriber);
        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        System.err.println(testSubscriber.values());
        // it should be between the take num and requested batch size across the async boundary
        System.out.println("Generated: " + generated.get());
        assertTrue(generated.get() >= numForBatches && generated.get() <= numForBatches + Flowable.bufferSize());
    }

    @Test
    public void backpressureWithTakeBefore() {
        final AtomicInteger generated = new AtomicInteger();
        Flowable<Integer> flowable = Flowable.fromIterable(new Iterable<Integer>() {
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

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        flowable
                .take(7)
                .observeOn(Schedulers.newThread())
                .subscribe(testSubscriber);

        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        testSubscriber.assertValues(0, 1, 2, 3, 4, 5, 6);
        assertEquals(7, generated.get());
    }

    @Test
    public void queueFullEmitsError() {
        final CountDownLatch latch = new CountDownLatch(1);
        Flowable<Integer> flowable = Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < Flowable.bufferSize() + 10; i++) {
                    subscriber.onNext(i);
                }
                latch.countDown();
                subscriber.onComplete();
            }

        });

        TestSubscriberEx<Integer> testSubscriber = new TestSubscriberEx<>(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {

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
        flowable.observeOn(Schedulers.newThread()).subscribe(testSubscriber);

        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        List<Throwable> errors = testSubscriber.errors();
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
    public void asyncChild() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        Flowable.range(0, 100000).observeOn(Schedulers.newThread()).observeOn(Schedulers.newThread()).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void onErrorCutsAheadOfOnNext() {
        for (int i = 0; i < 50; i++) {
            final PublishProcessor<Long> processor = PublishProcessor.create();

            final AtomicLong counter = new AtomicLong();
            TestSubscriberEx<Long> ts = new TestSubscriberEx<>(new DefaultSubscriber<Long>() {

                @Override
                public void onComplete() {

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
            processor.observeOn(Schedulers.computation()).subscribe(ts);

            // this will blow up with backpressure
            while (counter.get() < 102400) {
                processor.onNext(counter.get());
                counter.incrementAndGet();
            }

            ts.awaitDone(5, TimeUnit.SECONDS);
            assertEquals(1, ts.errors().size());
            ts.assertError(MissingBackpressureException.class);
            // assert that the values are sequential, that cutting in didn't allow skipping some but emitting others.
            // example [0, 1, 2] not [0, 1, 4]
            List<Long> onNextEvents = ts.values();
            assertTrue(onNextEvents.isEmpty() || onNextEvents.size() == onNextEvents.get(onNextEvents.size() - 1) + 1);
            // we should emit the error without emitting the full buffer size
            assertTrue(onNextEvents.size() < Flowable.bufferSize());
        }
    }

    /**
     * Make sure we get a MissingBackpressureException propagated through when we have a fast temporal (hot) producer.
     */
    @Test
    public void hotOperatorBackpressure() {
        TestSubscriberEx<String> ts = new TestSubscriberEx<>();
        Flowable.interval(0, 1, TimeUnit.MICROSECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Long, String>() {

                    @Override
                    public String apply(Long t1) {
                        System.out.println(t1);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                        return t1 + " slow value";
                    }

                }).subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        System.out.println("Errors: " + ts.errors());
        assertEquals(1, ts.errors().size());
        assertEquals(MissingBackpressureException.class, ts.errors().get(0).getClass());
    }

    @Test
    public void errorPropagatesWhenNoOutstandingRequests() {
        Flowable<Long> timer = Flowable.interval(0, 1, TimeUnit.MICROSECONDS)
                .doOnEach(new Consumer<Notification<Long>>() {

                    @Override
                    public void accept(Notification<Long> n) {
//                        System.out.println("BEFORE " + n);
                    }

                })
                .observeOn(Schedulers.newThread())
                .doOnEach(new Consumer<Notification<Long>>() {

                    @Override
                    public void accept(Notification<Long> n) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
//                        System.out.println("AFTER " + n);
                    }

                });

        TestSubscriberEx<Long> ts = new TestSubscriberEx<>();

        Flowable.combineLatest(timer, Flowable.<Integer> never(), new BiFunction<Long, Integer, Long>() {

            @Override
            public Long apply(Long t1, Integer t2) {
                return t1;
            }

        }).take(Flowable.bufferSize() * 2).subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(1, ts.errors().size());
        assertEquals(MissingBackpressureException.class, ts.errors().get(0).getClass());
    }

    @Test
    public void requestOverflow() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger count = new AtomicInteger();
        Flowable.range(1, 100).observeOn(Schedulers.computation())
                .subscribe(new DefaultSubscriber<Integer>() {

                    boolean first = true;

                    @Override
                    public void onStart() {
                        request(2);
                    }

                    @Override
                    public void onComplete() {
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
    public void noMoreRequestsAfterUnsubscribe() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final List<Long> requests = Collections.synchronizedList(new ArrayList<>());
        Flowable.range(1, 1000000)
                .doOnRequest(new LongConsumer() {

                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Integer t) {
                        cancel();
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        // FIXME observeOn requests bufferSize at first always
        assertEquals(Arrays.asList(128L), requests);
    }

    @Test
    public void errorDelayed() {
        TestScheduler s = new TestScheduler();

        Flowable<Integer> source = Flowable.just(1, 2, 3)
                .concatWith(Flowable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        source.observeOn(s, true).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        s.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(1);
        s.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(3); // requesting 2 doesn't switch to the error() source for some reason in concat.
        s.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValues(1, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void errorDelayedAsync() {
        Flowable<Integer> source = Flowable.just(1, 2, 3)
                .concatWith(Flowable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create();

        source.observeOn(Schedulers.computation(), true).subscribe(ts);

        ts.awaitDone(2, TimeUnit.SECONDS);
        ts.assertValues(1, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void requestExactCompletesImmediately() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        TestScheduler test = new TestScheduler();

        Flowable.range(1, 10).observeOn(test).subscribe(ts);

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.request(10);

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValueCount(10);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void fixedReplenishPattern() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        TestScheduler test = new TestScheduler();

        final List<Long> requests = new ArrayList<>();

        Flowable.range(1, 100)
        .doOnRequest(new LongConsumer() {
            @Override
            public void accept(long v) {
                requests.add(v);
            }
        })
        .observeOn(test, false, 16).subscribe(ts);

        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.request(20);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.request(10);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.request(50);
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        ts.request(35);
        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValueCount(100);
        ts.assertComplete();
        ts.assertNoErrors();

        assertEquals(Arrays.asList(16L, 12L, 12L, 12L, 12L, 12L, 12L, 12L, 12L), requests);
    }

    @Test
    public void bufferSizesWork() {
        for (int i = 1; i <= 1024; i = i * 2) {
            TestSubscriber<Integer> ts = TestSubscriber.create();

            Flowable.range(1, 1000 * 1000).observeOn(Schedulers.computation(), false, i)
            .subscribe(ts);

            ts.awaitDone(5, TimeUnit.SECONDS);
            ts.assertValueCount(1000 * 1000);
            ts.assertComplete();
            ts.assertNoErrors();
        }
    }

    @Test
    public void synchronousRebatching() {
        final List<Long> requests = new ArrayList<>();

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Flowable.range(1, 50)
        .doOnRequest(new LongConsumer() {
            @Override
            public void accept(long r) {
                requests.add(r);
            }
        })
       .rebatchRequests(20)
       .subscribe(ts);

       ts.assertValueCount(50);
       ts.assertNoErrors();
       ts.assertComplete();

       assertEquals(Arrays.asList(20L, 15L, 15L, 15L), requests);
    }

    @Test
    public void rebatchRequestsArgumentCheck() {
        try {
            Flowable.never().rebatchRequests(-99);
            fail("Didn't throw IAE");
        } catch (IllegalArgumentException ex) {
            assertEquals("bufferSize > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void delayError() {
        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException()))
        .observeOn(Schedulers.computation(), true)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (v == 1) {
                    Thread.sleep(100);
                }
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void conditionalConsumer() {
        Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void take() {
        Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .take(3)
        .take(3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void cancelCleanup() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .test(0L);

        ts.cancel();
    }

    @Test
    public void conditionalConsumerFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void conditionalConsumerFusedReject() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void requestOne() throws Exception {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .to(TestHelper.<Integer>testSubscriber(1L));

        Thread.sleep(100);

        ts.assertSubscribed().assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void requestOneConditional() throws Exception {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .to(TestHelper.<Integer>testSubscriber(1L));

        Thread.sleep(100);

        ts.assertSubscribed().assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void conditionalConsumerFusedAsync() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> up = UnicastProcessor.create();

        up
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        up.onNext(1);
        up.onNext(2);
        up.onNext(3);
        up.onNext(4);
        up.onNext(5);
        up.onComplete();

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void conditionalConsumerHidden() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5).hide()
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void conditionalConsumerBarrier() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5)
        .map(Functions.<Integer>identity())
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishProcessor.create().observeOn(new TestScheduler()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.observeOn(new TestScheduler());
            }
        });
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestScheduler scheduler = new TestScheduler();
            TestSubscriber<Integer> ts = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onComplete();
                    subscriber.onNext(1);
                    subscriber.onError(new TestException());
                    subscriber.onComplete();
                }
            }
            .observeOn(scheduler)
            .test();

            scheduler.triggerActions();

            ts.assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void inputSyncFused() {
        Flowable.range(1, 5)
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputAsyncFused() {
        UnicastProcessor<Integer> us = UnicastProcessor.create();

        TestSubscriber<Integer> ts = us.observeOn(Schedulers.single()).test();

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputAsyncFusedError() {
        UnicastProcessor<Integer> us = UnicastProcessor.create();

        TestSubscriber<Integer> ts = us.observeOn(Schedulers.single()).test();

        us.onError(new TestException());

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void inputAsyncFusedErrorDelayed() {
        UnicastProcessor<Integer> us = UnicastProcessor.create();

        TestSubscriber<Integer> ts = us.observeOn(Schedulers.single(), true).test();

        us.onError(new TestException());

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void outputFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 5).hide()
        .observeOn(Schedulers.single())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void outputFusedReject() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.range(1, 5).hide()
        .observeOn(Schedulers.single())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void inputOutputAsyncFusedError() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> us = UnicastProcessor.create();

        us.observeOn(Schedulers.single())
        .subscribe(ts);

        us.onError(new TestException());

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void inputOutputAsyncFusedErrorDelayed() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        UnicastProcessor<Integer> us = UnicastProcessor.create();

        us.observeOn(Schedulers.single(), true)
        .subscribe(ts);

        us.onError(new TestException());

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void outputFusedCancelReentrant() throws Exception {
        final UnicastProcessor<Integer> us = UnicastProcessor.create();

        final CountDownLatch cdl = new CountDownLatch(1);

        us.observeOn(Schedulers.single())
        .subscribe(new FlowableSubscriber<Integer>() {
            Subscription upstream;
            int count;
            @Override
            public void onSubscribe(Subscription s) {
                this.upstream = s;
                ((QueueSubscription<?>)s).requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer value) {
                if (++count == 1) {
                    us.onNext(2);
                    upstream.cancel();
                    cdl.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        us.onNext(1);

        cdl.await();
    }

    @Test
    public void nonFusedPollThrows() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());

                @SuppressWarnings("unchecked")
                BaseObserveOnSubscriber<Integer> oo = (BaseObserveOnSubscriber<Integer>)subscriber;

                oo.sourceMode = QueueFuseable.SYNC;
                oo.requested.lazySet(1);
                oo.queue = new SimpleQueue<Integer>() {

                    @Override
                    public boolean offer(Integer value) {
                        return false;
                    }

                    @Override
                    public boolean offer(Integer v1, Integer v2) {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Integer poll() throws Exception {
                        throw new TestException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {
                    }
                };

                oo.clear();

                oo.trySchedule();
            }
        }
        .observeOn(Schedulers.single())
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void conditionalNonFusedPollThrows() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());

                @SuppressWarnings("unchecked")
                BaseObserveOnSubscriber<Integer> oo = (BaseObserveOnSubscriber<Integer>)subscriber;

                oo.sourceMode = QueueFuseable.SYNC;
                oo.requested.lazySet(1);
                oo.queue = new SimpleQueue<Integer>() {

                    @Override
                    public boolean offer(Integer value) {
                        return false;
                    }

                    @Override
                    public boolean offer(Integer v1, Integer v2) {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Integer poll() throws Exception {
                        throw new TestException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {
                    }
                };

                oo.clear();

                oo.trySchedule();
            }
        }
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void asycFusedPollThrows() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());

                @SuppressWarnings("unchecked")
                BaseObserveOnSubscriber<Integer> oo = (BaseObserveOnSubscriber<Integer>)subscriber;

                oo.sourceMode = QueueFuseable.ASYNC;
                oo.requested.lazySet(1);
                oo.queue = new SimpleQueue<Integer>() {

                    @Override
                    public boolean offer(Integer value) {
                        return false;
                    }

                    @Override
                    public boolean offer(Integer v1, Integer v2) {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Integer poll() throws Exception {
                        throw new TestException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {
                    }
                };

                oo.clear();

                oo.trySchedule();
            }
        }
        .observeOn(Schedulers.single())
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void conditionalAsyncFusedPollThrows() {
        new Flowable<Integer>() {
            @Override
            protected void subscribeActual(Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());

                @SuppressWarnings("unchecked")
                BaseObserveOnSubscriber<Integer> oo = (BaseObserveOnSubscriber<Integer>)subscriber;

                oo.sourceMode = QueueFuseable.ASYNC;
                oo.requested.lazySet(1);
                oo.queue = new SimpleQueue<Integer>() {

                    @Override
                    public boolean offer(Integer value) {
                        return false;
                    }

                    @Override
                    public boolean offer(Integer v1, Integer v2) {
                        return false;
                    }

                    @Nullable
                    @Override
                    public Integer poll() throws Exception {
                        throw new TestException();
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public void clear() {
                    }
                };

                oo.clear();

                oo.trySchedule();
            }
        }
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void trampolineScheduler() {
        Flowable.just(1)
        .observeOn(Schedulers.trampoline())
        .test()
        .assertResult(1);
    }

    @Test
    public void conditionalNormal() {
        Flowable.range(1, 1000).hide()
        .observeOn(Schedulers.single())
        .filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v % 2 == 0;
            }
        })
        .take(250)
        .to(TestHelper.<Integer>testConsumer())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(250)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void syncFusedCancelAfterRequest() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 2) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 3)
        .observeOn(Schedulers.single())
        .subscribe(ts);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void syncFusedCancelAfterRequest2() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>(2L);

        Flowable.range(1, 2)
        .observeOn(Schedulers.single())
        .subscribe(ts);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void syncFusedCancelAfterRequestConditional() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(2L) {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 2) {
                    cancel();
                    onComplete();
                }
            }
        };

        Flowable.range(1, 3)
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void syncFusedCancelAfterRequestConditional2() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>(2L);

        Flowable.range(1, 2)
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void nonFusedCancelAfterRequestConditional2() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>(2L);

        Flowable.range(1, 2).hide()
        .observeOn(Schedulers.single())
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2);
    }

    @Test
    public void doubleObserveOn() {
        Flowable.just(1).hide()
        .observeOn(Schedulers.computation())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void doubleObserveOnError() {
        Flowable.error(new TestException())
        .observeOn(Schedulers.computation())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleObserveOnConditional() {
        Flowable.just(1).hide()
        .observeOn(Schedulers.computation())
        .distinct()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void doubleObserveOnErrorConditional() {
        Flowable.error(new TestException())
        .observeOn(Schedulers.computation())
        .distinct()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void request1Conditional() {
        Flowable.range(1, 10).hide()
        .observeOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .test(1L)
        .assertValue(1);
    }

    @Test
    public void backFusedConditional() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 100).hide()
        .observeOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertValueCount(100)
        .assertComplete()
        .assertNoErrors();
    }

    @Test
    public void backFusedErrorConditional() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.<Integer>error(new TestException())
        .observeOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.ASYNC)
        .assertFailure(TestException.class);
    }

    @Test
    public void backFusedCancelConditional() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

            final TestScheduler scheduler = new TestScheduler();

            Flowable.just(1).hide()
            .observeOn(scheduler)
            .filter(Functions.alwaysTrue())
            .subscribe(ts);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    scheduler.triggerActions();
                }
            };

            TestHelper.race(r1, r2);

            ts.assertFusionMode(QueueFuseable.ASYNC);

            if (ts.values().size() != 0) {
                ts.assertResult(1);
            }
        }
    }

    @Test
    public void syncFusedRequestOneByOneConditional() {
        Flowable.range(1, 5)
        .observeOn(ImmediateThinScheduler.INSTANCE)
        .filter(Functions.alwaysTrue())
        .rebatchRequests(1)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    public static final class DisposeTrackingScheduler extends Scheduler {

        public final AtomicInteger disposedCount = new AtomicInteger();

        @Override
        public Worker createWorker() {
            return new TrackingWorker();
        }

        final class TrackingWorker extends Scheduler.Worker {

            @Override
            public void dispose() {
                disposedCount.getAndIncrement();
            }

            @Override
            public boolean isDisposed() {
                return false;
            }

            @Override
            public Disposable schedule(Runnable run, long delay,
                    TimeUnit unit) {
                run.run();
                return Disposable.empty();
            }
        }
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Flowable.concat(
                Flowable.just(1).hide().observeOn(s),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelySyncInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Flowable.concat(
                Flowable.just(1).observeOn(s),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelyAsyncInNormalOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        up.onNext(1);
        up.onComplete();

        Flowable.concat(
                up.observeOn(s),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    static final class TestSubscriberFusedCanceling
            extends TestSubscriberEx<Integer> {

        TestSubscriberFusedCanceling() {
            super();
            initialFusionMode = QueueFuseable.ANY;
        }

        @Override
        public void onComplete() {
            cancel();
            super.onComplete();
        }
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInAsyncOut() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        TestSubscriberEx<Integer> ts = new TestSubscriberFusedCanceling();

        Flowable.just(1).hide().observeOn(s).subscribe(ts);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInNormalOutConditional() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Flowable.concat(
                Flowable.just(1).hide().observeOn(s).filter(Functions.alwaysTrue()),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelySyncInNormalOutConditional() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        Flowable.concat(
                Flowable.just(1).observeOn(s).filter(Functions.alwaysTrue()),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelyAsyncInNormalOutConditional() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        UnicastProcessor<Integer> up = UnicastProcessor.create();
        up.onNext(1);
        up.onComplete();

        Flowable.concat(
                up.observeOn(s).filter(Functions.alwaysTrue()),
                Flowable.just(2)
        )
        .test()
        .assertResult(1, 2);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void workerNotDisposedPrematurelyNormalInAsyncOutConditional() {
        DisposeTrackingScheduler s = new DisposeTrackingScheduler();

        TestSubscriberEx<Integer> ts = new TestSubscriberFusedCanceling();

        Flowable.just(1).hide().observeOn(s).filter(Functions.alwaysTrue()).subscribe(ts);

        assertEquals(1, s.disposedCount.get());
    }

    @Test
    public void fusedNoConcurrentCleanDueToCancel() {
        for (int j = 0; j < TestHelper.RACE_LONG_LOOPS; j++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final UnicastProcessor<Integer> up = UnicastProcessor.create();

                TestObserver<Integer> to = up.hide()
                .observeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .unsubscribeOn(Schedulers.computation())
                .firstOrError()
                .test();

                for (int i = 0; up.hasSubscribers() && i < 10000; i++) {
                    up.onNext(i);
                }

                to
                .awaitDone(5, TimeUnit.SECONDS)
                ;

                if (!errors.isEmpty()) {
                    throw new CompositeException(errors);
                }

                to.assertResult(0);
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void fusedParallelProcessing() {
        Flowable.range(0, 500000)
        .subscribeOn(Schedulers.single())
        .observeOn(Schedulers.computation())
        .parallel()
        .runOn(Schedulers.computation())
        .map(Functions.<Integer>identity())
        .sequential()
        .test()
        .awaitDone(20, TimeUnit.SECONDS)
        .assertValueCount(500000)
        .assertComplete()
        .assertNoErrors();
    }
}
