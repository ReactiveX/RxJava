/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps a publisher, expects it to emit 0 or 1 signal or an error.
 *
 * @param <T> the value type
 */
public final class MaybeFromObservable<T> extends Maybe<T> implements HasUpstreamObservableSource<T> {

    final ObservableSource<T> source;
    
    public MaybeFromObservable(ObservableSource<T> source) {
        this.source = source;
    }
    
    @Override
    public ObservableSource<T> source() {
        return source;
    }
    
    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new FromObservableToMaybeObserver<T>(observer));
    }
    
    static final class FromObservableToMaybeObserver<T> 
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = -8017657973346356002L;

        final MaybeObserver<? super T> actual;

        T value;
        
        public FromObservableToMaybeObserver(MaybeObserver<? super T> observer) {
            this.actual = observer;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.setOnce(this, s)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (!isDisposed()) {
                if (value != null) {
                    get().dispose();
                    onError(new IndexOutOfBoundsException("The source ObservableSource produced more than one item"));
                } else {
                    value = t;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!isDisposed()) {
                value = null;
                lazySet(DisposableHelper.DISPOSED);
                actual.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                lazySet(DisposableHelper.DISPOSED);
                T v = value;
                if (v == null) {
                    actual.onComplete();
                } else {
                    value = null;
                    actual.onSuccess(v);
                }
            }
        }
        
        
    }
}
