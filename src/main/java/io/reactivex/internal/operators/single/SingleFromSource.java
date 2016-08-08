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
package io.reactivex.internal.operators.single;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SingleFromSource<T> extends Single<T> {
    private final SingleSource<T> source;

    public SingleFromSource(SingleSource<T> source) {
        this.source = source;
    }

    @Override protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new DisposeAwareSingleObserver<T>(observer));
    }

    /**
     * An observer which does not send downstream notifications once disposed. Used to guard against
     * naive implementations of {@link SingleSource} which do not check for this.
     */
    static final class DisposeAwareSingleObserver<T>
    extends AtomicBoolean
    implements SingleObserver<T>, Disposable {
        private final SingleObserver<? super T> o;
        private Disposable d;

        DisposeAwareSingleObserver(SingleObserver<? super T> o) {
            this.o = o;
        }

        @Override
        public void onSuccess(T value) {
            if (!get()) {
                o.onSuccess(value);
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
