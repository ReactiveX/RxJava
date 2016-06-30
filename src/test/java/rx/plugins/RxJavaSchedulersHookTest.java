/**
 * Copyright 2016 Netflix, Inc.
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
package rx.plugins;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.internal.schedulers.SchedulerLifecycle;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class RxJavaSchedulersHookTest {
    @Test
    public void schedulerFactoriesDisallowNull() {
        try {
            RxJavaSchedulersHook.createComputationScheduler(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("threadFactory == null", e.getMessage());
        }
        try {
            RxJavaSchedulersHook.createIoScheduler(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("threadFactory == null", e.getMessage());
        }
        try {
            RxJavaSchedulersHook.createNewThreadScheduler(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("threadFactory == null", e.getMessage());
        }
    }

    @Test public void computationSchedulerUsesSuppliedThreadFactory() throws InterruptedException {
        final AtomicReference<Thread> threadRef = new AtomicReference<Thread>();
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                threadRef.set(thread);
                return thread;
            }
        };

        Scheduler scheduler = RxJavaSchedulersHook.createComputationScheduler(threadFactory);
        Worker worker = scheduler.createWorker();

        final CountDownLatch latch = new CountDownLatch(1);
        worker.schedule(new Action0() {
            @Override
            public void call() {
                assertSame(threadRef.get(), Thread.currentThread());
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        if (scheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) scheduler).shutdown();
        }
    }

    @Test public void ioSchedulerUsesSuppliedThreadFactory() throws InterruptedException {
        final AtomicReference<Thread> threadRef = new AtomicReference<Thread>();
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                threadRef.set(thread);
                return thread;
            }
        };

        Scheduler scheduler = RxJavaSchedulersHook.createIoScheduler(threadFactory);
        Worker worker = scheduler.createWorker();

        final CountDownLatch latch = new CountDownLatch(1);
        worker.schedule(new Action0() {
            @Override public void call() {
                assertSame(threadRef.get(), Thread.currentThread());
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, SECONDS));

        if (scheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) scheduler).shutdown();
        }
    }

    @Test public void newThreadSchedulerUsesSuppliedThreadFactory() throws InterruptedException {
        final AtomicReference<Thread> threadRef = new AtomicReference<Thread>();
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
              Thread thread = new Thread(r);
              threadRef.set(thread);
              return thread;
            }
        };

        Scheduler scheduler = RxJavaSchedulersHook.createNewThreadScheduler(threadFactory);
        Worker worker = scheduler.createWorker();

        final CountDownLatch latch = new CountDownLatch(1);
        worker.schedule(new Action0() {
            @Override public void call() {
              assertSame(threadRef.get(), Thread.currentThread());
              latch.countDown();
          }
          });
        assertTrue(latch.await(10, SECONDS));

        if (scheduler instanceof SchedulerLifecycle) {
            ((SchedulerLifecycle) scheduler).shutdown();
        }
    }
}
