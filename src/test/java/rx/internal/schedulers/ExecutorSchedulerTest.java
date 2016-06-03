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
package rx.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import rx.*;
import rx.Scheduler.Worker;
import rx.functions.*;
import rx.internal.schedulers.ExecutorScheduler.ExecutorSchedulerWorker;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.AbstractSchedulerConcurrencyTests;
import rx.schedulers.SchedulerTests;
import rx.schedulers.Schedulers;

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
    
    @Test(timeout = 60000)
    public void testCancelledTaskRetention() throws InterruptedException {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Scheduler s = Schedulers.from(exec);
        try {
            Scheduler.Worker w = s.createWorker();
            try {
                SchedulerTests.testCancelledRetention(w, false);
            } finally {
                w.unsubscribe();
            }
            
            w = s.createWorker();
            try {
                SchedulerTests.testCancelledRetention(w, true);
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
