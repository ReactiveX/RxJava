/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnGetSubscriptionFunc;
import rx.Observable.OnPartialSubscribeFunc;
import rx.Observable.OnPartialUnsubscribeFunc;
import rx.Observable.PartialSubscription;
import rx.Observer;

/**
 * Returns an Observable that emits the first <code>num</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/take.png">
 * <p>
 * You can choose to pay attention only to the first <code>num</code> items emitted by an Observable
 * by using the take operation. This operation returns an Observable that will invoke a subscribing
 * Observer's <code>onNext</code> function a maximum of <code>num</code> times before invoking
 * <code>onCompleted</code>.
 */
public final class OperationTake {

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param num
     * @return the specified number of contiguous values from the start of the given observable
     *         sequence
     */
    public static <T> OnGetSubscriptionFunc<T> take(final Observable<? extends T> items, final int num) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we
        // will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new Take<T>(items, num);
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class Take<T> implements OnGetSubscriptionFunc<T> {
        private final Observable<? extends T> items;
        private final int num;
        private final AtomicReference<PartialSubscription<? extends T>> subscription = new AtomicReference<Observable.PartialSubscription<? extends T>>();

        private Take(Observable<? extends T> items, int num) {
            this.items = items;
            this.num = num;
        }

        @Override
        public PartialSubscription<T> onGetSubscription() {
            return PartialSubscription.create(new OnPartialSubscribeFunc<T>() {
                @Override
                public void onSubscribe(Observer<? super T> observer) {
                    PartialSubscription<? extends T> partialSubscription = items.getSubscription();
                    subscription.set(partialSubscription);
                    if (num < 1) {
                        // signal that we don't really want any values by unsubscribing before we've
                        // even subscribed.
                        subscription.get().unsubscribe();
                    }

                    partialSubscription.subscribe(new ItemObserver(observer));
                }
            }, new OnPartialUnsubscribeFunc() {
                @Override
                public void onUnsubscribe() {
                    subscription.get().unsubscribe();
                }
            });
        }

        private class ItemObserver implements Observer<T> {
            private final Observer<? super T> observer;

            private final AtomicInteger counter = new AtomicInteger();
            private volatile boolean hasEmitedError = false;

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (hasEmitedError) {
                    return;
                }
                if (counter.getAndSet(num) < num) {
                    observer.onError(e);
                }
            }

            @Override
            public void onNext(T args) {
                if (hasEmitedError) {
                    return;
                }
                final int count = counter.incrementAndGet();
                if (count <= num) {
                    try {
                        observer.onNext(args);
                    } catch (Throwable ex) {
                        hasEmitedError = true;
                        observer.onError(ex);
                        subscription.get().unsubscribe();
                        return;
                    }
                    if (count == num) {
                        observer.onCompleted();
                    }
                }
                if (count >= num) {
                    // OLD: this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
                    // NEW: with the two phase getSubscription and subscribe event synchronous observables can be interrupted.
                    subscription.get().unsubscribe();
                }
            }

        }
    }
}
