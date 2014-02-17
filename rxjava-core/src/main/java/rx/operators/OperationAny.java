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

import static rx.functions.Functions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

/**
 * Returns an {@link Observable} that emits <code>true</code> if any element of
 * an observable sequence satisfies a condition, otherwise <code>false</code>.
 */
public final class OperationAny {

    /**
     * Returns an {@link Observable} that emits <code>true</code> if the source {@link Observable} is not empty, otherwise <code>false</code>.
     * 
     * @param source
     *            The source {@link Observable} to check if not empty.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<Boolean> any(Observable<? extends T> source) {
        return new Any<T>(source, alwaysTrue(), false);
    }

    public static <T> OnSubscribeFunc<Boolean> isEmpty(Observable<? extends T> source) {
        return new Any<T>(source, alwaysTrue(), true);
    }

    /**
     * Returns an {@link Observable} that emits <code>true</code> if any element
     * of the source {@link Observable} satisfies the given condition, otherwise
     * <code>false</code>. Note: always emit <code>false</code> if the source {@link Observable} is empty.
     * 
     * @param source
     *            The source {@link Observable} to check if any element
     *            satisfies the given condition.
     * @param predicate
     *            The condition to test every element.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<Boolean> any(Observable<? extends T> source, Func1<? super T, Boolean> predicate) {
        return new Any<T>(source, predicate, false);
    }

    public static <T> OnSubscribeFunc<Boolean> exists(Observable<? extends T> source, Func1<? super T, Boolean> predicate) {
        return any(source, predicate);
    }

    private static class Any<T> implements OnSubscribeFunc<Boolean> {

        private final Observable<? extends T> source;
        private final Func1<? super T, Boolean> predicate;
        private final boolean returnOnEmpty;

        private Any(Observable<? extends T> source, Func1<? super T, Boolean> predicate, boolean returnOnEmpty) {
            this.source = source;
            this.predicate = predicate;
            this.returnOnEmpty = returnOnEmpty;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Boolean> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(source.subscribe(new Observer<T>() {

                private final AtomicBoolean hasEmitted = new AtomicBoolean(false);

                @Override
                public void onNext(T value) {
                    try {
                        if (hasEmitted.get() == false) {
                            if (predicate.call(value) == true
                                    && hasEmitted.getAndSet(true) == false) {
                                observer.onNext(!returnOnEmpty);
                                observer.onCompleted();
                                // this will work if the sequence is asynchronous, it
                                // will have no effect on a synchronous observable
                                subscription.unsubscribe();
                            }
                        }
                    } catch (Throwable ex) {
                        observer.onError(ex);
                        // this will work if the sequence is asynchronous, it
                        // will have no effect on a synchronous observable
                        subscription.unsubscribe();
                    }

                }

                @Override
                public void onError(Throwable ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    if (!hasEmitted.get()) {
                        observer.onNext(returnOnEmpty);
                        observer.onCompleted();
                    }
                }
            }));
        }

    }
}
