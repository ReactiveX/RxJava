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
package rx.subjects;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class BufferUntilSubscriberTest {

    @Test
    public void testIssue1677() throws InterruptedException {
        final AtomicLong counter = new AtomicLong();
        final Integer[] numbers = new Integer[5000];
        for (int i = 0; i < numbers.length; i++)
            numbers[i] = i + 1;
        final int NITERS = 250;
        final CountDownLatch latch = new CountDownLatch(NITERS);
        for (int iters = 0; iters < NITERS; iters++) {
            final CountDownLatch innerLatch = new CountDownLatch(1);
            final PublishSubject<Void> s = PublishSubject.create();
            final AtomicBoolean completed = new AtomicBoolean();
            Observable.from(numbers)
                    .takeUntil(s)
                    .window(50)
                    .flatMap(new Func1<Observable<Integer>, Observable<Integer>>() {
                        @Override
                        public Observable<Integer> call(Observable<Integer> integerObservable) {
                            return integerObservable
                                    .subscribeOn(Schedulers.computation())
                                    .map(new Func1<Integer, Integer>() {
                                        @Override
                                        public Integer call(Integer integer) {
                                            if (integer >= 5 && completed.compareAndSet(false, true)) {
                                                s.onCompleted();
                                            }
                                            // do some work
                                            Math.pow(Math.random(), Math.random());
                                            return integer * 2;
                                        }
                                    });
                        }
                    })
                    .toList()
                    .doOnNext(new Action1<List<Integer>>() {
                        @Override
                        public void call(List<Integer> integers) {
                            counter.incrementAndGet();
                            latch.countDown();
                            innerLatch.countDown();
                        }
                    })
                    .subscribe();
            if (!innerLatch.await(30, TimeUnit.SECONDS))
                Assert.fail("Failed inner latch wait, iteration " + iters);
        }
        if (!latch.await(30, TimeUnit.SECONDS))
            Assert.fail("Incomplete! Went through " + latch.getCount() + " iterations");
        else
            Assert.assertEquals(NITERS, counter.get());
    }
    
    @Test
    public void testBackpressure() {
        UnicastSubject<Integer> bus = UnicastSubject.create();
        for (int i = 0; i < 32; i++) {
            bus.onNext(i);
        }
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        bus.subscribe(ts);
        
        ts.assertValueCount(0);
        ts.assertNoTerminalEvent();
        
        ts.requestMore(10);
        
        ts.assertValueCount(10);
        
        ts.requestMore(22);
        ts.assertValueCount(32);

        Assert.assertFalse(bus.state.caughtUp);

        ts.requestMore(Long.MAX_VALUE);
        
        Assert.assertTrue(bus.state.caughtUp);

        for (int i = 32; i < 64; i++) {
            bus.onNext(i);
        }
        bus.onCompleted();
        
        ts.assertValueCount(64);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    @Test
    public void testErrorCutsAhead() {
        UnicastSubject<Integer> bus = UnicastSubject.create();
        for (int i = 0; i < 32; i++) {
            bus.onNext(i);
        }
        bus.onError(new TestException());
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        bus.subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
    @Test
    public void testErrorCutsAheadAfterSubscribed() {
        UnicastSubject<Integer> bus = UnicastSubject.create();
        for (int i = 0; i < 32; i++) {
            bus.onNext(i);
        }
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        
        bus.subscribe(ts);

        ts.assertNoValues();
        ts.assertNoTerminalEvent();

        bus.onError(new TestException());
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }
    @Test
    public void testUnsubscribeClearsQueue() {
        UnicastSubject<Integer> bus = UnicastSubject.create();
        for (int i = 0; i < 32; i++) {
            bus.onNext(i);
        }
        
        TestSubscriber<Integer> ts = TestSubscriber.create(0);
        ts.unsubscribe();
        
        bus.subscribe(ts);
        
        ts.assertNoTerminalEvent();
        ts.assertNoValues();
        
        Assert.assertTrue(bus.state.queue.isEmpty());
        
        bus.onNext(32);
        
        Assert.assertTrue(bus.state.queue.isEmpty());
    }
}