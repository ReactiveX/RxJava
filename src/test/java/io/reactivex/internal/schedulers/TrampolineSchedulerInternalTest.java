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

package io.reactivex.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Scheduler.Worker;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;

public class TrampolineSchedulerInternalTest {

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

    @Test(timeout = 5000)
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
}
