/**
 * Copyright 2014 Netflix, Inc.
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

import static org.junit.Assert.*;

import java.lang.management.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.*;
import rx.Scheduler.Worker;
import rx.functions.*;
import rx.internal.schedulers.NewThreadWorker;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.ExecutorScheduler.ExecutorSchedulerWorker;

public class ExecutorSchedulerTest extends AbstractSchedulerConcurrencyTests {

    final static Executor executor = Executors.newFixedThreadPool(2, new RxThreadFactory("TestCustomPool-"));
    
    @Override
    protected Scheduler getScheduler() {
        return Schedulers.from(executor);
    }

    @Test
    public final void testUnhandledErrorIsDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testUnhandledErrorIsDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public final void testHandledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testHandledErrorIsNotDeliveredToThreadHandler(getScheduler());
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
    
    @Test(timeout = 30000)
    public void testCancelledTaskRetention() throws InterruptedException {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Scheduler s = Schedulers.from(exec);
        try {
            Scheduler.Worker w = s.createWorker();
            try {
                testCancelledRetention(w, false);
            } finally {
                w.unsubscribe();
            }
            
            w = s.createWorker();
            try {
                testCancelledRetention(w, true);
            } finally {
                w.unsubscribe();
            }
        } finally {
            exec.shutdownNow();
        }
    }
    
    /** A simple executor which queues tasks and executes them one-by-one if executeOne() is called. */
    static final class TestExecutor implements Executor {
        final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();
        @Override
        public void execute(Runnable command) {
            queue.offer(command);
        }
        public void executeOne() {
            Runnable r = queue.poll();
            if (r != null) {
                r.run();
            }
        }
        public void executeAll() {
            Runnable r;
            while ((r = queue.poll()) != null) {
                r.run();
            }
        }
    }
    
    @Test
    public void testCancelledTasksDontRun() {
        final AtomicInteger calls = new AtomicInteger();
        Action0 task = new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec);
        Worker w = custom.createWorker();
        try {
            Subscription s1 = w.schedule(task);
            Subscription s2 = w.schedule(task);
            Subscription s3 = w.schedule(task);
            
            s1.unsubscribe();
            s2.unsubscribe();
            s3.unsubscribe();
            
            exec.executeAll();
            
            assertEquals(0, calls.get());
        } finally {
            w.unsubscribe();
        }
    }
    @Test
    public void testCancelledWorkerDoesntRunTasks() {
        final AtomicInteger calls = new AtomicInteger();
        Action0 task = new Action0() {
            @Override
            public void call() {
                calls.getAndIncrement();
            }
        };
        TestExecutor exec = new TestExecutor();
        Scheduler custom = Schedulers.from(exec);
        Worker w = custom.createWorker();
        try {
            w.schedule(task);
            w.schedule(task);
            w.schedule(task);
        } finally {
            w.unsubscribe();
        }
        exec.executeAll();
        assertEquals(0, calls.get());
    }
    @Test
    public void testNoTimedTaskAfterScheduleRetention() throws InterruptedException {
        Executor e = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        ExecutorSchedulerWorker w = (ExecutorSchedulerWorker)Schedulers.from(e).createWorker();
        
        w.schedule(Actions.empty(), 50, TimeUnit.MILLISECONDS);
        
        assertTrue(w.tasks.hasSubscriptions());
        
        Thread.sleep(150);
        
        assertFalse(w.tasks.hasSubscriptions());
    }
    
    @Test
    public void testNoTimedTaskPartRetention() {
        Executor e = new Executor() {
            @Override
            public void execute(Runnable command) {
                
            }
        };
        ExecutorSchedulerWorker w = (ExecutorSchedulerWorker)Schedulers.from(e).createWorker();
        
        Subscription s = w.schedule(Actions.empty(), 1, TimeUnit.DAYS);
        
        assertTrue(w.tasks.hasSubscriptions());
        
        s.unsubscribe();
        
        assertFalse(w.tasks.hasSubscriptions());
    }
    
    @Test
    public void testNoPeriodicTimedTaskPartRetention() throws InterruptedException {
        Executor e = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        ExecutorSchedulerWorker w = (ExecutorSchedulerWorker)Schedulers.from(e).createWorker();
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Action0 action = new Action0() {
            @Override
            public void call() {
                cdl.countDown();
            }
        };
        
        Subscription s = w.schedulePeriodically(action, 0, 1, TimeUnit.DAYS);
        
        assertTrue(w.tasks.hasSubscriptions());
        
        cdl.await();
        
        s.unsubscribe();
        
        assertFalse(w.tasks.hasSubscriptions());
    }
}
