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

import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.exceptions.*;

/**
 * A producer which emits a single value and completes the child on the first positive request.
 *
 * @param <T> the value type
 */
public final class SingleProducer<T> extends AtomicBoolean implements Producer {
    /** */
    private static final long serialVersionUID = -3353584923995471404L;
    /** The child subscriber. */
    final Subscriber<? super T> child;
    /** The value to be emitted. */
    final T value;
    /**
     * Constructs the producer with the given target child and value to be emitted.
     * @param child the child subscriber, non-null
     * @param value the value to be emitted, may be null
     */
    public SingleProducer(Subscriber<? super T> child, T value) {
        this.child = child;
        this.value = value;
    }
    @Override
    public void request(long n) {
        // negative requests are bugs
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required");
        }
        // we ignore zero requests
        if (n == 0) {
            return;
        }
        // atomically change the state into emitting mode
        if (compareAndSet(false, true)) {
            // avoid re-reading the instance fields
            final Subscriber<? super T> c = child;
            T v = value;
            // eagerly check for unsubscription
            if (c.isUnsubscribed()) {
                return;
            }
            // emit the value
            try {
                c.onNext(v);
            } catch (Throwable e) {
                Exceptions.throwOrReport(e, c, v);
                return;
            }
            // eagerly check for unsubscription
            if (c.isUnsubscribed()) {
                return;
            }
            // complete the child
            c.onCompleted();
        }
    }
}
