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
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.core.config.ParallelSchedulerConfig;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class SchedulerMultiWorkerSupportParallelTest extends RxJavaTest {

    final int max = 2;

    Scheduler scheduler;

    @BeforeEach
    public void before() {
        scheduler = Schedulers.createParallel(new ParallelSchedulerConfig(2, true));
    }

    @AfterEach
    public void after() {
        scheduler.shutdown();
    }

    @Test
    public void moreThanMaxWorkers() {
        final List<Worker> list = new ArrayList<>();

        SchedulerMultiWorkerSupport mws = (SchedulerMultiWorkerSupport)scheduler;

        mws.createWorkers(max * 2, (_, w) -> list.add(w));

        assertEquals(max * 2, list.size());
    }

    @Test
    public void getShutdownWorkers() {
        final List<Worker> list = new ArrayList<>();

        scheduler.shutdown();
        SchedulerMultiWorkerSupport mws = (SchedulerMultiWorkerSupport)scheduler;

        mws.createWorkers(max * 2, (_, w) -> list.add(w));

        assertEquals(max * 2, list.size());

        for (Worker w : list) {
            assertEquals(ParallelScheduler.SHUTDOWN_TRACKING_WORKER, w, w.getClass().toString());
        }
    }

    @Test
    public void distinctThreads() throws Exception {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final CompositeDisposable composite = new CompositeDisposable();

            try {
                final CountDownLatch cdl = new CountDownLatch(max * 2);

                final Set<String> threads1 = Collections.synchronizedSet(new HashSet<>());

                final Set<String> threads2 = Collections.synchronizedSet(new HashSet<>());

                Runnable parallel1 = () -> {
                    final List<Worker> list1 = new ArrayList<>();

                    SchedulerMultiWorkerSupport mws = (SchedulerMultiWorkerSupport)scheduler;

                    mws.createWorkers(max, (_, w) -> {
                        list1.add(w);
                        composite.add(w);
                    });

                    Runnable run = () -> {
                        threads1.add(Thread.currentThread().getName());
                        cdl.countDown();
                    };

                    for (Worker w : list1) {
                        w.schedule(run);
                    }
                };

                Runnable parallel2 = () -> {
                    final List<Worker> list2 = new ArrayList<>();

                    SchedulerMultiWorkerSupport mws = (SchedulerMultiWorkerSupport)scheduler;

                    mws.createWorkers(max, (_, w) -> {
                        list2.add(w);
                        composite.add(w);
                    });

                    Runnable run = () -> {
                        threads2.add(Thread.currentThread().getName());
                        cdl.countDown();
                    };

                    for (Worker w : list2) {
                        w.schedule(run);
                    }
                };

                TestHelper.race(parallel1, parallel2);

                assertTrue(cdl.await(5, TimeUnit.SECONDS));

                assertEquals(max, threads1.size(), threads1.toString());
                assertEquals(max, threads2.size(), threads2.toString());
            } finally {
                composite.dispose();
            }
        }
    }
}
