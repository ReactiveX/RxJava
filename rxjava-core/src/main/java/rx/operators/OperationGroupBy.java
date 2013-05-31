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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.GroupedObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

public final class OperationGroupBy {

    public static <K, T, R> Func1<Observer<GroupedObservable<K, R>>, Subscription> groupBy(Observable<T> source, final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {

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

    public static <K, T> Func1<Observer<GroupedObservable<K, T>>, Subscription> groupBy(Observable<T> source, final Func1<T, K> keySelector) {
        return groupBy(source, keySelector, Functions.<T> identity());
    }

    private static class GroupBy<K, V> implements Func1<Observer<GroupedObservable<K, V>>, Subscription> {

        private final Observable<KeyValue<K, V>> source;
        private final ConcurrentHashMap<K, GroupedSubject<K, V>> groupedObservables = new ConcurrentHashMap<K, GroupedSubject<K, V>>();

        private GroupBy(Observable<KeyValue<K, V>> source) {
            this.source = source;
        }

        @Override
        public Subscription call(final Observer<GroupedObservable<K, V>> observer) {
            return source.subscribe(new Observer<KeyValue<K, V>>() {

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
                public void onError(Exception e) {
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
                        /*
                         * Technically the source should be single-threaded so we shouldn't need to do this but I am
                         * programming defensively as most operators are so this can work with a concurrent sequence
                         * if it ends up receiving one.
                         */
                        GroupedSubject<K, V> newGs = GroupedSubject.<K, V> create(value.key);
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
            });
        }
    }

    private static class GroupedSubject<K, T> extends GroupedObservable<K, T> implements Observer<T> {

        static <K, T> GroupedSubject<K, T> create(K key) {
            @SuppressWarnings("unchecked")
            final AtomicReference<Observer<T>> subscribedObserver = new AtomicReference<Observer<T>>(EMPTY_OBSERVER);

            return new GroupedSubject<K, T>(key, new Func1<Observer<T>, Subscription>() {

                @Override
                public Subscription call(Observer<T> observer) {
                    // register Observer
                    subscribedObserver.set(observer);

                    return new Subscription() {

                        @SuppressWarnings("unchecked")
                        @Override
                        public void unsubscribe() {
                            // we remove the Observer so we stop emitting further events (they will be ignored if parent continues to send)
                            subscribedObserver.set(EMPTY_OBSERVER);
                            // I don't believe we need to worry about the parent here as it's a separate sequence that would
                            // be unsubscribed to directly if that needs to happen.
                        }
                    };
                }
            }, subscribedObserver);
        }

        private final AtomicReference<Observer<T>> subscribedObserver;

        public GroupedSubject(K key, Func1<Observer<T>, Subscription> onSubscribe, AtomicReference<Observer<T>> subscribedObserver) {
            super(key, onSubscribe);
            this.subscribedObserver = subscribedObserver;
        }

        @Override
        public void onCompleted() {
            subscribedObserver.get().onCompleted();
        }

        @Override
        public void onError(Exception e) {
            subscribedObserver.get().onError(e);
        }

        @Override
        public void onNext(T v) {
            subscribedObserver.get().onNext(v);
        }

    }

    @SuppressWarnings("rawtypes")
    private static Observer EMPTY_OBSERVER = new Observer() {

        @Override
        public void onCompleted() {
            // do nothing            
        }

        @Override
        public void onError(Exception e) {
            // do nothing            
        }

        @Override
        public void onNext(Object args) {
            // do nothing
        }

    };

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

        private static <K, V> Map<K, Collection<V>> toMap(Observable<GroupedObservable<K, V>> observable) {

            final ConcurrentHashMap<K, Collection<V>> result = new ConcurrentHashMap<K, Collection<V>>();

            observable.forEach(new Action1<GroupedObservable<K, V>>() {

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
         * @throws Exception
         */
        @Test
        public void testGroupedEventStream() throws Exception {

            final AtomicInteger eventCounter = new AtomicInteger();
            final AtomicInteger subscribeCounter = new AtomicInteger();
            final AtomicInteger groupCounter = new AtomicInteger();
            final CountDownLatch latch = new CountDownLatch(1);
            final int count = 100;
            final int groupCount = 2;

            Observable<Event> es = Observable.create(new Func1<Observer<Event>, Subscription>() {

                @Override
                public Subscription call(final Observer<Event> observer) {
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

                };
            }).subscribe(new Observer<String>() {

                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
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
