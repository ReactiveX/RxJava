/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.assertTrue;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.functions.*;

public class CachedThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.io();
    }

    /**
     * IO scheduler defaults to using CachedThreadScheduler
     */
    @Test
    public final void testIOScheduler() {

        Flowable<Integer> o1 = Flowable.just(1, 2, 3, 4, 5);
        Flowable<Integer> o2 = Flowable.just(6, 7, 8, 9, 10);
        Flowable<String> o = Flowable.merge(o1, o2).map(new Function<Integer, String>() {

            @Override
            public String apply(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxCachedThreadScheduler"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.io()).toBlocking().forEach(new Consumer<String>() {

            @Override
            public void accept(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    @Ignore("Unhandled errors are no longer thrown")
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
            ExecutorSchedulerTest.testCancelledRetention(w, false);
        } finally {
            w.dispose();
        }
        w = Schedulers.io().createWorker();
        try {
            ExecutorSchedulerTest.testCancelledRetention(w, true);
        } finally {
            w.dispose();
        }
    }

}