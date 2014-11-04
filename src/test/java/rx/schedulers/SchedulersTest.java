/**
 * Copyright 2014 Netflix, Inc.
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

package rx.schedulers;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import rx.Scheduler.Worker;
import rx.functions.Action0;

/**
 * Tests the Schedulers utility class.
 */
public class SchedulersTest {
    @Test
    public void testShutdown() throws Exception {
        makesureSchedulerThreadsAreUp();
        // wait a bit more
        System.out.println("SchedulersTest.testShutdown => waiting a bit more");
        Thread.sleep(1000);
        // shutdown worker threads
        System.out.println("SchedulersTest.testShutdown => shutting down schedulers");
        Schedulers.shutdown();
        // wait a bit.
        System.out.println("SchedulersTest.testShutdown => giving some time to shutdown");
        Thread.sleep(5000);
        
        Assert.assertFalse("Computation scheduler restarted?", Schedulers.hasComputationScheduler());
        Assert.assertFalse("IO scheduler restarted?", Schedulers.hasIOScheduler());

        System.out.println("SchedulersTest.testShutdown => let's see if their threads are up or not");
        Map<Thread, StackTraceElement[]> ast = Thread.getAllStackTraces();
        for (Thread t : ast.keySet()) {
            if (!t.isAlive()) {
                continue;
            }
            if (t.getName().startsWith(EventLoopsScheduler.THREAD_NAME_PREFIX)
                    || t.getName().startsWith(CachedThreadScheduler.WORKER_THREAD_NAME_PREFIX)
                    || t.getName().startsWith(CachedThreadScheduler.EVICTOR_THREAD_NAME_PREFIX)) {
                System.out.println("Thread " + t.getName() + " still running");
                for (StackTraceElement ste : ast.get(t)) {
                    System.out.printf("\tat %s.%s(%s:%d)%n", ste.getClassName(), ste.getMethodName(), ste.getFileName(), ste.getLineNumber());
                }
                Assert.fail(t.getName());
                break;
            }
        }
        makesureSchedulerThreadsAreUp();
    }
    /**
     * Runs some tasks on the computation and io schedulers to make sure they are available and
     * can execute tasks before and after shutdown.
     * @throws InterruptedException
     */
    protected void makesureSchedulerThreadsAreUp() throws InterruptedException {
        System.out.println("SchedulersTest.testShutdown => Scheduling tasks");
        // make sure scheduler threads are created
        final CountDownLatch cdl = new CountDownLatch(2);
        final Worker cw = Schedulers.computation().createWorker();
        cw.schedule(new Action0() {
            @Override
            public void call() {
                System.out.println("SchedulersTest.testShutdown => Computation task run");
                cdl.countDown();
                cw.unsubscribe();
            } 
        });

        final Worker iw = Schedulers.io().createWorker();
        iw.schedule(new Action0() {
            @Override
            public void call() {
                System.out.println("SchedulersTest.testShutdown => IO task run");
                cdl.countDown();
                iw.unsubscribe();
            } 
        });

        if (!cdl.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Tasks didn't complete in time.");
        } else {
            System.out.println("SchedulersTest.testShutdown => both task run successfully");
        }
    }
}
