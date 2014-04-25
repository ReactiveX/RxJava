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

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Maps the elements of the source observable into a java.util.Map instance and
 * emits that once the source observable completes.
 * 
 * @see <a href='https://github.com/Netflix/RxJava/issues/96'>Issue #96</a>
 */
public final class OperatorToMap<T, K, V> implements Operator<Map<K, V>, T> {

    /**
     * The default map factory.
     */
    public static final class DefaultToMapFactory<K, V> implements Func0<Map<K, V>> {
        @Override
        public Map<K, V> call() {
            return new HashMap<K, V>();
        }
    }


    private final Func1<? super T, ? extends K> keySelector;

    private final Func1<? super T, ? extends V> valueSelector;

    private final Func0<? extends Map<K, V>> mapFactory;


    /**
     * ToMap with key selector, value selector and default HashMap factory.
     */
    public OperatorToMap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector) {
        this(keySelector, valueSelector, new DefaultToMapFactory<K, V>());
    }


    /**
     * ToMap with key selector, value selector and custom Map factory.
     */
    public OperatorToMap(
            Func1<? super T, ? extends K> keySelector,
            Func1<? super T, ? extends V> valueSelector,
            Func0<? extends Map<K, V>> mapFactory) {
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.mapFactory = mapFactory;

    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Map<K, V>> subscriber) {
        return new Subscriber<T>(subscriber) {

            private Map<K, V> map = mapFactory.call();

            @Override
            public void onNext(T v) {
                K key = keySelector.call(v);
                V value = valueSelector.call(v);
                map.put(key, value);
            }

            @Override
            public void onError(Throwable e) {
                map = null;
                subscriber.onError(e);
            }

            @Override
            public void onCompleted() {
                Map<K, V> map0 = map;
                map = null;
                subscriber.onNext(map0);
                subscriber.onCompleted();
            }
        };
    }
}
