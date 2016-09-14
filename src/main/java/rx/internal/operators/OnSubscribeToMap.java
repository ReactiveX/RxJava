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

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Maps the elements of the source observable into a java.util.Map instance and
 * emits that once the source observable completes.
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/issues/96">Issue #96</a>
 * @param <T> the value type of the input
 * @param <K> the map-key type
 * @param <V> the map-value type
 */
public final class OnSubscribeToMap<T, K, V> implements OnSubscribe<Map<K, V>>, Func0<Map<K, V>> {

    final Observable<T> source;

    final Func1<? super T, ? extends K> keySelector;

    final Func1<? super T, ? extends V> valueSelector;

    final Func0<? extends Map<K, V>> mapFactory;

    /**
     * ToMap with key selector, value selector and default HashMap factory.
     * @param source the source Observable instance
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     */
    public OnSubscribeToMap(Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        this(source, keySelector, valueSelector, null);
    }


    /**
     * ToMap with key selector, value selector and custom Map factory.
     * @param source the source Observable instance
     * @param keySelector the function extracting the map-key from the main value
     * @param valueSelector the function extracting the map-value from the main value
     * @param mapFactory function that returns a Map instance to store keys and values into
     */
    public OnSubscribeToMap(Observable<T> source,
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, V>> mapFactory) {
        this.source = source;
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        if (mapFactory == null) {
            this.mapFactory = this;
        } else {
            this.mapFactory = mapFactory;
        }
    }

    @Override
    public Map<K, V> call() {
        return new HashMap<K, V>();
    }

    @Override
    public void call(final Subscriber<? super Map<K, V>> subscriber) {
        Map<K, V> map;
        try {
            map = mapFactory.call();
        } catch (Throwable ex) {
            Exceptions.throwOrReport(ex, subscriber);
            return;
        }
        new ToMapSubscriber<T, K, V>(subscriber, map, keySelector, valueSelector)
            .subscribeTo(source);
    }

    static final class ToMapSubscriber<T, K, V> extends DeferredScalarSubscriberSafe<T, Map<K,V>> {

        final Func1<? super T, ? extends K> keySelector;
        final Func1<? super T, ? extends V> valueSelector;

        ToMapSubscriber(Subscriber<? super Map<K,V>> actual, Map<K,V> map, Func1<? super T, ? extends K> keySelector,
                Func1<? super T, ? extends V> valueSelector) {
            super(actual);
            this.value = map;
            this.hasValue = true;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
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
                K key = keySelector.call(t);
                V val = valueSelector.call(t);
                value.put(key, val);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(ex);
            }
        }
    }

}
