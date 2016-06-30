/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.schedulers;

import rx.CapturingUncaughtExceptionHandler;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.internal.schedulers.NewThreadWorker;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class SchedulerTests {
    private SchedulerTests() {
        // No instances.
    }

    /**
     * Verifies that the given Scheduler delivers unhandled errors to its executing thread's
     * {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * Schedulers which execute on a separate thread from their calling thread should exhibit this behavior. Schedulers
     * which execute on their calling thread may not.
     * 
     * @param scheduler the scheduler to test
     * @throws InterruptedException if some wait is interrupted
     */
    public static void testUnhandledErrorIsDeliveredToThreadHandler(Scheduler scheduler) throws InterruptedException {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(handler);
            IllegalStateException error = new IllegalStateException("Should be delivered to handler");
            Observable.error(error)
                    .subscribeOn(scheduler)
                    .subscribe();

            if (!handler.completed.await(3, TimeUnit.SECONDS)) {
                fail("timed out");
            }

            assertEquals("Should have received exactly 1 exception", 1, handler.count);
            Throwable cause = handler.caught;
            while (cause != null) {
                if (error.equals(cause)) break;
                if (cause == cause.getCause()) break;
                cause = cause.getCause();
            }
            assertEquals("Our error should have been delivered to the handler", error, cause);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    /**
     * Verifies that the given Scheduler does not deliver handled errors to its executing Thread's
     * {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * This is a companion test to {@link #testUnhandledErrorIsDeliveredToThreadHandler}, and is needed only for the
     * same Schedulers.
     * @param scheduler the scheduler to test
     * @throws InterruptedException if some wait is interrupted
     */
    public static void testHandledErrorIsNotDeliveredToThreadHandler(Scheduler scheduler) throws InterruptedException {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
            CapturingObserver<Object> observer = new CapturingObserver<Object>();
            Thread.setDefaultUncaughtExceptionHandler(handler);
            IllegalStateException error = new IllegalStateException("Should be delivered to handler");
            Observable.error(error)
                    .subscribeOn(scheduler)
                    .subscribe(observer);

            if (!observer.completed.await(3, TimeUnit.SECONDS)) {
                fail("timed out");
            }

            assertEquals("Handler should not have received anything", 0, handler.count);
            assertEquals("Observer should have received an error", 1, observer.errorCount);
            assertEquals("Observer should not have received a next value", 0, observer.nextCount);

            Throwable cause = observer.error;
            while (cause != null) {
                if (error.equals(cause)) break;
                if (cause == cause.getCause()) break;
                cause = cause.getCause();
            }
            assertEquals("Our error should have been delivered to the observer", error, cause);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    public static void testCancelledRetention(Scheduler.Worker w, boolean periodic) throws InterruptedException {
        System.out.println("Wait before GC");
        Thread.sleep(1000);

        System.out.println("GC");
        System.gc();

        Thread.sleep(1000);


        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        long initial = memHeap.getUsed();

        System.out.printf("Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        int n = 500 * 1000;
        if (periodic) {
            final CountDownLatch cdl = new CountDownLatch(n);
            final Action0 action = new Action0() {
                @Override
                public void call() {
                    cdl.countDown();
                }
            };
            for (int i = 0; i < n; i++) {
                if (i % 50000 == 0) {
                    System.out.println("  -> still scheduling: " + i);
                }
                w.schedulePeriodically(action, 0, 1, TimeUnit.DAYS);
            }

            System.out.println("Waiting for the first round to finish...");
            cdl.await();
        } else {
            for (int i = 0; i < n; i++) {
                if (i % 50000 == 0) {
                    System.out.println("  -> still scheduling: " + i);
                }
                w.schedule(Actions.empty(), 1, TimeUnit.DAYS);
            }
        }

        memHeap = memoryMXBean.getHeapMemoryUsage();
        long after = memHeap.getUsed();
        System.out.printf("Peak: %.3f MB%n", after / 1024.0 / 1024.0);

        w.unsubscribe();

        System.out.println("Wait before second GC");
        Thread.sleep(NewThreadWorker.PURGE_FREQUENCY + 2000);

        System.out.println("Second GC");
        System.gc();

        Thread.sleep(1000);

        memHeap = memoryMXBean.getHeapMemoryUsage();
        long finish = memHeap.getUsed();
        System.out.printf("After: %.3f MB%n", finish / 1024.0 / 1024.0);

        if (finish > initial * 5) {
            fail(String.format("Tasks retained: %.3f -> %.3f -> %.3f", initial / 1024 / 1024.0, after / 1024 / 1024.0, finish / 1024 / 1024d));
        }
    }

    private static final class CapturingObserver<T> implements Observer<T> {
        CountDownLatch completed = new CountDownLatch(1);
        int errorCount = 0;
        int nextCount = 0;
        Throwable error;

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            errorCount++;
            error = e;
            completed.countDown();
        }

        @Override
        public void onNext(T t) {
            nextCount++;
        }
    }
}
