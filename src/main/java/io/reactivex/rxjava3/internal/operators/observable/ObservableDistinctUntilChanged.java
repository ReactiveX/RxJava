/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.observable;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver;

public final class ObservableDistinctUntilChanged<T, K> extends AbstractObservableWithUpstream<T, T> {

    final Function<? super T, K> keySelector;

    final BiPredicate<? super K, ? super K> comparer;

    public ObservableDistinctUntilChanged(ObservableSource<T> source, Function<? super T, K> keySelector, BiPredicate<? super K, ? super K> comparer) {
        super(source);
        this.keySelector = keySelector;
        this.comparer = comparer;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new DistinctUntilChangedObserver<>(observer, keySelector, comparer));
    }

    static final class DistinctUntilChangedObserver<T, K> extends BasicFuseableObserver<T, T> {

        final Function<? super T, K> keySelector;

        final BiPredicate<? super K, ? super K> comparer;

        K last;

        boolean hasValue;

        DistinctUntilChangedObserver(Observer<? super T> actual,
                Function<? super T, K> keySelector,
                BiPredicate<? super K, ? super K> comparer) {
            super(actual);
            this.keySelector = keySelector;
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode != NONE) {
                downstream.onNext(t);
                return;
            }

            K key;

            try {
                key = keySelector.apply(t);
                if (hasValue) {
                    boolean equal = comparer.test(last, key);
                    last = key;
                    if (equal) {
                        return;
                    }
                } else {
                    hasValue = true;
                    last = key;
                }
            } catch (Throwable ex) {
               fail(ex);
               return;
            }

            downstream.onNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Throwable {
            for (;;) {
                T v = qd.poll();
                if (v == null) {
                    return null;
                }
                K key = keySelector.apply(v);
                if (!hasValue) {
                    hasValue = true;
                    last = key;
                    return v;
                }

                if (!comparer.test(last, key)) {
                    last = key;
                    return v;
                }
                last = key;
            }
        }

    }
}
