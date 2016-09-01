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

import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeSource;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MaybeFromSource<T> extends Maybe<T> {
    private final MaybeSource<T> source;

    public MaybeFromSource(MaybeSource<T> source) {
        this.source = source;
    }

    @Override protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DisposeAwareMaybeObserver<T>(observer));
    }

    /**
     * An observer which does not send downstream notifications once disposed. Used to guard against
     * naive implementations of {@link MaybeSource} which do not check for this.
     */
    static final class DisposeAwareMaybeObserver<T>
    extends AtomicBoolean
    implements MaybeObserver<T>, Disposable {
        private final MaybeObserver<? super T> o;
        private Disposable d;

        DisposeAwareMaybeObserver(MaybeObserver<? super T> o) {
            this.o = o;
        }

        @Override
        public void onSuccess(T value) {
            if (!get()) {
                o.onSuccess(value);
            }
        }

        @Override
        public void onComplete() {
            if (!get()) {
                o.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!get()) {
                o.onError(e);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.d = d;
            o.onSubscribe(this);
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                d.dispose();
                d = null;
            }
        }

        @Override
        public boolean isDisposed() {
            return get();
        }
    }
}
