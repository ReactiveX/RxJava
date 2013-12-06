/**
 * Copyright 2013 Netflix, Inc.
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
package rx;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable.OnSubscribeFunc;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class SchedulersTest {

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public void testPeriodicScheduling() {
        final Func1<Long, Void> calledOp = mock(Func1.class);

        final TestScheduler scheduler = new TestScheduler();
        Subscription subscription = scheduler.schedulePeriodically(new Action0() {
            @Override
            public void call() {
                System.out.println(scheduler.now());
                calledOp.call(scheduler.now());
            }
        }, 1, 2, TimeUnit.SECONDS);

        verify(calledOp, never()).call(anyLong());

        InOrder inOrder = Mockito.inOrder(calledOp);

        scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
        inOrder.verify(calledOp, never()).call(anyLong());

        scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
        inOrder.verify(calledOp, times(1)).call(1000L);

        scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
        inOrder.verify(calledOp, never()).call(3000L);

        scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
        inOrder.verify(calledOp, times(1)).call(3000L);

        scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
        inOrder.verify(calledOp, times(1)).call(5000L);
        inOrder.verify(calledOp, times(1)).call(7000L);

        subscription.unsubscribe();
        scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
        inOrder.verify(calledOp, never()).call(anyLong());
    }

    @Test
    public void testComputationThreadPool1() {

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.threadPoolForComputation()).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testIOThreadPool1() {

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxIOThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.threadPoolForIO()).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testMergeWithoutScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testMergeWithImmediateScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).subscribeOn(Schedulers.immediate()).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testMergeWithCurrentThreadScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).subscribeOn(Schedulers.currentThread()).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testMergeWithScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).subscribeOn(Schedulers.threadPoolForComputation()).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertFalse(Thread.currentThread().getName().equals(currentThreadName));
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testSubscribeWithScheduler1() throws InterruptedException {

        final AtomicInteger count = new AtomicInteger();

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);

        o1.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
            }
        });

        // the above should be blocking so we should see a count of 5
        assertEquals(5, count.get());

        count.set(0);

        // now we'll subscribe with a scheduler and it should be async

        final String currentThreadName = Thread.currentThread().getName();

        // latches for deterministically controlling the test below across threads
        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch first = new CountDownLatch(1);

        o1.subscribe(new Action1<Integer>() {

            @Override
            public void call(Integer t) {
                try {
                    // we block the first one so we can assert this executes asynchronously with a count
                    first.await(1000, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("The latch should have released if we are async.", e);
                }
                assertFalse(Thread.currentThread().getName().equals(currentThreadName));
                assertTrue(Thread.currentThread().getName().startsWith("RxComputationThreadPool"));
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
                latch.countDown();
            }
        }, Schedulers.threadPoolForComputation());

        // assert we are async
        assertEquals(0, count.get());
        // release the latch so it can go forward
        first.countDown();

        // wait for all 5 responses
        latch.await();
        assertEquals(5, count.get());
    }

    @Test
    public void testRecursiveScheduler1() {
        Observable<Integer> obs = Observable.create(new OnSubscribeFunc<Integer>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Integer> observer) {
                return Schedulers.currentThread().schedule(0, new Func2<Scheduler, Integer, Subscription>() {
                    @Override
                    public Subscription call(Scheduler scheduler, Integer i) {
                        if (i > 42) {
                            observer.onCompleted();
                            return Subscriptions.empty();
                        }

                        observer.onNext(i);

                        return scheduler.schedule(i + 1, this);
                    }
                });
            }
        });

        final AtomicInteger lastValue = new AtomicInteger();
        obs.toBlockingObservable().forEach(new Action1<Integer>() {

            @Override
            public void call(Integer v) {
                System.out.println("Value: " + v);
                lastValue.set(v);
            }
        });

        assertEquals(42, lastValue.get());
    }

    @Test
    public void testRecursiveScheduler2() throws InterruptedException {
        // use latches instead of Thread.sleep
        final CountDownLatch latch = new CountDownLatch(10);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        Observable<Integer> obs = Observable.create(new OnSubscribeFunc<Integer>() {
            @Override
            public Subscription onSubscribe(final Observer<? super Integer> observer) {

                return Schedulers.threadPoolForComputation().schedule(new BooleanSubscription(), new Func2<Scheduler, BooleanSubscription, Subscription>() {
                    @Override
                    public Subscription call(Scheduler scheduler, BooleanSubscription cancel) {
                        if (cancel.isUnsubscribed()) {
                            observer.onCompleted();
                            completionLatch.countDown();
                            return Subscriptions.empty();
                        }

                        observer.onNext(42);
                        latch.countDown();

                        // this will recursively schedule this task for execution again
                        scheduler.schedule(cancel, this);

                        return cancel;
                    }
                });
            }
        });

        final AtomicInteger count = new AtomicInteger();
        final AtomicBoolean completed = new AtomicBoolean(false);
        Subscription subscribe = obs.subscribe(new Observer<Integer>() {
            @Override
            public void onCompleted() {
                System.out.println("Completed");
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Error");
            }

            @Override
            public void onNext(Integer args) {
                count.incrementAndGet();
                System.out.println(args);
            }
        });

        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Timed out waiting on onNext latch");
        }

        // now unsubscribe and ensure it stops the recursive loop
        subscribe.unsubscribe();
        System.out.println("unsubscribe");

        if (!completionLatch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Timed out waiting on completion latch");
        }

        // the count can be 10 or higher due to thread scheduling of the unsubscribe vs the scheduler looping to emit the count
        assertTrue(count.get() >= 10);
        assertTrue(completed.get());
    }

    @Test
    public void testSchedulingWithDueTime() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(5);
        final AtomicInteger counter = new AtomicInteger();

        long start = System.currentTimeMillis();

        Schedulers.threadPoolForComputation().schedule(null, new Func2<Scheduler, String, Subscription>() {

            @Override
            public Subscription call(Scheduler scheduler, String state) {
                System.out.println("doing work");
                counter.incrementAndGet();
                latch.countDown();
                if (latch.getCount() == 0) {
                    return Subscriptions.empty();
                } else {
                    return scheduler.schedule(state, this, new Date(System.currentTimeMillis() + 50));
                }
            }
        }, new Date(System.currentTimeMillis() + 100));

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            fail("didn't execute ... timed out");
        }

        long end = System.currentTimeMillis();

        assertEquals(5, counter.get());
        if ((end - start) < 250) {
            fail("it should have taken over 250ms since each step was scheduled 50ms in the future");
        }
    }

    @Test
    public void testConcurrentOnNextFailsValidation() throws InterruptedException {

        final int count = 10;
        final CountDownLatch latch = new CountDownLatch(count);
        Observable<String> o = Observable.create(new OnSubscribeFunc<String>() {

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                for (int i = 0; i < count; i++) {
                    final int v = i;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            observer.onNext("v: " + v);

                            latch.countDown();
                        }
                    }).start();
                }
                return Subscriptions.empty();
            }
        });

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();
        // this should call onNext concurrently
        o.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() == null) {
            fail("We expected error messages due to schedulers");
        }
    }

    @Test
    public void testObserveOn() throws InterruptedException {

        Observable<String> o = Observable.from("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();

        o.observeOn(Schedulers.threadPoolForComputation()).subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    @Test
    public void testSubscribeOnNestedConcurrency() throws InterruptedException {

        Observable<String> o = Observable.from("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
                .mapMany(new Func1<String, Observable<String>>() {

                    @Override
                    public Observable<String> call(final String v) {
                        return Observable.create(new OnSubscribeFunc<String>() {

                            @Override
                            public Subscription onSubscribe(final Observer<? super String> observer) {
                                observer.onNext("value_after_map-" + v);
                                observer.onCompleted();
                                return Subscriptions.empty();
                            }
                        }).subscribeOn(Schedulers.newThread()); // subscribe on a new thread
                    }
                });

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<String>();

        o.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    @Test
    public void testRecursion() {
        TestScheduler s = new TestScheduler();

        final AtomicInteger counter = new AtomicInteger(0);

        Subscription subscription = s.schedule(new Action1<Action0>() {

            @Override
            public void call(Action0 self) {
                counter.incrementAndGet();
                System.out.println("counter: " + counter.get());
                self.call();
            }

        });
        subscription.unsubscribe();
        assertEquals(0, counter.get());
    }

    /**
     * Used to determine if onNext is being invoked concurrently.
     * 
     * @param <T>
     */
    private static class ConcurrentObserverValidator<T> implements Observer<T> {

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void onCompleted() {
            completed.countDown();
        }

        @Override
        public void onError(Throwable e) {
            completed.countDown();
            error.set(e);
        }

        @Override
        public void onNext(T args) {
            int count = concurrentCounter.incrementAndGet();
            System.out.println("ConcurrentObserverValidator.onNext: " + args);
            if (count > 1) {
                onError(new RuntimeException("we should not have concurrent execution of onNext"));
            }
            try {
                try {
                    // take some time so other onNext calls could pile up (I haven't yet thought of a way to do this without sleeping)
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // ignore
                }
            } finally {
                concurrentCounter.decrementAndGet();
            }
        }

    }
}
