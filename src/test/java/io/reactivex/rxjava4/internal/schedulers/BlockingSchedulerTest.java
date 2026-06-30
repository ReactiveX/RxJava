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

package io.reactivex.rxjava4.internal.schedulers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class BlockingSchedulerTest {

    TestSubscriber<Integer> ts = new TestSubscriber<>();

    @Test
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void workerUntimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                Flowable.range(1, 5)
                .subscribeOn(scheduler)
                .doAfterTerminate(scheduler::shutdown)
                .subscribe(ts);

                ts.assertEmpty();
            });

            ts.assertResult(1, 2, 3, 4, 5);

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void workerUntimedVia() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final var scheduler = Schedulers.createBlocking();
            scheduler.execute(() -> {
                Flowable.range(1, 5)
                .subscribeOn(scheduler.scheduler())
                .doAfterTerminate(scheduler::shutdown)
                .subscribe(ts);

                ts.assertEmpty();
            });

            ts.assertResult(1, 2, 3, 4, 5);

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void workerTimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                Flowable.range(1, 5)
                .subscribeOn(scheduler)
                .delay(100, TimeUnit.MILLISECONDS, scheduler)
                .doAfterTerminate(scheduler::shutdown)
                .subscribe(ts);

                ts.assertEmpty();
            });

            ts.assertResult(1, 2, 3, 4, 5);
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void directCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> scheduler.scheduleDirect(() -> {
                scheduler.shutdown();
                throw new IllegalArgumentException();
            }));

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void workerCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                final Worker worker = scheduler.createWorker();
                worker.schedule(() -> {
                    worker.dispose();
                    scheduler.shutdown();
                    throw new IllegalArgumentException();
                });
            });

            TestHelper.assertError(errors, 0, IllegalArgumentException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void directUntimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                ts.onSubscribe(new BooleanSubscription());

                scheduler.scheduleDirect(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();

                    scheduler.shutdown();
                });

                ts.assertEmpty();
            });

            ts.assertResult(1, 2, 3, 4, 5);
            for (Throwable t : errors) {
                t.printStackTrace();
            }
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void directTimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                ts.onSubscribe(new BooleanSubscription());

                scheduler.scheduleDirect(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();

                    scheduler.shutdown();
                }, 100, TimeUnit.MILLISECONDS);

                ts.assertEmpty();
            });

            ts.assertResult(1, 2, 3, 4, 5);
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void cancelDirect() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                ts.onSubscribe(new BooleanSubscription());

                Disposable d = scheduler.scheduleDirect(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();
                }, 100, TimeUnit.MILLISECONDS);

                assertFalse(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());

                scheduler.scheduleDirect(scheduler::shutdown);

                ts.assertEmpty();
            });

            ts.assertEmpty();
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void cancelDirectUntimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {
                ts.onSubscribe(new BooleanSubscription());

                Disposable d = scheduler.scheduleDirect(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();
                });

                assertFalse(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());

                scheduler.scheduleDirect(scheduler::shutdown);

                ts.assertEmpty();
            });

            ts.assertEmpty();
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void cancelWorker() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {

                ts.onSubscribe(new BooleanSubscription());

                final Worker w = scheduler.createWorker();

                Disposable d = w.schedule(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();
                }, 100, TimeUnit.MILLISECONDS);

                assertFalse(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());

                w.schedule(() -> {
                    w.dispose();
                    scheduler.shutdown();
                });

                ts.assertEmpty();
            });

            ts.assertEmpty();
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void cancelWorkerUntimed() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();
            scheduler.execute(() -> {

                ts.onSubscribe(new BooleanSubscription());

                final Worker w = scheduler.createWorker();

                Disposable d = w.schedule(() -> {
                    ts.onNext(1);
                    ts.onNext(2);
                    ts.onNext(3);
                    ts.onNext(4);
                    ts.onNext(5);
                    ts.onComplete();
                });

                assertFalse(d.isDisposed());
                d.dispose();
                assertTrue(d.isDisposed());

                w.schedule(() -> {
                    w.dispose();
                    scheduler.shutdown();

                    assertTrue(w.isDisposed());
                });

                ts.assertEmpty();
            });

            ts.assertEmpty();
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void asyncShutdown() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            Schedulers.single().scheduleDirect(() -> {
                scheduler.scheduleDirect(Functions.EMPTY_RUNNABLE);
                scheduler.shutdown();
                scheduler.shutdown();
                assertTrue(scheduler.scheduleDirect(Functions.EMPTY_RUNNABLE).isDisposed());
            }, 500, TimeUnit.MILLISECONDS);

            scheduler.execute(() -> { });

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void asyncInterrupt() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            Schedulers.single().scheduleDirect(() -> {
                scheduler.shutdown.set(true);
                scheduler.thread.interrupt();
            }, 500, TimeUnit.MILLISECONDS);

            scheduler.execute(() -> { });

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void asyncDispose() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            scheduler.execute(() -> {

                final Disposable d = scheduler.scheduleDirect(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        // ignored
                        Thread.currentThread().interrupt();
                    }
                    scheduler.shutdown();
                });

                Schedulers.single().scheduleDirect(() -> {
                    d.dispose();
                    d.dispose();
                }, 500, TimeUnit.MILLISECONDS);
            });

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void asyncFeedInto() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            final int n = 10000;

            final int[] counter = { 0 };

            scheduler.execute(() -> Schedulers.single().scheduleDirect(() -> {
                for (int i = 0; i < n; i++) {
                    scheduler.scheduleDirect(() -> counter[0]++);
                }
                scheduler.scheduleDirect(scheduler::shutdown);
            }));

            assertEquals(n, counter[0]);
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void asyncFeedInto2() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            final int n = 1000;

            final int[] counter = { 0 };

            scheduler.execute(() -> {
                for (int i = 0; i < n; i++) {
                    scheduler.scheduleDirect(() -> counter[0]++, i, TimeUnit.MILLISECONDS);
                }
                scheduler.scheduleDirect(scheduler::shutdown, n + 10, TimeUnit.MILLISECONDS);
            });

            assertEquals(n, counter[0]);
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    public void backtoSameThread() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final BlockingCurrentThreadScheduler scheduler = new BlockingCurrentThreadScheduler();

            final Thread t0 = Thread.currentThread();
            final Thread[] t1 = { null };

            scheduler.execute(() -> Flowable.just(1)
            .subscribeOn(Schedulers.cached())
            .observeOn(scheduler)
            .doAfterTerminate(scheduler::shutdown)
            .subscribe(_ -> t1[0] = Thread.currentThread()));

            assertSame(t0, t1[0]);

            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
