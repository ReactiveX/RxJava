/**
 * Copyright one 2014 Netflix, Inc.
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

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Func0;
import rx.functions.Func1;

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
public final class OnSubscribeToMultimap<T, K, V> implements OnSubscribe<Map<K, Collection<V>>>, Func0<Map<K, Collection<V>>> {

    private final Func1<? super T, ? extends K> keySelector;
    private final Func1<? super T, ? extends V> valueSelector;
    private final Func0<? extends Map<K, Collection<V>>> mapFactory;
    private final Func1<? super K, ? extends Collection<V>> collectionFactory;
    private final Observable<T> source;

    /**
     * ToMultimap with key selector, custom value selector,
     * default HashMap factory and default ArrayList collection factory.
     * @param source the source Observable instance
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     */
    public OnSubscribeToMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        this(source, keySelector, valueSelector,
                null,
                DefaultMultimapCollectionFactory.<K, V>instance());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and default ArrayList collection factory.
     * @param source the source Observable instance
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     * @param mapFactory function that returns a Map instance to store keys and values into
     */
    public OnSubscribeToMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory) {
        this(source, keySelector, valueSelector,
                mapFactory,
                DefaultMultimapCollectionFactory.<K, V>instance());
    }

    /**
     * ToMultimap with key selector, custom value selector,
     * custom Map factory and custom collection factory.
     * @param source the observable source
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     * @param mapFactory function that returns a Map instance to store keys and values into
     * @param collectionFactory function that returns a Collection for a particular key to store values into
     */
    public OnSubscribeToMultimap(
            Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, Collection<V>>> mapFactory,
            Func1<? super K, ? extends Collection<V>> collectionFactory) {
        this.source = source;
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        if (mapFactory == null) {
            this.mapFactory = this;
        } else {
            this.mapFactory = mapFactory;
        }
        this.collectionFactory = collectionFactory;
    }

    // default map factory
    @Override
    public Map<K, Collection<V>> call() {
        return new HashMap<K, Collection<V>>();
    }

    @Override
    public void call(final Subscriber<? super Map<K, Collection<V>>> subscriber) {

        Map<K, Collection<V>> map;
        try {
            map = mapFactory.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            subscriber.onError(ex);
            return;
        }
        new ToMultimapSubscriber<T, K, V>(
                subscriber, map, keySelector, valueSelector, collectionFactory)
            .subscribeTo(source);
    }

    private static final class ToMultimapSubscriber<T, K, V>
        extends DeferredScalarSubscriberSafe<T,Map<K, Collection<V>>> {

        private final Func1<? super T, ? extends K> keySelector;
        private final Func1<? super T, ? extends V> valueSelector;
        private final Func1<? super K, ? extends Collection<V>> collectionFactory;

        ToMultimapSubscriber(
            Subscriber<? super Map<K, Collection<V>>> subscriber,
            Map<K, Collection<V>> map,
            Func1<? super T, ? extends K> keySelector, Func1<? super T, ? extends V> valueSelector,
            Func1<? super K, ? extends Collection<V>> collectionFactory) {
            super(subscriber);
            this.value = map;
            this.hasValue = true;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.collectionFactory = collectionFactory;
        }

        @Override
        public void onStart() {
            request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                // any interaction with keySelector, valueSelector, collectionFactory, collection or value
                // may fail unexpectedly because their behaviour is customisable by the user. For this
                // reason we wrap their calls in try-catch block.

                K key = keySelector.call(t);
                V v = valueSelector.call(t);
                Collection<V> collection = value.get(key);
                if (collection == null) {
                    collection = collectionFactory.call(key);
                    value.put(key, collection);
                }
                collection.add(v);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(ex);
            }

          }
    }

    /**
     * The default collection factory for a key in the multimap returning
     * an ArrayList independent of the key.
     * @param <K> the key type
     * @param <V> the value type
     */
    private static final class DefaultMultimapCollectionFactory<K, V>
            implements Func1<K, Collection<V>> {

        private static final DefaultMultimapCollectionFactory<Object,Object> INSTANCE = new DefaultMultimapCollectionFactory<Object, Object>();

        @SuppressWarnings("unchecked")
        static <K, V>  DefaultMultimapCollectionFactory<K,V> instance() {
            return (DefaultMultimapCollectionFactory<K, V>) INSTANCE;
        }

        @Override
        public Collection<V> call(K t1) {
            return new ArrayList<V>();
        }
    }

}