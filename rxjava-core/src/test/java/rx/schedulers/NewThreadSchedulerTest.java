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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

public class NewThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.newThread();
    }

    /**
     * IO scheduler defaults to using NewThreadScheduler
     */
    @Test
    public final void testIOScheduler() {

        Observable<Integer> o1 = Observable.<Integer> from(1, 2, 3, 4, 5);
        Observable<Integer> o2 = Observable.<Integer> from(6, 7, 8, 9, 10);
        Observable<String> o = Observable.<Integer> merge(o1, o2).map(new Func1<Integer, String>() {

            @Override
            public String call(Integer t) {
                assertTrue(Thread.currentThread().getName().startsWith("RxNewThreadScheduler"));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.subscribeOn(Schedulers.io()).toBlockingObservable().forEach(new Action1<String>() {

            @Override
            public void call(String t) {
                System.out.println("t: " + t);
            }
        });
    }
    @Test(timeout = 10000)
    public void testPeriodicCanBeUnsubscribed() throws InterruptedException {
        final Worker w = getScheduler().createWorker();
        
        final AtomicInteger counter = new AtomicInteger();
        
        final CompositeSubscription csub = new CompositeSubscription();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        Subscription s = w.schedulePeriodically(new Action0() {

            @Override
            public void call() {
                try {
                    cdl.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                int c = counter.incrementAndGet();
                if (c == 3) {
                    csub.unsubscribe();
                    return;
                }
                if (c == 6) {
                    w.unsubscribe();
                }
            }
            
        }, 100, 100, TimeUnit.MILLISECONDS);
        csub.add(s);
        
        cdl.countDown();
        
        Thread.sleep(1500);
        
        assertEquals(3, counter.get());
    }
}
