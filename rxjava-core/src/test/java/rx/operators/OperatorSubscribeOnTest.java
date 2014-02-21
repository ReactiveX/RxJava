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
package rx.operators;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observers.TestObserver;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorSubscribeOnTest {

    @Test(timeout = 2000)
    public void testIssue813() throws InterruptedException {
        // https://github.com/Netflix/RxJava/issues/813
        final CountDownLatch scheduled = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);

        TestObserver<Integer> observer = new TestObserver<Integer>();

        final Subscription subscription = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(
                            final Subscriber<? super Integer> subscriber) {
                        scheduled.countDown();
                        try {
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                // this means we were unsubscribed (Scheduler shut down and interrupts)
                                // ... but we'll pretend we are like many Observables that ignore interrupts
                            }

                            subscriber.onCompleted();
                        } catch (Throwable e) {
                            subscriber.onError(e);
                        } finally {
                            doneLatch.countDown();
                        }
                    }
                }).subscribeOn(Schedulers.computation()).subscribe(observer);

        // wait for scheduling
        scheduled.await();
        // trigger unsubscribe
        subscription.unsubscribe();
        latch.countDown();
        doneLatch.await();
        assertEquals(0, observer.getOnErrorEvents().size());
        assertEquals(1, observer.getOnCompletedEvents().size());
    }

    public static class SlowScheduler extends Scheduler {
        final Scheduler actual;
        final long delay;
        final TimeUnit unit;

        public SlowScheduler() {
            this(Schedulers.computation(), 2, TimeUnit.SECONDS);
        }

        public SlowScheduler(Scheduler actual, long delay, TimeUnit unit) {
            this.actual = actual;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        public Subscription schedule(final Action1<Scheduler.Inner> action) {
            return actual.schedule(action, delay, unit);
        }

        @Override
        public Subscription schedule(final Action1<Scheduler.Inner> action, final long delayTime, final TimeUnit delayUnit) {
            TimeUnit common = delayUnit.compareTo(unit) < 0 ? delayUnit : unit;
            long t = common.convert(delayTime, delayUnit) + common.convert(delay, unit);
            return actual.schedule(action, t, common);
        }
    }

    @Test(timeout = 5000)
    public void testUnsubscribeInfiniteStream() throws InterruptedException {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                for (int i = 1; !sub.isUnsubscribed(); i++) {
                    count.incrementAndGet();
                    sub.onNext(i);
                }
            }

        }).subscribeOn(Schedulers.newThread()).take(10).subscribe(ts);

        ts.awaitTerminalEventAndUnsubscribeOnTimeout(1000, TimeUnit.MILLISECONDS);
        Thread.sleep(200); // give time for the loop to continue
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(10, count.get());
    }

}
