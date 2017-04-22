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

package io.reactivex.schedulers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.NonNull;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;

public class SchedulerTest {

    @Test
    public void defaultPeriodicTask() {
        final int[] count = { 0 };

        TestScheduler scheduler = new TestScheduler();

        Disposable d = scheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                count[0]++;
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        assertEquals(0, count[0]);
        assertFalse(d.isDisposed());

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, count[0]);

        d.dispose();

        assertTrue(d.isDisposed());

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, count[0]);
    }

    @Test(expected = TestException.class)
    public void periodicDirectThrows() {
        TestScheduler scheduler = new TestScheduler();

        scheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                throw new TestException();
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void disposePeriodicDirect() {
        final int[] count = { 0 };

        TestScheduler scheduler = new TestScheduler();

        Disposable d = scheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                count[0]++;
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        d.dispose();

        assertEquals(0, count[0]);
        assertTrue(d.isDisposed());

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(0, count[0]);
        assertTrue(d.isDisposed());
    }

    @Test
    public void scheduleDirect() {
        final int[] count = { 0 };

        TestScheduler scheduler = new TestScheduler();

        scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                count[0]++;
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(0, count[0]);

        scheduler.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(1, count[0]);
    }

    @Test
    public void disposeSelfPeriodicDirect() {
        final int[] count = { 0 };

        TestScheduler scheduler = new TestScheduler();

        final SequentialDisposable sd = new SequentialDisposable();

        Disposable d = scheduler.schedulePeriodicallyDirect(new Runnable() {
            @Override
            public void run() {
                count[0]++;
                sd.dispose();
            }
        }, 100, 100, TimeUnit.MILLISECONDS);

        sd.set(d);

        assertEquals(0, count[0]);
        assertFalse(d.isDisposed());

        scheduler.advanceTimeBy(400, TimeUnit.MILLISECONDS);

        assertEquals(1, count[0]);
        assertTrue(d.isDisposed());
    }

    @Test
    public void disposeSelfPeriodic() {
        final int[] count = { 0 };

        TestScheduler scheduler = new TestScheduler();

        Worker worker = scheduler.createWorker();

        try {
            final SequentialDisposable sd = new SequentialDisposable();

            Disposable d = worker.schedulePeriodically(new Runnable() {
                @Override
                public void run() {
                    count[0]++;
                    sd.dispose();
                }
            }, 100, 100, TimeUnit.MILLISECONDS);

            sd.set(d);

            assertEquals(0, count[0]);
            assertFalse(d.isDisposed());

            scheduler.advanceTimeBy(400, TimeUnit.MILLISECONDS);

            assertEquals(1, count[0]);
            assertTrue(d.isDisposed());
        } finally {
            worker.dispose();
        }
    }

    @Test
    public void periodicDirectTaskRace() {
        final TestScheduler scheduler = new TestScheduler();

        for (int i = 0; i < 100; i++) {
            final Disposable d = scheduler.schedulePeriodicallyDirect(Functions.EMPTY_RUNNABLE, 1, 1, TimeUnit.MILLISECONDS);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    d.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
                }
            };

            TestHelper.race(r1, r2, Schedulers.io());
        }

    }


    @Test
    public void periodicDirectTaskRaceIO() throws Exception {
        final Scheduler scheduler = Schedulers.io();

        for (int i = 0; i < 100; i++) {
            final Disposable d = scheduler.schedulePeriodicallyDirect(
                    Functions.EMPTY_RUNNABLE, 0, 0, TimeUnit.MILLISECONDS);

            Thread.sleep(1);

            d.dispose();
        }

    }

    @Test
    public void scheduleDirectThrows() throws Exception {
        List<Throwable> list = TestHelper.trackPluginErrors();
        try {
            Schedulers.io().scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            });

            Thread.sleep(250);

            assertEquals(1, list.size());
            TestHelper.assertUndeliverable(list, 0, TestException.class, null);

        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void schedulersUtility() {
        TestHelper.checkUtilityClass(Schedulers.class);
    }

    @Test
    public void defaultSchedulePeriodicallyDirectRejects() {
        Scheduler s = new Scheduler() {
            @NonNull
            @Override
            public Worker createWorker() {
                return new Worker() {
                    @NonNull
                    @Override
                    public Disposable schedule(@NonNull Runnable run, long delay, @NonNull TimeUnit unit) {
                        return EmptyDisposable.INSTANCE;
                    }

                    @Override
                    public void dispose() {

                    }

                    @Override
                    public boolean isDisposed() {
                        return false;
                    }
                };
            }
        };

        assertSame(EmptyDisposable.INSTANCE, s.schedulePeriodicallyDirect(Functions.EMPTY_RUNNABLE, 1, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void holders() {
        assertNotNull(new Schedulers.ComputationHolder());

        assertNotNull(new Schedulers.IoHolder());

        assertNotNull(new Schedulers.NewThreadHolder());

        assertNotNull(new Schedulers.SingleHolder());
    }

    static final class CustomScheduler extends Scheduler {

        @Override
        public Worker createWorker() {
            return Schedulers.single().createWorker();
        }

    }

    @Test
    public void customScheduleDirectDisposed() {
        CustomScheduler scheduler = new CustomScheduler();

        Disposable d = scheduler.scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.MINUTES);

        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());
    }
}
