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

import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.plugins.RxJavaHooks;

public class OperatorOnBackpressureDrop<T> implements Operator<T, T> {

    final Action1<? super T> onDrop;

    /** Lazy initialization via inner-class holder. */
    static final class Holder {
        /** A singleton instance. */
        static final OperatorOnBackpressureDrop<Object> INSTANCE = new OperatorOnBackpressureDrop<Object>();
    }

    /**
     * @param <T> the value type
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> OperatorOnBackpressureDrop<T> instance() {
        return (OperatorOnBackpressureDrop<T>)Holder.INSTANCE;
    }

    OperatorOnBackpressureDrop() {
        this(null);
    }

    public OperatorOnBackpressureDrop(Action1<? super T> onDrop) {
        this.onDrop = onDrop;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final AtomicLong requested = new AtomicLong();

        child.setProducer(new Producer() {

            @Override
            public void request(long n) {
                BackpressureUtils.getAndAddRequest(requested, n);
            }

        });
        return new Subscriber<T>(child) {

            boolean done;

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!done) {
                    done = true;
                    child.onError(e);
                } else {
                   RxJavaHooks.onError(e);
                }
            }

            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                if (requested.get() > 0) {
                    child.onNext(t);
                    requested.decrementAndGet();
                } else {
                    // item dropped
                    if (onDrop != null) {
                        try {
                            onDrop.call(t);
                        } catch (Throwable e) {
                            Exceptions.throwOrReport(e, this, t);
                        }
                    }
                }
            }

        };
    }

}
