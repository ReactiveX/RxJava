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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Returns an Observable that emits items emitted by the source Observable as long as a specified
 * condition is true.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
 */
public final class OperationTakeWhile {

    /**
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param items
     * @param predicate
     *            a function to test each source element for a condition
     * @return sequence of observable values from the start as long as the predicate is true
     */
    public static <T> OnSubscribeFunc<T> takeWhile(final Observable<? extends T> items, final Func1<? super T, Boolean> predicate) {
        return takeWhileWithIndex(items, OperationTakeWhile.<T> skipIndex(predicate));
    }

    /**
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param items
     * @param predicate
     *            a function to test each element for a condition; the second parameter of the function represents the index of the source element; otherwise, false.
     * @return sequence of observable values from the start as long as the predicate is true
     */
    public static <T> OnSubscribeFunc<T> takeWhileWithIndex(final Observable<? extends T> items, final Func2<? super T, ? super Integer, Boolean> predicate) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new TakeWhile<T>(items, predicate).onSubscribe(observer);
            }

        };
    }

    private static <T> Func2<T, Integer, Boolean> skipIndex(final Func1<? super T, Boolean> underlying) {
        return new Func2<T, Integer, Boolean>() {
            @Override
            public Boolean call(T input, Integer index) {
                return underlying.call(input);
            }
        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the takeWhileWithIndex() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class TakeWhile<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> items;
        private final Func2<? super T, ? super Integer, Boolean> predicate;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        private TakeWhile(Observable<? extends T> items, Func2<? super T, ? super Integer, Boolean> predicate) {
            this.items = items;
            this.predicate = predicate;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {
            private final Observer<? super T> observer;

            private final AtomicInteger counter = new AtomicInteger();

            public ItemObserver(Observer<? super T> observer) {
                // Using AtomicObserver because the unsubscribe, onCompleted, onError and error handling behavior
                // needs "isFinished" logic to not send duplicated events
                // The 'testTakeWhile1' and 'testTakeWhile2' tests fail without this.
                this.observer = new SafeObserver<T>(subscription, observer);
            }

            @Override
            public void onCompleted() {
                observer.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                Boolean isSelected;
                try {
                    isSelected = predicate.call(args, counter.getAndIncrement());
                } catch (Throwable e) {
                    observer.onError(e);
                    return;
                }
                if (isSelected) {
                    observer.onNext(args);
                } else {
                    observer.onCompleted();
                    // this will work if the sequence is asynchronous, it will have no effect on a synchronous observable
                    subscription.unsubscribe();
                }
            }

        }

    }
}
