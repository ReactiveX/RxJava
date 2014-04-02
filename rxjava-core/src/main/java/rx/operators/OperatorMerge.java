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
package rx.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 */
public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> outerOperation) {

        final Subscriber<T> o = new SerializedSubscriber<T>(outerOperation);
        final CompositeSubscription childrenSubscriptions = new CompositeSubscription();
        outerOperation.add(childrenSubscriptions);

        return new Subscriber<Observable<? extends T>>(outerOperation) {

            private volatile boolean completed = false;
            private final AtomicInteger runningCount = new AtomicInteger();

            @Override
            public void onCompleted() {
                completed = true;
                if (runningCount.get() == 0) {
                    o.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(Observable<? extends T> innerObservable) {
                runningCount.incrementAndGet();
                Subscriber<T> i = new InnerObserver();
                childrenSubscriptions.add(i);
                innerObservable.unsafeSubscribe(i);
            }

            final class InnerObserver extends Subscriber<T> {

                private boolean innerCompleted = false;

                public InnerObserver() {
                }

                @Override
                public void onCompleted() {
                    if (!innerCompleted) {
                        // we check if already completed otherwise a misbehaving Observable that emits onComplete more than once
                        // will cause the runningCount to decrement multiple times.
                        innerCompleted = true;
                        if (runningCount.decrementAndGet() == 0 && completed) {
                            o.onCompleted();
                        }
                        cleanup();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                    cleanup();
                }

                @Override
                public void onNext(T a) {
                    o.onNext(a);
                }

                private void cleanup() {
                    // remove subscription onCompletion so it cleans up immediately and doesn't memory leak
                    // see https://github.com/Netflix/RxJava/issues/897
                    childrenSubscriptions.remove(this);
                }

            };

        };

    }
}
