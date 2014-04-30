/**
 * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/merge.png">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 * 
 * @param <T> the emitted value type
 */
public final class OperatorMergeMaxConcurrent<T> implements Operator<T, Observable<? extends T>> {
    final int maxConcurrency;

    public OperatorMergeMaxConcurrent(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
    
    @Override
    public Subscriber<? super Observable<? extends T>> call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        return new Subscriber<Observable<? extends T>>(child) {
            final Subscriber<?> self = this;
            final AtomicInteger wip = new AtomicInteger(1);
            final Object guard = new Object();
            /** Guarded by guard. */
            int active;
            /** Guarded by guard. */
            Queue<Observable<? extends T>> queue = new LinkedList<Observable<? extends T>>();

            @Override
            public void onNext(Observable<? extends T> t) {
                synchronized (guard) {
                    queue.add(t);
                }
                subscribeNext();
            }

            void subscribeNext() {
                Observable<? extends T> t;
                synchronized (guard) {
                    t = queue.peek();
                    if (t == null || active >= maxConcurrency) {
                        return;
                    }
                    active++;
                    queue.poll();
                }
                wip.incrementAndGet();

                Subscriber<T> itemSub = new Subscriber<T>() {
                    boolean once = true;
                    @Override
                    public void onNext(T t) {
                        s.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        self.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        if (once) {
                            once = false;
                            synchronized (guard) {
                                active--;
                            }
                            csub.remove(this);

                            subscribeNext();

                            self.onCompleted();
                        }
                    }

                };
                csub.add(itemSub);

                t.unsafeSubscribe(itemSub);
            }
            
            @Override
            public void onError(Throwable e) {
                s.onError(e);
                unsubscribe();
            }

            @Override
            public void onCompleted() {
                if (wip.decrementAndGet() == 0) {
                    s.onCompleted();
                }
            }
            
        };
    }
    
}
