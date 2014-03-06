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

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class NewThreadSchedulerTest extends AbstractSchedulerConcurrencyTests {

    @Override
    protected Scheduler getScheduler() {
        return NewThreadScheduler.getInstance();
    }

    @Test
    public void testCurrentScheduler() throws InterruptedException {

        final AtomicReference<Scheduler.Inner> capturedCurrent = new AtomicReference<Scheduler.Inner>();
        final AtomicReference<Scheduler.Inner> capturedInner = new AtomicReference<Scheduler.Inner>();
        final AtomicReference<Thread> first = new AtomicReference<Thread>();
        final AtomicReference<Thread> second = new AtomicReference<Thread>();
        final AtomicInteger c = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        Schedulers.newThread().schedule(new Action1<Inner>() {

            @Override
            public void call(Inner inner) {
                c.incrementAndGet();
                capturedInner.set(inner);
                first.set(Thread.currentThread());
                capturedCurrent.set(Schedulers.current());
                Schedulers.current().schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner t1) {
                        c.incrementAndGet();
                        second.set(Thread.currentThread());
                        latch.countDown();
                    }

                });
            }

        });
        latch.await(1000, TimeUnit.MILLISECONDS);
        assertNull(Schedulers.current());
        assertEquals(2, c.get());
        assertEquals(first.get(), second.get());
        assertEquals(capturedCurrent.get(), capturedInner.get());
    }

    @Test
    public void testCurrentSchedulerViaSubscribeOn() throws InterruptedException {

        final AtomicReference<Scheduler.Inner> capturedCurrent = new AtomicReference<Scheduler.Inner>();
        final AtomicReference<Thread> first = new AtomicReference<Thread>();
        final AtomicReference<Thread> second = new AtomicReference<Thread>();
        final AtomicInteger c = new AtomicInteger();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                c.incrementAndGet();
                first.set(Thread.currentThread());
                capturedCurrent.set(Schedulers.current());
                Schedulers.current().schedule(new Action1<Inner>() {

                    @Override
                    public void call(Inner t1) {
                        c.incrementAndGet();
                        second.set(Thread.currentThread());
                        s.onCompleted();
                    }

                });
            }

        }).subscribeOn(Schedulers.newThread()).subscribe(ts);

        ts.awaitTerminalEvent();
        assertNotNull(capturedCurrent.get());
        assertNull(Schedulers.current());
        assertEquals(2, c.get());
        assertEquals(first.get(), second.get());
    }
}
