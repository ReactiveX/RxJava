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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorPivotTest {

    @Test
    public void testPivotEvenAndOdd() throws InterruptedException {
        Observable<GroupedObservable<Boolean, Integer>> o1 = Observable.range(1, 10).groupBy(modKeySelector).subscribeOn(Schedulers.newThread());
        Observable<GroupedObservable<Boolean, Integer>> o2 = Observable.range(11, 10).groupBy(modKeySelector).subscribeOn(Schedulers.newThread());
        Observable<GroupedObservable<String, GroupedObservable<Boolean, Integer>>> groups = Observable.from(GroupedObservable.from("o1", o1), GroupedObservable.from("o2", o2));
        Observable<GroupedObservable<Boolean, GroupedObservable<String, Integer>>> pivoted = Observable.pivot(groups);

        final AtomicInteger count = new AtomicInteger();

        final CountDownLatch latch = new CountDownLatch(1);

        pivoted.flatMap(new Func1<GroupedObservable<Boolean, GroupedObservable<String, Integer>>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Boolean, GroupedObservable<String, Integer>> outerGroup) {
                return outerGroup.flatMap(new Func1<GroupedObservable<String, Integer>, Observable<String>>() {

                    @Override
                    public Observable<String> call(final GroupedObservable<String, Integer> innerGroup) {
                        return innerGroup.map(new Func1<Integer, String>() {

                            @Override
                            public String call(Integer i) {
                                return (outerGroup.getKey() ? "Even" : "Odd ") + " => from source: " + innerGroup.getKey() + " Value: " + i;
                            }

                        });
                    }

                });
            }

        }).subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {
                System.out.println("============> OnCompleted");
                System.out.println("-------------------------------------------------------------------------------------");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("============> Error: " + e);
                System.out.println("-------------------------------------------------------------------------------------");
                latch.countDown();
            }

            @Override
            public void onNext(String t) {
                System.out.println("============> OnNext: " + t);
                count.incrementAndGet();
            }

        });

        if (!latch.await(800, TimeUnit.MILLISECONDS)) {
            System.out.println("xxxxxxxxxxxxxxxxxx> TIMED OUT <xxxxxxxxxxxxxxxxxxxx");
            System.out.println("Received count: " + count.get());
            fail("Timed Out");
        }

        System.out.println("Received count: " + count.get());
        assertEquals(20, count.get());
    }

    /**
     * The Parent and Inner must be unsubscribed, otherwise it will keep waiting for more.
     * 
     * Unsubscribing from pivot is not a very easy (or typical) thing ... but it sort of works.
     * 
     * It's NOT easy to understand though, and easy to end up with far more data consumed than expected, because pivot by definition
     * is inverting the data so we can not unsubscribe from the parent until all children are done since the top key becomes the leaf once pivoted.
     */
    @Test
    public void testUnsubscribeFromGroups() throws InterruptedException {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        Observable<GroupedObservable<Boolean, Integer>> o1 = getSource(2000, counter1).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<Boolean, Integer>> o2 = getSource(4000, counter2).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<String, GroupedObservable<Boolean, Integer>>> groups = Observable.from(GroupedObservable.from("o1", o1), GroupedObservable.from("o2", o2));
        Observable<GroupedObservable<Boolean, GroupedObservable<String, Integer>>> pivoted = Observable.pivot(groups);
        TestSubscriber<String> ts = new TestSubscriber<String>();
        pivoted.take(2).flatMap(new Func1<GroupedObservable<Boolean, GroupedObservable<String, Integer>>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Boolean, GroupedObservable<String, Integer>> outerGroup) {
                return outerGroup.flatMap(new Func1<GroupedObservable<String, Integer>, Observable<String>>() {

                    @Override
                    public Observable<String> call(final GroupedObservable<String, Integer> innerGroup) {
                        return innerGroup.take(10).map(new Func1<Integer, String>() {

                            @Override
                            public String call(Integer i) {
                                return (outerGroup.getKey() ? "Even" : "Odd ") + " => from source: " + innerGroup.getKey() + " Value: " + i;
                            }

                        });
                    }

                });
            }

        }).subscribe(ts);

        ts.awaitTerminalEvent();
        System.out.println("onNext [" + ts.getOnNextEvents().size() + "]: " + ts.getOnNextEvents());
        assertEquals(40, ts.getOnNextEvents().size()); // 2 (o1 + o2) + 2 (odd + even) * take(10) on each

        int c1 = counter1.get();
        int c2 = counter2.get();

        System.out.println("Counter1: " + c1);
        System.out.println("Counter2: " + c2);

        Thread.sleep(200);

        System.out.println("Counter1: " + counter1.get());
        System.out.println("Counter2: " + counter2.get());

        // after time it should be same if unsubscribed
        assertEquals(c1, counter1.get(), 1); // delta of 1 for race to unsubscribe
        assertEquals(c2, counter2.get(), 1); // delta of 1 for race to unsubscribe

        assertTrue(counter1.get() < 50000); // should be much smaller (< 1000) but this will be non-deterministic
        assertTrue(counter2.get() < 50000); // should be much smaller (< 1000) but this will be non-deterministic
    }

    private static Observable<Integer> getSource(final int start, final AtomicInteger counter) {
        return Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                for (int i = start; i < 1000000; i++) {
                    if (s.isUnsubscribed()) {
                        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> unsubscribed so quitting in source: " + start);
                        return;
                    }
                    counter.incrementAndGet();
                    s.onNext(i);
                    try {
                        // slow it down so it's not just emitting it all into a buffer (merge)
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                s.onCompleted();
            }

        });
    }

    final static Func1<Integer, Boolean> modKeySelector = new Func1<Integer, Boolean>() {

        @Override
        public Boolean call(Integer i) {
            return i % 2 == 0;
        }

    };
}
