/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

import org.mockito.Mockito;

public class ObservableGroupByTest {

    final Function<String, Integer> length = new Function<String, Integer>() {
        @Override
        public Integer apply(String s) {
            return s.length();
        }
    };

    @Test
    public void testGroupBy() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector() {
        Observable<String> source = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<GroupedObservable<Integer, Integer>> grouped = source.groupBy(length, length);

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
        Observable<GroupedObservable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testError() {
        Observable<String> sourceStrings = Observable.just("one", "two", "three", "four", "five", "six");
        Observable<String> errorSource = Observable.error(new RuntimeException("forced failure"));
        Observable<String> source = Observable.concat(sourceStrings, errorSource);

        Observable<GroupedObservable<Integer, String>> grouped = source.groupBy(length);

        final AtomicInteger groupCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        grouped.flatMap(new Function<GroupedObservable<Integer, String>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, String> o) {
                groupCounter.incrementAndGet();
                return o.map(new Function<String, String>() {

                    @Override
                    public String apply(String v) {
                        return "Event => key: " + o.getKey() + " value: " + v;
                    }
                });
            }
        }).subscribe(new DefaultObserver<String>() {

            @Override
            public void onComplete() {

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

        observable.blockingForEach(new Consumer<GroupedObservable<K, V>>() {

            @Override
            public void accept(final GroupedObservable<K, V> o) {
                result.put(o.getKey(), new ConcurrentLinkedQueue<V>());
                o.subscribe(new Consumer<V>() {

                    @Override
                    public void accept(V v) {
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
     * @throws Throwable some method may throw
     */
    @Test
    public void testGroupedEventStream() throws Throwable {

        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        final int groupCount = 2;

        Observable<Event> es = Observable.unsafeCreate(new ObservableSource<Event>() {

            @Override
            public void subscribe(final Observer<? super Event> observer) {
                observer.onSubscribe(Disposables.empty());
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
                        observer.onComplete();
                    }

                }).start();
            }

        });

        es.groupBy(new Function<Event, Integer>() {

            @Override
            public Integer apply(Event e) {
                return e.source;
            }
        }).flatMap(new Function<GroupedObservable<Integer, Event>, Observable<String>>() {

            @Override
            public Observable<String> apply(GroupedObservable<Integer, Event> eventGroupedObservable) {
                System.out.println("GroupedObservable Key: " + eventGroupedObservable.getKey());
                groupCounter.incrementAndGet();

                return eventGroupedObservable.map(new Function<Event, String>() {

                    @Override
                    public String apply(Event event) {
                        return "Source: " + event.source + "  Message: " + event.message;
                    }
                });

            }
        }).subscribe(new DefaultObserver<String>() {

            @Override
            public void onComplete() {
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

        es.groupBy(new Function<Event, Integer>() {

            @Override
            public Integer apply(Event e) {
                return e.source;
            }
        })
                .take(1) // we want only the first group
                .flatMap(new Function<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> apply(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        System.out.println("testUnsubscribe => GroupedObservable Key: " + eventGroupedObservable.getKey());
                        groupCounter.incrementAndGet();

                        return eventGroupedObservable
                                .take(20) // limit to only 20 events on this group
                                .map(new Function<Event, String>() {

                                    @Override
                                    public String apply(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                }).subscribe(new DefaultObserver<String>() {

                    @Override
                    public void onComplete() {
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
                .groupBy(new Function<Event, Integer>() {

                    @Override
                    public Integer apply(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Function<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> apply(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        return eventGroupedObservable
                                .map(new Function<Event, String>() {

                                    @Override
                                    public String apply(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .take(30).subscribe(new Consumer<String>() {

                    @Override
                    public void accept(String s) {
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
                .groupBy(new Function<Event, Integer>() {

                    @Override
                    public Integer apply(Event e) {
                        return e.source;
                    }
                })
                // take 2 of the 4 groups
                .take(2)
                .flatMap(new Function<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> apply(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        int numToTake = 0;
                        if (eventGroupedObservable.getKey() == 1) {
                            numToTake = 10;
                        } else if (eventGroupedObservable.getKey() == 2) {
                            numToTake = 5;
                        }
                        return eventGroupedObservable
                                .take(numToTake)
                                .map(new Function<Event, String>() {

                                    @Override
                                    public String apply(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .subscribe(new Consumer<String>() {

                    @Override
                    public void accept(String s) {
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
                .groupBy(new Function<Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i) {
                        return i % 2;
                    }
                })
                .flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> apply(GroupedObservable<Integer, Integer> group) {
                        if (group.getKey() == 0) {
                            return group.delay(100, TimeUnit.MILLISECONDS).map(new Function<Integer, Integer>() {
                                @Override
                                public Integer apply(Integer t) {
                                    return t * 10;
                                }

                            });
                        } else {
                            return group;
                        }
                    }
                })
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onComplete() {
                        System.out.println("=> onComplete");
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
                .groupBy(new Function<Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i) {
                        return i % 2;
                    }
                })
                .subscribe(new DefaultObserver<GroupedObservable<Integer, Integer>>() {

                    @Override
                    public void onComplete() {
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
                .groupBy(new Function<Event, Integer>() {

                    @Override
                    public Integer apply(Event e) {
                        return e.source;
                    }
                })
                .flatMap(new Function<GroupedObservable<Integer, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> apply(GroupedObservable<Integer, Event> eventGroupedObservable) {
                        Observable<Event> eventStream = eventGroupedObservable;
                        if (eventGroupedObservable.getKey() >= 2) {
                            // filter these
                            eventStream = eventGroupedObservable.filter(new Predicate<Event>() {
                                @Override
                                public boolean test(Event t1) {
                                    return false;
                                }
                            });
                        }

                        return eventStream
                                .map(new Function<Event, String>() {

                                    @Override
                                    public String apply(Event event) {
                                        return "testUnsubscribe => Source: " + event.source + "  Message: " + event.message;
                                    }
                                });

                    }
                })
                .take(30).subscribe(new Consumer<String>() {

                    @Override
                    public void accept(String s) {
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
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
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
                sub.onComplete();
            }

        }).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t) {
                return t;
            }

        }).flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Action() {

                                @Override
                                public void run() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String s) {
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
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
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
                sub.onComplete();
            }

        }).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t) {
                return t;
            }

        }).flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Action() {

                                @Override
                                public void run() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.subscribeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "last group: " + t1;
                        }

                    }).doOnEach(new Consumer<Notification<String>>() {

                        @Override
                        public void accept(Notification<String> t1) {
                            System.err.println("subscribeOn notification => " + t1);
                        }

                    });
                }
            }

        }).doOnEach(new Consumer<Notification<String>>() {

            @Override
            public void accept(Notification<String> t1) {
                System.err.println("outer notification => " + t1);
            }

        }).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String s) {
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
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
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
                sub.onComplete();
            }

        }).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t) {
                return t;
            }

        }).flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Action() {

                                @Override
                                public void run() {
                                    first.countDown();
                                }

                            });
                } else {
                    return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "last group: " + t1;
                        }

                    });
                }
            }

        }).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void testGroupsWithNestedSubscribeOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }

        }).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t) {
                return t;
            }

        }).flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, Integer> group) {
                return group.subscribeOn(Schedulers.newThread()).map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer t1) {
                        System.out.println("Received: " + t1 + " on group : " + group.getKey());
                        return "first groups: " + t1;
                    }

                });
            }

        }).doOnEach(new Consumer<Notification<String>>() {

            @Override
            public void accept(Notification<String> t1) {
                System.out.println("notification => " + t1);
            }

        }).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String s) {
                results.add(s);
            }

        });

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
    }

    @Test
    public void testGroupsWithNestedObserveOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<String>();
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> sub) {
                sub.onSubscribe(Disposables.empty());
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(1);
                sub.onNext(2);
                sub.onComplete();
            }

        }).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t) {
                return t;
            }

        }).flatMap(new Function<GroupedObservable<Integer, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Integer, Integer> group) {
                return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer t1) {
                        return "first groups: " + t1;
                    }

                });
            }

        }).blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String s) {
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
        return Observable.unsafeCreate(new ObservableSource<Event>() {

            @Override
            public void subscribe(final Observer<? super Event> op) {
                Disposable d = Disposables.empty();
                op.onSubscribe(d);
                subscribeCounter.incrementAndGet();
                int i = 0;
                while (!d.isDisposed()) {
                    i++;
                    Event e = new Event();
                    e.source = i % numGroups;
                    e.message = "Event-" + i;
                    op.onNext(e);
                    sentEventCounter.incrementAndGet();
                }
                op.onComplete();
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
        DefaultObserver<GroupedObservable<Boolean, Long>> o1 = mock(DefaultObserver.class);
        @SuppressWarnings("unchecked")
        DefaultObserver<GroupedObservable<Boolean, Long>> o2 = mock(DefaultObserver.class);

        // subscribe with the observers
        stream.subscribe(o1);
        stream.subscribe(o2);

        // check that subscriptions were successful
        verify(o1, never()).onError(Mockito.<Throwable> any());
        verify(o2, never()).onError(Mockito.<Throwable> any());
    }

    private static Function<Long, Boolean> IS_EVEN = new Function<Long, Boolean>() {

        @Override
        public Boolean apply(Long n) {
            return n % 2 == 0;
        }
    };

    private static Function<Integer, Boolean> IS_EVEN2 = new Function<Integer, Boolean>() {

        @Override
        public Boolean apply(Integer n) {
            return n % 2 == 0;
        }
    };

    @Test
    public void testGroupByBackpressure() throws InterruptedException {

        TestObserver<String> ts = new TestObserver<String>();

        Observable.range(1, 4000)
                .groupBy(IS_EVEN2)
                .flatMap(new Function<GroupedObservable<Boolean, Integer>, Observable<String>>() {

                    @Override
                    public Observable<String> apply(final GroupedObservable<Boolean, Integer> g) {
                        return g.observeOn(Schedulers.computation()).map(new Function<Integer, String>() {

                            @Override
                            public String apply(Integer l) {
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

    <T, R> Function<T, R> just(final R value) {
        return new Function<T, R>() {
            @Override
            public R apply(T t1) {
                return value;
            }
        };
    }

    <T> Function<Integer, T> fail(T dummy) {
        return new Function<Integer, T>() {
            @Override
            public T apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    <T, R> Function<T, R> fail2(R dummy2) {
        return new Function<T, R>() {
            @Override
            public R apply(T t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }

    Function<Integer, Integer> dbl = new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer t1) {
            return t1 * 2;
        }
    };
    Function<Integer, Integer> identity = new Function<Integer, Integer>() {
        @Override
        public Integer apply(Integer v) {
            return v;
        }
    };

    @Test
    public void normalBehavior() {
        Observable<String> source = Observable.fromIterable(Arrays.asList(
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

        /*
         * foo FoO foO FOO fOo
         * baR bar BAR
         * Baz baz bAZ
         * qux
         *
         */
        Function<String, String> keysel = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                return t1.trim().toLowerCase();
            }
        };
        Function<String, String> valuesel = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                return t1 + t1;
            }
        };

        Observable<String> m = source.groupBy(keysel, valuesel)
        .flatMap(new Function<GroupedObservable<String, String>, Observable<String>>() {
            @Override
            public Observable<String> apply(final GroupedObservable<String, String> g) {
                System.out.println("-----------> NEXT: " + g.getKey());
                return g.take(2).map(new Function<String, String>() {

                    int count;

                    @Override
                    public String apply(String v) {
                        System.out.println(v);
                        return g.getKey() + "-" + count++;
                    }

                });
            }
        });

        TestObserver<String> ts = new TestObserver<String>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        System.out.println("ts .get " + ts.values());
        ts.assertNoErrors();
        assertEquals(ts.values(),
                Arrays.asList("foo-0", "foo-1", "bar-0", "foo-0", "baz-0", "qux-0", "bar-1", "bar-0", "foo-1", "baz-1", "baz-0", "foo-0"));

    }

    @Test
    public void keySelectorThrows() {
        Observable<Integer> source = Observable.just(0, 1, 2, 3, 4, 5, 6);

        Observable<Integer> m = source.groupBy(fail(0), dbl).flatMap(FLATTEN_INTEGER);

        TestObserver<Integer> ts = new TestObserver<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();
    }

    @Test
    public void valueSelectorThrows() {
        Observable<Integer> source = Observable.just(0, 1, 2, 3, 4, 5, 6);

        Observable<Integer> m = source.groupBy(identity, fail(0)).flatMap(FLATTEN_INTEGER);
        TestObserver<Integer> ts = new TestObserver<Integer>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();

    }

    @Test
    public void innerEscapeCompleted() {
        Observable<Integer> source = Observable.just(0);

        Observable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestObserver<Object> ts = new TestObserver<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println(ts.values());
    }

    /**
     * Assert we get an IllegalStateException if trying to subscribe to an inner GroupedObservable more than once.
     */
    @Test
    public void testExceptionIfSubscribeToChildMoreThanOnce() {
        Observable<Integer> source = Observable.just(0);

        final AtomicReference<GroupedObservable<Integer, Integer>> inner = new AtomicReference<GroupedObservable<Integer, Integer>>();

        Observable<GroupedObservable<Integer, Integer>> m = source.groupBy(identity, dbl);

        m.subscribe(new Consumer<GroupedObservable<Integer, Integer>>() {
            @Override
            public void accept(GroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }
        });

        inner.get().subscribe();

        @SuppressWarnings("unchecked")
        DefaultObserver<Integer> o2 = mock(DefaultObserver.class);

        inner.get().subscribe(o2);

        verify(o2, never()).onComplete();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }

    @Test
    public void testError2() {
        Observable<Integer> source = Observable.concat(Observable.just(0),
                Observable.<Integer> error(new TestException("Forced failure")));

        Observable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestObserver<Object> ts = new TestObserver<Object>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertValueCount(1);
    }

    @Test
    public void testgroupByBackpressure() throws InterruptedException {
        TestObserver<String> ts = new TestObserver<String>();

        Observable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Function<GroupedObservable<Boolean, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Boolean, Integer> g) {
                return g.doOnComplete(new Action() {

                    @Override
                    public void run() {
                        System.out.println("//////////////////// COMPLETED-A");
                    }

                }).observeOn(Schedulers.computation()).map(new Function<Integer, String>() {

                    int c;

                    @Override
                    public String apply(Integer l) {
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

                }).doOnComplete(new Action() {

                    @Override
                    public void run() {
                        System.out.println("//////////////////// COMPLETED-B");
                    }

                });
            }

        }).doOnEach(new Consumer<Notification<String>>() {

            @Override
            public void accept(Notification<String> t1) {
                System.out.println("NEXT: " + t1);
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void testgroupByBackpressure2() throws InterruptedException {

        TestObserver<String> ts = new TestObserver<String>();

        Observable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Function<GroupedObservable<Boolean, Integer>, Observable<String>>() {

            @Override
            public Observable<String> apply(final GroupedObservable<Boolean, Integer> g) {
                return g.take(2).observeOn(Schedulers.computation()).map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer l) {
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

    static Function<GroupedObservable<Integer, Integer>, Observable<Integer>> FLATTEN_INTEGER = new Function<GroupedObservable<Integer, Integer>, Observable<Integer>>() {

        @Override
        public Observable<Integer> apply(GroupedObservable<Integer, Integer> t) {
            return t;
        }

    };

    @Test
    public void testGroupByWithNullKey() {
        final String[] key = new String[]{"uninitialized"};
        final List<String> values = new ArrayList<String>();
        Observable.just("a", "b", "c").groupBy(new Function<String, String>() {

            @Override
            public String apply(String value) {
                return null;
            }
        }).subscribe(new Consumer<GroupedObservable<String, String>>() {

            @Override
            public void accept(GroupedObservable<String, String> groupedObservable) {
                key[0] = groupedObservable.getKey();
                groupedObservable.subscribe(new Consumer<String>() {

                    @Override
                    public void accept(String s) {
                        values.add(s);
                    }
                });
            }
        });
        assertEquals(null, key[0]);
        assertEquals(Arrays.asList("a", "b", "c"), values);
    }

    @Test
    public void testGroupByUnsubscribe() {
        final Disposable s = mock(Disposable.class);
        Observable<Integer> o = Observable.unsafeCreate(
                new ObservableSource<Integer>() {
                    @Override
                    public void subscribe(Observer<? super Integer> observer) {
                        observer.onSubscribe(s);
                    }
                }
        );
        TestObserver<Object> ts = new TestObserver<Object>();

        o.groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer integer) {
                return null;
            }
        }).subscribe(ts);

        ts.dispose();

        verify(s).dispose();
    }

    @Test
    public void testGroupByShouldPropagateError() {
        final Throwable e = new RuntimeException("Oops");
        final TestObserver<Integer> inner1 = new TestObserver<Integer>();
        final TestObserver<Integer> inner2 = new TestObserver<Integer>();

        final TestObserver<GroupedObservable<Integer, Integer>> outer
                = new TestObserver<GroupedObservable<Integer, Integer>>(new DefaultObserver<GroupedObservable<Integer, Integer>>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(GroupedObservable<Integer, Integer> o) {
                if (o.getKey() == 0) {
                    o.subscribe(inner1);
                } else {
                    o.subscribe(inner2);
                }
            }
        });
        Observable.unsafeCreate(
                new ObservableSource<Integer>() {
                    @Override
                    public void subscribe(Observer<? super Integer> observer) {
                        observer.onSubscribe(Disposables.empty());
                        observer.onNext(0);
                        observer.onNext(1);
                        observer.onError(e);
                    }
                }
        ).groupBy(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i % 2;
            }
        }).subscribe(outer);
        assertEquals(Arrays.asList(e), outer.errors());
        assertEquals(Arrays.asList(e), inner1.errors());
        assertEquals(Arrays.asList(e), inner2.errors());
    }

    @Test
    public void keySelectorAndDelayError() {
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), true)
        .flatMap(new Function<GroupedObservable<Integer, Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(GroupedObservable<Integer, Integer> g) throws Exception {
                return g;
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void keyAndValueSelectorAndDelayError() {
        Observable.just(1).concatWith(Observable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), true)
        .flatMap(new Function<GroupedObservable<Integer, Integer>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(GroupedObservable<Integer, Integer> g) throws Exception {
                return g;
            }
        })
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).groupBy(Functions.justFunction(1)));

        Observable.just(1)
        .groupBy(Functions.justFunction(1))
        .doOnNext(new Consumer<GroupedObservable<Integer, Integer>>() {
            @Override
            public void accept(GroupedObservable<Integer, Integer> g) throws Exception {
                TestHelper.checkDisposed(g);
            }
        })
        .test();
    }

    @Test
    public void reentrantComplete() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onComplete();
                }
            }
        };

        Observable.merge(ps.groupBy(Functions.justFunction(1)))
        .subscribe(to);

        ps.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void reentrantCompleteCancel() {
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onComplete();
                    dispose();
                }
            }
        };

        Observable.merge(ps.groupBy(Functions.justFunction(1)))
        .subscribe(to);

        ps.onNext(1);

        to.assertSubscribed().assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void delayErrorSimpleComplete() {
        Observable.just(1)
        .groupBy(Functions.justFunction(1), true)
        .flatMap(Functions.<Observable<Integer>>identity())
        .test()
        .assertResult(1);
    }
}
