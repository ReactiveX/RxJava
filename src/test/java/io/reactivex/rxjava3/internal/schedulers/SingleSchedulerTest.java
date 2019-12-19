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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.schedulers.SingleScheduler.ScheduledWorker;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleSchedulerTest extends AbstractSchedulerTests {

    @Test
    public void shutdownRejects() {
        final int[] calls = { 0 };

        Runnable r = new Runnable() {
            @Override
            public void run() {
                calls[0]++;
            }
        };

        Scheduler s = new SingleScheduler();
        s.shutdown();

        assertEquals(Disposable.disposed(), s.scheduleDirect(r));

        assertEquals(Disposable.disposed(), s.scheduleDirect(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), s.schedulePeriodicallyDirect(r, 1, 1, TimeUnit.SECONDS));

        Worker w = s.createWorker();
        ((ScheduledWorker)w).executor.shutdownNow();

        assertEquals(Disposable.disposed(), w.schedule(r));

        assertEquals(Disposable.disposed(), w.schedule(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS));

        assertEquals(0, calls[0]);

        w.dispose();

        assertTrue(w.isDisposed());
    }

    @Test
    public void startRace() {
        final Scheduler s = new SingleScheduler();
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            s.shutdown();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    s.start();
                }
            };

            TestHelper.race(r1, r1);
        }
    }

    @Test
    public void runnableDisposedAsync() throws Exception {
        final Scheduler s = Schedulers.single();
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsyncCrash() throws Exception {
        final Scheduler s = Schedulers.single();

        Disposable d = s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                throw new IllegalStateException();
            }
        });

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void runnableDisposedAsyncTimed() throws Exception {
        final Scheduler s = Schedulers.single();

        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.MILLISECONDS);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Override protected Scheduler getScheduler() {
        return Schedulers.single();
    }

}
