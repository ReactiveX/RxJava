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
import rx.subscriptions.SerialSubscription;

/**
 * If the Observable completes without emitting any items, subscribe to an alternate Observable. Allows for similar
 * functionality to {@link rx.internal.operators.OperatorDefaultIfEmpty} except instead of one item being emitted when
 * empty, the results of the given Observable will be emitted.
 */
public final class OperatorSwitchIfEmpty<T> implements Observable.Operator<T, T> {
    private final Observable<? extends T> alternate;

    public OperatorSwitchIfEmpty(Observable<? extends T> alternate) {
        this.alternate = alternate;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final SerialSubscription ssub = new SerialSubscription();
        final SwitchIfEmptySubscriber parent = new SwitchIfEmptySubscriber(child, ssub);
        ssub.set(parent);
        child.add(ssub);
        return parent;
    }

    private class SwitchIfEmptySubscriber extends Subscriber<T> {

        boolean empty = true;
        final AtomicLong consumerCapacity = new AtomicLong(0l);

        private final Subscriber<? super T> child;
        final SerialSubscription ssub;

        public SwitchIfEmptySubscriber(Subscriber<? super T> child, final SerialSubscription ssub) {
            this.child = child;
            this.ssub = ssub;
        }

        @Override
        public void setProducer(final Producer producer) {
            super.setProducer(new Producer() {
                @Override
                public void request(long n) {
                    if (empty) {
                        consumerCapacity.set(n);
                    }
                    producer.request(n);
                }
            });
        }

        @Override
        public void onCompleted() {
            if (!empty) {
                child.onCompleted();
            } else if (!child.isUnsubscribed()) {
                subscribeToAlternate();
            }
        }

        private void subscribeToAlternate() {
            ssub.set(alternate.unsafeSubscribe(new Subscriber<T>() {

                @Override
                public void setProducer(final Producer producer) {
                    child.setProducer(new Producer() {
                        @Override
                        public void request(long n) {
                            producer.request(n);
                        }
                    });
                }

                @Override
                public void onStart() {
                    final long capacity = consumerCapacity.get();
                    if (capacity > 0) {
                        request(capacity);
                    }
                }

                @Override
                public void onCompleted() {
                    child.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    child.onError(e);
                }

                @Override
                public void onNext(T t) {
                    child.onNext(t);
                }
            }));
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(T t) {
            empty = false;
            child.onNext(t);
        }
    }
}
