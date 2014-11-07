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
 */
public final class OperatorTake<T> implements Operator<T, T> {

    final int limit;

    public OperatorTake(int limit) {
        this.limit = limit;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Subscriber<T> parent = new Subscriber<T>() {

            int count = 0;
            boolean completed = false;

            @Override
            public void onCompleted() {
                if (!completed) {
                    child.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!completed) {
                    child.onError(e);
                }
            }

            @Override
            public void onNext(T i) {
                if (!isUnsubscribed()) {
                    if (++count >= limit) {
                        completed = true;
                    }
                    child.onNext(i);
                    if (completed) {
                        child.onCompleted();
                        unsubscribe();
                    }
                }
            }

            /**
             * We want to adjust the requested values based on the `take` count.
             */
            @Override
            public void setProducer(final Producer producer) {
                child.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (!completed) {
                            long c = limit - count;
                            if (n < c) {
                                producer.request(n);
                            } else {
                                producer.request(c);
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
