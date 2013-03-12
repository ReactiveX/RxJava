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
package rx.observables;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * An {@link Observable} that has been grouped by a key whose value can be obtained using {@link #getKey()} <p>
 * 
 * @see {@link Observable#groupBy(Observable, Func1)}
 * 
 * @param <K>
 * @param <T>
 */
public class GroupedObservable<K, T> extends Observable<T> {
    private final K key;

    public GroupedObservable(K key, Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
        this.key = key;
    }

    public K getKey() {
        return key;
    }

}
