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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.schedulers.IoScheduler;

public class CachedThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.io();
    }

    /**
     * IO scheduler defaults to using CachedThreadScheduler.
     */
    @Test
    public final void iOScheduler() {

        Flowable<Integer> f1 = Flowable.just(1, 2, 3, 4, 5);
        Flowable<Integer> f2 = Flowable.just(6, 7, 8, 9, 10);
        Flowable<String> f = Flowable.merge(f1, f2).map(new Function<Integer, String>() {

            @Override
            public String apply(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxCachedThreadScheduler"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        f.subscribeOn(Schedulers.io()).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public final void handledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.handledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public void cancelledTaskRetention() throws InterruptedException {
        Worker w = Schedulers.io().createWorker();
        try {
            ExecutorSchedulerTest.cancelledRetention(w, false);
        } finally {
            w.dispose();
        }
        w = Schedulers.io().createWorker();
        try {
            ExecutorSchedulerTest.cancelledRetention(w, true);
        } finally {
            w.dispose();
        }
    }

    @Test
    public void workerDisposed() {
        Worker w = Schedulers.io().createWorker();

        assertFalse(((Disposable)w).isDisposed());

        w.dispose();

        assertTrue(((Disposable)w).isDisposed());
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

        IoScheduler s = new IoScheduler();
        s.shutdown();
        s.shutdown();

        s.scheduleDirect(r);

        s.scheduleDirect(r, 1, TimeUnit.SECONDS);

        s.schedulePeriodicallyDirect(r, 1, 1, TimeUnit.SECONDS);

        Worker w = s.createWorker();
        w.dispose();

        assertEquals(Disposable.disposed(), w.schedule(r));

        assertEquals(Disposable.disposed(), w.schedule(r, 1, TimeUnit.SECONDS));

        assertEquals(Disposable.disposed(), w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS));

        assertEquals(0, calls[0]);
    }
}
