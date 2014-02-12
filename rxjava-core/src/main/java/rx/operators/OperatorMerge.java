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
import rx.observers.SynchronizedSubscriber;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-Observers/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 */
public final class OperatorMerge<T> implements Operator<T, Observable<T>> {

    @Override
    public Subscriber<Observable<T>> call(final Subscriber<? super T> outerOperation) {

        final Subscriber<T> o = new SynchronizedSubscriber<T>(outerOperation);
        return new Subscriber<Observable<T>>(outerOperation) {

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
            public void onNext(Observable<T> innerObservable) {
                runningCount.incrementAndGet();
                innerObservable.subscribe(new InnerObserver());
            }

            final class InnerObserver extends Subscriber<T> {

                public InnerObserver() {
                    super(o);
                }

                @Override
                public void onCompleted() {
                    if (runningCount.decrementAndGet() == 0 && completed) {
                        o.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    o.onError(e);
                }

                @Override
                public void onNext(T a) {
                    o.onNext(a);
                }

            };

        };

    }
}
