 /**
  * Copyright 2013 Netflix, Inc.
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class ReentrantSchedulerTest {
    @Test
    public void testReentrantSchedulerIsProvided() throws InterruptedException {
        final AtomicReference<Object> ref = new AtomicReference<Object>();
        final CountDownLatch cdl = new CountDownLatch(1);
        Scheduler scheduler = Schedulers.threadPoolForComputation();
        scheduler.schedule(1, new Func2<Scheduler, Integer, Subscription>() {
            
            @Override
            public Subscription call(Scheduler t1, Integer t2) {
                ref.set(t1);
                cdl.countDown();
                return Subscriptions.empty();
            }
        });
        
        if (!cdl.await(1000, TimeUnit.MILLISECONDS)) {
            fail("Should have countdown the latch!");
        }
        
        assertTrue(ref.get() instanceof ReentrantScheduler);
    }
    
    @Test
    public void testReentrantSchedulerIsProvided2() throws InterruptedException {
        final AtomicReference<Object> ref = new AtomicReference<Object>();
        final CountDownLatch cdl = new CountDownLatch(1);
        Scheduler scheduler = Schedulers.threadPoolForComputation();
        scheduler.schedule(1, new Func2<Scheduler, Integer, Subscription>() {
            
            @Override
            public Subscription call(Scheduler t1, Integer t2) {
                ref.set(t1);
                cdl.countDown();
                return Subscriptions.empty();
            }
        }, 100, TimeUnit.MILLISECONDS);
        
        if (!cdl.await(1000, TimeUnit.MILLISECONDS)) {
            fail("Should have countdown the latch!");
        }
        
        assertTrue(ref.get() instanceof ReentrantScheduler);
    }
    
    @Test
    public void testReentrantSchedulerIsProvided3() throws InterruptedException {
        final AtomicReference<Object> ref = new AtomicReference<Object>();
        final CountDownLatch cdl = new CountDownLatch(1);
        Scheduler scheduler = Schedulers.threadPoolForComputation();
        Subscription s = scheduler.schedulePeriodically(1, new Func2<Scheduler, Integer, Subscription>() {
            int count;
            @Override
            public Subscription call(Scheduler t1, Integer t2) {
                if (count++ == 3) {
                    cdl.countDown();
                    ref.set(t1);
                }
                return Subscriptions.empty();
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
        
        if (!cdl.await(5000, TimeUnit.MILLISECONDS)) {
            fail("Should have countdown the latch!");
        }
        
        s.unsubscribe();
        
        assertTrue(ref.get() instanceof ReentrantScheduler);
    }
}
