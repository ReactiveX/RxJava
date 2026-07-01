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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.internal.schedulers.SharedScheduler.SharedWorker.SharedAction;
import io.reactivex.rxjava4.schedulers.*;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SharedSchedulerTest extends RxJavaTest implements Runnable {

    volatile int calls;

    @Override
    public void run() {
        calls++;
    }

    @Test @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    public void normal() {
        var scheduler = Schedulers.cached().share();
        try {
            final Set<String> threads = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                Flowable.just(1).subscribeOn(scheduler)
                .map((Function<Integer, Object>) _ -> threads.add(Thread.currentThread().getName()))
                .blockingLast();
            }

            assertEquals(1, threads.size());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    public void delay() {
        var scheduler = Schedulers.cached().share();
        try {
            final Set<String> threads = new HashSet<>();

            for (int i = 0; i < 100; i++) {
                Flowable.just(1).delay(1, TimeUnit.MILLISECONDS, scheduler)
                .map((Function<Integer, Object>) _ -> threads.add(Thread.currentThread().getName()))
                .blockingLast();
            }

            assertEquals(1, threads.size());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void now() {
        TestScheduler test = new TestScheduler();

        var scheduler = test.share();

        assertEquals(0L, scheduler.now(TimeUnit.MILLISECONDS));

        assertEquals(0L, scheduler.createWorker().now(TimeUnit.MILLISECONDS));
    }

    @Test
    public void direct() {
        TestScheduler test = new TestScheduler();

        var scheduler = test.share();

        scheduler.scheduleDirect(this);

        test.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        assertEquals(1, calls);

        scheduler.scheduleDirect(this, 1, TimeUnit.MILLISECONDS);

        test.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        assertEquals(2, calls);

        scheduler.schedulePeriodicallyDirect(this, 1, 1, TimeUnit.MILLISECONDS);

        test.advanceTimeBy(10, TimeUnit.MILLISECONDS);

        assertEquals(12, calls);

        Worker worker = scheduler.createWorker();
        worker.dispose();

        assertSame(Disposable.disposed(), worker.schedule(this));

        assertSame(Disposable.disposed(), worker.schedule(this, 1, TimeUnit.MILLISECONDS));
    }

    @Test
    public void taskCrash() {
        TestScheduler test = new TestScheduler();

        var scheduler = test.share();

        Disposable d = scheduler.createWorker().schedule(() -> {
            throw new IllegalArgumentException();
        });

        assertFalse(d.isDisposed());

        try {
            test.triggerActions();
        } catch (IllegalArgumentException ex) {
            // expected
        }

        assertTrue(d.isDisposed());
    }

    @Test @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    public void futureDisposeRace() throws Exception {
        var scheduler = Schedulers.computation().share();
        try {
            Worker w = scheduler.createWorker();
            for (int i = 0; i < 1000; i++) {
                w.schedule(this);
            }

            while (calls != 1000) {
                Thread.sleep(100);
            }
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void disposeSetFutureRace() {
        for (int i = 0; i < 1000; i++) {
            var sa = new SharedAction(this, new CompositeDisposable());
            final Disposable d = Disposable.empty();

            Runnable r1 = () -> sa.setFuture(d);

            Runnable r2 = sa::dispose;

            TestHelper.race(r1, r2, Schedulers.single());

            assertTrue(d.isDisposed(), "Future not disposed");
        }
    }

    @Test
    public void runSetFutureRace() {
        for (int i = 0; i < 1000; i++) {
            var sa = new SharedAction(this, new CompositeDisposable());
            final Disposable d = Disposable.empty();

            Runnable r1 = () -> sa.setFuture(d);

            Runnable r2 = sa::run;

            TestHelper.race(r1, r2, Schedulers.single());

            assertFalse(d.isDisposed(), "Future disposed");
            assertEquals(i + 1, calls);
        }
    }

    @Test
    public void introspection() {
        Runnable run = () -> { };
        var scheduler = Schedulers.computation().share();
        try {
            var worker = scheduler.createWorker();
            try {
                var task = worker.schedule(run);

                if (task instanceof SchedulerRunnableIntrospection intro) {
                    assertSame(run, intro.getWrappedRunnable());
                } else {
                    fail(task.getClass() + " doesn't implement SchedulerRunnableIntrospection");
                }
            } finally {
                worker.dispose();
            }
        } finally {
            scheduler.shutdown();
        }
    }
}
