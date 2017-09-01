/**
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

package io.reactivex.internal.operators.observable;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.internal.observers.BasicFuseableObserver;

public final class ObservableValueFilter<T> extends AbstractObservableWithUpstream<T, T> {
    final T matcher;
    public ObservableValueFilter(ObservableSource<T> source, T matcher) {
        super(source);
        this.matcher = matcher;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        source.subscribe(new FilterObserver<T>(s, matcher));
    }

    static final class FilterObserver<T> extends BasicFuseableObserver<T, T> {
        final T matcher;

        FilterObserver(Observer<? super T> actual, T matcher) {
            super(actual);
            this.matcher = matcher;
        }

        @Override
        public void onNext(T t) {
            if (sourceMode == NONE) {
                boolean b;
                try {
                    b = matcher.equals(t);
                } catch (Throwable e) {
                    fail(e);
                    return;
                }
                if (b) {
                    actual.onNext(t);
                }
            } else {
                actual.onNext(null);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            for (;;) {
                T v = qs.poll();
                if (v == null || matcher.equals(v)) {
                    return v;
                }
            }
        }
    }
}
