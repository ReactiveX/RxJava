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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorSubscribeOnTest {

    @Test(timeout = 2000)
    public void testIssue813() throws InterruptedException {
        // https://github.com/ReactiveX/RxJava/issues/813
        final CountDownLatch scheduled = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);

        TestObserver<Integer> NbpObserver = new TestObserver<Integer>();

        Observable
        .create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(
                    final Observer<? super Integer> NbpSubscriber) {
                NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
                scheduled.countDown();
                try {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        // this means we were unsubscribed (Scheduler shut down and interrupts)
                        // ... but we'll pretend we are like many Observables that ignore interrupts
                    }

                    NbpSubscriber.onComplete();
                } catch (Throwable e) {
                    NbpSubscriber.onError(e);
                } finally {
                    doneLatch.countDown();
                }
            }
        }).subscribeOn(Schedulers.computation()).subscribe(NbpObserver);

        // wait for scheduling
        scheduled.await();
        // trigger unsubscribe
        NbpObserver.dispose();
        latch.countDown();
        doneLatch.await();
        NbpObserver.assertNoErrors();
        NbpObserver.assertComplete();
    }

    @Test
    @Ignore("NbpOnSubscribe.subscribe can't throw")
    public void testThrownErrorHandling() {
        TestObserver<String> ts = new TestObserver<String>();
        Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(Observer<? super String> s) {
                throw new RuntimeException("fail");
            }

        }).subscribeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
    }

    @Test
    public void testOnError() {
        TestObserver<String> ts = new TestObserver<String>();
        Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(Observer<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onError(new RuntimeException("fail"));
            }

        }).subscribeOn(Schedulers.computation()).subscribe(ts);
        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.assertTerminated();
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
        public Worker createWorker() {
            return new SlowInner(actual.createWorker());
        }

        private final class SlowInner extends Worker {

            private final Scheduler.Worker actualInner;

            private SlowInner(Worker actual) {
                this.actualInner = actual;
            }

            @Override
            public void dispose() {
                actualInner.dispose();
            }

            // FIXME not available
//            @Override
//            public boolean isDisposed() {
//                return actualInner.isDisposed();
//            }

            @Override
            public Disposable schedule(final Runnable action) {
                return actualInner.schedule(action, delay, unit);
            }

            @Override
            public Disposable schedule(final Runnable action, final long delayTime, final TimeUnit delayUnit) {
                TimeUnit common = delayUnit.compareTo(unit) < 0 ? delayUnit : unit;
                long t = common.convert(delayTime, delayUnit) + common.convert(delay, unit);
                return actualInner.schedule(action, t, common);
            }

        }

    }

    @Test(timeout = 5000)
    public void testUnsubscribeInfiniteStream() throws InterruptedException {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        final AtomicInteger count = new AtomicInteger();
        Observable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(Observer<? super Integer> sub) {
                BooleanDisposable bs = new BooleanDisposable();
                sub.onSubscribe(bs);
                for (int i = 1; !bs.isDisposed(); i++) {
                    count.incrementAndGet();
                    sub.onNext(i);
                }
            }

        }).subscribeOn(Schedulers.newThread()).take(10).subscribe(ts);

        ts.awaitTerminalEvent(1000, TimeUnit.MILLISECONDS);
        ts.dispose();
        Thread.sleep(200); // give time for the loop to continue
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(10, count.get());
    }
}