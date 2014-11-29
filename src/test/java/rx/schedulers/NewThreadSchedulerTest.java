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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;

import rx.Scheduler;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.subscriptions.Subscriptions;

public class NewThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.newThread();
    }

    @Test
    public final void testUnhandledErrorIsDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testUnhandledErrorIsDeliveredToThreadHandler(getScheduler());
    }

    @Test
    public final void testHandledErrorIsNotDeliveredToThreadHandler() throws InterruptedException {
        SchedulerTests.testHandledErrorIsNotDeliveredToThreadHandler(getScheduler());
    }
    @Test(timeout = 3000)
    public void testNoSelfInterrupt() throws InterruptedException {
        Scheduler.Worker worker = Schedulers.newThread().createWorker();
        try {
            final CountDownLatch run = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
            final AtomicBoolean interruptFlag = new AtomicBoolean();
            
            ScheduledAction sa = (ScheduledAction)worker.schedule(new Action0() {
                @Override
                public void call() {
                    try {
                        run.await();
                    } catch (InterruptedException ex) {
                        exception.set(ex);
                    }
                }
            });
            
            sa.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    interruptFlag.set(Thread.currentThread().isInterrupted());
                    done.countDown();
                }
            }));
            
            run.countDown();
            
            done.await();
            
            Assert.assertEquals(null, exception.get());
            Assert.assertFalse("Interrupted?!", interruptFlag.get());
        } finally {
            worker.unsubscribe();
        }
    }
}
