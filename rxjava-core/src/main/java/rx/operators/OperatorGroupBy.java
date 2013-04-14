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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.GroupedObservable;
import rx.util.functions.Func1;
import rx.util.functions.Functions;

public final class OperatorGroupBy {

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

        private GroupBy(Observable<KeyValue<K, V>> source) {
            this.source = source;
        }

        @Override
        public Subscription call(final Observer<GroupedObservable<K, V>> observer) {
            return source.subscribe(new GroupByObserver(observer));
        }

        private class GroupByObserver implements Observer<KeyValue<K, V>> {
            private final Observer<GroupedObservable<K, V>> underlying;

            private final ConcurrentHashMap<K, Boolean> keys = new ConcurrentHashMap<K, Boolean>();

            private GroupByObserver(Observer<GroupedObservable<K, V>> underlying) {
                this.underlying = underlying;
            }

            @Override
            public void onCompleted() {
                underlying.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                underlying.onError(e);
            }

            @Override
            public void onNext(final KeyValue<K, V> args) {
                K key = args.key;
                boolean newGroup = keys.putIfAbsent(key, true) == null;
                if (newGroup) {
                    underlying.onNext(buildObservableFor(source, key));
                }
            }
        }
    }

    private static <K, R> GroupedObservable<K, R> buildObservableFor(Observable<KeyValue<K, R>> source, final K key) {
        final Observable<R> observable = source.filter(new Func1<KeyValue<K, R>, Boolean>() {
            @Override
            public Boolean call(KeyValue<K, R> pair) {
                return key.equals(pair.key);
            }
        }).map(new Func1<KeyValue<K, R>, R>() {
            @Override
            public R call(KeyValue<K, R> pair) {
                return pair.value;
            }
        });
        return new GroupedObservable<K, R>(key, new Func1<Observer<R>, Subscription>() {

            @Override
            public Subscription call(Observer<R> observer) {
                return observable.subscribe(observer);
            }

        });
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

            Map<Integer, List<String>> map = toMap(grouped);

            assertEquals(3, map.size());
            assertEquals(Arrays.asList("one", "two", "six"), map.get(3));
            assertEquals(Arrays.asList("four", "five"), map.get(4));
            assertEquals(Arrays.asList("three"), map.get(5));

        }

        @Test
        public void testEmpty() {
            Observable<String> source = Observable.from();
            Observable<GroupedObservable<Integer, String>> grouped = Observable.create(groupBy(source, length));

            Map<Integer, List<String>> map = toMap(grouped);

            assertTrue(map.isEmpty());
        }

        private static <K, V> Map<K, List<V>> toMap(Observable<GroupedObservable<K, V>> observable) {
            Map<K, List<V>> result = new HashMap<K, List<V>>();
            for (GroupedObservable<K, V> g : observable.toIterable()) {
                K key = g.getKey();

                for (V value : g.toIterable()) {
                    List<V> values = result.get(key);
                    if (values == null) {
                        values = new ArrayList<V>();
                        result.put(key, values);
                    }

                    values.add(value);
                }

            }

            return result;
        }

    }

}
