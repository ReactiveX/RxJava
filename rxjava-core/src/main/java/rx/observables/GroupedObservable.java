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
package rx.observables;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * An {@link Observable} that has been grouped by a key whose value can be obtained using {@link #getKey()} <p>
 * 
 * @see Observable#groupBy(Func1)
 * 
 * @param <K>
 *            the type of the key
 * @param <T>
 *            the type of the elements in the group
 */
public class GroupedObservable<K, T> extends Observable<T> {
    private final K key;

    public static <K, T> GroupedObservable<K, T> from(K key, final Observable<T> o) {
        return new GroupedObservable<K, T>(key, new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> s) {
                o.unsafeSubscribe(s);
            }
        });
    }

    public GroupedObservable(K key, OnSubscribe<T> onSubscribe) {
        super(onSubscribe);
        this.key = key;
    }

    /**
     * Returns the key the elements in this observable were grouped by.
     * 
     * @return the key the elements in this observable were grouped by
     */
    public K getKey() {
        return key;
    }
}
