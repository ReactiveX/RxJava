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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits all distinct items emitted by the source.
 * 
 * Be careful with this operation when using infinite or very large observables
 * as it has to store all distinct values it has received.
 */
public final class OperationDistinct {

    /**
     * Returns an Observable that emits all distinct items emitted by the source
     * 
     * @param source
     *            The source Observable to emit the distinct items for.
     * @return A subscription function for creating the target Observable.
     */
    public static <T, U> OnSubscribeFunc<T> distinct(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector) {
        return new Distinct<T, U>(source, keySelector);
    }

    /**
     * Returns an Observable that emits all distinct items emitted by the source
     * 
     * @param source
     *            The source Observable to emit the distinct items for.
     * @param equalityComparator
     *            The comparator to use for deciding whether to consider two items as equal or not.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> distinct(Observable<? extends T> source, Comparator<T> equalityComparator) {
        return new DistinctWithComparator<T, T>(source, Functions.<T> identity(), equalityComparator);
    }

    /**
     * Returns an Observable that emits all distinct items emitted by the source
     * 
     * @param source
     *            The source Observable to emit the distinct items for.
     * @param equalityComparator
     *            The comparator to use for deciding whether to consider the two item keys as equal or not.
     * @return A subscription function for creating the target Observable.
     */
    public static <T, U> OnSubscribeFunc<T> distinct(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector, Comparator<U> equalityComparator) {
        return new DistinctWithComparator<T, U>(source, keySelector, equalityComparator);
    }

    /**
     * Returns an Observable that emits all distinct items emitted by the source
     * 
     * @param source
     *            The source Observable to emit the distinct items for.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> distinct(Observable<? extends T> source) {
        return new Distinct<T, T>(source, Functions.<T> identity());
    }

    private static class Distinct<T, U> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Func1<? super T, ? extends U> keySelector;

        private Distinct(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector) {
            this.source = source;
            this.keySelector = keySelector;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final Subscription sourceSub = source.subscribe(new Observer<T>() {
                private final Set<U> emittedKeys = new HashSet<U>();

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(T next) {
                    U nextKey = keySelector.call(next);
                    if (!emittedKeys.contains(nextKey)) {
                        emittedKeys.add(nextKey);
                        observer.onNext(next);
                    }
                }
            });

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    sourceSub.unsubscribe();
                }
            });
        }
    }

    private static class DistinctWithComparator<T, U> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Func1<? super T, ? extends U> keySelector;
        private final Comparator<U> equalityComparator;

        private DistinctWithComparator(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector, Comparator<U> equalityComparator) {
            this.source = source;
            this.keySelector = keySelector;
            this.equalityComparator = equalityComparator;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final Subscription sourceSub = source.subscribe(new Observer<T>() {

                // due to the totally arbitrary equality comparator, we can't use anything more efficient than lists here 
                private final List<U> emittedKeys = new ArrayList<U>();

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }

                @Override
                public void onNext(T next) {
                    U nextKey = keySelector.call(next);
                    if (!alreadyEmitted(nextKey)) {
                        emittedKeys.add(nextKey);
                        observer.onNext(next);
                    }
                }

                private boolean alreadyEmitted(U newKey) {
                    for (U key : emittedKeys) {
                        if (equalityComparator.compare(key, newKey) == 0) {
                            return true;
                        }
                    }
                    return false;
                }
            });

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    sourceSub.unsubscribe();
                }
            });
        }
    }
}
