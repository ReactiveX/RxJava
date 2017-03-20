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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;
import org.mockito.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class FlowableTimerTest {
    @Mock
    Subscriber<Object> observer;
    @Mock
    Subscriber<Long> observer2;

    TestScheduler scheduler;

    @Before
    public void before() {
        observer = TestHelper.mockSubscriber();

        observer2 = TestHelper.mockSubscriber();

        scheduler = new TestScheduler();
    }

    @Test
    public void testTimerOnce() {
        Flowable.timer(100, TimeUnit.MILLISECONDS, scheduler).subscribe(observer);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(observer, times(1)).onNext(0L);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTimerPeriodically() {
        TestSubscriber<Long> ts = new TestSubscriber<Long>();

        Flowable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler).subscribe(ts);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        ts.assertValue(0L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L, 3L);

        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        ts.assertValues(0L, 1L, 2L, 3L);

        ts.assertNotComplete();
        ts.assertNoErrors();
    }
    @Test
    public void testInterval() {
        Flowable<Long> w = Flowable.interval(1, TimeUnit.SECONDS, scheduler);
        TestSubscriber<Long> ts = new TestSubscriber<Long>();
        w.subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        ts.assertValues(0L, 1L);
        ts.assertNoErrors();
        ts.assertNotComplete();

        ts.dispose();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);
        ts.assertValues(0L, 1L);
        ts.assertNoErrors();
        ts.assertNotComplete();
    }

    @Test
    public void testWithMultipleSubscribersStartingAtSameTime() {
        Flowable<Long> w = Flowable.interval(1, TimeUnit.SECONDS, scheduler);

        TestSubscriber<Long> ts1 = new TestSubscriber<Long>();
        TestSubscriber<Long> ts2 = new TestSubscriber<Long>();

        w.subscribe(ts1);
        w.subscribe(ts2);

        ts1.assertNoValues();
        ts2.assertNoValues();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();

        ts1.dispose();
        ts2.dispose();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }

    @Test
    public void testWithMultipleStaggeredSubscribers() {
        Flowable<Long> w = Flowable.interval(1, TimeUnit.SECONDS, scheduler);

        TestSubscriber<Long> ts1 = new TestSubscriber<Long>();

        w.subscribe(ts1);

        ts1.assertNoErrors();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        TestSubscriber<Long> ts2 = new TestSubscriber<Long>();

        w.subscribe(ts2);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L, 2L, 3L);

        ts2.assertValues(0L, 1L);

        ts1.dispose();
        ts2.dispose();

        ts1.assertValues(0L, 1L, 2L, 3L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(0L, 1L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }

    @Test
    public void testWithMultipleStaggeredSubscribersAndPublish() {
        ConnectableFlowable<Long> w = Flowable.interval(1, TimeUnit.SECONDS, scheduler).publish();

        TestSubscriber<Long> ts1 = new TestSubscriber<Long>();

        w.subscribe(ts1);
        w.connect();

        ts1.assertNoValues();

        scheduler.advanceTimeTo(2, TimeUnit.SECONDS);

        TestSubscriber<Long> ts2 = new TestSubscriber<Long>();
        w.subscribe(ts2);

        ts1.assertValues(0L, 1L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();

        scheduler.advanceTimeTo(4, TimeUnit.SECONDS);

        ts1.assertValues(0L, 1L, 2L, 3L);

        ts2.assertValues(2L, 3L);

        ts1.dispose();
        ts2.dispose();

        ts1.assertValues(0L, 1L, 2L, 3L);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertValues(2L, 3L);
        ts2.assertNoErrors();
        ts2.assertNotComplete();
    }
    @Test
    public void testOnceObserverThrows() {
        Flowable<Long> source = Flowable.timer(100, TimeUnit.MILLISECONDS, scheduler);

        source.safeSubscribe(new DefaultSubscriber<Long>() {

            @Override
            public void onNext(Long t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        verify(observer).onError(any(TestException.class));
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onComplete();
    }
    @Test
    public void testPeriodicObserverThrows() {
        Flowable<Long> source = Flowable.interval(100, 100, TimeUnit.MILLISECONDS, scheduler);

        InOrder inOrder = inOrder(observer);

        source.safeSubscribe(new DefaultSubscriber<Long>() {

            @Override
            public void onNext(Long t) {
                if (t > 0) {
                    throw new TestException();
                }
                observer.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onComplete() {
                observer.onComplete();
            }
        });

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        inOrder.verify(observer).onNext(0L);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(observer, never()).onComplete();
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.timer(1, TimeUnit.DAYS));
    }

    @Test
    public void backpressureNotReady() {
        Flowable.timer(1, TimeUnit.MILLISECONDS)
        .test(0L)
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void timerCancelRace() {
        for (int i = 0; i < 500; i++) {
            final TestSubscriber<Long> ts = new TestSubscriber<Long>();

            final TestScheduler scheduler = new TestScheduler();

            Flowable.timer(1, TimeUnit.SECONDS, scheduler)
            .subscribe(ts);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void timerDelayZero() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            for (int i = 0; i < 1000; i++) {
                Flowable.timer(0, TimeUnit.MILLISECONDS).blockingFirst();
            }

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void timerInterruptible() throws Exception {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        try {
            for (Scheduler s : new Scheduler[] { Schedulers.single(), Schedulers.computation(), Schedulers.newThread(), Schedulers.io(), Schedulers.from(exec) }) {
                final AtomicBoolean interrupted = new AtomicBoolean();
                TestSubscriber<Long> ts = Flowable.timer(1, TimeUnit.MILLISECONDS, s)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long v) throws Exception {
                        try {
                        Thread.sleep(3000);
                        } catch (InterruptedException ex) {
                            interrupted.set(true);
                        }
                        return v;
                    }
                })
                .test();

                Thread.sleep(500);

                ts.cancel();

                Thread.sleep(500);

                assertTrue(s.getClass().getSimpleName(), interrupted.get());
            }
        } finally {
            exec.shutdown();
        }
    }
}
