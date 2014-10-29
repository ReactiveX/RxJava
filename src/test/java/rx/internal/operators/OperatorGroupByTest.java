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
package rx.internal.operators;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observables.GroupedObservable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorGroupByTest {

    final Func1<String, Integer> length = new Func1<String, Integer>() {
        @Override
        public Integer call(String s) {
            return s.length();
        }
    };

    @Test
    public void testGroupBy() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, String>> grouped = source.lift(new OperatorGroupBy<String, Integer, String>(length));

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, Integer>> grouped = source.lift(new OperatorGroupBy<String, Integer, Integer>(length, length));

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector2() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testEmpty() {
        Observable<String> source = Observable.empty();
        Observable<GroupedObservable<Integer, String>> grouped = source.lift(new OperatorGroupBy<String, Integer, String>(length));

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testError() {
        Observable<String> sourceStrings = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<String> errorSource = Observable.error(new RuntimeException("forced failure"));
        Observable<String> source = Observable.concat(sourceStrings, errorSource);

        Observable<GroupedObservable<Integer, String>> grouped = source.lift(new OperatorGroupBy<String, Integer, String>(length));

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
        }).subscribe(new Subscriber<String>() {

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

        observable.toBlocking().forEach(new Action1<GroupedObservable<K, V>>() {

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

        Observable<Event> es = Observable.create(new Observable.OnSubscribe<Event>() {

            @Override
            public void call(final Subscriber<? super Event> observer) {
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
        }).subscribe(new Subscriber<String>() {

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
        Thread.sleep(500);
        assertEquals(39, sentEventCounter.get());
    }

    /*
     * We will only take 1 group with 20 events from it and then unsubscribe.
     */
    @Test
    public void testUnsubscribeOnNestedTakeAndAsyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(ASYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
        Thread.sleep(500);
        assertEquals(39, sentEventCounter.get());
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
                        System.out.println("testUnsubscribe => GroupedObservable Key: " + eventGroupedObservable.getKey());
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
                }).subscribe(new Subscriber<String>() {

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
                            return group.delay(100, TimeUnit.MILLISECONDS).map(new Func1<Integer, Integer>() {
                                @Override
                                public Integer call(Integer t) {
                                    return t * 10;
                                }

                            });
                        } else {
                            return group;
                        }
                    }
                })
                .subscribe(new Subscriber<Integer>() {

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

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            fail("timed out");
        }

        assertEquals(100, eventCounter.get());
    }

    @Test(timeout = 1000)
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
                .subscribe(new Subscriber<GroupedObservable<Integer, Integer>>() {

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

    @Test
    public void testIgnoringGroups() {
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
                        Observable<Event> eventStream = eventGroupedObservable;
                        if (eventGroupedObservable.getKey() >= 2) {
                            // filter these
                            eventStream = eventGroupedObservable.filter(new Func1<Event, Boolean>() {

                                @Override
                                public Boolean call(Event t1) {
                                    return false;
                                }

                            });
                        }

                        return eventStream
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
        // we should send 30 additional events that are filtered out as they are in the groups we skip
        assertEquals(60, sentEventCounter.get());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsAndThenComplete() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onCompleted();
            }

        }).groupBy(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t) {
                return t;
            }

        }).flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnCompleted(new Action0() {

                                @Override
                                public void call() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenSubscribesOnAndDelaysAndThenCompletes() throws InterruptedException {
        System.err.println("----------------------------------------------------------------------------------------------");
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onCompleted();
            }

        }).groupBy(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t) {
                return t;
            }

        }).flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnCompleted(new Action0() {

                                @Override
                                public void call() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.subscribeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "last group: " + t1;
                        }

                    }).doOnEach(new Action1<Notification<? super String>>() {

                        @Override
                        public void call(Notification<? super String> t1) {
                            System.err.println("subscribeOn notification => " + t1);
                        }

                    });
                }
            }

        }).doOnEach(new Action1<Notification<? super String>>() {

            @Override
            public void call(Notification<? super String> t1) {
                System.err.println("outer notification => " + t1);
            }

        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testFirstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenObservesOnAndDelaysAndThenCompletes() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<String>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                try {
                    first.await();
                } catch (InterruptedException e) {
                    sub.onError(e);
                    return;
                }
                sub.onNext(3);
                sub.onNext(3);
                sub.onCompleted();
            }

        }).groupBy(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t) {
                return t;
            }

        }).flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnCompleted(new Action0() {

                                @Override
                                public void call() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testGroupsWithNestedSubscribeOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onCompleted();
            }

        }).groupBy(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t) {
                return t;
            }

        }).flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, Integer> group) {
                return group.subscribeOn(Schedulers.newThread()).map(new Func1<Integer, String>() {

                    @Override
                    public String call(Integer t1) {
                        System.out.println("Received: " + t1 + " on group : " + group.getKey());
                        return "first groups: " + t1;
                    }

                });
            }

        }).doOnEach(new Action1<Notification<? super String>>() {

            @Override
            public void call(Notification<? super String> t1) {
                System.out.println("notification => " + t1);
            }

        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
    }

    @Test
    public void testGroupsWithNestedObserveOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> sub) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onCompleted();
            }

        }).groupBy(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t) {
                return t;
            }

        }).flatMap(new Func1<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Integer, Integer> group) {
                return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Func1<Integer, String>() {

                    @Override
                    public String call(Integer t1) {
                        return "first groups: " + t1;
                    }

                });
            }

        }).toBlocking().forEach(new Action1<String>() {

            @Override
            public void call(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
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
        return Observable.create(new OnSubscribe<Event>() {

            @Override
            public void call(final Subscriber<? super Event> op) {
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

    @Test
    public void testGroupByOnAsynchronousSourceAcceptsMultipleSubscriptions() throws InterruptedException {

        // choose an asynchronous source
        Observable<Long> source = Observable.interval(10, TimeUnit.MILLISECONDS).take(1);

        // apply groupBy to the source
        Observable<GroupedObservable<Boolean, Long>> stream = source.groupBy(IS_EVEN);

        // create two observers
        @SuppressWarnings("unchecked")
        Observer<GroupedObservable<Boolean, Long>> o1 = mock(Observer.class);
        @SuppressWarnings("unchecked")
        Observer<GroupedObservable<Boolean, Long>> o2 = mock(Observer.class);

        // subscribe with the observers
        stream.subscribe(o1);
        stream.subscribe(o2);

        // check that subscriptions were successful
        verify(o1, never()).onError(Matchers.<Throwable> any());
        verify(o2, never()).onError(Matchers.<Throwable> any());
    }

    private static Func1<Long, Boolean> IS_EVEN = new Func1<Long, Boolean>() {

        @Override
        public Boolean call(Long n) {
            return n % 2 == 0;
        }
    };

    private static Func1<Integer, Boolean> IS_EVEN2 = new Func1<Integer, Boolean>() {

        @Override
        public Boolean call(Integer n) {
            return n % 2 == 0;
        }
    };

    @Test
    public void testGroupByBackpressure() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable.range(1, 4000)
                .groupBy(IS_EVEN2)
                .flatMap(new Func1<GroupedObservable<Boolean, Integer>, Observable<String>>() {

                    @Override
                    public Observable<String> call(final GroupedObservable<Boolean, Integer> g) {
                        return g.observeOn(Schedulers.computation()).map(new Func1<Integer, String>() {

                            @Override
                            public String call(Integer l) {
                                if (g.getKey()) {
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                    }
                                    return l + " is even.";
                                } else {
                                    return l + " is odd.";
                                }
                            }

                        });
                    }

                }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    <T, R> Func1<T, R> just(final R value) {
        return new Func1<T, R>() {
            @Override
            public R call(T t1) {
                return value;
            }
        };
    }

    <T> Func1<Integer, T> fail(T dummy) {
        return new Func1<Integer, T>() {
            @Override
            public T call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    <T, R> Func1<T, R> fail2(R dummy2) {
        return new Func1<T, R>() {
            @Override
            public R call(T t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer t1) {
            return t1 * 2;
        }
    };
    Func1<Integer, Integer> identity = UtilityFunctions.identity();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void normalBehavior() {
        Observable<String> source = Observable.from(Arrays.asList(
                "  foo",
                " FoO ",
                "baR  ",
                "foO ",
                " Baz   ",
                "  qux ",
                "   bar",
                " BAR  ",
                "FOO ",
                "baz  ",
                " bAZ ",
                "    fOo    "
                ));

        /**
         * foo FoO foO FOO fOo
         * baR bar BAR
         * Baz baz bAZ
         * qux
         * 
         */
        Func1<String, String> keysel = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                return t1.trim().toLowerCase();
            }
        };
        Func1<String, String> valuesel = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                return t1 + t1;
            }
        };

        Observable<String> m = source.groupBy(
                keysel, valuesel).flatMap(new Func1<GroupedObservable<String, String>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<String, String> g) {
                System.out.println("-----------> NEXT: " + g.getKey());
                return g.take(2).map(new Func1<String, String>() {

                    int count = 0;

                    @Override
                    public String call(String v) {
                        return g.getKey() + "-" + count++;
                    }

                });
            }

        });

        TestSubscriber<String> ts = new TestSubscriber<String>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        System.out.println("ts .get " + ts.getOnNextEvents());
        ts.assertNoErrors();
        assertEquals(ts.getOnNextEvents(),
                Arrays.asList("foo-0", "foo-1", "bar-0", "foo-0", "baz-0", "qux-0", "bar-1", "bar-0", "foo-1", "baz-1", "baz-0", "foo-0"));

    }

    @Test
    public void keySelectorThrows() {
        Observable<Integer> source = Observable.just(0, 1, 2, 3, 4, 5, 6);

        Observable<Integer> m = source.groupBy(fail(0), dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals(0, ts.getOnNextEvents().size());
    }

    @Test
    public void valueSelectorThrows() {
        Observable<Integer> source = Observable.just(0, 1, 2, 3, 4, 5, 6);

        Observable<Integer> m = source.groupBy(identity, fail(0)).flatMap(FLATTEN_INTEGER);
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals(0, ts.getOnNextEvents().size());

    }

    @Test
    public void innerEscapeCompleted() {
        Observable<Integer> source = Observable.just(0);

        Observable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println(ts.getOnNextEvents());
    }

    /**
     * Assert we get an IllegalStateException if trying to subscribe to an inner GroupedObservable more than once
     */
    @Test
    public void testExceptionIfSubscribeToChildMoreThanOnce() {
        Observable<Integer> source = Observable.just(0);

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Observable<GroupedObservable<Integer, Integer>> m = source.groupBy(identity, dbl);

        m.subscribe(new Action1<GroupedObservable<Integer, Integer>>() {
            @Override
            public void call(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }
        });

        inner.get().subscribe();

        @SuppressWarnings("unchecked")
        Observer<Integer> o2 = mock(Observer.class);

        inner.get().subscribe(o2);

        verify(o2, never()).onCompleted();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }

    @Test
    public void testError2() {
        Observable<Integer> source = Observable.concat(Observable.just(0),
                Observable.<Integer> error(new TestException("Forced failure")));

        Observable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.getOnErrorEvents().size());
        assertEquals(1, ts.getOnNextEvents().size());
    }

    @Test
    public void testgroupByBackpressure() throws InterruptedException {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Func1<GroupedObservable<Boolean, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Boolean, Integer> g) {
                return g.doOnCompleted(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("//////////////////// COMPLETED-A");
                    }

                }).observeOn(Schedulers.computation()).map(new Func1<Integer, String>() {

                    int c = 0;

                    @Override
                    public String call(Integer l) {
                        if (g.getKey()) {
                            if (c++ < 400) {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                }
                            }
                            return l + " is even.";
                        } else {
                            return l + " is odd.";
                        }
                    }

                }).doOnCompleted(new Action0() {

                    @Override
                    public void call() {
                        System.out.println("//////////////////// COMPLETED-B");
                    }

                });
            }

        }).doOnEach(new Action1<Notification<? super String>>() {

            @Override
            public void call(Notification<? super String> t1) {
                System.out.println("NEXT: " + t1);
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void testgroupByBackpressure2() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Func1<GroupedObservable<Boolean, Integer>, Observable<String>>() {

            @Override
            public Observable<String> call(final GroupedObservable<Boolean, Integer> g) {
                return g.take(2).observeOn(Schedulers.computation()).map(new Func1<Integer, String>() {

                    @Override
                    public String call(Integer l) {
                        if (g.getKey()) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                            }
                            return l + " is even.";
                        } else {
                            return l + " is odd.";
                        }
                    }

                });
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    static Func1<GroupedObservable<Integer, Integer>, Observable<Integer>> FLATTEN_INTEGER = new Func1<GroupedObservable<Integer, Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> call(GroupedObservable<Integer, Integer> t) {
            return t;
        }

    };

}