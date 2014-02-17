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

import java.util.Comparator;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits all sequentially distinct items emitted by the source.
 */
public final class OperationDistinctUntilChanged {

    /**
     * Returns an Observable that emits all sequentially distinct items emitted by the source.
     * 
     * @param source
     *            The source Observable to emit the sequentially distinct items for.
     * @param equalityComparator
     *            The comparator to use for deciding whether to consider two items as equal or not.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> distinctUntilChanged(Observable<? extends T> source, Comparator<T> equalityComparator) {
        return new DistinctUntilChanged<T, T>(source, Functions.<T> identity(), equalityComparator);
    }

    /**
     * Returns an Observable that emits all sequentially distinct items emitted by the source.
     * 
     * @param source
     *            The source Observable to emit the sequentially distinct items for.
     * @param keySelector
     *            The function to select the key to use for the equality checks.
     * @param equalityComparator
     *            The comparator to use for deciding whether to consider the two item keys as equal or not.
     * @return A subscription function for creating the target Observable.
     */
    public static <T, U> OnSubscribeFunc<T> distinctUntilChanged(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector, Comparator<U> equalityComparator) {
        return new DistinctUntilChanged<T, U>(source, keySelector, equalityComparator);
    }

    /**
     * Returns an Observable that emits all sequentially distinct items emitted by the source.
     * 
     * @param source
     *            The source Observable to emit the sequentially distinct items for.
     * @param keySelector
     *            The function to select the key to use for the equality checks.
     * @return A subscription function for creating the target Observable.
     */
    public static <T, U> OnSubscribeFunc<T> distinctUntilChanged(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector) {
        return new DistinctUntilChanged<T, U>(source, keySelector, new DefaultEqualityComparator<U>());
    }

    /**
     * Returns an Observable that emits all sequentially distinct items emitted by the source.
     * 
     * @param source
     *            The source Observable to emit the sequentially distinct items for.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<T> distinctUntilChanged(Observable<? extends T> source) {
        return new DistinctUntilChanged<T, T>(source, Functions.<T> identity(), new DefaultEqualityComparator<T>());
    }

    // does not define a useful ordering; it's only used for equality tests here
    private static class DefaultEqualityComparator<T> implements Comparator<T> {
        @Override
        public int compare(T t1, T t2) {
            if (t1 == null) {
                return t2 == null ? 0 : 1;
            } else {
                return t1.equals(t2) ? 0 : 1;
            }
        }
    }

    private static class DistinctUntilChanged<T, U> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Func1<? super T, ? extends U> keySelector;
        private final Comparator<U> equalityComparator;

        private DistinctUntilChanged(Observable<? extends T> source, Func1<? super T, ? extends U> keySelector, Comparator<U> equalityComparator) {
            this.source = source;
            this.keySelector = keySelector;
            this.equalityComparator = equalityComparator;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final Subscription sourceSub = source.subscribe(new Observer<T>() {
                private U lastEmittedKey;
                private boolean hasEmitted;

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
                    U lastKey = lastEmittedKey;
                    U nextKey = keySelector.call(next);
                    lastEmittedKey = nextKey;
                    if (!hasEmitted) {
                        hasEmitted = true;
                        observer.onNext(next);
                    } else if (equalityComparator.compare(lastKey, nextKey) != 0) {
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
}
