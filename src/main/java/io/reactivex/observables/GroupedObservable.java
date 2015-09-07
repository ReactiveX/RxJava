/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.observables;

import org.reactivestreams.Publisher;

import io.reactivex.Observable;

public class GroupedObservable<K, T> extends Observable<T> {
    final K key;
    protected GroupedObservable(Publisher<T> onSubscribe, K key) {
        super(onSubscribe);
        this.key = key;
    }
    
    // FIXME short one or long one?
    public K key() {
        return key;
    }
    
    public K getKey() {
        return key;
    }
}
