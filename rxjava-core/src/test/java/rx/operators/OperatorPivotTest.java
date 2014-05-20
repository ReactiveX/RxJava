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

import java.util.Random;
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
                                return (outerGroup.getKey() ? "Even" : "Odd ") + " => from source: " + outerGroup.getKey() + "." + innerGroup.getKey() + " Value: " + i + " Thread: " + Thread.currentThread();
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

    /**
     * The pivot operator does not need to add any serialization but this is confirming the expected behavior.
     * 
     * It does not need serializing as it never merges groups, it just re-arranges them.
     * 
     * For example, a simple 2-stream case with odd/even:
     * 
     * Observable<GroupedObservable<Boolean, Integer>> o1 = Observable.range(1, 10).groupBy(modKeySelector).subscribeOn(Schedulers.newThread()); // thread 1
     * Observable<GroupedObservable<Boolean, Integer>> o2 = Observable.range(11, 10).groupBy(modKeySelector).subscribeOn(Schedulers.newThread()); // thread 2
     * Observable<GroupedObservable<String, GroupedObservable<Boolean, Integer>>> groups = Observable.from(GroupedObservable.from("o1", o1), GroupedObservable.from("o2", o2));
     * Observable<GroupedObservable<Boolean, GroupedObservable<String, Integer>>> pivoted = Observable.pivot(groups);
     * 
     * ============> OnNext: Odd => from source: false.o1 Value: 1 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o2 Value: 12 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Even => from source: true.o1 Value: 2 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Odd => from source: false.o2 Value: 11 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Odd => from source: false.o2 Value: 13 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Odd => from source: false.o2 Value: 15 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Odd => from source: false.o2 Value: 17 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Even => from source: true.o2 Value: 14 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Odd => from source: false.o1 Value: 3 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Odd => from source: false.o1 Value: 5 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Odd => from source: false.o1 Value: 7 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Odd => from source: false.o1 Value: 9 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o2 Value: 16 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Even => from source: true.o2 Value: 18 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Odd => from source: false.o2 Value: 19 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnNext: Even => from source: true.o1 Value: 4 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o1 Value: 6 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o1 Value: 8 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o1 Value: 10 Thread: Thread[RxNewThreadScheduler-1,5,main]
     * ============> OnNext: Even => from source: true.o2 Value: 20 Thread: Thread[RxNewThreadScheduler-2,5,main]
     * ============> OnCompleted
     * 
     * This starts as:
     * 
     * => Observable<GroupedObservable<String, GroupedObservable<Boolean, Integer>>>:
     * 
     *  o1.odd:  1,  3,  5,  7,  9 on Thread 1
     * o1.even:  2,  4,  6,  8, 10 on Thread 1
     *  o2.odd: 11, 13, 15, 17, 19 on Thread 2
     * o2.even: 12, 14, 16, 18, 20 on Thread 2
     * 
     * It pivots to become: 
     * 
     * => Observable<GroupedObservable<Boolean, GroupedObservable<String, Integer>>>:
     * 
     *  odd.o1:  1,  3,  5,  7,  9 on Thread 1
     *  odd.o2: 11, 13, 15, 17, 19 on Thread 2
     * even.o1:  2,  4,  6,  8, 10 on Thread 1
     * even.o2: 12, 14, 16, 18, 20 on Thread 2
     * 
     * Then a subsequent step can merge them if desired and add serialization, such as merge(even.o1, even.o2) to become a serialized "even"
     */
    @Test
    public void testConcurrencyAndSerialization() throws InterruptedException {
        final AtomicInteger maxOuterConcurrency = new AtomicInteger();
        final AtomicInteger maxGroupConcurrency = new AtomicInteger();
        Observable<GroupedObservable<Boolean, Integer>> o1 = getSource(2000).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<Boolean, Integer>> o2 = getSource(4000).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<Boolean, Integer>> o3 = getSource(6000).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<Boolean, Integer>> o4 = getSource(8000).subscribeOn(Schedulers.newThread()).groupBy(modKeySelector);
        Observable<GroupedObservable<String, GroupedObservable<Boolean, Integer>>> groups = Observable.from(GroupedObservable.from("o1", o1), GroupedObservable.from("o2", o2),
                GroupedObservable.from("o3", o3), GroupedObservable.from("o4", o4));
        Observable<GroupedObservable<Boolean, GroupedObservable<String, Integer>>> pivoted = Observable.pivot(groups);
        TestSubscriber<String> ts = new TestSubscriber<String>();
        pivoted.take(2).flatMap(new Func1<GroupedObservable<Boolean, GroupedObservable<String, Integer>>, Observable<String>>() {

            final AtomicInteger outerThreads = new AtomicInteger();

            @Override
            public Observable<String> call(final GroupedObservable<Boolean, GroupedObservable<String, Integer>> outerGroup) {
                return outerGroup.flatMap(new Func1<GroupedObservable<String, Integer>, Observable<String>>() {
                    @Override
                    public Observable<String> call(final GroupedObservable<String, Integer> innerGroup) {
                        final AtomicInteger threadsPerGroup = new AtomicInteger();
                        return innerGroup.take(100).map(new Func1<Integer, String>() {
                            final ThreadLocal<Random> tlr = new ThreadLocal<Random>() {
                                @Override
                                protected Random initialValue() {
                                    return new Random();
                                }
                            };
                            @Override
                            public String call(Integer i) {
                                int outerThreadCount = outerThreads.incrementAndGet();
                                setMaxConcurrency(maxOuterConcurrency, outerThreadCount);
                                int innerThreadCount = threadsPerGroup.incrementAndGet();
                                setMaxConcurrency(maxGroupConcurrency, innerThreadCount);
                                if (innerThreadCount > 1) {
                                    System.err.println("more than 1 thread for this group [" + innerGroup.getKey() + "]: " + innerThreadCount + " (before)");
                                    throw new RuntimeException("more than 1 thread for this group [" + innerGroup.getKey() + "]: " + innerThreadCount + " (before)");
                                }
                                try {
                                    // give the other threads a shot.
                                    Thread.sleep(tlr.get().nextInt(10) + 1);
                                    return (outerGroup.getKey() ? "Even" : "Odd ") + " => from source: " + innerGroup.getKey() + " Value: " + i;
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException("Interrupted [" + innerGroup.getKey() + "]: " + i);
                                } finally {
                                    int outerThreadCountAfter = outerThreads.decrementAndGet();
                                    setMaxConcurrency(maxOuterConcurrency, outerThreadCountAfter);
                                    int innerThreadCountAfter = threadsPerGroup.decrementAndGet();
                                    setMaxConcurrency(maxGroupConcurrency, innerThreadCountAfter);
                                    if (innerThreadCountAfter > 0) {
                                        System.err.println("more than 1 thread for this group [" + innerGroup.getKey() + "]: " + innerThreadCount + " (after)");
                                        throw new RuntimeException("more than 1 thread for this group [" + innerGroup.getKey() + "]: " + innerThreadCountAfter + " (after)");
                                    }
                                }
                            }

                            private void setMaxConcurrency(final AtomicInteger maxOuterConcurrency, int outerThreadCount) {
                                int max = maxOuterConcurrency.get();
                                if (outerThreadCount > max) {
                                    maxOuterConcurrency.compareAndSet(max, outerThreadCount);
                                }
                            }

                        });
                    }

                });
            }

        }).subscribe(ts);

        ts.awaitTerminalEvent();

        System.out.println("onNext [" + ts.getOnNextEvents().size() + "]: " + ts.getOnNextEvents());
        if (Runtime.getRuntime().availableProcessors() >= 4) {
            System.out.println("max outer concurrency: " + maxOuterConcurrency.get());
            assertTrue(maxOuterConcurrency.get() > 1); // should be 4 since we have 4 threads and cores running but setting at just > 1 as this is non-deterministic
        }
        System.out.println("max group concurrency: " + maxGroupConcurrency.get());
        assertTrue(maxGroupConcurrency.get() == 1); // should always be 1

        assertEquals(800, ts.getOnNextEvents().size());

    }

    private static Observable<Integer> getSource(final int start) {
        return getSource(start, new AtomicInteger());
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
