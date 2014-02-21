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
 * Maps the elements of the source observable into a java.util.Map instance and
 * emits that once the source observable completes.
 * 
 * @see <a href='https://github.com/Netflix/RxJava/issues/96'>Issue #96</a>
 */
public class OperationToMap {
    /**
     * ToMap with key selector, identity value selector and default HashMap factory.
     */
    public static <T, K> OnSubscribeFunc<Map<K, T>> toMap(Observable<T> source,
            Func1<? super T, ? extends K> keySelector) {
        return new ToMap<T, K, T>(source, keySelector,
                Functions.<T> identity(), new DefaultToMapFactory<K, T>());
    }

    /**
     * ToMap with key selector, value selector and default HashMap factory.
     */
    public static <T, K, V> OnSubscribeFunc<Map<K, V>> toMap(Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        return new ToMap<T, K, V>(source, keySelector,
                valueSelector, new DefaultToMapFactory<K, V>());
    }

    /**
     * ToMap with key selector, value selector and custom Map factory.
     */
    public static <T, K, V> OnSubscribeFunc<Map<K, V>> toMap(Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, V>> mapFactory) {
        return new ToMap<T, K, V>(source, keySelector,
                valueSelector, mapFactory);
    }

    /** The default map factory. */
    public static class DefaultToMapFactory<K, V> implements Func0<Map<K, V>> {
        @Override
        public Map<K, V> call() {
            return new HashMap<K, V>();
        }
    }

    /**
     * Maps the elements of the source observable into a java.util.Map instance
     * returned by the mapFactory function by using the keySelector and
     * valueSelector.
     * 
     * @param <T>
     *            the source's value type
     * @param <K>
     *            the key type
     * @param <V>
     *            the value type
     */
    public static class ToMap<T, K, V> implements OnSubscribeFunc<Map<K, V>> {
        /** The source. */
        private final Observable<T> source;
        /** Key extractor. */
        private final Func1<? super T, ? extends K> keySelector;
        /** Value extractor. */
        private final Func1<? super T, ? extends V> valueSelector;
        /** Map factory. */
        private final Func0<? extends Map<K, V>> mapFactory;

        public ToMap(
                Observable<T> source,
                Func1<? super T, ? extends K> keySelector,
                Func1<? super T, ? extends V> valueSelector,
                Func0<? extends Map<K, V>> mapFactory) {
            this.source = source;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.mapFactory = mapFactory;

        }

        @Override
        public Subscription onSubscribe(Observer<? super Map<K, V>> t1) {
            Map<K, V> map;
            try {
                map = mapFactory.call();
            } catch (Throwable t) {
                t1.onError(t);
                return Subscriptions.empty();
            }
            return source.subscribe(new ToMapObserver<K, V, T>(
                    t1, keySelector, valueSelector, map));
        }

        /**
         * Observer that collects the source values of T into
         * a map.
         */
        public static class ToMapObserver<K, V, T> implements Observer<T> {
            /** The map. */
            Map<K, V> map;
            /** Key extractor. */
            private final Func1<? super T, ? extends K> keySelector;
            /** Value extractor. */
            private final Func1<? super T, ? extends V> valueSelector;
            /** The observer who is receiving the completed map. */
            private final Observer<? super Map<K, V>> t1;

            public ToMapObserver(
                    Observer<? super Map<K, V>> t1,
                    Func1<? super T, ? extends K> keySelector,
                    Func1<? super T, ? extends V> valueSelector,
                    Map<K, V> map) {
                this.map = map;
                this.t1 = t1;
                this.keySelector = keySelector;
                this.valueSelector = valueSelector;
            }

            @Override
            public void onNext(T args) {
                K key = keySelector.call(args);
                V value = valueSelector.call(args);
                map.put(key, value);
            }

            @Override
            public void onError(Throwable e) {
                map = null;
                t1.onError(e);
            }

            @Override
            public void onCompleted() {
                Map<K, V> map0 = map;
                map = null;
                t1.onNext(map0);
                t1.onCompleted();
            }
        }
    }
}
