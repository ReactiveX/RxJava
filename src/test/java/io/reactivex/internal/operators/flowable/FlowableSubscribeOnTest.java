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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.annotations.NonNull;
import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.flowable.FlowableSubscribeOn.SubscribeOnSubscriber;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class FlowableSubscribeOnTest {

    @Test(timeout = 2000)
    public void testIssue813() throws InterruptedException {
        // https://github.com/ReactiveX/RxJava/issues/813
        final CountDownLatch scheduled = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);

        TestSubscriber<Integer> observer = new TestSubscriber<Integer>();

        Flowable
        .unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(
                    final Subscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                scheduled.countDown();
                try {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        // this means we were unsubscribed (Scheduler shut down and interrupts)
                        // ... but we'll pretend we are like many Flowables that ignore interrupts
                    }

                    subscriber.onComplete();
                } catch (Throwable e) {
                    subscriber.onError(e);
                } finally {
                    doneLatch.countDown();
                }
            }
        }).subscribeOn(Schedulers.computation()).subscribe(observer);

        // wait for scheduling
        scheduled.await();
        // trigger unsubscribe
        observer.dispose();
        latch.countDown();
        doneLatch.await();
        observer.assertNoErrors();
        observer.assertComplete();
    }

    @Test
    @Ignore("Publisher.subscribe can't throw")
    public void testThrownErrorHandling() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> s) {
                throw new RuntimeException("fail");
            }

        }).subscribeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
    }

    @Test
    public void testOnError() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> s) {
                s.onSubscribe(new BooleanSubscription());
                s.onError(new RuntimeException("fail"));
            }

        }).subscribeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
    }

    public static class SlowScheduler extends Scheduler {
        final Scheduler actual;
        final long delay;
        final TimeUnit unit;

        public SlowScheduler() {
            this(Schedulers.computation(), 2, TimeUnit.SECONDS);
        }

        public SlowScheduler(Scheduler actual, long delay, TimeUnit unit) {
            this.actual = actual;
            this.delay = delay;
            this.unit = unit;
        }

        @NonNull
        @Override
        public Worker createWorker() {
            return new SlowInner(actual.createWorker());
        }

        private final class SlowInner extends Worker {

            private final Scheduler.Worker actualInner;

            private SlowInner(Worker actual) {
                this.actualInner = actual;
            }

            @Override
            public void dispose() {
                actualInner.dispose();
            }

            @Override
            public boolean isDisposed() {
                return actualInner.isDisposed();
            }

            @NonNull
            @Override
            public Disposable schedule(@NonNull final Runnable action) {
                return actualInner.schedule(action, delay, unit);
            }

            @NonNull
            @Override
            public Disposable schedule(@NonNull final Runnable action, final long delayTime, @NonNull final TimeUnit delayUnit) {
                TimeUnit common = delayUnit.compareTo(unit) < 0 ? delayUnit : unit;
                long t = common.convert(delayTime, delayUnit) + common.convert(delay, unit);
                return actualInner.schedule(action, t, common);
            }

        }

    }

    @Test(timeout = 5000)
    public void testUnsubscribeInfiniteStream() throws InterruptedException {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Flowable.unsafeCreate(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> sub) {
                BooleanSubscription bs = new BooleanSubscription();
                sub.onSubscribe(bs);
                for (int i = 1; !bs.isCancelled(); i++) {
                    count.incrementAndGet();
                    sub.onNext(i);
                }
            }

        }).subscribeOn(Schedulers.newThread()).take(10).subscribe(ts);

        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.dispose();
        Thread.sleep(200); // give time for the loop to continue
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(10, count.get());
    }

    @Test
    public void testBackpressureReschedulesCorrectly() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(10);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(new DefaultSubscriber<Integer>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer t) {
                latch.countDown();
            }

        });
        ts.request(10);
        Flowable.range(1, 10000000).subscribeOn(Schedulers.newThread()).take(20).subscribe(ts);
        latch.await();
        Thread t = ts.lastThread();
        System.out.println("First schedule: " + t);
        assertTrue(t.getName().startsWith("Rx"));
        ts.request(10);
        ts.awaitTerminalEvent();
        System.out.println("After reschedule: " + ts.lastThread());
        assertEquals(t, ts.lastThread());
    }

    @Test
    public void testSetProducerSynchronousRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Flowable.just(1, 2, 3).lift(new FlowableOperator<Integer, Integer>() {

            @Override
            public Subscriber<? super Integer> apply(final Subscriber<? super Integer> child) {
                final AtomicLong requested = new AtomicLong();
                child.onSubscribe(new Subscription() {

                    @Override
                    public void request(long n) {
                        if (!requested.compareAndSet(0, n)) {
                            child.onError(new RuntimeException("Expected to receive request before onNext but didn't"));
                        }
                    }

                    @Override
                    public void cancel() {

                    }

                });
                Subscriber<Integer> parent = new DefaultSubscriber<Integer>() {

                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        if (requested.compareAndSet(0, -99)) {
                            child.onError(new RuntimeException("Got values before requested"));
                        }
                    }
                };

                return parent;
            }

        }).subscribeOn(Schedulers.newThread()).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void cancelBeforeActualSubscribe() {
        TestScheduler test = new TestScheduler();

        TestSubscriber<Integer> ts = Flowable.just(1).hide()
                .subscribeOn(test).test(Long.MAX_VALUE, true);

        test.advanceTimeBy(1, TimeUnit.SECONDS);

        ts
        .assertSubscribed()
        .assertNoValues()
        .assertNotTerminated();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).subscribeOn(Schedulers.single()));
    }

    @Test
    public void deferredRequestRace() {
        for (int i = 0; i < 500; i++) {

            final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0L);

            Worker w = Schedulers.computation().createWorker();

            final SubscribeOnSubscriber<Integer> so = new SubscribeOnSubscriber<Integer>(ts, w, Flowable.<Integer>never(), true);
            ts.onSubscribe(so);

            final BooleanSubscription bs = new BooleanSubscription();

            try {
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        so.onSubscribe(bs);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        so.request(1);
                    }
                };

                TestHelper.race(r1, r2);
            } finally {
                w.dispose();
            }
        }
    }

    @Test
    public void nonScheduledRequests() {
        TestSubscriber<Object> ts = Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception {
                for (int i = 1; i < 1001; i++) {
                    s.onNext(i);
                    Thread.sleep(1);
                }
                s.onComplete();
            }
        }, BackpressureStrategy.DROP)
        .subscribeOn(Schedulers.single())
        .observeOn(Schedulers.computation())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();

        int c = ts.valueCount();

        assertTrue("" + c, c > Flowable.bufferSize());
    }

    @Test
    public void scheduledRequests() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception {
                for (int i = 1; i < 1001; i++) {
                    s.onNext(i);
                    Thread.sleep(1);
                }
                s.onComplete();
            }
        }, BackpressureStrategy.DROP)
        .map(Functions.identity())
        .subscribeOn(Schedulers.single())
        .observeOn(Schedulers.computation())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(Flowable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void nonScheduledRequestsNotSubsequentSubscribeOn() {
        TestSubscriber<Object> ts = Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception {
                for (int i = 1; i < 1001; i++) {
                    s.onNext(i);
                    Thread.sleep(1);
                }
                s.onComplete();
            }
        }, BackpressureStrategy.DROP)
        .map(Functions.identity())
        .subscribeOn(Schedulers.single(), false)
        .observeOn(Schedulers.computation())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();

        int c = ts.valueCount();

        assertTrue("" + c, c > Flowable.bufferSize());
    }

    @Test
    public void scheduledRequestsNotSubsequentSubscribeOn() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception {
                for (int i = 1; i < 1001; i++) {
                    s.onNext(i);
                    Thread.sleep(1);
                }
                s.onComplete();
            }
        }, BackpressureStrategy.DROP)
        .map(Functions.identity())
        .subscribeOn(Schedulers.single(), true)
        .observeOn(Schedulers.computation())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(Flowable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }
}
