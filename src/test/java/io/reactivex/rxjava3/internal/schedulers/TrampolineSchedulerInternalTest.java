/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.TrampolineScheduler.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TrampolineSchedulerInternalTest extends RxJavaTest {

    @Test
    public void scheduleDirectInterrupt() {
        Thread.currentThread().interrupt();

        final int[] calls = { 0 };

        assertSame(EmptyDisposable.INSTANCE, Schedulers.trampoline().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                calls[0]++;
            }
        }, 1, TimeUnit.SECONDS));

        assertTrue(Thread.interrupted());

        assertEquals(0, calls[0]);
    }

    @Test
    public void dispose() {
        Worker w = Schedulers.trampoline().createWorker();

        assertFalse(w.isDisposed());

        w.dispose();

        assertTrue(w.isDisposed());

        assertEquals(EmptyDisposable.INSTANCE, w.schedule(Functions.EMPTY_RUNNABLE));
    }

    @Test
    public void reentrantScheduleDispose() {
        final Worker w = Schedulers.trampoline().createWorker();
        try {
            final int[] calls = { 0, 0 };
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                    w.schedule(new Runnable() {
                        @Override
                        public void run() {
                            calls[1]++;
                        }
                    })
                    .dispose();
                }
            });

            assertEquals(1, calls[0]);
            assertEquals(0, calls[1]);
        } finally {
            w.dispose();
        }
    }

    @Test
    public void reentrantScheduleShutdown() {
        final Worker w = Schedulers.trampoline().createWorker();
        try {
            final int[] calls = { 0, 0 };
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                    w.schedule(new Runnable() {
                        @Override
                        public void run() {
                            calls[1]++;
                        }
                    }, 1, TimeUnit.MILLISECONDS);

                    w.dispose();
                }
            });

            assertEquals(1, calls[0]);
            assertEquals(0, calls[1]);
        } finally {
            w.dispose();
        }
    }

    @Test
    public void reentrantScheduleShutdown2() {
        final Worker w = Schedulers.trampoline().createWorker();
        try {
            final int[] calls = { 0, 0 };
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                    w.dispose();

                    assertSame(EmptyDisposable.INSTANCE, w.schedule(new Runnable() {
                        @Override
                        public void run() {
                            calls[1]++;
                        }
                    }, 1, TimeUnit.MILLISECONDS));
                }
            });

            assertEquals(1, calls[0]);
            assertEquals(0, calls[1]);
        } finally {
            w.dispose();
        }
    }

    @Test
    public void reentrantScheduleInterrupt() {
        final Worker w = Schedulers.trampoline().createWorker();
        try {
            final int[] calls = { 0 };
            Thread.currentThread().interrupt();
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    calls[0]++;
                }
            }, 1, TimeUnit.DAYS);

            assertTrue(Thread.interrupted());

            assertEquals(0, calls[0]);
        } finally {
            w.dispose();
        }
    }

    @Test
    public void sleepingRunnableDisposedOnRun() {
        TrampolineWorker w = new TrampolineWorker();

        Runnable r = mock(Runnable.class);

        SleepingRunnable run = new SleepingRunnable(r, w, 0);
        w.dispose();
        run.run();

        verify(r, never()).run();
    }

    @Test
    public void sleepingRunnableNoDelayRun() {
        TrampolineWorker w = new TrampolineWorker();

        Runnable r = mock(Runnable.class);

        SleepingRunnable run = new SleepingRunnable(r, w, 0);

        run.run();

        verify(r).run();
    }

    @Test
    public void sleepingRunnableDisposedOnDelayedRun() {
        final TrampolineWorker w = new TrampolineWorker();

        Runnable r = mock(Runnable.class);

        SleepingRunnable run = new SleepingRunnable(r, w, System.currentTimeMillis() + 200);

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                w.dispose();
            }
        }, 100, TimeUnit.MILLISECONDS);

        run.run();

        verify(r, never()).run();
    }
}
