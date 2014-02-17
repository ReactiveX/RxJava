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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.subscriptions.Subscriptions;

/**
 * Maps the elements of the source observable into a multimap
 * (Map&lt;K, Collection&lt;V>>) where each
 * key entry has a collection of the source's values.
 * 
 * @see <a href='https://github.com/Netflix/RxJava/issues/97'>Issue #97</a>
 */
public class OperationToMultimap {
    /**
     * ToMultimap with key selector, identitiy value selector,
     * default HashMap factory and default ArrayList collection factory.
     */
    public static <T, K> OnSubscribeFunc<Map<K, Collection<T>>> toMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector
            ) {
        return new ToMultimap<T, K, T>(
                source, keySelector, Functions.<T> identity(),
                new DefaultToMultimapFactory<K, T>(),
                new DefaultMultimapCollectionFactory<K, T>());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * default HashMap factory and default ArrayList collection factory.
     */
    public static <T, K, V> OnSubscribeFunc<Map<K, Collection<V>>> toMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector
            ) {
        return new ToMultimap<T, K, V>(
                source, keySelector, valueSelector,
                new DefaultToMultimapFactory<K, V>(),
                new DefaultMultimapCollectionFactory<K, V>());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and default ArrayList collection factory.
     */
    public static <T, K, V> OnSubscribeFunc<Map<K, Collection<V>>> toMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory
            ) {
        return new ToMultimap<T, K, V>(
                source, keySelector, valueSelector,
                mapFactory,
                new DefaultMultimapCollectionFactory<K, V>());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and custom collection factory.
     */
    public static <T, K, V> OnSubscribeFunc<Map<K, Collection<V>>> toMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory,
            Func1<? super K, ? extends Collection<V>> collectionFactory
            ) {
        return new ToMultimap<T, K, V>(
                source, keySelector, valueSelector,
                mapFactory,
                collectionFactory);
    }

    /**
     * The default multimap factory returning a HashMap.
     */
    public static class DefaultToMultimapFactory<K, V> implements Func0<Map<K, Collection<V>>> {
        @Override
        public Map<K, Collection<V>> call() {
            return new HashMap<K, Collection<V>>();
        }
    }

    /**
     * The default collection factory for a key in the multimap returning
     * an ArrayList independent of the key.
     */
    public static class DefaultMultimapCollectionFactory<K, V>
            implements Func1<K, Collection<V>> {
        @Override
        public Collection<V> call(K t1) {
            return new ArrayList<V>();
        }
    }

    /**
     * Maps the elements of the source observable int a multimap customized
     * by various selectors and factories.
     */
    public static class ToMultimap<T, K, V> implements OnSubscribeFunc<Map<K, Collection<V>>> {
        private final Observable<T> source;
        private final Func1<? super T, ? extends K> keySelector;
        private final Func1<? super T, ? extends V> valueSelector;
        private final Func0<? extends Map<K, Collection<V>>> mapFactory;
        private final Func1<? super K, ? extends Collection<V>> collectionFactory;

        public ToMultimap(
                Observable<T> source,
                Func1<? super T, ? extends K> keySelector,
                Func1<? super T, ? extends V> valueSelector,
                Func0<? extends Map<K, Collection<V>>> mapFactory,
                Func1<? super K, ? extends Collection<V>> collectionFactory) {
            this.source = source;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.mapFactory = mapFactory;
            this.collectionFactory = collectionFactory;
        }

        @Override
        public Subscription onSubscribe(Observer<? super Map<K, Collection<V>>> t1) {
            Map<K, Collection<V>> map;
            try {
                map = mapFactory.call();
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            return source.subscribe(new ToMultimapObserver<T, K, V>(
                    t1, keySelector, valueSelector, map, collectionFactory
                    ));
        }

        /**
         * Observer that collects the source values of Ts into a multimap.
         */
        public static class ToMultimapObserver<T, K, V> implements Observer<T> {
            private final Func1<? super T, ? extends K> keySelector;
            private final Func1<? super T, ? extends V> valueSelector;
            private final Func1<? super K, ? extends Collection<V>> collectionFactory;
            private Map<K, Collection<V>> map;
            private Observer<? super Map<K, Collection<V>>> t1;

            public ToMultimapObserver(
                    Observer<? super Map<K, Collection<V>>> t1,
                    Func1<? super T, ? extends K> keySelector,
                    Func1<? super T, ? extends V> valueSelector,
                    Map<K, Collection<V>> map,
                    Func1<? super K, ? extends Collection<V>> collectionFactory) {
                this.t1 = t1;
                this.keySelector = keySelector;
                this.valueSelector = valueSelector;
                this.collectionFactory = collectionFactory;
                this.map = map;
            }

            @Override
            public void onNext(T args) {
                K key = keySelector.call(args);
                V value = valueSelector.call(args);
                Collection<V> collection = map.get(key);
                if (collection == null) {
                    collection = collectionFactory.call(key);
                    map.put(key, collection);
                }
                collection.add(value);
            }

            @Override
            public void onError(Throwable e) {
                map = null;
                t1.onError(e);
            }

            @Override
            public void onCompleted() {
                Map<K, Collection<V>> map0 = map;
                map = null;
                t1.onNext(map0);
                t1.onCompleted();
            }
        }
    }
}
