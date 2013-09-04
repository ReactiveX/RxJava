/**
 * Copyright 2013 Netflix, Inc.
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.observables.GroupedObservable;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

/**
 * Groups the items emitted by an Observable according to a specified criterion, and emits these
 * grouped items as Observables, one Observable per group.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
 */
public final class OperationGroupBy {

    public static <K, T, R> OnSubscribeFunc<GroupedObservable<K, R>> groupBy(Observable<? extends T> source, final Func1<? super T, ? extends K> keySelector, final Func1<? super T, ? extends R> elementSelector) {

        final Observable<KeyValue<K, R>> keyval = source.map(new Func1<T, KeyValue<K, R>>() {
            @Override
            public KeyValue<K, R> call(T t) {
                K key = keySelector.call(t);
                R value = elementSelector.call(t);

                return new KeyValue<K, R>(key, value);
            }
        });

        return new GroupBy<K, R>(keyval);
    }

    public static <K, T> OnSubscribeFunc<GroupedObservable<K, T>> groupBy(Observable<? extends T> source, final Func1<? super T, ? extends K> keySelector) {
        return groupBy(source, keySelector, Functions.<T> identity());
    }

    private static class GroupBy<K, V> implements OnSubscribeFunc<GroupedObservable<K, V>> {

        private final Observable<KeyValue<K, V>> source;
        private final ConcurrentHashMap<K, GroupedSubject<K, V>> groupedObservables = new ConcurrentHashMap<K, GroupedSubject<K, V>>();
        private final SafeObservableSubscription actualParentSubscription = new SafeObservableSubscription();
        private final AtomicInteger numGroupSubscriptions = new AtomicInteger();
        private final AtomicBoolean unsubscribeRequested = new AtomicBoolean(false);

        private GroupBy(Observable<KeyValue<K, V>> source) {
            this.source = source;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super GroupedObservable<K, V>> observer) {
            final GroupBy<K, V> _this = this;
            actualParentSubscription.wrap(source.subscribe(new Observer<KeyValue<K, V>>() {

                @Override
                public void onCompleted() {
                    // we need to propagate to all children I imagine ... we can't just leave all of those Observable/Observers hanging
                    for (GroupedSubject<K, V> o : groupedObservables.values()) {
                        o.onCompleted();
                    }
                    // now the parent
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    // we need to propagate to all children I imagine ... we can't just leave all of those Observable/Observers hanging 
                    for (GroupedSubject<K, V> o : groupedObservables.values()) {
                        o.onError(e);
                    }
                    // now the parent
                    observer.onError(e);
                }

                @Override
                public void onNext(KeyValue<K, V> value) {
                    GroupedSubject<K, V> gs = groupedObservables.get(value.key);
                    if (gs == null) {
                        if (unsubscribeRequested.get()) {
                            // unsubscribe has been requested so don't create new groups
                            // only send data to groups already created
                            return;
                        }
                        /*
                         * Technically the source should be single-threaded so we shouldn't need to do this but I am
                         * programming defensively as most operators are so this can work with a concurrent sequence
                         * if it ends up receiving one.
                         */
                        GroupedSubject<K, V> newGs = GroupedSubject.<K, V> create(value.key, _this);
                        GroupedSubject<K, V> existing = groupedObservables.putIfAbsent(value.key, newGs);
                        if (existing == null) {
                            // we won so use the one we created
                            gs = newGs;
                            // since we won the creation we emit this new GroupedObservable
                            observer.onNext(gs);
                        } else {
                            // another thread beat us so use the existing one
                            gs = existing;
                        }
                    }
                    gs.onNext(value.value);
                }
            }));

            return new Subscription() {

                @Override
                public void unsubscribe() {
                    if (numGroupSubscriptions.get() == 0) {
                        // if we have no group subscriptions we will unsubscribe
                        actualParentSubscription.unsubscribe();
                        // otherwise we mark to not send any more groups (waiting on existing groups to finish)
                        unsubscribeRequested.set(true);
                    }
                }
            };
        }

        /**
         * Children notify of being subscribed to.
         * 
         * @param key
         */
        private void subscribeKey(K key) {
            numGroupSubscriptions.incrementAndGet();
        }

        /**
         * Children notify of being unsubscribed from.
         * 
         * @param key
         */
        private void unsubscribeKey(K key) {
            int c = numGroupSubscriptions.decrementAndGet();
            if (c == 0) {
                actualParentSubscription.unsubscribe();
            }
        }
    }

    private static class GroupedSubject<K, T> extends GroupedObservable<K, T> implements Observer<T> {

        static <K, T> GroupedSubject<K, T> create(final K key, final GroupBy<K, T> parent) {
            final AtomicReference<Observer<? super T>> subscribedObserver = new AtomicReference<Observer<? super T>>(OperationGroupBy.<T>emptyObserver());
            return new GroupedSubject<K, T>(key, new OnSubscribeFunc<T>() {

                private final SafeObservableSubscription subscription = new SafeObservableSubscription();

                @Override
                public Subscription onSubscribe(Observer<? super T> observer) {
                    // register Observer
                    subscribedObserver.set(observer);

                    parent.subscribeKey(key);

                    return subscription.wrap(new Subscription() {
                        @Override
                        public void unsubscribe() {
                            // we remove the Observer so we stop emitting further events (they will be ignored if parent continues to send)
                            subscribedObserver.set(OperationGroupBy.<T>emptyObserver());
                            // now we need to notify the parent that we're unsubscribed
                            parent.unsubscribeKey(key);
                        }
                    });
                }
            }, subscribedObserver);
        }

        private final AtomicReference<Observer<? super T>> subscribedObserver;

        public GroupedSubject(K key, OnSubscribeFunc<T> onSubscribe, AtomicReference<Observer<? super T>> subscribedObserver) {
            super(key, onSubscribe);
            this.subscribedObserver = subscribedObserver;
        }

        @Override
        public void onCompleted() {
            subscribedObserver.get().onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            subscribedObserver.get().onError(e);
        }

        @Override
        public void onNext(T v) {
            subscribedObserver.get().onNext(v);
        }

    }

    private static <T> Observer<T> emptyObserver() { 
        return new Observer<T>() {
            @Override
            public void onCompleted() {
                // do nothing            
            }
    
            @Override
            public void onError(Throwable e) {
                // do nothing            
            }
    
            @Override
            public void onNext(T t) {
                // do nothing
            }
        };
    }

    private static class KeyValue<K, V> {
        private final K key;
        private final V value;

        private KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class UnitTest {
        final Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String s) {
                return s.length();
            }
        };

        @Test
        public void testGroupBy() {
            Observable<String> source = Observable.from("one", "two", "three", "four", "five", "six");
            Observable<GroupedObservable<Integer, String>> grouped = Observable.create(groupBy(source, length));

            Map<Integer, Collection<String>> map = toMap(grouped);

            assertEquals(3, map.size());
            assertArrayEquals(Arrays.asList("one", "two", "six").toArray(), map.get(3).toArray());
            assertArrayEquals(Arrays.asList("four", "five").toArray(), map.get(4).toArray());
            assertArrayEquals(Arrays.asList("three").toArray(), map.get(5).toArray());
        }

        @Test
        public void testEmpty() {
            Observable<String> source = Observable.from();
            Observable<GroupedObservable<Integer, String>> grouped = Observable.create(groupBy(source, length));

            Map<Integer, Collection<String>> map = toMap(grouped);

            assertTrue(map.isEmpty());
        }

        @Test
        public void testError() {
            Observable<String> sourceStrings = Observable.from("one", "two", "three", "four", "five", "six");
            Observable<String> errorSource = Observable.error(new RuntimeException("forced failure"));
            @SuppressWarnings("unchecked")
            Observable<String> source = Observable.concat(sourceStrings, errorSource);

            Observable<GroupedObservable<Integer, String>> grouped = Observable.create(groupBy(source, length));

            final AtomicInteger groupCounter = new AtomicInteger();
            final AtomicInteger eventCounter = new AtomicInteger();
            final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

            grouped.mapMany(new Func1<GroupedObservable<Integer, String>, Observable<String>>() {

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

            Observable<Event> es = Observable.create(new OnSubscribeFunc<Event>() {

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
            }).mapMany(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

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
        public void testUnsubscribe() throws InterruptedException {

            final AtomicInteger eventCounter = new AtomicInteger();
            final AtomicInteger subscribeCounter = new AtomicInteger();
            final AtomicInteger groupCounter = new AtomicInteger();
            final AtomicInteger sentEventCounter = new AtomicInteger();
            final CountDownLatch latch = new CountDownLatch(1);
            final int count = 100;
            final int groupCount = 2;

            Observable<Event> es = Observable.create(new OnSubscribeFunc<Event>() {

                @Override
                public Subscription onSubscribe(final Observer<? super Event> observer) {
                    final BooleanSubscription s = new BooleanSubscription();
                    System.out.println("testUnsubscribe => *** Subscribing to EventStream ***");
                    subscribeCounter.incrementAndGet();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            for (int i = 0; i < count; i++) {
                                if (s.isUnsubscribed()) {
                                    break;
                                }
                                Event e = new Event();
                                e.source = i % groupCount;
                                e.message = "Event-" + i;
                                observer.onNext(e);
                                sentEventCounter.incrementAndGet();
                            }
                            observer.onCompleted();
                        }

                    }).start();
                    return s;
                }

            });

            es.groupBy(new Func1<Event, Integer>() {

                @Override
                public Integer call(Event e) {
                    return e.source;
                }
            })
                    .take(1) // we want only the first group
                    .mapMany(new Func1<GroupedObservable<Integer, Event>, Observable<String>>() {

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
            assertEquals(1, groupCounter.get());
            assertEquals(20, eventCounter.get());
            // sentEvents will go until 'eventCounter' hits 20 and then unsubscribes
            // which means it will also send (but ignore) the 19/20 events for the other group
            // It will not however send all 100 events.
            assertEquals(39, sentEventCounter.get(), 10);
            // gave it a delta of 10 to account for the threading/unsubscription race condition which can vary depending on a machines performance, thread-scheduler, etc
        }

        private static class Event {
            int source;
            String message;

            @Override
            public String toString() {
                return "Event => source: " + source + " message: " + message;
            }
        }

    }

}
