/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.schedulers;

import static org.junit.Assert.fail;

import java.lang.management.*;
import java.util.concurrent.*;

import io.reactivex.Scheduler;
import io.reactivex.internal.schedulers.SchedulerPoolHelper;

public class SchedulerRetentionTest {
    
    static void testCancelledRetentionWith(Scheduler.Worker w, boolean periodic) throws InterruptedException {
        System.out.println("  Wait before GC");
        Thread.sleep(1000);
        
        System.out.println("  GC");
        System.gc();
        
        Thread.sleep(1000);

        
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage memHeap = memoryMXBean.getHeapMemoryUsage();
        long initial = memHeap.getUsed();
        
        System.out.printf("  Starting: %.3f MB%n", initial / 1024.0 / 1024.0);

        int n = 100 * 1000;
        if (periodic) {
            final CountDownLatch cdl = new CountDownLatch(n);
            final Runnable action = new Runnable() {
                @Override
                public void run() {
                    cdl.countDown();
                }
            };
            for (int i = 0; i < n; i++) {
                if (i % 50000 == 0) {
                    System.out.println("  -> still scheduling: " + i);
                }
                w.schedulePeriodically(action, 0, 1, TimeUnit.DAYS);
            }
            if (n % 50000 != 0) {
                System.out.println("  -> still scheduling: " + n);
            }
            
            System.out.println("  Waiting for the first round to finish...");
            cdl.await();
        } else {
            for (int i = 0; i < n; i++) {
                if (i % 50000 == 0) {
                    System.out.println("  -> still scheduling: " + i);
                }
                w.schedule(() -> { }, 1, TimeUnit.DAYS);
            }
        }
        
        memHeap = memoryMXBean.getHeapMemoryUsage();
        long after = memHeap.getUsed();
        System.out.printf("  Peak: %.3f MB%n", after / 1024.0 / 1024.0);
        
        w.dispose();
        
        System.out.println("  Wait before second GC");
        int wait = 1000;
        
        if (SchedulerPoolHelper.forcePurge()) {
            System.out.println("  Purging is enabled, increasing wait time.");
            wait += SchedulerPoolHelper.purgeFrequency();
            wait += (int)(n * Math.log(n) / 200);
        }
        
        while (wait > 0) {
            System.out.printf("  -> Waiting before scond GC: %.0f seconds remaining%n", wait / 1000d);
            Thread.sleep(1000);
            
            wait -= 1000;
        }
        
        System.out.println("  Second GC");
        System.gc();
        
        Thread.sleep(1000);
        
        memHeap = memoryMXBean.getHeapMemoryUsage();
        long finish = memHeap.getUsed();
        System.out.printf("  After: %.3f MB%n", finish / 1024.0 / 1024.0);
        
        if (finish > initial * 5) {
            fail(String.format("  Tasks retained: %.3f -> %.3f -> %.3f", initial / 1024 / 1024.0, after / 1024 / 1024.0, finish / 1024 / 1024d));
        }
    }

    static void runCancellationRetention(Scheduler s) throws InterruptedException {
        Scheduler.Worker w = s.createWorker();
        System.out.println("<<<< Testing with one-shot delayed tasks");
        try {
            testCancelledRetentionWith(w, false);
        } finally {
            w.dispose();
        }
        System.out.println("<<<< Testing with periodic delayed tasks");
        w = Schedulers.computation().createWorker();
        try {
            testCancelledRetentionWith(w, true);
        } finally {
            w.dispose();
        }
    }
    
    /**
     * Check if the worker handles the cancellation properly and doesn't retain cancelled
     * 
     * @param s the scheduler to test
     * @param bothMode if supported, check both setRemoveOnCancelPolicy and purge mode.
     * @throws InterruptedException
     */
    public static void testCancellationRetention(Scheduler s, boolean bothMode) throws InterruptedException {
        System.out.println("------------------------------------------------------------------------");
        System.out.println(">> testCancellationRetention : " + s.getClass());
        if (bothMode && SchedulerPoolHelper.isRemoveOnCancelPolicySupported()) {
            boolean force = SchedulerPoolHelper.forcePurge();

            // switch to the other mode
            
            System.out.println("  Shutting down pool helper: force is " + force);
            // don't clear any purge registrations from other schedulers
            s.shutdown();
            SchedulerPoolHelper.shutdown(false);
            System.out.println("  Letting the pool helper terminate");
            Thread.sleep(1000);
            
            System.out.println("  Setting forcePurge to " + !force);
            SchedulerPoolHelper.forcePurge(!force);
            
            System.out.println("  Starting pool helper");
            SchedulerPoolHelper.start();
            s.start();
            
            runCancellationRetention(s);
            
            // switch back to the original mode

            System.out.println("  Shutting down pool helper again");
            // clear the pool if the original mode wasn't force
            s.shutdown();
            SchedulerPoolHelper.shutdown(!force);

            System.out.println("  Letting the pool helper terminate again");
            Thread.sleep(1000);
            
            System.out.println("  Restoring forcePurge to " + force);
            SchedulerPoolHelper.forcePurge(force);
            
            System.out.println("  Starting pool helper");
            SchedulerPoolHelper.start();
            s.start();
        }
        // run the regular mode checks
        runCancellationRetention(s);
    }
}
