/*
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import com.google.common.base.Ticker;
import com.google.common.cache.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.GroupedFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subscribers.*;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableGroupByTest extends RxJavaTest {

    static Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>> FLATTEN_INTEGER = t -> t;

    final Function<String, Integer> length = String::length;

    @Test
    public void groupBy() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
        assertArrayEquals(Collections.singletonList("three").toArray(), map.get(5).toArray());
    }

    @Test
    public void groupByWithElementSelector() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Collections.singletonList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void groupByWithElementSelector2() {
        Flowable<String> source = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<GroupedFlowable<Integer, Integer>> grouped = source.groupBy(length, length);

        Map<Integer, Collection<Integer>> map = toMap(grouped);

        assertEquals(3, map.size());
        assertArrayEquals(Arrays.asList(3, 3, 3).toArray(), map.get(3).toArray());
        assertArrayEquals(Arrays.asList(4, 4).toArray(), map.get(4).toArray());
        assertArrayEquals(Collections.singletonList(5).toArray(), map.get(5).toArray());
    }

    @Test
    public void empty() {
        Flowable<String> source = Flowable.empty();
        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        Map<Integer, Collection<String>> map = toMap(grouped);

        assertTrue(map.isEmpty());
    }

    @Test
    @SuppressUndeliverable
    public void error() {
        Flowable<String> sourceStrings = Flowable.just("one", "two", "three", "four", "five", "six");
        Flowable<String> errorSource = Flowable.error(new TestException("forced failure"));
        Flowable<String> source = Flowable.concat(sourceStrings, errorSource);

        Flowable<GroupedFlowable<Integer, String>> grouped = source.groupBy(length);

        final AtomicInteger groupCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();

        grouped.flatMap((Function<GroupedFlowable<Integer, String>, Flowable<String>>) f -> {
            groupCounter.incrementAndGet();
            return f.map(v -> "Event => key: " + f.getKey() + " value: " + v);
        }).subscribe(new DefaultSubscriber<String>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
//                e.printStackTrace();
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
        assertTrue("" + error.get(), error.get() instanceof TestException);
        assertEquals(error.get().getMessage(), "forced failure");
    }

    private static <K, V> Map<K, Collection<V>> toMap(Flowable<GroupedFlowable<K, V>> flowable) {

        final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<>();

        flowable.doOnNext(f -> {
            result.put(f.getKey(), new ConcurrentLinkedQueue<>());
            f.subscribe(v -> result.get(f.getKey()).add(v));
        }).blockingSubscribe();

        return result;
    }

    /**
     * Assert that only a single subscription to a stream occurs and that all events are received.
     *
     * @throws Throwable some method call is declared throws
     */
    @Test
    public void groupedEventStream() throws Throwable {

        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        final int groupCount = 2;

        Flowable<Event> es = Flowable.unsafeCreate(subscriber -> {
            subscriber.onSubscribe(new BooleanSubscription());
            System.out.println("*** Subscribing to EventStream ***");
            subscribeCounter.incrementAndGet();
            new Thread(() -> {
                for (int i = 0; i < count; i++) {
                    Event e = new Event();
                    e.source = i % groupCount;
                    e.message = "Event-" + i;
                    subscriber.onNext(e);
                }
                subscriber.onComplete();
            }).start();
        });

        es.groupBy(e -> e.source).flatMap((Function<GroupedFlowable<Integer, Event>, Flowable<String>>) eventGroupedFlowable -> {
            System.out.println("GroupedFlowable Key: " + eventGroupedFlowable.getKey());
            groupCounter.incrementAndGet();

            return eventGroupedFlowable.map(event -> "Source: " + event.source + "  Message: " + event.message);

        }).subscribe(new DefaultSubscriber<String>() {

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
    public void unsubscribeOnNestedTakeAndSyncInfiniteStream() throws InterruptedException {
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
    public void unsubscribeOnNestedTakeAndAsyncInfiniteStream() throws InterruptedException {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(ASYNC_INFINITE_OBSERVABLE_OF_EVENT(2, subscribeCounter, sentEventCounter), subscribeCounter);
        Thread.sleep(500);
        assertEquals(39, sentEventCounter.get());
    }

    private void doTestUnsubscribeOnNestedTakeAndAsyncInfiniteStream(Flowable<Event> es, AtomicInteger subscribeCounter) throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final AtomicInteger groupCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        es.groupBy(e -> e.source)
                .take(1) // we want only the first group
                .flatMap((Function<GroupedFlowable<Integer, Event>, Flowable<String>>) eventGroupedFlowable -> {
                    System.out.println("testUnsubscribe => GroupedFlowable Key: " + eventGroupedFlowable.getKey());
                    groupCounter.incrementAndGet();

                    return eventGroupedFlowable
                            .take(20) // limit to only 20 events on this group
                            .map(event -> "testUnsubscribe => Source: " + event.source + "  Message: " + event.message);

                }).subscribe(new DefaultSubscriber<String>() {

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
    public void unsubscribeViaTakeOnGroupThenMergeAndTake() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(e -> e.source)
                // take 2 of the 4 groups
                .take(2)
                .flatMap((Function<GroupedFlowable<Integer, Event>, Flowable<String>>) eventGroupedFlowable -> eventGroupedFlowable
                        .map(event -> "testUnsubscribe => Source: " + event.source + "  Message: " + event.message))
                .take(30).subscribe(s -> {
                    eventCounter.incrementAndGet();
                    System.out.println("=> " + s);
                });

        assertEquals(30, eventCounter.get());
        // we should send 28 additional events that are filtered out as they are in the groups we skip
        assertEquals(58, sentEventCounter.get());
    }

    @Test
    public void unsubscribeViaTakeOnGroupThenTakeOnInner() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(e -> e.source)
                // take 2 of the 4 groups
                .take(2)
                .flatMap((Function<GroupedFlowable<Integer, Event>, Flowable<String>>) eventGroupedFlowable -> {
                    int numToTake = 0;
                    if (eventGroupedFlowable.getKey() == 1) {
                        numToTake = 10;
                    } else if (eventGroupedFlowable.getKey() == 2) {
                        numToTake = 5;
                    }
                    return eventGroupedFlowable
                            .take(numToTake)
                            .map(event -> "testUnsubscribe => Source: " + event.source + "  Message: " + event.message);

                })
                .subscribe(s -> {
                    eventCounter.incrementAndGet();
                    System.out.println("=> " + s);
                });

        assertEquals(15, eventCounter.get());
        // we should send 22 additional events that are filtered out as they are skipped while taking the 15 we want
        assertEquals(37, sentEventCounter.get());
    }

    @Test
    public void staggeredCompletion() throws InterruptedException {
        final AtomicInteger eventCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);
        Flowable.range(0, 100)
                .groupBy(i -> i % 2)
                .flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>>) group -> {
                    if (group.getKey() == 0) {
                        return group.delay(100, TimeUnit.MILLISECONDS).map(t -> t * 10);
                    } else {
                        return group;
                    }
                })
                .subscribe(new DefaultSubscriber<Integer>() {

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

    @Test
    public void completionIfInnerNotSubscribed() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger eventCounter = new AtomicInteger();
        Flowable.range(0, 100)
                .groupBy(i -> i % 2)
                .subscribe(new DefaultSubscriber<GroupedFlowable<Integer, Integer>>() {

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
                    public void onNext(GroupedFlowable<Integer, Integer> s) {
                        eventCounter.incrementAndGet();
                        System.out.println("=> " + s);
                    }
                });
        if (!latch.await(500, TimeUnit.MILLISECONDS)) {
            fail("timed out - never got completion");
        }
        // Behavior change: groups not subscribed immediately will be automatically abandoned
        // so this leads to group recreation
        assertEquals(100, eventCounter.get());
    }

    @Test
    public void ignoringGroups() {
        final AtomicInteger subscribeCounter = new AtomicInteger();
        final AtomicInteger sentEventCounter = new AtomicInteger();
        final AtomicInteger eventCounter = new AtomicInteger();

        SYNC_INFINITE_OBSERVABLE_OF_EVENT(4, subscribeCounter, sentEventCounter)
                .groupBy(e -> e.source)
                .flatMap((Function<GroupedFlowable<Integer, Event>, Flowable<String>>) eventGroupedFlowable -> {
                    Flowable<Event> eventStream = eventGroupedFlowable;
                    if (eventGroupedFlowable.getKey() >= 2) {
                        // filter these
                        eventStream = eventGroupedFlowable.filter(t1 -> false);
                    }

                    return eventStream
                            .map(event -> "testUnsubscribe => Source: " + event.source + "  Message: " + event.message);

                })
                .take(30).subscribe(s -> {
                    eventCounter.incrementAndGet();
                    System.out.println("=> " + s);
                });

        assertEquals(30, eventCounter.get());
        // we should send 30 additional events that are filtered out as they are in the groups we skip
        assertEquals(60, sentEventCounter.get());
    }

    @Test
    public void firstGroupsCompleteAndParentSlowToThenEmitFinalGroupsAndThenComplete() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<>();
        Flowable.unsafeCreate((Publisher<Integer>) sub -> {
            sub.onSubscribe(new BooleanSubscription());
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
        }).groupBy(t -> t).flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<String>>) group -> {
            if (group.getKey() < 3) {
                return group.map(t1 -> "first groups: " + t1)
                        // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                        .take(2).doOnComplete(first::countDown);
            } else {
                return group.map(t1 -> "last group: " + t1);
            }
        }).blockingForEach(results::add);

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void firstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenSubscribesOnAndDelaysAndThenCompletes() throws InterruptedException {
        System.err.println("----------------------------------------------------------------------------------------------");
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<>();
        Flowable.unsafeCreate((Publisher<Integer>) sub -> {
            sub.onSubscribe(new BooleanSubscription());
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
        }).groupBy(t -> t).flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<String>>) group -> {
            if (group.getKey() < 3) {
                return group.map(t1 -> "first groups: " + t1)
                        // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                        .take(2).doOnComplete(first::countDown);
            } else {
                return group.subscribeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(t1 -> "last group: " + t1).doOnEach(t1 -> System.err.println("subscribeOn notification => " + t1));
            }
        }).doOnEach(t1 -> System.err.println("outer notification => " + t1)).blockingForEach(results::add);

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void firstGroupsCompleteAndParentSlowToThenEmitFinalGroupsWhichThenObservesOnAndDelaysAndThenCompletes() throws InterruptedException {
        final CountDownLatch first = new CountDownLatch(2); // there are two groups to first complete
        final ArrayList<String> results = new ArrayList<>();
        Flowable.unsafeCreate((Publisher<Integer>) sub -> {
            sub.onSubscribe(new BooleanSubscription());
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
        }).groupBy(t -> t).flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<String>>) group -> {
            if (group.getKey() < 3) {
                return group.map(t1 -> "first groups: " + t1)
                        // must take(2) so an onComplete + unsubscribe happens on these first 2 groups
                        .take(2).doOnComplete(first::countDown);
            } else {
                return group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(t1 -> "last group: " + t1);
            }
        }).blockingForEach(results::add);

        System.out.println("Results: " + results);
        assertEquals(6, results.size());
    }

    @Test
    public void groupsWithNestedSubscribeOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<>();
        Flowable.unsafeCreate((Publisher<Integer>) sub -> {
            sub.onSubscribe(new BooleanSubscription());
            sub.onNext(1);
            sub.onNext(2);
            sub.onNext(1);
            sub.onNext(2);
            sub.onComplete();
        }).groupBy(t -> t).flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<String>>) group -> group.subscribeOn(Schedulers.newThread()).map(t1 -> {
            System.out.println("Received: " + t1 + " on group : " + group.getKey());
            return "first groups: " + t1;
        })).doOnEach(t1 -> System.out.println("notification => " + t1)).blockingForEach(results::add);

        System.out.println("Results: " + results);
        assertEquals(4, results.size());
    }

    @Test
    public void groupsWithNestedObserveOn() throws InterruptedException {
        final ArrayList<String> results = new ArrayList<>();
        Flowable.unsafeCreate((Publisher<Integer>) sub -> {
            sub.onSubscribe(new BooleanSubscription());
            sub.onNext(1);
            sub.onNext(2);
            sub.onNext(1);
            sub.onNext(2);
            sub.onComplete();
        }).groupBy(t -> t).flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<String>>) group -> group.observeOn(Schedulers.newThread()).delay(400, TimeUnit.MILLISECONDS).map(t1 -> "first groups: " + t1)).blockingForEach(results::add);

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

    Flowable<Event> ASYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return SYNC_INFINITE_OBSERVABLE_OF_EVENT(numGroups, subscribeCounter, sentEventCounter).subscribeOn(Schedulers.newThread());
    }

    Flowable<Event> SYNC_INFINITE_OBSERVABLE_OF_EVENT(final int numGroups, final AtomicInteger subscribeCounter, final AtomicInteger sentEventCounter) {
        return Flowable.unsafeCreate(op -> {
            BooleanSubscription bs = new BooleanSubscription();
            op.onSubscribe(bs);
            subscribeCounter.incrementAndGet();
            int i = 0;
            while (!bs.isCancelled()) {
                i++;
                Event e = new Event();
                e.source = i % numGroups;
                e.message = "Event-" + i;
                op.onNext(e);
                sentEventCounter.incrementAndGet();
            }
            op.onComplete();
        });
    }

    @Test
    public void groupByOnAsynchronousSourceAcceptsMultipleSubscriptions() throws InterruptedException {

        // choose an asynchronous source
        Flowable<Long> source = Flowable.interval(10, TimeUnit.MILLISECONDS).take(1);

        // apply groupBy to the source
        Flowable<GroupedFlowable<Boolean, Long>> stream = source.groupBy(IS_EVEN);

        // create two observers
        Subscriber<GroupedFlowable<Boolean, Long>> f1 = TestHelper.mockSubscriber();
        Subscriber<GroupedFlowable<Boolean, Long>> f2 = TestHelper.mockSubscriber();

        // subscribe with the observers
        stream.subscribe(f1);
        stream.subscribe(f2);

        // check that subscriptions were successful
        verify(f1, never()).onError(Mockito.<Throwable> any());
        verify(f2, never()).onError(Mockito.<Throwable> any());
    }

    private static final Function<Long, Boolean> IS_EVEN = n -> n % 2 == 0;

    private static final Function<Integer, Boolean> IS_EVEN2 = n -> n % 2 == 0;

    @Test
    public void groupByBackpressure() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<>();

        Flowable.range(1, 4000)
                .groupBy(IS_EVEN2)
                .flatMap((Function<GroupedFlowable<Boolean, Integer>, Flowable<String>>) g -> g.observeOn(Schedulers.computation()).map(l -> {
                    if (g.getKey()) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                        }
                        return l + " is even.";
                    } else {
                        return l + " is odd.";
                    }
                })).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    <T, R> Function<T, R> just(final R value) {
        return t1 -> value;
    }

    <T> Function<Integer, T> fail(T dummy) {
        return t1 -> {
            throw new RuntimeException("Forced failure");
        };
    }

    <T, R> Function<T, R> fail2(R dummy2) {
        return t1 -> {
            throw new RuntimeException("Forced failure");
        };
    }

    Function<Integer, Integer> dbl = t1 -> t1 * 2;
    Function<Integer, Integer> identity = v -> v;

    @Test
    public void normalBehavior() {
        Flowable<String> source = Flowable.fromIterable(Arrays.asList(
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
        Function<String, String> keysel = t1 -> t1.trim().toLowerCase();
        Function<String, String> valuesel = t1 -> t1 + t1;

        Flowable<String> m = source.groupBy(keysel, valuesel)
        .flatMap((Function<GroupedFlowable<String, String>, Publisher<String>>) g -> {
            System.out.println("-----------> NEXT: " + g.getKey());
            return g.take(2).map(new Function<String, String>() {

                int count;

                @Override
                public String apply(String v) {
                    System.out.println(v);
                    return g.getKey() + "-" + count++;
                }

            });
        });

        TestSubscriber<String> ts = new TestSubscriber<>();
        m.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        System.out.println("ts .get " + ts.values());
        ts.assertNoErrors();
        assertEquals(ts.values(),
                Arrays.asList("foo-0", "foo-1", "bar-0", "foo-0", "baz-0", "qux-0", "bar-1", "bar-0", "foo-1", "baz-1", "baz-0", "foo-0"));

    }

    @Test
    public void keySelectorThrows() {
        Flowable<Integer> source = Flowable.just(0, 1, 2, 3, 4, 5, 6);

        Flowable<Integer> m = source.groupBy(fail(0), dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        m.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(1, ts.errors().size());
        ts.assertNoValues();
    }

    @Test
    @SuppressUndeliverable
    public void valueSelectorThrows() {
        Flowable<Integer> source = Flowable.just(0, 1, 2, 3, 4, 5, 6);

        Flowable<Integer> m = source.groupBy(identity, fail(0)).flatMap(FLATTEN_INTEGER);
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<>();
        m.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(1, ts.errors().size());
        ts.assertNoValues();

    }

    @Test
    public void innerEscapeCompleted() {
        Flowable<Integer> source = Flowable.just(0);

        Flowable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriber<Object> ts = new TestSubscriber<>();
        m.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        System.out.println(ts.values());
    }

    /**
     * Assert we get an IllegalStateException if trying to subscribe to an inner GroupedFlowable more than once.
     */
    @Test
    public void exceptionIfSubscribeToChildMoreThanOnce() {
        Flowable<Integer> source = Flowable.just(0);

        final AtomicReference<GroupedFlowable<Integer, Integer>> inner = new AtomicReference<>();

        Flowable<GroupedFlowable<Integer, Integer>> m = source.groupBy(identity, dbl);

        m.subscribe(inner::set);

        inner.get().subscribe();

        Subscriber<Integer> subscriber2 = TestHelper.mockSubscriber();

        inner.get().subscribe(subscriber2);

        verify(subscriber2, never()).onComplete();
        verify(subscriber2, never()).onNext(anyInt());
        verify(subscriber2).onError(any(IllegalStateException.class));
    }

    @Test
    @SuppressUndeliverable
    public void error2() {
        Flowable<Integer> source = Flowable.concat(Flowable.just(0),
                Flowable.<Integer> error(new TestException("Forced failure")));

        Flowable<Integer> m = source.groupBy(identity, dbl).flatMap(FLATTEN_INTEGER);

        TestSubscriberEx<Object> ts = new TestSubscriberEx<>();
        m.subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        assertEquals(1, ts.errors().size());
        ts.assertValueCount(1);
    }

    @Test
    public void groupByBackpressure3() throws InterruptedException {
        TestSubscriber<String> ts = new TestSubscriber<>();

        Flowable.range(1, 4000).groupBy(IS_EVEN2).flatMap((Function<GroupedFlowable<Boolean, Integer>, Flowable<String>>) g -> g.doOnComplete(() -> System.out.println("//////////////////// COMPLETED-A")).observeOn(Schedulers.computation()).map(new Function<Integer, String>() {

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

        }).doOnComplete(() -> System.out.println("//////////////////// COMPLETED-B"))).doOnEach(t1 -> System.out.println("NEXT: " + t1)).subscribe(ts);
        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void groupByBackpressure2() throws InterruptedException {

        TestSubscriber<String> ts = new TestSubscriber<>();

        Flowable.range(1, 4000)
            .doOnNext(v -> System.out.println("testgroupByBackpressure2 >> " + v))
            .groupBy(IS_EVEN2)
            .flatMap((Function<GroupedFlowable<Boolean, Integer>, Flowable<String>>) g -> g.take(2)
                    .observeOn(Schedulers.computation())
                    .map(l -> {
                        if (g.getKey()) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                            }
                            return l + " is even.";
                        } else {
                            return l + " is odd.";
                        }
                    }), 4000) // a lot of groups are created due to take(2)
            .subscribe(ts);

        ts.awaitDone(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
    }

    @Test
    public void groupByWithNullKey() {
        final String[] key = new String[]{"uninitialized"};
        final List<String> values = new ArrayList<>();
        Flowable.just("a", "b", "c").groupBy((Function<String, String>) value -> null).subscribe(groupedFlowable -> {
            key[0] = groupedFlowable.getKey();
            groupedFlowable.subscribe(values::add);
        });
        assertNull(key[0]);
        assertEquals(Arrays.asList("a", "b", "c"), values);
    }

    @Test
    public void groupByUnsubscribe() {
        final Subscription s = mock(Subscription.class);
        Flowable<Integer> f = Flowable.unsafeCreate(
                subscriber -> subscriber.onSubscribe(s)
        );
        TestSubscriber<Object> ts = new TestSubscriber<>();

        f.groupBy((Function<Integer, Integer>) integer -> null).subscribe(ts);

        ts.cancel();

        verify(s).cancel();
    }

    @Test
    public void groupByShouldPropagateError() {
        final Throwable e = new RuntimeException("Oops");
        final TestSubscriberEx<Integer> inner1 = new TestSubscriberEx<>();
        final TestSubscriberEx<Integer> inner2 = new TestSubscriberEx<>();

        final TestSubscriberEx<GroupedFlowable<Integer, Integer>> outer
                = new TestSubscriberEx<>(new DefaultSubscriber<GroupedFlowable<Integer, Integer>>() {

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(GroupedFlowable<Integer, Integer> f) {
                if (f.getKey() == 0) {
                    f.subscribe(inner1);
                } else {
                    f.subscribe(inner2);
                }
            }
        });
        Flowable.unsafeCreate(
                (Publisher<Integer>) subscriber -> {
                    subscriber.onSubscribe(new BooleanSubscription());
                    subscriber.onNext(0);
                    subscriber.onNext(1);
                    subscriber.onError(e);
                }
        ).groupBy(i -> i % 2).subscribe(outer);
        assertEquals(Collections.singletonList(e), outer.errors());
        assertEquals(Collections.singletonList(e), inner1.errors());
        assertEquals(Collections.singletonList(e), inner2.errors());
    }

    @Test
    public void requestOverflow() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Flowable
                .just(1, 2, 3)
                // group into one group
                .groupBy(t -> 1)
                // flatten
                .concatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>>) g -> g)
                .subscribe(new DefaultSubscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(2);
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        System.out.println(t);
                        //provoke possible request overflow
                        request(Long.MAX_VALUE - 1);
                    }});
        assertTrue(completed.get());
    }

    /**
     * Issue #3425.
     *
     * The problem is that a request of 1 may create a new group, emit to the desired group
     * or emit to a completely different group. In this test, the merge requests N which
     * must be produced by the range, however it will create a bunch of groups before the actual
     * group receives a value.
     * 
     * 12/03/2019: this test produces abandoned groups and as such keeps producing new groups
     * that have to be ready to be received by observeOn and merge.
     */
    @Test
    public void backpressureObserveOnOuter() {
        int n = 500;
        for (int j = 0; j < 1000; j++) {
            Flowable.merge(
                    Flowable.range(0, n)
                    .groupBy((Function<Integer, Object>) i -> i % (Flowable.bufferSize() + 2))
                    .observeOn(Schedulers.computation(), false, n)
            , n)
            .blockingLast();
        }
    }

    @Test(expected = MissingBackpressureException.class)
    public void backpressureObserveOnOuterMissingBackpressure() {
        for (int j = 0; j < 1000; j++) {
            Flowable.merge(
                    Flowable.range(0, 500)
                    .groupBy((Function<Integer, Object>) i -> i % (Flowable.bufferSize() + 2))
                    .observeOn(Schedulers.computation())
            ).blockingLast();
        }
    }

    /**
     * Synchronous verification of issue #3425.
     */
    @Test
    public void backpressureInnerDoesntOverflowOuter() {
        TestSubscriber<GroupedFlowable<Integer, Integer>> ts = new TestSubscriber<>(0L);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        pp.groupBy(v -> v)
                .doOnNext(Flowable::subscribe) // this will request Long.MAX_VALUE
                .subscribe(ts)
                ;
        ts.request(1);

        pp.onNext(1);

        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }

    @Test
    public void backpressureInnerDoesntOverflowOuterMissingBackpressure() {
        TestSubscriber<GroupedFlowable<Integer, Integer>> ts = new TestSubscriber<>(1);

        Flowable.fromArray(1, 2)
                .groupBy(v -> v)
                .doOnNext(Flowable::subscribe) // this will request Long.MAX_VALUE
                .subscribe(ts)
                ;
        ts.assertValueCount(1)
        .assertError(MissingBackpressureException.class)
        .assertNotComplete();
    }

    @Test
    public void oneGroupInnerRequestsTwiceBuffer() {
        // FIXME: delayed requesting in groupBy results in group abandonment
        TestSubscriber<Object> ts1 = new TestSubscriber<>(1L);

        final TestSubscriber<Object> ts2 = new TestSubscriber<>(0L);

        Flowable.range(1, Flowable.bufferSize() * 2)
        .groupBy((Function<Integer, Object>) v -> 1)
        .doOnNext(g -> g.subscribe(ts2))
        .subscribe(ts1);

        ts1.assertValueCount(1);
        ts1.assertNoErrors();
        ts1.assertNotComplete();

        ts2.assertNoValues();
        ts2.assertNoErrors();
        ts2.assertNotComplete();

        ts2.request(Flowable.bufferSize() * 2L);

        ts2.assertValueCount(Flowable.bufferSize() * 2);
        ts2.assertNoErrors();
        ts2.assertComplete();
    }

    @Test
    public void outerInnerFusion() {
        final TestSubscriberEx<Integer> ts1 = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        final TestSubscriberEx<GroupedFlowable<Integer, Integer>> ts2 = new TestSubscriberEx<GroupedFlowable<Integer, Integer>>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 10).groupBy(v -> 1, v -> v + 1)
        .doOnNext(g -> g.subscribe(ts1))
        .subscribe(ts2);

        ts1
        // FIXME fusion mode causes hangs
        //.assertFusionMode(QueueFuseable.ASYNC)
        .assertFusionMode(QueueFuseable.NONE)
        .assertValues(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        .assertNoErrors()
        .assertComplete();

        ts2
        .assertValueCount(1)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    @SuppressUndeliverable
    public void keySelectorAndDelayError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), true)
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>>) g -> g)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    @SuppressUndeliverable
    public void keyAndValueSelectorAndDelayError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), true)
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>>) g -> g)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).groupBy(Functions.justFunction(1)));

        Flowable.just(1)
        .groupBy(Functions.justFunction(1))
        .doOnNext(TestHelper::checkDisposed)
        .test();
    }

    @Test
    public void reentrantComplete() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(@NonNull Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onComplete();
                }
            }
        };

        Flowable.merge(pp.groupBy(Functions.justFunction(1)))
        .subscribe(ts);

        pp.onNext(1);

        ts.assertResult(1);
    }

    @Test
    public void reentrantCompleteCancel() {
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onComplete();
                    dispose();
                }
            }
        };

        Flowable.merge(pp.groupBy(Functions.justFunction(1)))
        .subscribe(ts);

        pp.onNext(1);

        ts.assertSubscribed().assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void delayErrorSimpleComplete() {
        Flowable.just(1)
        .groupBy(Functions.justFunction(1), true)
        .flatMap(Functions.<Flowable<Integer>>identity())
        .test()
        .assertResult(1);
    }

    @Test
    public void mainFusionRejected() {
        TestSubscriberEx<Flowable<Integer>> ts = new TestSubscriberEx<Flowable<Integer>>().setInitialFusionMode(QueueFuseable.SYNC);

        Flowable.just(1)
        .groupBy(Functions.justFunction(1))
        .subscribe(ts);

        ts.assertFusionMode(QueueFuseable.NONE)
        .assertValueCount(1)
        .assertComplete()
        .assertNoErrors();
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable((Function<Flowable<Object>, Object>) f -> f.groupBy(Functions.justFunction(1)), false, 1, 1, (Object[])null);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1).hide()
                .groupBy(Functions.justFunction(1)));
    }

    @Test
    public void badRequestInner() {
        Flowable.just(1).hide()
        .groupBy(Functions.justFunction(1))
        .doOnNext(TestHelper::assertBadRequestReported)
        .test()
        .assertNoErrors();
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(f -> f.groupBy(Functions.justFunction(1)));
    }

    @Test
    public void nullKeyTakeInner() {
        Flowable.just(1)
        .groupBy(v -> null)
        .flatMap((Function<GroupedFlowable<Object, Integer>, Publisher<Integer>>) g -> g.take(1))
        .test()
        .assertResult(1);
    }

    @Test
    @SuppressUndeliverable
    public void groupError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .groupBy(Functions.justFunction(1), true)
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) Flowable::hide)
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void groupComplete() {
        Flowable.just(1)
        .groupBy(Functions.justFunction(1), true)
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) Flowable::hide)
        .test()
        .assertResult(1);
    }

    @Test
    public void mapFactoryThrows() {
        final IOException ex = new IOException("boo");
        Function<Consumer<Object>, Map<Integer, Object>> evictingMapFactory =  //
                notify -> {
                    throw ex;
                };
        Flowable.just(1)
          .groupBy(Functions.<Integer>identity(), Functions.identity(), true, 16, evictingMapFactory)
          .test()
          .assertNoValues()
          .assertError(ex);
    }
    // -----------------------------------------------------------------------------------------------------------------------

    private static final Function<Integer, Integer> mod5 = n -> n % 5;

    private static Function<GroupedFlowable<Integer, Integer>, Publisher<? extends Integer>> addCompletedKey(
            final List<Integer> completed) {
        return g -> g.doOnComplete(() -> completed.add(g.getKey()));
    }

    private static final class TestTicker extends Ticker {
        long tick;

        @Override
        public long read() {
            return tick;
        }
    }

    @Test
    public void mapFactoryExpiryCompletesGroupedFlowable() {
        final List<Integer> completed = new CopyOnWriteArrayList<>();
        Function<Consumer<Object>, Map<Integer, Object>> evictingMapFactory = createEvictingMapFactorySynchronousOnly(1);
        PublishSubject<Integer> subject = PublishSubject.create();
        TestSubscriberEx<Integer> ts = subject.toFlowable(BackpressureStrategy.BUFFER)
                .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), true, 16, evictingMapFactory)
                .flatMap(addCompletedKey(completed))
                .to(TestHelper.<Integer>testConsumer());
        subject.onNext(1);
        subject.onNext(2);
        subject.onNext(3);
        ts.assertValues(1, 2, 3)
          .assertNotTerminated();
        assertEquals(Arrays.asList(1, 2), completed);
        //ensure coverage of the code that clears the evicted queue
        subject.onComplete();
        ts.assertComplete();
        ts.assertValueCount(3);
    }

    @Test
    public void mapFactoryEvictionQueueClearedOnErrorCoverageOnly() {
        Function<Consumer<Object>, Map<Integer, Object>> evictingMapFactory = createEvictingMapFactorySynchronousOnly(1);
        PublishSubject<Integer> subject = PublishSubject.create();
        TestSubscriber<Integer> ts = subject
                .toFlowable(BackpressureStrategy.BUFFER)
                .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), true, 16, evictingMapFactory)
                .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) g -> g)
                .test();
        RuntimeException ex = new RuntimeException();
        //ensure coverage of the code that clears the evicted queue
        subject.onError(ex);
        ts.assertNoValues()
          .assertError(ex);
    }

    @Test
    public void mapFactoryWithExpiringGuavaCacheDemonstrationCodeForUseInJavadoc() {
        //javadoc will be a version of this using lambdas and without assertions
        final List<Integer> completed = new CopyOnWriteArrayList<>();

        AtomicReference<Cache<Integer, Object>> cacheOut = new AtomicReference<>();

        //size should be less than 5 to notice the effect
        Function<Consumer<Object>, Map<Integer, Object>> evictingMapFactory = createEvictingMapFactoryGuava(3, cacheOut);
        int numValues = 1000;
        TestSubscriber<Integer> ts =
            Flowable.range(1, numValues)
                .groupBy(mod5, Functions.<Integer>identity(), true, 16, evictingMapFactory)
                .flatMap(addCompletedKey(completed))
                .test()
                .assertComplete()
                ;
        ts.assertValueCount(numValues);
        //the exact eviction behaviour of the guava cache is not specified so we make some approximate tests
        assertTrue(completed.size() > numValues * 0.9);

        cacheOut.get().invalidateAll();
    }

    @Test
    public void groupByEvictionCancellationOfSource5933() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final TestTicker testTicker = new TestTicker();

        Function<Consumer<Object>, Map<Integer, Object>> mapFactory = action -> CacheBuilder.newBuilder() //
                .expireAfterAccess(5, TimeUnit.SECONDS).removalListener(notification -> {
                    try {
                        action.accept(notification.getValue());
                    } catch (Throwable ex) {
                        throw new RuntimeException(ex);
                    }
                }).ticker(testTicker) //
                .<Integer, Object>build().asMap();

        final List<String> list = new CopyOnWriteArrayList<>();
        Flowable<Integer> stream = source //
                .doOnCancel(() -> list.add("Source canceled"))
                .<Integer, Integer>groupBy(Functions.<Integer>identity(), Functions.<Integer>identity(), false,
                        Flowable.bufferSize(), mapFactory) //
                .flatMap(group -> group //
                        .doOnComplete(() -> list.add("Group completed")).doOnCancel(() -> list.add("Group canceled")));
        TestSubscriber<Integer> ts = stream //
                .doOnCancel(() -> list.add("Outer group by canceled")).test();

        // Send 3 in the same group and wait for them to be seen
        source.onNext(1);
        source.onNext(1);
        source.onNext(1);
        ts.awaitCount(3);

        // Advance time far enough to evict the group.
        // NOTE -- Comment this line out to make the test "pass".
        testTicker.tick = TimeUnit.SECONDS.toNanos(6);

        // Send more data in the group (triggering eviction and recreation)
        source.onNext(1);

        // Wait for the last 2 and then cancel the subscription
        ts.awaitCount(4);
        ts.cancel();

        // Observe the result.  Note that right now the result differs depending on whether eviction occurred or
        // not.  The observed sequence in that case is:  Group completed, Outer group by canceled., Group canceled.
        // The addition of the "Group completed" is actually fine, but the fact that the cancel doesn't reach the
        // source seems like a bug.  Commenting out the setting of "tick" above will produce the "expected" sequence.
        System.out.println(list);
        assertTrue(list.contains("Source canceled"));
        assertEquals(Arrays.asList(
                "Group completed", // this is here when eviction occurs
                "Outer group by canceled",
                "Group canceled",
                "Source canceled"  // This is *not* here when eviction occurs
        ), list);
    }

    //not thread safe
    private static final class SingleThreadEvictingHashMap<K, V> implements Map<K, V> {

        private final List<K> list = new ArrayList<>();
        private final Map<K, V> map = new HashMap<>();
        private final int maxSize;
        private final Consumer<V> evictedListener;

        SingleThreadEvictingHashMap(int maxSize, Consumer<V> evictedListener) {
            this.maxSize = maxSize;
            this.evictedListener = evictedListener;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public V put(K key, V value) {
            list.remove(key);
            V v;
            if (maxSize > 0 && list.size() == maxSize) {
                //remove first
                K k = list.get(0);
                list.remove(0);
                v = map.remove(k);
            } else {
                v = null;
            }
            list.add(key);
            V result = map.put(key, value);
            if (v != null) {
                try {
                    evictedListener.accept(v);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            return result;
        }

        @Override
        public V remove(Object key) {
            list.remove(key);
            return map.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
           for (Entry<? extends K, ? extends V> entry: m.entrySet()) {
               put(entry.getKey(), entry.getValue());
           }
        }

        @Override
        public void clear() {
            list.clear();
            map.clear();
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<V> values() {
            return map.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }
    }

    private static Function<Consumer<Object>, Map<Integer, Object>> createEvictingMapFactoryGuava(final int maxSize,
            final AtomicReference<Cache<Integer, Object>> cacheOut) {
        return notify -> {
            Cache<Integer, Object> cache = CacheBuilder.newBuilder() //
                    .maximumSize(maxSize) //
                    .removalListener((RemovalListener<Integer, Object>) notification -> {
                        try {
                            notify.accept(notification.getValue());
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .<Integer, Object> build();
            cacheOut.set(cache);
            return cache.asMap();
        };
    }

    private static Function<Consumer<Object>, Map<Integer, Object>> createEvictingMapFactorySynchronousOnly(final int maxSize) {
        return notify -> new SingleThreadEvictingHashMap<>(maxSize, object -> {
            try {
                notify.accept(object);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }

    // -----------------------------------------------------------------------------------------------------------------------

    @Test
    public void cancellationOfUpstreamWhenGroupedFlowableCompletes() {
        final AtomicBoolean cancelled = new AtomicBoolean();
        Flowable.just(1).repeat().doOnCancel(() -> cancelled.set(true))
        .groupBy(Functions.<Integer>identity(), Functions.<Integer>identity()) //
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<?>>) g -> g.first(0).toFlowable())
        .take(4) //
        .test() //
        .assertComplete();
        assertTrue(cancelled.get());
    }

    @Test
    public void cancelOverFlatmapRace() {
        for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {

            final TestSubscriber<Integer> ts = new TestSubscriber<>();

            final PublishProcessor<Integer> pp = PublishProcessor.create();

            pp.groupBy(v -> v % 10, Functions.<Integer>identity(), false, 2048)
            .flatMap((Function<GroupedFlowable<Integer, Integer>, GroupedFlowable<Integer, Integer>>) v -> v)
            .subscribe(ts);

            Runnable r1 = () -> {
                for (int j = 0; j < 1000; j++) {
                    pp.onNext(j);
                }
            };

            Runnable r2 = ts::cancel;

            TestHelper.race(r1, r2);

            assertFalse("Round " + i, pp.hasSubscribers());
        }
    }

    @Test
    public void abandonedGroupsNoDataloss() {
        final List<GroupedFlowable<Integer, Integer>> groups = new ArrayList<>();

        Flowable.range(1, 1000)
        .groupBy(v -> v % 10)
        .doOnNext(groups::add)
        .test()
        .assertValueCount(1000)
        .assertComplete()
        .assertNoErrors();

        Flowable.concat(groups)
        .test()
        .assertValueCount(1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void newGroupValueSelectorFails() {
        TestSubscriber<Object> ts1 = new TestSubscriber<>();
        final TestSubscriber<Object> ts2 = new TestSubscriber<>();

        Flowable.just(1)
        .groupBy(Functions.<Integer>identity(), v -> {
            throw new TestException();
        })
        .doOnNext(g -> g.subscribe(ts2))
        .subscribe(ts1);

        ts1.assertValueCount(1)
        .assertError(TestException.class)
        .assertNotComplete();

        ts2.assertFailure(TestException.class);
    }

    @Test
    public void existingGroupValueSelectorFails() {
        TestSubscriber<Object> ts1 = new TestSubscriber<>();
        final TestSubscriber<Object> ts2 = new TestSubscriber<>();

        Flowable.just(1, 2)
        .groupBy(Functions.justFunction(1), (Function<Integer, Object>) v -> {
            if (v == 2) {
                throw new TestException();
            }
            return v;
        })
        .doOnNext(g -> g.subscribe(ts2))
        .subscribe(ts1);

        ts1.assertValueCount(1)
        .assertError(TestException.class)
        .assertNotComplete();

        ts2.assertFailure(TestException.class, 1);
    }

    @Test
    public void fusedParallelGroupProcessing() {
        Flowable.range(0, 500000)
        .subscribeOn(Schedulers.single())
        .groupBy(i -> i % 2)
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) g -> {
            return g.getKey() == 0
                ? g
                    .parallel()
                    .runOn(Schedulers.computation())
                    .map(Functions.<Integer>identity())
                    .sequential()
                : g.map(Functions.<Integer>identity()) // no need to use hide
            ;
        })
        .test()
        .awaitDone(20, TimeUnit.SECONDS)
        .assertValueCount(500000)
        .assertComplete()
        .assertNoErrors();
    }

    @Test
    public void valueSelectorCrashAndMissingBackpressure() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriberEx<GroupedFlowable<Integer, Integer>> ts = pp.groupBy(Functions.justFunction(1), (Function<Integer, Integer>) t -> {
            throw new TestException();
        })
        .subscribeWith(new TestSubscriberEx<>(0L));

        assertTrue(pp.offer(1));

        ts.assertFailure(MissingBackpressureException.class);

        assertTrue("" + ts.errors().get(0).getCause(), ts.errors().get(0).getCause() instanceof TestException);
    }

    @Test
    public void fusedGroupClearedOnCancel() {
        Flowable.just(1)
        .groupBy(Functions.<Integer>identity())
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) g -> g.observeOn(ImmediateThinScheduler.INSTANCE).take(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void fusedGroupClearedOnCancelDelayed() {
        Flowable.range(1, 100)
        .groupBy(Functions.<Integer, Integer>justFunction(1))
        .flatMap((Function<GroupedFlowable<Integer, Integer>, Publisher<Integer>>) g -> g.observeOn(Schedulers.io())
                .doOnNext(v -> Thread.sleep(100))
                .take(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void cancelledGroupResumesRequesting() {
        final List<TestSubscriber<Integer>> tss = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean done = new AtomicBoolean();
        Flowable.range(1, 1000)
                .doOnNext(v -> counter.getAndIncrement())
                .groupBy(Functions.justFunction(1))
                .subscribe(v -> {
                    TestSubscriber<Integer> ts = TestSubscriber.create(0L);
                    tss.add(ts);
                    v.subscribe(ts);
                }, Functions.emptyConsumer(), () -> done.set(true));

        while (!done.get()) {
            tss.remove(0).cancel();
        }

        assertEquals(1000, counter.get());
    }

    @Test
    public void delayErrorCompleteMoreWorkInGroup() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.groupBy(v -> 1, true)
        .flatMap(g -> g.doOnNext(v -> {
            if (v == 1) {
                pp.onNext(2);
                pp.onComplete();
            }
        })
        )
        .test()
        ;

        pp.onNext(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void groupSyncFusionRejected() {
        Flowable.just(1)
        .groupBy(v -> 1)
        .doOnNext(g -> g.subscribeWith(new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.SYNC))
        .assertFuseable()
        .assertFusionMode(QueueFuseable.NONE))
        .test()
        .assertComplete();
    }

    @Test
    public void subscribeAbandonRace() throws Throwable {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestSubscriber<Integer> ts = TestSubscriber.create();

            CountDownLatch cdl = new CountDownLatch(1);

            pp.groupBy(v -> 1)
            .doOnNext(g -> TestHelper.raceOther(() -> g.subscribe(ts), cdl))
            .test();

            pp.onNext(1);

            cdl.await();

            ts.assertValueCount(1);
        }
    }

    @Test
    public void issue6974() {

        FlowableTransformer<Integer, Integer> operation =
                source -> source.publish(shared ->
                    shared
                    .firstElement()
                    .flatMapPublisher(firstElement ->
                        Flowable.just(firstElement).concatWith(shared)
                    )
                );

        issue6974Run(20, 500_000, 20 - 1, 20 * 2, operation, false);

        issue6974Run(20, 500_000, 20, 20 * 2, operation, false);
    }

    static void issue6974Run(int groups, int iterations, int sizeCap, int flatMapConcurrency,
            FlowableTransformer<Integer, Integer> operation, boolean notifyOnExplicitRevoke) {
        TestSubscriber<Integer> test = Flowable
                .range(1, groups)
                .repeat(iterations / groups)
                .groupBy(i -> i, i -> i, false, 128, sizeCap(sizeCap, notifyOnExplicitRevoke))
                .flatMap(gf -> gf.compose(operation), flatMapConcurrency)
                .test();
        test.awaitDone(5, TimeUnit.SECONDS);
        test.assertValueCount(iterations);
    }

    static <T> Function<Consumer<Object>, Map<T, Object>> sizeCap(int maxCapacity, boolean notifyOnExplicit) {
        return itemEvictConsumer ->
        CacheBuilder
        .newBuilder()
        .maximumSize(maxCapacity)
        .removalListener(notification -> {
            if (notification.getCause() != RemovalCause.EXPLICIT || notifyOnExplicit) {
                try {
                    itemEvictConsumer.accept(notification.getValue());
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
            }
        })
        .<T, Object>build().asMap();
    }

    static void issue6974RunPart2(int groupByBufferSize, int flatMapMaxConcurrency, int groups,
            boolean notifyOnExplicitEviction) {
        TestSubscriber<Integer> ts = Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .groupBy(i -> i, i -> i, false, groupByBufferSize,
                // set cap too high
                sizeCap(groups * 100, notifyOnExplicitEviction))
        .flatMap(gf -> gf
                .take(10, TimeUnit.MILLISECONDS)
                , flatMapMaxConcurrency)
        .test();

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void issue6974Part2Case1() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;
        issue6974RunPart2(groupByBufferSize, flatMapMaxConcurrency, groups, notifyOnExplicitEviction);
    }

    @Test
    public void issue6974Part2Case2() {
        final int groups = 20;

        // Timeout... explicit eviction notification makes difference
        int groupByBufferSize = groups * 30;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = true;
        issue6974RunPart2(groupByBufferSize, flatMapMaxConcurrency, groups, notifyOnExplicitEviction);
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6974Part2Case2Loop() {
        for (int i = 0; i < 1000; i++) {
            issue6974Part2Case2();
        }
    }
    */

    static void issue6974RunPart2NoEvict(int groupByBufferSize, int flatMapMaxConcurrency, int groups,
            boolean notifyOnExplicitEviction) {

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .groupBy(i -> i)
        .flatMap(gf -> gf
                .take(10, TimeUnit.MILLISECONDS)
                , flatMapMaxConcurrency)
        .subscribeWith(new TestSubscriberEx<>())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertTerminated(); // MBE is possible if the async group closing is slow
    }

    @Test
    public void issue6974Part2Case1NoEvict() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;
        issue6974RunPart2NoEvict(groupByBufferSize, flatMapMaxConcurrency, groups, notifyOnExplicitEviction);
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6974Part2Case1NoEvictLoop() {
        for (int i = 0; i < 1000; i++) {
            issue6974Part2Case1NoEvict();
        }
    }
    */

    @Test
    public void issue6974Part2Case1ObserveOn() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnCancel(() -> System.out.println("Cancelling upstream"))
        .groupBy(i -> i, i -> i, false, groupByBufferSize,
                              sizeCap(groups * 2, notifyOnExplicitEviction))
        .flatMap(gf -> gf
                     .observeOn(Schedulers.computation())
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .subscribeWith(new TestSubscriberEx<>())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertTerminated(); // MBE is possible if the async group closing is slow
    }

    @Test
    public void issue6974Part2Case1ObserveOnHide() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnCancel(() -> System.out.println("Cancelling upstream"))
        .groupBy(i -> i, i -> i, false, groupByBufferSize,
                              sizeCap(groups * 2, notifyOnExplicitEviction))
        .flatMap(gf -> gf
                     .hide()
                     .observeOn(Schedulers.computation())
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .subscribeWith(new TestSubscriberEx<>())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertTerminated(); // MBE is possible if the async group closing is slow
    }

    @Test
    public void issue6974Part2Case1ObserveOnNoCap() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int flatMapMaxConcurrency = 1_000_000;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnRequest(v -> System.out.println("Source: " + v))
        .groupBy(i -> i)
        .flatMap(gf -> gf
                     .observeOn(Schedulers.computation())
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void issue6974Part2Case1ObserveOnNoCapHide() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int flatMapMaxConcurrency = 1_000_000;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnRequest(v -> System.out.println("Source: " + v))
        .groupBy(i -> i)
        .flatMap(gf -> gf
                     .hide()
                     .observeOn(Schedulers.computation())
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6974Part2Case1ObserveOnNoCapHideLoop() {
        for (int i = 0; i < 100; i++) {
            issue6974Part2Case1ObserveOnNoCapHide();
        }
    }
    */

    @Test
    public void issue6974Part2Case1ObserveOnConditional() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnCancel(() -> System.out.println("Cancelling upstream"))
        .groupBy(i -> i, i -> i, false, groupByBufferSize,
                              sizeCap(groups * 2, notifyOnExplicitEviction))
        .flatMap(gf -> gf
                     .observeOn(Schedulers.computation())
                     .filter(v -> true)
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .subscribeWith(new TestSubscriberEx<>())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertTerminated(); // MBE is possible if the async group closing is slow
    }

    @Test
    public void issue6974Part2Case1ObserveOnConditionalHide() {
        final int groups = 20;

        // Not completed (Timed out), buffer is too small
        int groupByBufferSize = groups * 2;
        int flatMapMaxConcurrency = 2 * groups;
        boolean notifyOnExplicitEviction = false;

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .doOnCancel(() -> System.out.println("Cancelling upstream"))
        .groupBy(i -> i, i -> i, false, groupByBufferSize,
                              sizeCap(groups * 2, notifyOnExplicitEviction))
        .flatMap(gf -> gf
                     .hide()
                     .observeOn(Schedulers.computation())
                     .filter(v -> true)
                     // .take(10)
                     .take(10, TimeUnit.MILLISECONDS)
            , flatMapMaxConcurrency)
        .subscribeWith(new TestSubscriberEx<>())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertTerminated(); // MBE is possible if the async group closing is slow
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6974Part2Case1ObserveOnHideLoop() {
        for (int i = 0; i < 100; i++) {
            issue6974Part2Case1ObserveOnHide();
        }
    }
    */

    static <T> Function<Consumer<Object>, ConcurrentMap<T, Object>> ttlCapGuava(Duration ttl) {
        return itemEvictConsumer ->
            CacheBuilder
            .newBuilder()
            .expireAfterWrite(ttl)
            .removalListener(n -> {
                if (n.getCause() != com.google.common.cache.RemovalCause.EXPLICIT) {
                    try {
                        itemEvictConsumer.accept(n.getValue());
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                }
            }).<T, Object>build().asMap();
    }

    @Test
    public void issue6982Case1() {
        final int groups = 20;

        int groupByBufferSize = 2;
        int flatMapMaxConcurrency = 200 * groups;

        // ~50% of executions - Not completed (latch = 1, values = 500000, errors = 0, completions = 0, timeout!,
        // disposed!)

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .groupBy(i -> i, i -> i, false, groupByBufferSize, ttlCapGuava(Duration.ofMillis(10)))
        .flatMap(gf -> gf.observeOn(Schedulers.computation()), flatMapMaxConcurrency)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6982Case1Loop() {
        for (int i = 0; i < 200; i++) {
            System.out.println("issue6982Case1Loop "  + i);
            issue6982Case1();
        }
    }
     */

    @Test
    public void issue6982Case2() {
        final int groups = 20;

        int groupByBufferSize = groups * 30;
        int flatMapMaxConcurrency = groups * 500;
        // Always : Not completed (latch = 1, values = 14100, errors = 0, completions = 0, timeout!, disposed!)

        Flowable
        .range(1, 500_000)
        .map(i -> i % groups)
        .groupBy(i -> i, i -> i, false, groupByBufferSize, ttlCapGuava(Duration.ofMillis(10)))
        .flatMap(gf -> gf.observeOn(Schedulers.computation()), flatMapMaxConcurrency)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertNoErrors()
        .assertComplete();
    }

    /*
     * Disabled: Takes very long. Run it locally only.
    @Test
    public void issue6982Case2Loop() {
        for (int i = 0; i < 200; i++) {
            System.out.println("issue6982Case2Loop "  + i);
            issue6982Case2();
        }
    }
     */
}
