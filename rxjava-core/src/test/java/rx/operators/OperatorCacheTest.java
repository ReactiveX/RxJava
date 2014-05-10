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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

public class OperatorCacheTest {

    @Test
    public void testCache() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger();
        Observable<String> o = Observable.create(new Observable.OnSubscribe<String>() {

            @Override
            public void call(final Subscriber<? super String> observer) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        counter.incrementAndGet();
                        System.out.println("published observable being executed");
                        observer.onNext("one");
                        observer.onCompleted();
                    }
                }).start();
            }
        }).cache();

        // we then expect the following 2 subscriptions to get that same value
        final CountDownLatch latch = new CountDownLatch(2);

        // subscribe once
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        // subscribe again
        o.subscribe(new Action1<String>() {

            @Override
            public void call(String v) {
                assertEquals("one", v);
                System.out.println("v: " + v);
                latch.countDown();
            }
        });

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            fail("subscriptions did not receive values");
        }
        assertEquals(1, counter.get());
    }
    void testWithCustomSubjectAndRepeat(Subject<Integer, Integer> subject, Integer... expected) {
        Observable<Integer> source0 = Observable.from(1, 2, 3)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(final Integer i) {
                        return Observable.timer(i * 20, TimeUnit.MILLISECONDS).map(new Func1<Long, Integer>() {
                            @Override
                            public Integer call(Long t1) {
                                return i;
                            }
                        });
                    }
                });
        
        Observable<Integer> source1 = Observable.create(new OperatorCache<Integer>(source0, subject));
        
        Observable<Integer> source2 = source1
                .repeat(4)
                .zip(Observable.timer(0, 10, TimeUnit.MILLISECONDS, Schedulers.newThread()), new Func2<Integer, Long, Integer>() {
                    @Override
                    public Integer call(Integer t1, Long t2) {
                        return t1;
                    }
                    
                });
        final CountDownLatch cdl = new CountDownLatch(1);
        TestObserver<Integer> test = new TestObserver<Integer>(new Observer<Integer>() {
            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable e) {
                cdl.countDown();
            }

            @Override
            public void onCompleted() {
                cdl.countDown();
            }
        });
        source2.subscribe(test);

        try {
            cdl.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            fail("Interrupted");
        }
        
        test.assertReceivedOnNext(Arrays.asList(expected));
        test.assertTerminalEvent();
        assertTrue(test.getOnErrorEvents().isEmpty());
    }
    @Test(timeout = 10000)
    public void testWithAsyncSubjectAndRepeat() {
        testWithCustomSubjectAndRepeat(AsyncSubject.<Integer>create(), 3, 3, 3, 3);
    }
    @Test(timeout = 10000)
    public void testWithBehaviorSubjectAndRepeat() {
        // BehaviorSubject just completes when repeated
        testWithCustomSubjectAndRepeat(BehaviorSubject.create(0), 0, 1, 2, 3);
    }
    @Test(timeout = 10000)
    public void testWithPublishSubjectAndRepeat() {
        // PublishSubject just completes when repeated
        testWithCustomSubjectAndRepeat(PublishSubject.<Integer>create(), 1, 2, 3);
    }
    @Test(timeout = 10000)
    public void testWithReplaySubjectAndRepeat() {
        testWithCustomSubjectAndRepeat(ReplaySubject.<Integer>create(), 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3);
    }
}
