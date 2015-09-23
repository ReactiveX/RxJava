/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.Test;
import org.mockito.Matchers;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observables.nbp.NbpGroupedObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorGroupByTest {

    final Function<String, Integer> length = String::length;

    @Test
    public void testGroupBy() {
        NbpObservable<String> source = NbpObservable.just("one", "two", "three", "four", "five", "six");
        NbpObservable<NbpGroupedObservable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector() {
        NbpObservable<String> source = NbpObservable.just("one", "two", "three", "four", "five", "six");
        NbpObservable<NbpGroupedObservable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testGroupByWithElementSelector2() {
        NbpObservable<String> source = NbpObservable.just("one", "two", "three", "four", "five", "six");
        NbpObservable<NbpGroupedObservable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Arrays.asList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void testEmpty() {
        NbpObservable<String> source = NbpObservable.empty();
        NbpObservable<NbpGroupedObservable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    public void testError() {
        NbpObservable<String> sourceStrings = NbpObservable.just("one", "two", "three", "four", "five", "six");
        NbpObservable<String> errorSource = NbpObservable.error(new RuntimeException("forced failure"));
        NbpObservable<String> source = NbpObservable.concat(sourceStrings, errorSource);

        NbpObservable<NbpGroupedObservable<Integer, String>> grouped = source.groupBy(length);

        final AtomicInteger groupCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        grouped.flatMap(new Function<NbpGroupedObservable<Integer, String>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, String> o) {
                groupCounter.incrementAndGet();
                return o.map(new Function<String, String>() {

                    @Override
                    public String apply(String v) {
                        return "Event => key: " + o.getKey() + " value: " + v;
                    }
                });
            }
        }).subscribe(new NbpObserver<String>() {

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

    private static <K, V> Map<K, Collection<V>> toMap(NbpObservable<NbpGroupedObservable<K, V>> NbpObservable) {

        final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<>();

        NbpObservable.toBlocking().forEach(new Consumer<NbpGroupedObservable<K, V>>() {

            @Override
            public void accept(final NbpGroupedObservable<K, V> o) {
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

        NbpObservable<Event> es = NbpObservable.create(new NbpOnSubscribe<Event>() {

            @Override
            public void accept(final NbpSubscriber<? super Event> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                System.out.println("*** Subscribing to EventStream ***");
                subscribeCounter.incrementAndGet();
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < count; i++) {
                            Event e = new Event();
                            e.source = i % groupCount;
                            e.message = "Event-" + i;
                            NbpObserver.onNext(e);
                        }
                        NbpObserver.onComplete();
                    }

                }).start();
            }

        });

        es.groupBy(new Function<Event, Integer>() {

            @Override
            public Integer apply(Event e) {
                return e.source;
            }
        }).flatMap(new Function<NbpGroupedObservable<Integer, Event>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(NbpGroupedObservable<Integer, Event> eventGroupedObservable) {
                System.out.println("NbpGroupedObservable Key: " + eventGroupedObservable.getKey());
                groupCounter.incrementAndGet();

                return eventGroupedObservable.map(new Function<Event, String>() {

                    @Override
                    public String apply(Event event) {
                        return "Source: " + event.source + "  Message: " + event.message;
                    }
                });

            }
        }).subscribe(new NbpObserver<String>() {

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

    private void doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(NbpObservable<Event> es, AtomicInteger subscribeCounter) throws InterruptedException {
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
                .flatMap(new Function<NbpGroupedObservable<Integer, Event>, NbpObservable<String>>() {

                    @Override
                    public NbpObservable<String> apply(NbpGroupedObservable<Integer, Event> eventGroupedObservable) {
                        System.out.println("testUnsubscribe => NbpGroupedObservable Key: " + eventGroupedObservable.getKey());
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
                }).subscribe(new NbpObserver<String>() {

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
                .flatMap(new Function<NbpGroupedObservable<Integer, Event>, NbpObservable<String>>() {

                    @Override
                    public NbpObservable<String> apply(NbpGroupedObservable<Integer, Event> eventGroupedObservable) {
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
                .flatMap(new Function<NbpGroupedObservable<Integer, Event>, NbpObservable<String>>() {

                    @Override
                    public NbpObservable<String> apply(NbpGroupedObservable<Integer, Event> eventGroupedObservable) {
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
        NbpObservable.range(0, 100)
                .groupBy(new Function<Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i) {
                        return i % 2;
                    }
                })
                .flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<Integer>>() {

                    @Override
                    public NbpObservable<Integer> apply(NbpGroupedObservable<Integer, Integer> group) {
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
                .subscribe(new NbpObserver<Integer>() {

                    @Override
                    public void onComplete() {
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
        NbpObservable.range(0, 100)
                .groupBy(new Function<Integer, Integer>() {

                    @Override
                    public Integer apply(Integer i) {
                        return i % 2;
                    }
                })
                .subscribe(new NbpObserver<NbpGroupedObservable<Integer, Integer>>() {

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
                    public void onNext(NbpGroupedObservable<Integer, Integer> s) {
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
                .flatMap(new Function<NbpGroupedObservable<Integer, Event>, NbpObservable<String>>() {

                    @Override
                    public NbpObservable<String> apply(NbpGroupedObservable<Integer, Event> eventGroupedObservable) {
                        NbpObservable<Event> eventStream = eventGroupedObservable;
                        if (eventGroupedObservable.getKey() >= 2) {
                            // filter these
                            eventStream = eventGroupedObservable.filter(t1 -> false);
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
        final ArrayList<String> results = new ArrayList<>();
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> sub) {
                sub.onSubscribe(EmptyDisposable.INSTANCE);
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

        }).flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Runnable() {

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

        }).toBlocking().forEach(new Consumer<String>() {

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
        final ArrayList<String> results = new ArrayList<>();
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> sub) {
                sub.onSubscribe(EmptyDisposable.INSTANCE);
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

        }).flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Runnable() {

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

                    }).doOnEach(new Consumer<Try<Optional<String>>>() {

                        @Override
                        public void accept(Try<Optional<String>> t1) {
                            System.err.println("subscribeOn notification => " + t1);
                        }

                    });
                }
            }

        }).doOnEach(new Consumer<Try<Optional<String>>>() {

            @Override
            public void accept(Try<Optional<String>> t1) {
                System.err.println("outer notification => " + t1);
            }

        }).toBlocking().forEach(new Consumer<String>() {

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
        final ArrayList<String> results = new ArrayList<>();
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> sub) {
                sub.onSubscribe(EmptyDisposable.INSTANCE);
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

        }).flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, Integer> group) {
                if (group.getKey() < 3) {
                    return group.map(new Function<Integer, String>() {

                        @Override
                        public String apply(Integer t1) {
                            return "first groups: " + t1;
                        }

                    })
                            // must take(2) so an onCompleted + unsubscribe happens on these first 2 groups
                            .take(2).doOnComplete(new Runnable() {

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

        }).toBlocking().forEach(new Consumer<String>() {

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
        final ArrayList<String> results = new ArrayList<>();
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> sub) {
                sub.onSubscribe(EmptyDisposable.INSTANCE);
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

        }).flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, Integer> group) {
                return group.subscribeOn(Schedulers.newThread()).map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer t1) {
                        System.out.println("Received: " + t1 + " on group : " + group.getKey());
                        return "first groups: " + t1;
                    }

                });
            }

        }).doOnEach(new Consumer<Try<Optional<String>>>() {

            @Override
            public void accept(Try<Optional<String>> t1) {
                System.out.println("notification => " + t1);
            }

        }).toBlocking().forEach(new Consumer<String>() {

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
        final ArrayList<String> results = new ArrayList<>();
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> sub) {
                sub.onSubscribe(EmptyDisposable.INSTANCE);
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

        }).flatMap(new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Integer, Integer> group) {
                return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(new Function<Integer, String>() {

                    @Override
                    public String apply(Integer t1) {
                        return "first groups: " + t1;
                    }

                });
            }

        }).toBlocking().forEach(new Consumer<String>() {

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

    NbpObservable<Event> ASYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return SYNC_INFINITE_OBSERVABLE_OF_EVENT(numGroups, subscribeCounter, sentEventCounter).subscribeOn(Schedulers.newThread());
    };

    NbpObservable<Event> SYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return NbpObservable.create(new NbpOnSubscribe<Event>() {

            @Override
            public void accept(final NbpSubscriber<? super Event> op) {
                BooleanDisposable bs = new BooleanDisposable();
                op.onSubscribe(bs);
                subscribeCounter.incrementAndGet();
                int i = 0;
                while (!bs.isDisposed()) {
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
        NbpObservable<Long> source = NbpObservable.interval(10, TimeUnit.MILLISECONDS).take(1);

        // apply groupBy to the source
        NbpObservable<NbpGroupedObservable<Boolean, Long>> stream = source.groupBy(IS_EVEN);

        // create two observers
        @SuppressWarnings("unchecked")
        NbpObserver<NbpGroupedObservable<Boolean, Long>> o1 = mock(NbpObserver.class);
        @SuppressWarnings("unchecked")
        NbpObserver<NbpGroupedObservable<Boolean, Long>> o2 = mock(NbpObserver.class);

        // subscribe with the observers
        stream.subscribe(o1);
        stream.subscribe(o2);

        // check that subscriptions were successful
        verify(o1, never()).onError(Matchers.<Throwable> any());
        verify(o2, never()).onError(Matchers.<Throwable> any());
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

        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpObservable.range(1, 4000)
                .groupBy(IS_EVEN2)
                .flatMap(new Function<NbpGroupedObservable<Boolean, Integer>, NbpObservable<String>>() {

                    @Override
                    public NbpObservable<String> apply(final NbpGroupedObservable<Boolean, Integer> g) {
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
    Function<Integer, Integer> identity = v -> v;

    @Test
    public void normalBehavior() {
        NbpObservable<String> source = NbpObservable.fromIterable(Arrays.asList(
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

        NbpObservable<String> m = source.groupBy(keysel, valuesel)
        .flatMap(g -> {
            System.out.println("-----------> NEXT: " + g.getKey());
            return g.take(2).map(new Function<String, String>() {

                int count = 0;

                @Override
                public String apply(String v) {
                    System.out.println(v);
                    return g.getKey() + "-" + count++;
                }

            });
        });

        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        System.out.println("ts .get " + ts.values());
        ts.assertNoErrors();
        assertEquals(ts.values(),
                Arrays.asList("foo-0", "foo-1", "bar-0", "foo-0", "baz-0", "qux-0", "bar-1", "bar-0", "foo-1", "baz-1", "baz-0", "foo-0"));

    }

    @Test
    public void keySelectorThrows() {
        NbpObservable<Integer> source = NbpObservable.just(0, 1, 2, 3, 4, 5, 6);

        NbpObservable<Integer> m = source.groupBy(fail(0), dbl).flatMap(FLATTEN_INTEGER);

        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();
    }

    @Test
    public void valueSelectorThrows() {
        NbpObservable<Integer> source = NbpObservable.just(0, 1, 2, 3, 4, 5, 6);

        NbpObservable<Integer> m = source.groupBy(identity, fail(0)).flatMap(FLATTEN_INTEGER);
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertNoValues();

    }

    @Test
    public void innerEscapeCompleted() {
        NbpObservable<Integer> source = NbpObservable.just(0);

        NbpObservable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        System.out.println(ts.values());
    }

    /**
     * Assert we get an IllegalStateException if trying to subscribe to an inner NbpGroupedObservable more than once
     */
    @Test
    public void testExceptionIfSubscribeToChildMoreThanOnce() {
        NbpObservable<Integer> source = NbpObservable.just(0);

        final AtomicReference<NbpGroupedObservable<Integer, Integer>> inner = new AtomicReference<>();

        NbpObservable<NbpGroupedObservable<Integer, Integer>> m = source.groupBy(identity, dbl);

        m.subscribe(new Consumer<NbpGroupedObservable<Integer, Integer>>() {
            @Override
            public void accept(NbpGroupedObservable<Integer, Integer> t1) {
                inner.set(t1);
            }
        });

        inner.get().subscribe();

        @SuppressWarnings("unchecked")
        NbpObserver<Integer> o2 = mock(NbpObserver.class);

        inner.get().subscribe(o2);

        verify(o2, never()).onComplete();
        verify(o2, never()).onNext(anyInt());
        verify(o2).onError(any(IllegalStateException.class));
    }

    @Test
    public void testError2() {
        NbpObservable<Integer> source = NbpObservable.concat(NbpObservable.just(0),
                NbpObservable.<Integer> error(new TestException("Forced failure")));

        NbpObservable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        m.subscribe(ts);
        ts.awaitTerminalEvent();
        assertEquals(1, ts.errorCount());
        ts.assertValueCount(1);
    }

    @Test
    public void testgroupByBackpressure() throws InterruptedException {
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpObservable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Function<NbpGroupedObservable<Boolean, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Boolean, Integer> g) {
                return g.doOnComplete(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("//////////////////// COMPLETED-A");
                    }

                }).observeOn(Schedulers.computation()).map(new Function<Integer, String>() {

                    int c = 0;

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

                }).doOnComplete(new Runnable() {

                    @Override
                    public void run() {
                        System.out.println("//////////////////// COMPLETED-B");
                    }

                });
            }

        }).doOnEach(new Consumer<Try<Optional<String>>>() {

            @Override
            public void accept(Try<Optional<String>> t1) {
                System.out.println("NEXT: " + t1);
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    @Test
    public void testgroupByBackpressure2() throws InterruptedException {

        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();

        NbpObservable.range(1, 4000).groupBy(IS_EVEN2).flatMap(new Function<NbpGroupedObservable<Boolean, Integer>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(final NbpGroupedObservable<Boolean, Integer> g) {
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

    static Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<Integer>> FLATTEN_INTEGER = new Function<NbpGroupedObservable<Integer, Integer>, NbpObservable<Integer>>() {

        @Override
        public NbpObservable<Integer> apply(NbpGroupedObservable<Integer, Integer> t) {
            return t;
        }

    };

    @Test
    public void testGroupByWithNullKey() {
        final String[] key = new String[]{"uninitialized"};
        final List<String> values = new ArrayList<>();
        NbpObservable.just("a", "b", "c").groupBy(new Function<String, String>() {

            @Override
            public String apply(String value) {
                return null;
            }
        }).subscribe(new Consumer<NbpGroupedObservable<String, String>>() {

            @Override
            public void accept(NbpGroupedObservable<String, String> NbpGroupedObservable) {
                key[0] = NbpGroupedObservable.getKey();
                NbpGroupedObservable.subscribe(new Consumer<String>() {

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
        NbpObservable<Integer> o = NbpObservable.create(
                new NbpOnSubscribe<Integer>() {
                    @Override
                    public void accept(NbpSubscriber<? super Integer> NbpSubscriber) {
                        NbpSubscriber.onSubscribe(s);
                    }
                }
        );
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>();
        
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
        final NbpTestSubscriber<Integer> inner1 = new NbpTestSubscriber<>();
        final NbpTestSubscriber<Integer> inner2 = new NbpTestSubscriber<>();

        final NbpTestSubscriber<NbpGroupedObservable<Integer, Integer>> outer
                = new NbpTestSubscriber<>(new NbpObserver<NbpGroupedObservable<Integer, Integer>>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(NbpGroupedObservable<Integer, Integer> o) {
                if (o.getKey() == 0) {
                    o.subscribe(inner1);
                } else {
                    o.subscribe(inner2);
                }
            }
        });
        NbpObservable.create(
                new NbpOnSubscribe<Integer>() {
                    @Override
                    public void accept(NbpSubscriber<? super Integer> NbpSubscriber) {
                        NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
                        NbpSubscriber.onNext(0);
                        NbpSubscriber.onNext(1);
                        NbpSubscriber.onError(e);
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
}