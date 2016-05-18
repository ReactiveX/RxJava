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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.Subscribers;

/**
 * Maps the elements of the source observable into a multimap
 * (Map&lt;K, Collection&lt;V>>) where each
 * key entry has a collection of the source's values.
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/issues/97">Issue #97</a>
 * @param <T> the value type of the input
 * @param <K> the multimap-key type
 * @param <V> the multimap-value type
 */
public final class OperatorToMultimap<T, K, V> implements Operator<Map<K, Collection<V>>, T> {
    /**
     * The default multimap factory returning a HashMap.
     * @param <K> the key type
     * @param <V> the value type
     */
    public static final class DefaultToMultimapFactory<K, V> implements Func0<Map<K, Collection<V>>> {
        @Override
        public Map<K, Collection<V>> call() {
            return new HashMap<K, Collection<V>>();
        }
    }

    /**
     * The default collection factory for a key in the multimap returning
     * an ArrayList independent of the key.
     * @param <K> the key type
     * @param <V> the value type
     */
    public static final class DefaultMultimapCollectionFactory<K, V>
            implements Func1<K, Collection<V>> {
        @Override
        public Collection<V> call(K t1) {
            return new ArrayList<V>();
        }
    }

    final Func1<? super T, ? extends K> keySelector;
    final Func1<? super T, ? extends V> valueSelector;
    private final Func0<? extends Map<K, Collection<V>>> mapFactory;
    final Func1<? super K, ? extends Collection<V>> collectionFactory;

    /**
     * ToMultimap with key selector, custom value selector,
     * default HashMap factory and default ArrayList collection factory.
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     */
    public OperatorToMultimap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        this(keySelector, valueSelector,
                new DefaultToMultimapFactory<K, V>(),
                new DefaultMultimapCollectionFactory<K, V>());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and default ArrayList collection factory.
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     * @param mapFactory function that returns a Map instance to store keys and values into
     */
    public OperatorToMultimap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory) {
        this(keySelector, valueSelector,
                mapFactory,
                new DefaultMultimapCollectionFactory<K, V>());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and custom collection factory.
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     * @param mapFactory function that returns a Map instance to store keys and values into
     * @param collectionFactory function that returns a Collection for a particular key to store values into
     */
    public OperatorToMultimap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory,
            Func1<? super K, ? extends Collection<V>> collectionFactory) {
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.mapFactory = mapFactory;
        this.collectionFactory = collectionFactory;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Map<K, Collection<V>>> subscriber) {
        
        Map<K, Collection<V>> localMap;
        
        try {
            localMap = mapFactory.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            subscriber.onError(ex);
            
            Subscriber<? super T> parent = Subscribers.empty();
            parent.unsubscribe();
            return parent;
        }
        
        final Map<K, Collection<V>> fLocalMap = localMap;
        
        return new Subscriber<T>(subscriber) {
            private Map<K, Collection<V>> map = fLocalMap;

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(T v) {
                K key;
                V value;

                try {
                    key = keySelector.call(v);
                    value = valueSelector.call(v);
                } catch (Throwable ex) {
                    Exceptions.throwOrReport(ex, subscriber);
                    return;
                }
                
                Collection<V> collection = map.get(key);
                if (collection == null) {
                    try {
                        collection = collectionFactory.call(key);
                    } catch (Throwable ex) {
                        Exceptions.throwOrReport(ex, subscriber);
                        return;
                    }
                    map.put(key, collection);
                }
                collection.add(value);
            }

            @Override
            public void onError(Throwable e) {
                map = null;
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                Map<K, Collection<V>> map0 = map;
                map = null;
                subscriber.onNext(map0);
                subscriber.onCompleted();
            }

        };
    }
}