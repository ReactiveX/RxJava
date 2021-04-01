/*
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

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler.*;

public class TestSchedulerTest extends RxJavaTest {

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void periodicScheduling() throws Throwable {
        final Function<Long, Void> calledOp = mock(Function.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            inner.schedulePeriodically(new Runnable() {
                @Override
                public void run() {
                    System.out.println(scheduler.now(TimeUnit.MILLISECONDS));
                    try {
                        calledOp.apply(scheduler.now(TimeUnit.MILLISECONDS));
                    } catch (Throwable ex) {
                        ExceptionHelper.wrapOrThrow(ex);
                    }
                }
            }, 1, 2, TimeUnit.SECONDS);

            verify(calledOp, never()).apply(anyLong());

            InOrder inOrder = Mockito.inOrder(calledOp);

            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).apply(anyLong());

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).apply(1000L);

            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).apply(3000L);

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).apply(3000L);

            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).apply(5000L);
            inOrder.verify(calledOp, times(1)).apply(7000L);

            inner.dispose();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).apply(anyLong());
        } finally {
            inner.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void periodicSchedulingUnsubscription() throws Throwable {
        final Function<Long, Void> calledOp = mock(Function.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final Disposable subscription = inner.schedulePeriodically(new Runnable() {
                @Override
                public void run() {
                    System.out.println(scheduler.now(TimeUnit.MILLISECONDS));
                    try {
                        calledOp.apply(scheduler.now(TimeUnit.MILLISECONDS));
                    } catch (Throwable ex) {
                        ExceptionHelper.wrapOrThrow(ex);
                    }
                }
            }, 1, 2, TimeUnit.SECONDS);

            verify(calledOp, never()).apply(anyLong());

            InOrder inOrder = Mockito.inOrder(calledOp);

            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).apply(anyLong());

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).apply(1000L);

            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).apply(3000L);

            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).apply(3000L);

            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).apply(5000L);
            inOrder.verify(calledOp, times(1)).apply(7000L);

            subscription.dispose();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).apply(anyLong());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void immediateUnsubscribes() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        final AtomicInteger counter = new AtomicInteger(0);

        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }

            });
            inner.dispose();
            assertEquals(0, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void immediateUnsubscribes2() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        try {
            final AtomicInteger counter = new AtomicInteger(0);

            final Disposable subscription = inner.schedule(new Runnable() {

                @Override
                public void run() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }

            });
            subscription.dispose();
            assertEquals(0, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void nestedSchedule() {
        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final Runnable calledOp = mock(Runnable.class);

            Flowable<Object> poller;
            poller = Flowable.unsafeCreate(new Publisher<Object>() {
                @Override
                public void subscribe(final Subscriber<? super Object> aSubscriber) {
                    final BooleanSubscription bs = new BooleanSubscription();
                    aSubscriber.onSubscribe(bs);
                    inner.schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (!bs.isCancelled()) {
                                calledOp.run();
                                inner.schedule(this, 5, TimeUnit.SECONDS);
                            }
                        }
                    });
                }
            });

            InOrder inOrder = Mockito.inOrder(calledOp);

            Disposable sub;
            sub = poller.subscribe();

            scheduler.advanceTimeTo(6, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(2)).run();

            sub.dispose();
            scheduler.advanceTimeTo(11, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).run();

            sub = poller.subscribe();
            scheduler.advanceTimeTo(12, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).run();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void timedRunnableToString() {
        TimedRunnable r = new TimedRunnable((TestWorker) new TestScheduler().createWorker(), 5, new Runnable() {
            @Override
            public void run() {
                // deliberately no-op
            }

            @Override
            public String toString() {
                return "Runnable";
            }
        }, 1);

        assertEquals("TimedRunnable(time = 5, run = Runnable)", r.toString());
    }

    @Test
    public void workerDisposed() {
        TestScheduler scheduler = new TestScheduler();

        Worker w = scheduler.createWorker();
        w.dispose();
        assertTrue(w.isDisposed());
    }

    @Test
    public void constructorTimeSetsTime() {
        TestScheduler ts = new TestScheduler(5, TimeUnit.SECONDS);
        assertEquals(5, ts.now(TimeUnit.SECONDS));
        assertEquals(5000, ts.now(TimeUnit.MILLISECONDS));
    }

    @Test
    public void withOnScheduleHook() {
        AtomicInteger run = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        RxJavaPlugins.setScheduleHandler(r -> {
            counter.getAndIncrement();
            return r;
        });
        try {
            Runnable r = () -> run.getAndIncrement();
            TestScheduler ts = new TestScheduler(true);

            ts.createWorker().schedule(r);
            ts.createWorker().schedule(r, 1, TimeUnit.SECONDS);

            ts.advanceTimeBy(1, TimeUnit.SECONDS);

            assertEquals(2, run.get());
            assertEquals(2, counter.get());

            ts = new TestScheduler();

            ts.createWorker().schedule(r);
            ts.createWorker().schedule(r, 1, TimeUnit.SECONDS);

            ts.advanceTimeBy(1, TimeUnit.SECONDS);

            assertEquals(4, run.get());
            assertEquals(2, counter.get());
        } finally {
            RxJavaPlugins.setScheduleHandler(null);
        }
    }

    @Test
    public void withOnScheduleHookInitialTime() {
        AtomicInteger run = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        RxJavaPlugins.setScheduleHandler(r -> {
            counter.getAndIncrement();
            return r;
        });
        try {
            Runnable r = () -> run.getAndIncrement();
            TestScheduler ts = new TestScheduler(1, TimeUnit.HOURS, true);

            ts.createWorker().schedule(r);
            ts.createWorker().schedule(r, 1, TimeUnit.SECONDS);

            ts.advanceTimeBy(1, TimeUnit.SECONDS);

            assertEquals(2, run.get());
            assertEquals(2, counter.get());

            ts = new TestScheduler(1, TimeUnit.HOURS);

            ts.createWorker().schedule(r);
            ts.createWorker().schedule(r, 1, TimeUnit.SECONDS);

            ts.advanceTimeBy(1, TimeUnit.SECONDS);

            assertEquals(4, run.get());
            assertEquals(2, counter.get());
        } finally {
            RxJavaPlugins.setScheduleHandler(null);
        }
    }

    @Test
    public void disposeWork() {
        AtomicInteger run = new AtomicInteger();
        Runnable r = () -> run.getAndIncrement();
        TestScheduler ts = new TestScheduler(1, TimeUnit.HOURS, true);

        Disposable d = ts.createWorker().schedule(r);

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        ts.advanceTimeBy(1, TimeUnit.SECONDS);

        assertEquals(0, run.get());
    }
}
