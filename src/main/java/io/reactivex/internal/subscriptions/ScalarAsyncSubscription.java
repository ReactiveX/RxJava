/*
 * Copyright 2011-2015 David Karnok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.internal.subscriptions;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class ScalarAsyncSubscription<T> extends AtomicInteger implements Subscription {
    /** */
    private static final long serialVersionUID = -7135031000854703151L;
    
    private T value;
    
    private final Subscriber<? super T> subscriber;
    
    private static final int EMPTY = 0;
    private static final int REQUESTED = 1;
    private static final int VALUE = 2;
    
    public ScalarAsyncSubscription(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
    }
    
    @Override
    public void request(long n) {
        if (n <= 0) {
            RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
            return;
        }
        for (;;) {
            int state = get();
            if ((state & REQUESTED) != 0) {
                return;
            }
            if (state == EMPTY) {
                if (compareAndSet(state, REQUESTED)) {
                    return;
                }
            } else
            if (state == VALUE) {
                if (compareAndSet(state, REQUESTED | VALUE)) {
                    subscriber.onNext(value);
                    subscriber.onComplete();
                    return;
                }
            }
        }
    }
    
    public void setValue(T v) {
        Objects.requireNonNull(v);
        
        for (;;) {
            int state = get();
            if ((state & VALUE) != 0) {
                return;
            } else
            if (state == EMPTY) {
                value = v;
                if (compareAndSet(state, VALUE)) {
                    return;
                }
            } else
            if (state == REQUESTED) {
                if (compareAndSet(state, REQUESTED | VALUE)) {
                    subscriber.onNext(v);
                    subscriber.onComplete();
                    return;
                }
            }
        }
    }
    
    @Override
    public void cancel() {
        int state = get();
        if (state != (REQUESTED | VALUE)) {
            state = getAndSet(REQUESTED | VALUE);
            if (state != (REQUESTED | VALUE)) {
                value = null;
            }
        }
    }
    
    public boolean isComplete() {
        return get() == (REQUESTED | VALUE);
    }
}
