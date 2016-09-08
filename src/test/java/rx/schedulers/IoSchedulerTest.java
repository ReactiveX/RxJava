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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.*;
import rx.Scheduler.Worker;
import rx.functions.*;

public class IoSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.io();
    }

    /**
     * IO scheduler defaults to using CachedThreadScheduler
     */
    @Test
    public final void testIOScheduler() {

        Observable<Integer> o1 = Observable.just(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.just(6, 7, 8, 9, 10);
        Observable<String> o = Observable.merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxIoScheduler"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.io()).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
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
        Worker w = Schedulers.io().createWorker();
        try {
            SchedulerTests.testCancelledRetention(w, false);
        } finally {
            w.unsubscribe();
        }
        w = Schedulers.io().createWorker();
        try {
            SchedulerTests.testCancelledRetention(w, true);
        } finally {
            w.unsubscribe();
        }
    }

    // Tests that an uninterruptible worker does not get reused
    @Test(timeout = 10000)
    public void testUninterruptibleActionDoesNotBlockOtherAction() throws InterruptedException {
        final Worker uninterruptibleWorker = Schedulers.io().createWorker();
        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicBoolean shouldQuit = new AtomicBoolean(false);
        try {
            uninterruptibleWorker.schedule(new Action0() {
                @Override
                public void call() {
                    synchronized (running) {
                        running.set(true);
                        running.notifyAll();
                    }
                    synchronized (shouldQuit) {
                        while (!shouldQuit.get()) {
                            try {
                                shouldQuit.wait();
                            } catch (final InterruptedException ignored) {
                            }
                        }
                    }
                    synchronized (running) {
                        running.set(false);
                        running.notifyAll();
                    }
                }
            });

            // Wait for the action to start executing
            synchronized (running) {
                while (!running.get()) {
                    running.wait();
                }
            }
        } finally {
            uninterruptibleWorker.unsubscribe();
        }

        final Worker otherWorker = Schedulers.io().createWorker();
        final AtomicBoolean otherActionRan = new AtomicBoolean(false);
        try {
            otherWorker.schedule(new Action0() {
                @Override
                public void call() {
                    otherActionRan.set(true);
                }
            });
            Thread.sleep(1000); // give the action a chance to run
        } finally {
            otherWorker.unsubscribe();
        }

        assertTrue(running.get()); // uninterruptible action keeps on running since InterruptedException is swallowed
        assertTrue(otherActionRan.get());

        // Wait for uninterruptibleWorker to exit (to clean up after ourselves)
        synchronized (shouldQuit) {
            shouldQuit.set(true);
            shouldQuit.notifyAll();
        }
        synchronized (running) {
            while (running.get()) {
                running.wait();
            }
        }
    }
}
