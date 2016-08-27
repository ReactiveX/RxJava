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

import org.junit.*;

import io.reactivex.Scheduler;

public class NewThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.newThread();
    }

    @Test
    @Ignore("Unhandled errors are no longer thrown")
    public final void testUnhandledErrorIsDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.testUnhandledErrorIsDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public final void testHandledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTestHelper.testHandledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }
    
    // FIXME no longer testable due to internal changes
//    @Test(timeout = 3000)
//    public void testNoSelfInterrupt() throws InterruptedException {
//        Scheduler.Worker worker = Schedulers.newThread().createWorker();
//        try {
//            final CountDownLatch run = new CountDownLatch(1);
//            final CountDownLatch done = new CountDownLatch(1);
//            final AtomicReference<Throwable> exception = new AtomicReference<T>();
//            final AtomicBoolean interruptFlag = new AtomicBoolean();
//            
//            ScheduledRunnable sa = (ScheduledRunnable)worker.schedule(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        run.await();
//                    } catch (InterruptedException ex) {
//                        exception.set(ex);
//                    }
//                }
//            });
//            
//            sa.add(new Disposable() {
//                @Override
//                public void dispose() {
//                    interruptFlag.set(Thread.currentThread().isInterrupted());
//                    done.countDown();
//                }
//            });
//            
//            run.countDown();
//            
//            done.await();
//            
//            Assert.assertEquals(null, exception.get());
//            Assert.assertFalse("Interrupted?!", interruptFlag.get());
//        } finally {
//            worker.dispose();
//        }
//    }
}