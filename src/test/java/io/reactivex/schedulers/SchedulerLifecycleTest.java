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

package io.reactivex.schedulers;

import static org.junit.Assert.fail;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.CompositeDisposable;

public class SchedulerLifecycleTest {
    @Test
    public void testShutdown() throws InterruptedException {
        tryOutSchedulers();

        System.out.println("testShutdown >> Giving time threads to spin-up");
        Thread.sleep(500);

        Set<Thread> rxThreads = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreads.add(t);
            }
        }
        Schedulers.shutdown();
        System.out.println("testShutdown >> Giving time to threads to stop");
        Thread.sleep(500);

        StringBuilder b = new StringBuilder();
        for (Thread t : rxThreads) {
            if (t.isAlive()) {
                b.append("Thread " + t + " failed to shutdown\r\n");
                for (StackTraceElement ste : t.getStackTrace()) {
                    b.append("  ").append(ste).append("\r\n");
                }
            }
        }
        if (b.length() > 0) {
            System.out.print(b);
            System.out.println("testShutdown >> Restarting schedulers...");
            Schedulers.start(); // restart them anyways
            fail("Rx Threads were still alive:\r\n" + b);
        }

        System.out.println("testShutdown >> Restarting schedulers...");
        Schedulers.start();

        tryOutSchedulers();
    }

    private void tryOutSchedulers() throws InterruptedException {
        final CountDownLatch cdl = new CountDownLatch(4);

        final Runnable countAction = new Runnable() {
            @Override
            public void run() {
                cdl.countDown();
            }
        };

        CompositeDisposable csub = new CompositeDisposable();

        try {
            Worker w1 = Schedulers.computation().createWorker();
            csub.add(w1);
            w1.schedule(countAction);

            Worker w2 = Schedulers.io().createWorker();
            csub.add(w2);
            w2.schedule(countAction);

            Worker w3 = Schedulers.newThread().createWorker();
            csub.add(w3);
            w3.schedule(countAction);

            Worker w4 = Schedulers.single().createWorker();
            csub.add(w4);
            w4.schedule(countAction);


            if (!cdl.await(3, TimeUnit.SECONDS)) {
                fail("countAction was not run by every worker");
            }
        } finally {
            csub.dispose();
        }
    }

    @Test
    public void testStartIdempotence() throws InterruptedException {
        tryOutSchedulers();

        System.out.println("testStartIdempotence >> giving some time");
        Thread.sleep(500);

        Set<Thread> rxThreadsBefore = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreadsBefore.add(t);
                System.out.println("testStartIdempotence >> " + t);
            }
        }
        System.out.println("testStartIdempotence >> trying to start again");
        Schedulers.start();
        System.out.println("testStartIdempotence >> giving some time again");
        Thread.sleep(500);

        Set<Thread> rxThreadsAfter = new HashSet<Thread>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("Rx")) {
                rxThreadsAfter.add(t);
                System.out.println("testStartIdempotence >>>> " + t);
            }
        }

        // cached threads may get dropped between the two checks
        rxThreadsAfter.removeAll(rxThreadsBefore);

        Assert.assertTrue("Some new threads appeared: " + rxThreadsAfter, rxThreadsAfter.isEmpty());
    }
}
