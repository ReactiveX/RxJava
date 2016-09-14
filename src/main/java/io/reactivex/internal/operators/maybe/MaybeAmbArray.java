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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Signals the event of the first MaybeSource that signals.
 *
 * @param <T> the value type emitted
 */
public final class MaybeAmbArray<T> extends Maybe<T> {

    final MaybeSource<? extends T>[] sources;

    public MaybeAmbArray(MaybeSource<? extends T>[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {

        AmbMaybeObserver<T> parent = new AmbMaybeObserver<T>(observer);
        observer.onSubscribe(parent);

        for (MaybeSource<? extends T> s : sources) {
            if (parent.isDisposed()) {
                return;
            }

            if (s == null) {
                parent.onError(new NullPointerException("One of the MaybeSources is null"));
                return;
            }

            s.subscribe(parent);
        }
    }

    static final class AmbMaybeObserver<T>
    extends AtomicBoolean
    implements MaybeObserver<T>, Disposable {


        private static final long serialVersionUID = -7044685185359438206L;

        final MaybeObserver<? super T> actual;

        final CompositeDisposable set;

        AmbMaybeObserver(MaybeObserver<? super T> actual) {
            this.actual = actual;
            this.set = new CompositeDisposable();
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                set.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return get();
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            if (compareAndSet(false, true)) {
                set.dispose();

                actual.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (compareAndSet(false, true)) {
                set.dispose();

                actual.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (compareAndSet(false, true)) {
                set.dispose();

                actual.onComplete();
            }
        }

    }
}
