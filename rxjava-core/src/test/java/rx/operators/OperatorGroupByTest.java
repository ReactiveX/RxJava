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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Operator;
import rx.Subscription;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class OperatorGroupByTest {

    final Func1<String, Integer> length = new Func1<String, Integer>() {
        @Override
        public Integer call(String s) {
            return s.length();
        }
    };

    @Test
    public void testGroupBy() {
        Observable<String> source = Observable.from("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, String>> grouped = source.bind(new OperatorGroupBy<Integer, String>(length));

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void testEmpty() {
        Observable<String> source = Observable.empty();
        Observable<GroupedObservable<Integer, String>> grouped = source.bind(new OperatorGroupBy<Integer, String>(length));

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testError() {
        Observable<String> sourceStrings = Observable.from("one", "two", "three", "four", "five", "six");
        Observable<String> errorSource = Observable.error(new RuntimeException("forced failure"));
        Observable<String> source = Observable.concat(sourceStrings, errorSource);

        Observable<GroupedObservable<Integer, String>> grouped = source.bind(new OperatorGroupBy<Integer, String>(length));

        final AtomicInteger groupCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        grouped.flatMap(new Func1<GroupedObservable<Integer, String>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, String> o) {
                groupCounter.incrementAndGet();
                return o.map(new Func1<String, String>() {

                    @Override
                    public String call(String v) {
                        return "Event => key: " + o.getKey() + " value: " + v;
                    }
                });
            }
        }).subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                error.set(e);
            }

            @Override
            public void onNext(String v) {
                eventCounter.incrementAndGet();
                System.out.println(v);

            }
        });

        assertEquals(3, groupCounter.get());
        assertEquals(6, eventCounter.get());
        assertNotNull(error.get());
    }

    private static <K, V> Map<K, Collection<V>> toMap(Observable<GroupedObservable<K, V>> observable) {

        final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<K, Collection<V>>();

        observable.toBlockingObservable().forEach(new Action1<GroupedObservable<K, V>>() {

            @Override
            public void call(final GroupedObservable<K, V> o) {
                result.put(o.getKey(), new ConcurrentLinkedQueue<V>());
                o.subscribe(new Action1<V>() {

                    @Override
                    public void call(V v) {
                        result.get(o.getKey()).add(v);
                    }

                });
            }
        });

        return result;
    }

    /**
     * Assert that only a single subscription to a stream occurs and that all events are received.
     * 
     * @throws Throwable
     */
    @Test
    public void testGroupedEventStream() throws Throwable {

        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        final int groupCount = 2;

        Observable<Event> es = Observable.create(new Observable.OnSubscribeFunc<Event>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Event> observer) {
                System.out.println("*** Subscribing to EventStream ***");
                subscribeCounter.incrementAndGet();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < count; i++) {
                            Event e = new Event();
                            e.source = i % groupCount;
                            e.message = "Event-" + i;
                            observer.onNext(e);
                        }
                        observer.onCompleted();
                    }

                }).start();
                return Subscriptions.empty();
            }

        });

        es.groupBy(new Func1<Event, Integer>() {

            @Override
            public Integer call(Event e) {
                return e.source;
            }
        }).flatMap(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

            @Override
            public Observable<String> call(GroupedObservable<Integer, Event> eventGroupedObservable) {
                System.out.println("GroupedObservable Key: " + eventGroupedObservable.getKey());
                groupCounter.incrementAndGet();

                return eventGroupedObservable.map(new Func1<Event, String>() {

                    @Override
                    public String call(Event event) {
                        return "Source: " + event.source + "  Message: " + event.message;
                    }
                });

            }
        }).subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onNext(String outputMessage) {
                System.out.println(outputMessage);
                eventCounter.incrementAndGet();
            }
        });

        latch.await(5000, TimeUnit.MILLISECONDS);
        assertEquals(1, subscribeCounter.get());
        assertEquals(groupCount, groupCounter.get());
        assertEquals(count, eventCounter.get());

    }

    /*
     * We will only take 1 group with 20 events from it and then unsubscribe.
     */
    @Test
    public void testUnsubscribeOnNestedTakeAndSyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(SYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
    }

    /*
     * We will only take 1 group with 20 events from it and then unsubscribe.
     */
    @Test
    public void testUnsubscribeOnNestedTakeAndAsyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(ASYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
    }

    private void doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(Observable<Event> es, AtomicInteger subscribeCounter) throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        es.groupBy(new Func1<Event, Integer>() {

            @Override
            public Integer call(Event e) {
                return e.source;
            }
        })
                .take(1) // we want only the first group
                .flatMap(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> call(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        //                        System.out.println("testUnsubscribe => GroupedObservable Key: " + eventGroupedObservable.getKey());
                        groupCounter.incrementAndGet();

                        return eventGroupedObservable
                                .take(20) // limit to only 20 events on this group
                                .map(new Func1<Event, String>() {

                                    @Override
                                    public String call(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                }).subscribe(new Observer<String>() {

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(String outputMessage) {
                        System.out.println(outputMessage);
                        eventCounter.incrementAndGet();
                    }
                });

        if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
            fail("timed out so likely did not unsubscribe correctly");
        }
        assertEquals(1, subscribeCounter.get());
        assertEquals(1, groupCounter.get());
        assertEquals(20, eventCounter.get());
        // sentEvents will go until 'eventCounter' hits 20 and then unsubscribes
        // which means it will also send (but ignore) the 19/20 events for the other group
        // It will not however send all 100 events.
    }

    @Test
    public void testUnsubscribeViaTakeOnGroupThenMergeAndTake() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Func1<Event, Integer>() {

                    @Override
                    public Integer call(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> call(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        return eventGroupedObservable
                                .map(new Func1<Event, String>() {

                                    @Override
                                    public String call(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .take(30).subscribe(new Action1<String>() {

                    @Override
                    public void call(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }

                });

        assertEquals(30, eventCounter.get());
        // we should send 28 additional events that are filtered out as they are in the groups we skip
        assertEquals(58, sentEventCounter.get());
    }

    @Test
    public void testUnsubscribeViaTakeOnGroupThenTakeOnInner() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Func1<Event, Integer>() {

                    @Override
                    public Integer call(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> call(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        int numToTake = 0;
                        if (eventGroupedObservable.getKey() == 1) {
                            numToTake = 10;
                        } else if (eventGroupedObservable.getKey() == 2) {
                            numToTake = 5;
                        }
                        return eventGroupedObservable
                                .take(numToTake)
                                .map(new Func1<Event, String>() {

                                    @Override
                                    public String call(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .subscribe(new Action1<String>() {

                    @Override
                    public void call(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }

                });

        assertEquals(15, eventCounter.get());
        // we should send 22 additional events that are filtered out as they are skipped while taking the 15 we want
        assertEquals(37, sentEventCounter.get());
    }

    @Test
    public void testUnsubscribeOnGroupViaOnlyTakeOnInner() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(new Func1<Event, Integer>() {

                    @Override
                    public Integer call(Event e) {
                        return e.source;
                    }
                })
                .flatMap(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> call(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        int numToTake = 0;
                        if (eventGroupedObservable.getKey() == 1) {
                            numToTake = 10;
                        } else if (eventGroupedObservable.getKey() == 2) {
                            numToTake = 5;
                        }
                        return eventGroupedObservable
                                .take(numToTake)
                                .map(new Func1<Event, String>() {

                                    @Override
                                    public String call(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .subscribe(new Action1<String>() {

                    @Override
                    public void call(String s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }

                });

        assertEquals(15, eventCounter.get());
        // we should send 22 additional events that are filtered out as they are skipped while taking the 15 we want
        assertEquals(37, sentEventCounter.get());
    }

    @Test
    public void testStaggeredCompletion() throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        Observable.range(0, 100)
                .groupBy(new Func1<Integer, Integer>() {

                    @Override
                    public Integer call(Integer i) {
                        return i % 2;
                    }
                })
                .flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> call(GroupedObservable<Integer, Integer> group) {
                        if (group.getKey() == 0) {
                            return group.observeOn(Schedulers.newThread()).map(new Func1<Integer, Integer>() {

                                @Override
                                public Integer call(Integer t) {
                                    try {
                                        Thread.sleep(2);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    return t * 10;
                                }

                            });
                        } else {
                            return group.observeOn(Schedulers.newThread());
                        }
                    }
                })
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onCompleted() {
                        System.out.println("=> onCompleted");
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(Integer s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }
                });

        if (!latch.await(2000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(100, eventCounter.get());
    }

    @Test
    public void testCompletionIfInnerNotSubscribed() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger eventCounter = new AtomicInteger();
        Observable.range(0, 100)
                .groupBy(new Func1<Integer, Integer>() {

                    @Override
                    public Integer call(Integer i) {
                        return i % 2;
                    }
                })
                .subscribe(new Observer<GroupedObservable<Integer, Integer>>() {

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onNext(GroupedObservable<Integer, Integer> s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }
                });
        if (!latch.await(500, TimeUnit.MILLISECONDS)) {
            fail("timed out - never got completion");
        }
        assertEquals(2, eventCounter.get());
    }

    private static class Event {
        int source;
        String message;

        @Override
        public String toString() {
            return "Event => source: " + source + " message: " + message;
        }
    }

    Observable<Event> ASYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return SYNC_INFINITE_OBSERVABLE_OF_EVENT(numGroups, subscribeCounter, sentEventCounter).subscribeOn(Schedulers.newThread());
    };

    Observable<Event> SYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return Observable.create(new Action1<Operator<? super Event>>() {

            @Override
            public void call(final Operator<? super Event> op) {
                subscribeCounter.incrementAndGet();
                int i = 0;
                while (!op.isUnsubscribed()) {
                    i++;
                    Event e = new Event();
                    e.source = i % numGroups;
                    e.message = "Event-" + i;
                    op.onNext(e);
                    sentEventCounter.incrementAndGet();
                }
                op.onCompleted();
            }

        });
    };

}
