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

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.SequentialDisposable;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

/**
 * Base tests for all schedulers including Immediate/Current.
 */
public abstract class AbstractSchedulerTests extends RxJavaTest {

    /**
     * The scheduler to test.
     *
     * @return the Scheduler instance
     */
    protected abstract Scheduler getScheduler();

    @Test
    public void nestedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            final Runnable firstStepStart = mock(Runnable.class);
            final Runnable firstStepEnd = mock(Runnable.class);

            final Runnable secondStepStart = mock(Runnable.class);
            final Runnable secondStepEnd = mock(Runnable.class);

            final Runnable thirdStepStart = mock(Runnable.class);
            final Runnable thirdStepEnd = mock(Runnable.class);

            final Runnable firstAction = new Runnable() {
                @Override
                public void run() {
                    firstStepStart.run();
                    firstStepEnd.run();
                    latch.countDown();
                }
            };
            final Runnable secondAction = new Runnable() {
                @Override
                public void run() {
                    secondStepStart.run();
                    inner.schedule(firstAction);
                    secondStepEnd.run();

                }
            };
            final Runnable thirdAction = new Runnable() {
                @Override
                public void run() {
                    thirdStepStart.run();
                    inner.schedule(secondAction);
                    thirdStepEnd.run();
                }
            };

            InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

            inner.schedule(thirdAction);

            latch.await();

            inOrder.verify(thirdStepStart, times(1)).run();
            inOrder.verify(thirdStepEnd, times(1)).run();
            inOrder.verify(secondStepStart, times(1)).run();
            inOrder.verify(secondStepEnd, times(1)).run();
            inOrder.verify(firstStepStart, times(1)).run();
            inOrder.verify(firstStepEnd, times(1)).run();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void nestedScheduling() {

        Flowable<Integer> ids = Flowable.fromIterable(Arrays.asList(1, 2)).subscribeOn(getScheduler());

        Flowable<String> m = ids.flatMap(new Function<Integer, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Integer id) {
                return Flowable.fromIterable(Arrays.asList("a-" + id, "b-" + id)).subscribeOn(getScheduler())
                        .map(new Function<String, String>() {

                            @Override
                            public String apply(String s) {
                                return "names=>" + s;
                            }
                        });
            }

        });

        List<String> strings = m.toList().blockingGet();

        assertEquals(4, strings.size());
        // because flatMap does a merge there is no guarantee of order
        assertTrue(strings.contains("names=>a-1"));
        assertTrue(strings.contains("names=>a-2"));
        assertTrue(strings.contains("names=>b-1"));
        assertTrue(strings.contains("names=>b-2"));
    }

    /**
     * The order of execution is nondeterministic.
     *
     * @throws InterruptedException if the await is interrupted
     */
    @SuppressWarnings("rawtypes")
    @Test
    public final void sequenceOfActions() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();
        try {
            final CountDownLatch latch = new CountDownLatch(2);
            final Runnable first = mock(Runnable.class);
            final Runnable second = mock(Runnable.class);

            // make it wait until both the first and second are called
            doAnswer(new Answer() {

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    try {
                        return invocation.getMock();
                    } finally {
                        latch.countDown();
                    }
                }
            }).when(first).run();
            doAnswer(new Answer() {

                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    try {
                        return invocation.getMock();
                    } finally {
                        latch.countDown();
                    }
                }
            }).when(second).run();

            inner.schedule(first);
            inner.schedule(second);

            latch.await();

            verify(first, times(1)).run();
            verify(second, times(1)).run();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void sequenceOfDelayedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final Runnable first = mock(Runnable.class);
            final Runnable second = mock(Runnable.class);

            inner.schedule(new Runnable() {
                @Override
                public void run() {
                    inner.schedule(first, 30, TimeUnit.MILLISECONDS);
                    inner.schedule(second, 10, TimeUnit.MILLISECONDS);
                    inner.schedule(new Runnable() {

                        @Override
                        public void run() {
                            latch.countDown();
                        }
                    }, 40, TimeUnit.MILLISECONDS);
                }
            });

            latch.await();
            InOrder inOrder = inOrder(first, second);

            inOrder.verify(second, times(1)).run();
            inOrder.verify(first, times(1)).run();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void mixOfDelayedAndNonDelayedActions() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final CountDownLatch latch = new CountDownLatch(1);
            final Runnable first = mock(Runnable.class);
            final Runnable second = mock(Runnable.class);
            final Runnable third = mock(Runnable.class);
            final Runnable fourth = mock(Runnable.class);

            inner.schedule(new Runnable() {
                @Override
                public void run() {
                    inner.schedule(first);
                    inner.schedule(second, 300, TimeUnit.MILLISECONDS);
                    inner.schedule(third, 100, TimeUnit.MILLISECONDS);
                    inner.schedule(fourth);
                    inner.schedule(new Runnable() {

                        @Override
                        public void run() {
                            latch.countDown();
                        }
                    }, 400, TimeUnit.MILLISECONDS);
                }
            });

            latch.await();
            InOrder inOrder = inOrder(first, second, third, fourth);

            inOrder.verify(first, times(1)).run();
            inOrder.verify(fourth, times(1)).run();
            inOrder.verify(third, times(1)).run();
            inOrder.verify(second, times(1)).run();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void recursiveExecution() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {

            final AtomicInteger i = new AtomicInteger();
            final CountDownLatch latch = new CountDownLatch(1);
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    if (i.incrementAndGet() < 100) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            latch.await();
            assertEquals(100, i.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void recursiveExecutionWithDelayTime() throws InterruptedException {
        Scheduler scheduler = getScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final AtomicInteger i = new AtomicInteger();
            final CountDownLatch latch = new CountDownLatch(1);

            inner.schedule(new Runnable() {

                int state;

                @Override
                public void run() {
                    i.set(state);
                    if (state++ < 100) {
                        inner.schedule(this, 1, TimeUnit.MILLISECONDS);
                    } else {
                        latch.countDown();
                    }
                }

            });

            latch.await();
            assertEquals(100, i.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void recursiveSchedulerInObservable() {
        Flowable<Integer> obs = Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(final Subscriber<? super Integer> subscriber) {
                final Scheduler.Worker inner = getScheduler().createWorker();

                AsyncSubscription as = new AsyncSubscription();
                subscriber.onSubscribe(as);
                as.setResource(inner);

                inner.schedule(new Runnable() {
                    int i;

                    @Override
                    public void run() {
                        if (i > 42) {
                            try {
                                subscriber.onComplete();
                            } finally {
                                inner.dispose();
                            }
                            return;
                        }

                        subscriber.onNext(i++);

                        inner.schedule(this);
                    }
                });
            }
        });

        final AtomicInteger lastValue = new AtomicInteger();
        obs.blockingForEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer v) {
                System.out.println("Value: " + v);
                lastValue.set(v);
            }
        });

        assertEquals(42, lastValue.get());
    }

    @Test
    public final void concurrentOnNextFailsValidation() throws InterruptedException {
        final int count = 10;
        final CountDownLatch latch = new CountDownLatch(count);
        Flowable<String> f = Flowable.unsafeCreate(new Publisher<String>() {

            @Override
            public void subscribe(final Subscriber<? super String> subscriber) {
                subscriber.onSubscribe(new BooleanSubscription());
                for (int i = 0; i < count; i++) {
                    final int v = i;
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            subscriber.onNext("v: " + v);

                            latch.countDown();
                        }
                    }).start();
                }
            }
        });

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<>();
        // this should call onNext concurrently
        f.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() == null) {
            fail("We expected error messages due to concurrency");
        }
    }

    @Test
    public final void observeOn() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        Flowable<String> f = Flowable.fromArray("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten");

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<>();

        f.observeOn(scheduler).subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    @Test
    public final void subscribeOnNestedConcurrency() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        Flowable<String> f = Flowable.fromArray("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
                .flatMap(new Function<String, Flowable<String>>() {

                    @Override
                    public Flowable<String> apply(final String v) {
                        return Flowable.unsafeCreate(new Publisher<String>() {

                            @Override
                            public void subscribe(Subscriber<? super String> subscriber) {
                                subscriber.onSubscribe(new BooleanSubscription());
                                subscriber.onNext("value_after_map-" + v);
                                subscriber.onComplete();
                            }
                        }).subscribeOn(scheduler);
                    }
                });

        ConcurrentObserverValidator<String> observer = new ConcurrentObserverValidator<>();

        f.subscribe(observer);

        if (!observer.completed.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        if (observer.error.get() != null) {
            observer.error.get().printStackTrace();
            fail("Error: " + observer.error.get().getMessage());
        }
    }

    /**
     * Used to determine if onNext is being invoked concurrently.
     *
     * @param <T> the element type
     */
    private static class ConcurrentObserverValidator<T> extends DefaultSubscriber<T> {

        final AtomicInteger concurrentCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void onComplete() {
            completed.countDown();
        }

        @Override
        public void onError(Throwable e) {
            error.set(e);
            completed.countDown();
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

    @Test
    public void scheduleDirect() throws Exception {
        Scheduler s = getScheduler();

        final CountDownLatch cdl = new CountDownLatch(1);

        s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        });

        assertTrue(cdl.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleDirectDelayed() throws Exception {
        Scheduler s = getScheduler();

        final CountDownLatch cdl = new CountDownLatch(1);

        s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        }, 50, TimeUnit.MILLISECONDS);

        assertTrue(cdl.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void scheduleDirectPeriodic() throws Exception {
        Scheduler s = getScheduler();
        if (s instanceof TrampolineScheduler) {
            // can't properly stop a trampolined periodic task
            return;
        }

        final CountDownLatch cdl = new CountDownLatch(5);

        Disposable d = s.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        }, 10, 10, TimeUnit.MILLISECONDS);

        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));
        } finally {
            d.dispose();
        }
        assertTrue(d.isDisposed());
    }

    @Test
    public void schedulePeriodicallyDirectZeroPeriod() throws Exception {
        Scheduler s = getScheduler();
        if (s instanceof TrampolineScheduler) {
            // can't properly stop a trampolined periodic task
            return;
        }

        for (int initial = 0; initial < 2; initial++) {
            final CountDownLatch cdl = new CountDownLatch(1);

            final SequentialDisposable sd = new SequentialDisposable();

            try {
                sd.replace(s.schedulePeriodicallyDirect(new Runnable() {
                    int count;

                    @Override
                    public void run() {
                        if (++count == 10) {
                            sd.dispose();
                            cdl.countDown();
                        }
                    }
                }, initial, 0, TimeUnit.MILLISECONDS));

                assertTrue("" + initial, cdl.await(5, TimeUnit.SECONDS));
            } finally {
                sd.dispose();
            }
        }
    }

    @Test
    public void schedulePeriodicallyZeroPeriod() throws Exception {
        Scheduler s = getScheduler();
        if (s instanceof TrampolineScheduler) {
            // can't properly stop a trampolined periodic task
            return;
        }

        for (int initial = 0; initial < 2; initial++) {
            final CountDownLatch cdl = new CountDownLatch(1);

            final SequentialDisposable sd = new SequentialDisposable();

            Scheduler.Worker w = s.createWorker();

            try {
                sd.replace(w.schedulePeriodically(new Runnable() {
                    int count;

                    @Override
                    public void run() {
                        if (++count == 10) {
                            sd.dispose();
                            cdl.countDown();
                        }
                    }
                }, initial, 0, TimeUnit.MILLISECONDS));

                assertTrue("" + initial, cdl.await(5, TimeUnit.SECONDS));
            } finally {
                sd.dispose();
                w.dispose();
            }
        }
    }

    private void assertRunnableDecorated(Runnable scheduleCall) throws InterruptedException {
        try {
            final CountDownLatch decoratedCalled = new CountDownLatch(1);

            RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
                @Override
                public Runnable apply(final Runnable actual) throws Exception {
                    return new Runnable() {
                        @Override
                        public void run() {
                            decoratedCalled.countDown();
                            actual.run();
                        }
                    };
                }
            });

            scheduleCall.run();

            assertTrue(decoratedCalled.await(5, TimeUnit.SECONDS));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void scheduleDirectDecoratesRunnable() throws InterruptedException {
        assertRunnableDecorated(new Runnable() {
            @Override
            public void run() {
                getScheduler().scheduleDirect(Functions.EMPTY_RUNNABLE);
            }
        });
    }

    @Test
    public void scheduleDirectWithDelayDecoratesRunnable() throws InterruptedException {
        assertRunnableDecorated(new Runnable() {
            @Override
            public void run() {
                getScheduler().scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.MILLISECONDS);
            }
        });
    }

    @Test
    public void schedulePeriodicallyDirectDecoratesRunnable() throws InterruptedException {
        final Scheduler scheduler = getScheduler();
        if (scheduler instanceof TrampolineScheduler) {
            // Can't properly stop a trampolined periodic task.
            return;
        }

        final AtomicReference<Disposable> disposable = new AtomicReference<>();

        try {
            assertRunnableDecorated(new Runnable() {
                @Override
                public void run() {
                    disposable.set(scheduler.schedulePeriodicallyDirect(Functions.EMPTY_RUNNABLE, 1, 10000, TimeUnit.MILLISECONDS));
                }
            });
        } finally {
            disposable.get().dispose();
        }
    }

    @Test
    public void unwrapDefaultPeriodicTask() throws InterruptedException {
        Scheduler s = getScheduler();
        if (s instanceof TrampolineScheduler) {
            // TrampolineScheduler always return EmptyDisposable
            return;
        }

        final CountDownLatch cdl = new CountDownLatch(1);
        Runnable countDownRunnable = new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        };
        Disposable disposable = s.schedulePeriodicallyDirect(countDownRunnable, 100, 100, TimeUnit.MILLISECONDS);
        SchedulerRunnableIntrospection wrapper = (SchedulerRunnableIntrospection) disposable;

        assertSame(countDownRunnable, wrapper.getWrappedRunnable());
        assertTrue(cdl.await(5, TimeUnit.SECONDS));
        disposable.dispose();
    }

    @Test
    public void unwrapScheduleDirectTask() {
        Scheduler scheduler = getScheduler();
        if (scheduler instanceof TrampolineScheduler) {
            // TrampolineScheduler always return EmptyDisposable
            return;
        }
        final CountDownLatch cdl = new CountDownLatch(1);
        Runnable countDownRunnable = new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        };
        Disposable disposable = scheduler.scheduleDirect(countDownRunnable, 100, TimeUnit.MILLISECONDS);
        SchedulerRunnableIntrospection wrapper = (SchedulerRunnableIntrospection) disposable;
        assertSame(countDownRunnable, wrapper.getWrappedRunnable());
        disposable.dispose();
    }

    @Test
    public void scheduleDirectNullRunnable() {
        try {
            getScheduler().scheduleDirect(null);
            fail();
        } catch (NullPointerException npe) {
            assertEquals("run is null", npe.getMessage());
        }
    }

    @Test
    public void scheduleDirectWithDelayNullRunnable() {
        try {
            getScheduler().scheduleDirect(null, 10, TimeUnit.MILLISECONDS);
            fail();
        } catch (NullPointerException npe) {
            assertEquals("run is null", npe.getMessage());
        }
    }

    @Test
    public void schedulePeriodicallyDirectNullRunnable() {
        try {
            getScheduler().schedulePeriodicallyDirect(null, 5, 10, TimeUnit.MILLISECONDS);
            fail();
        } catch (NullPointerException npe) {
            assertEquals("run is null", npe.getMessage());
        }
    }
}
