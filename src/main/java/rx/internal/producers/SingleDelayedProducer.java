/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.producers;

import java.util.concurrent.atomic.AtomicInteger;

import rx.*;
import rx.exceptions.*;

/**
 * Producer that emits a single value and completes the child subscriber once that
 * single value is set on it and the child requested items (maybe both asynchronously).
 *
 * @param <T> the value type
 */
public final class SingleDelayedProducer<T> extends AtomicInteger implements Producer {
    /** */
    private static final long serialVersionUID = -2873467947112093874L;
    /** The child to emit the value and completion once possible. */
    final Subscriber<? super T> child;
    /** The value to emit.*/
    T value;
    
    static final int NO_REQUEST_NO_VALUE = 0;
    static final int NO_REQUEST_HAS_VALUE = 1;
    static final int HAS_REQUEST_NO_VALUE = 2;
    static final int HAS_REQUEST_HAS_VALUE = 3;
  
    /**
     * Constructor, wraps the target child subscriber.
     * @param child the child subscriber, not null
     */
    public SingleDelayedProducer(Subscriber<? super T> child) {
        this.child = child;
    }
     
    @Override
    public void request(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required");
        }
        if (n == 0) {
            return;
        }
        for (;;) {
            int s = get();
            if (s == NO_REQUEST_NO_VALUE) {
                if (!compareAndSet(NO_REQUEST_NO_VALUE, HAS_REQUEST_NO_VALUE)) {
                    continue;
                }
            } else
            if (s == NO_REQUEST_HAS_VALUE) {
                if (compareAndSet(NO_REQUEST_HAS_VALUE, HAS_REQUEST_HAS_VALUE)) {
                    emit(child, value);
                }
            }
            return;
        }
    }
     
    public void setValue(T value) {
        for (;;) {
            int s = get();
            if (s == NO_REQUEST_NO_VALUE) {
                this.value = value;
                if (!compareAndSet(NO_REQUEST_NO_VALUE, NO_REQUEST_HAS_VALUE)) {
                    continue;
                }
            } else
            if (s == HAS_REQUEST_NO_VALUE) {
                if (compareAndSet(HAS_REQUEST_NO_VALUE, HAS_REQUEST_HAS_VALUE)) {
                    emit(child, value);
                }
            }
            return;
        }
    }
    /**
     * Emits the given value to the child subscriber and completes it
     * and checks for unsubscriptions eagerly.
     * @param c
     * @param v
     */
    private static <T> void emit(Subscriber<? super T> c, T v) {
        if (c.isUnsubscribed()) {
            return;
        }
        try {
            c.onNext(v);
        } catch (Throwable e) {
            Exceptions.throwOrReport(e, c, v);
            return;
        }
        if (c.isUnsubscribed()) {
            return;
        }
        c.onCompleted();
        
    }
}