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

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.schedulers.Schedulers;

@Isolated
public class SharedSchedulerIsolatedTest extends RxJavaTest {

    long memoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    @Test
    public void noleak() throws Exception {
        var scheduler = Schedulers.cached().share();
        try {
            Worker worker = scheduler.createWorker();

            worker.schedule(Functions.EMPTY_RUNNABLE);

            System.gc();
            Thread.sleep(500);

            long before = memoryUsage();
            System.out.printf("Start: %.1f%n", before / 1024.0 / 1024.0);

            for (int i = 0; i < 300 * 1000; i++) {
                worker.schedule(Functions.EMPTY_RUNNABLE, 1, TimeUnit.DAYS);
            }

            long middle = memoryUsage();

            System.out.printf("Middle: %.1f -> %.1f%n", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0);

            worker.dispose();

            System.gc();

            Thread.sleep(100);

            int wait = 400;

            long after = memoryUsage();

            while (wait-- > 0) {
                System.out.printf("Usage: %.1f -> %.1f -> %.1f%n", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0, after / 1024.0 / 1024.0);

                if (middle > after * 2) {
                    return;
                }

                Thread.sleep(100);

                System.gc();

                Thread.sleep(100);

                after = memoryUsage();
            }

            fail(String.format("Looks like there is a memory leak: %.1f -> %.1f -> %.1f", before / 1024.0 / 1024.0, middle / 1024.0 / 1024.0, after / 1024.0 / 1024.0));

        } finally {
            scheduler.shutdown();
        }
    }
}
