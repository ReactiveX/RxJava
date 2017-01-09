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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class ObservableSwitchIfEmpty<T> extends AbstractObservableWithUpstream<T, T> {
    final ObservableSource<? extends T> other;
    public ObservableSwitchIfEmpty(ObservableSource<T> source, ObservableSource<? extends T> other) {
        super(source);
        this.other = other;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        SwitchIfEmptyObserver<T> parent = new SwitchIfEmptyObserver<T>(t, other);
        t.onSubscribe(parent.arbiter);
        source.subscribe(parent);
    }

    static final class SwitchIfEmptyObserver<T> implements Observer<T> {
        final Observer<? super T> actual;
        final ObservableSource<? extends T> other;
        final SequentialDisposable arbiter;

        boolean empty;

        SwitchIfEmptyObserver(Observer<? super T> actual, ObservableSource<? extends T> other) {
            this.actual = actual;
            this.other = other;
            this.empty = true;
            this.arbiter = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable s) {
            arbiter.update(s);
        }

        @Override
        public void onNext(T t) {
            if (empty) {
                empty = false;
            }
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (empty) {
                empty = false;
                other.subscribe(this);
            } else {
                actual.onComplete();
            }
        }
    }
}
