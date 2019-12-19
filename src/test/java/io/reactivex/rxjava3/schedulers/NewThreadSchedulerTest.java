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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.schedulers.NewThreadWorker;

public class NewThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.newThread();
    }

    @Test
    public final void handledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.handledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public void shutdownRejects() {
        final int[] calls = { 0 };

        Runnable r = new Runnable() {
            @Override
            public void run() {
                calls[0]++;
            }
        };

        Scheduler s = getScheduler();
        Worker w = s.createWorker();
        w.dispose();

        assertTrue(w.isDisposed());

        assertEquals(Disposable.disposed(), w.schedule(r));

        assertEquals(Disposable.disposed(), w.schedule(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS));

        NewThreadWorker actual = (NewThreadWorker)w;

        CompositeDisposable cd = new CompositeDisposable();

        actual.scheduleActual(r, 1, TimeUnit.SECONDS, cd);

        assertEquals(0, cd.size());

        assertEquals(0, calls[0]);
    }

    /**
     * Regression test to ensure there is no NPE when the worker has been disposed.
     * @throws Exception on error
     */
    @Test
    public void npeRegression() throws Exception {
        Scheduler s = getScheduler();
        NewThreadWorker w = (NewThreadWorker) s.createWorker();
        w.dispose();

        //This method used to throw a NPE when the worker has been disposed and the parent is null
        w.scheduleActual(new Runnable() {
            @Override
            public void run() {
            }
        }, 0, TimeUnit.MILLISECONDS, null);

    }
}
