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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observer;
import rx.Operator;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Func1;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 */
public final class OperatorMerge<T> implements Func1<Operator<? super T>, Operator<? extends Observable<? extends T>>> {
    private final int maxConcurrent;

    public OperatorMerge() {
        maxConcurrent = Integer.MAX_VALUE;
    }

    public OperatorMerge(int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent must be positive");
        }
        this.maxConcurrent = maxConcurrent;
    }

    @Override
    public Operator<? extends Observable<? extends T>> call(final Operator<? super T> outerOperation) {

        final AtomicInteger completionCounter = new AtomicInteger(1);
        final AtomicInteger concurrentCounter = new AtomicInteger(1);
        // Concurrent* since we'll be accessing them from the inner Observers which can be on other threads
        final ConcurrentLinkedQueue<Observable<? extends T>> pending = new ConcurrentLinkedQueue<Observable<? extends T>>();

        final Observer<T> o = new SynchronizedObserver<T>(outerOperation);
        return new Operator<Observable<? extends T>>(outerOperation) {

            @Override
            public void onCompleted() {
                complete();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(Observable<? extends T> innerObservable) {
                // track so we send onComplete only when all have finished
                completionCounter.incrementAndGet();
                // check concurrency
                if (concurrentCounter.incrementAndGet() > maxConcurrent) {
                    pending.add(innerObservable);
                    concurrentCounter.decrementAndGet();
                } else {
                    // we are able to proceed
                    CompositeSubscription innerSubscription = new CompositeSubscription();
                    outerOperation.add(innerSubscription);
                    innerObservable.subscribe(Operator.create(new InnerObserver(), innerSubscription));
                }
            }

            private void complete() {
                if (completionCounter.decrementAndGet() == 0) {
                    o.onCompleted();
                    return;
                } else {
                    // not all are completed and some may still need to run
                    concurrentCounter.decrementAndGet();
                }

                // do work-stealing on whatever thread we're on and subscribe to pending observables
                if (concurrentCounter.incrementAndGet() > maxConcurrent) {
                    // still not space to run
                    concurrentCounter.decrementAndGet();
                } else {
                    // we can run
                    Observable<? extends T> outstandingObservable = pending.poll();
                    if (outstandingObservable != null) {
                        CompositeSubscription innerSubscription = new CompositeSubscription();
                        outerOperation.add(innerSubscription);
                        outstandingObservable.subscribe(Operator.create(new InnerObserver(), innerSubscription));
                    }
                }
            }

            final class InnerObserver implements Observer<T> {

                @Override
                public void onCompleted() {
                    complete();
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
