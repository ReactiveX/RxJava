/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.plugins.RxJavaPlugins;

/**
 * A Subscription that holds a constant value and emits it only when requested.
 * @param <T> the value type
 */
public final class ScalarSubscription<T> extends AtomicBoolean implements Subscription {
    /** */
    private static final long serialVersionUID = -3830916580126663321L;
    /** The single value to emit, set to null. */
    private final T value;
    /** The actual subscriber. */
    private final Subscriber<? super T> subscriber;
    
    public ScalarSubscription(Subscriber<? super T> subscriber, T value) {
        this.subscriber = subscriber;
        this.value = value;
    }
    
    @Override
    public void request(long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return;
        }
        if (compareAndSet(false, true)) {
            T v = value;
            Subscriber<? super T> s = subscriber;

            s.onNext(v);
            s.onComplete();
        }
        
    }
    
    @Override
    public void cancel() {
        lazySet(true);
    }
}
