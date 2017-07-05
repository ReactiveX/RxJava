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

package io.reactivex.internal.schedulers;

import static org.junit.Assert.*;

import io.reactivex.schedulers.AbstractSchedulerTests;
import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.schedulers.SingleScheduler.ScheduledWorker;
import io.reactivex.schedulers.Schedulers;

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

        assertEquals(Disposables.disposed(), s.scheduleDirect(r));

        assertEquals(Disposables.disposed(), s.scheduleDirect(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposables.disposed(), s.schedulePeriodicallyDirect(r, 1, 1, TimeUnit.SECONDS));

        Worker w = s.createWorker();
        ((ScheduledWorker)w).executor.shutdownNow();

        assertEquals(Disposables.disposed(), w.schedule(r));

        assertEquals(Disposables.disposed(), w.schedule(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposables.disposed(), w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS));

        assertEquals(0, calls[0]);

        w.dispose();

        assertTrue(w.isDisposed());
    }

    @Test
    public void startRace() {
        final Scheduler s = new SingleScheduler();
        for (int i = 0; i < 500; i++) {
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

    @Test(timeout = 1000)
    public void runnableDisposedAsync() throws Exception {
        final Scheduler s = Schedulers.single();
        Disposable d = s.scheduleDirect(Functions.EMPTY_RUNNABLE);

        while (!d.isDisposed()) {
            Thread.sleep(1);
        }
    }

    @Test(timeout = 1000)
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

    @Test(timeout = 1000)
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
