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

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;

/**
 * An {@code Observable} that emits the first {@code num} items emitted by the source {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/take.png" alt="" />
 * <p>
 * You can choose to pay attention only to the first {@code num} items emitted by an {@code Observable} by using
 * the {@code take} operator. This operator returns an {@code Observable} that will invoke a subscriber's
 * {@link Subscriber#onNext onNext} function a maximum of {@code num} times before invoking
 * {@link Subscriber#onCompleted onCompleted}.
 * @param <T> the value type
 */
public final class OperatorTake<T> implements Operator<T, T> {

    final int limit;

    public OperatorTake(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit >= 0 required but it was " + limit);
        }
        this.limit = limit;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Subscriber<T> parent = new Subscriber<T>() {

            int count;
            boolean completed;

            @Override
            public void onCompleted() {
                if (!completed) {
                    completed = true;
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!completed) {
                    completed = true;
                    try {
                        child.onError(e);
                    } finally {
                        unsubscribe();
                    }
                }
            }

            @Override
            public void onNext(T i) {
                if (!isUnsubscribed() && count++ < limit) {
                    boolean stop = count == limit;
                    child.onNext(i);
                    if (stop && !completed) {
                        completed = true;
                        try {
                            child.onCompleted();
                        } finally {
                            unsubscribe();
                        }
                    }
                }
            }

            /**
             * We want to adjust the requested values based on the `take` count.
             */
            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(new Producer() {
                    
                    // keeps track of requests up to maximum of `limit`
                    final AtomicLong requested = new AtomicLong(0);
                    
                    @Override
                    public void request(long n) {
                        if (n >0 && !completed) {
                            // because requests may happen concurrently use a CAS loop to 
                            // ensure we only request as much as needed, no more no less
                            while (true) {
                                long r = requested.get();
                                long c = Math.min(n, limit - r);
                                if (c == 0)
                                    break;
                                else if (requested.compareAndSet(r, r + c)) {
                                    producer.request(c);
                                    break;
                                }
                            }
                        }
                    }
                });
            }

        };

        if (limit == 0) {
            child.onCompleted();
            parent.unsubscribe();
        }

        /*
         * We decouple the parent and child subscription so there can be multiple take() in a chain such as for
         * the groupBy Observer use case where you may take(1) on groups and take(20) on the children.
         * 
         * Thus, we only unsubscribe UPWARDS to the parent and an onComplete DOWNSTREAM.
         * 
         * However, if we receive an unsubscribe from the child we still want to propagate it upwards so we
         * register 'parent' with 'child'
         */
        child.add(parent);

        return parent;
    }

}
