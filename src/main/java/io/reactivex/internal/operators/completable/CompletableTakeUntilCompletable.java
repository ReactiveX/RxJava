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

package io.reactivex.internal.operators.completable;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Terminates the sequence if either the main or the other Completable terminate.
 * <p>History: 2.1.17 - experimental
 * @since 2.2
 */
public final class CompletableTakeUntilCompletable extends Completable {

    final Completable source;

    final CompletableSource other;

    public CompletableTakeUntilCompletable(Completable source,
            CompletableSource other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(CompletableObserver s) {
        TakeUntilMainObserver parent = new TakeUntilMainObserver(s);
        s.onSubscribe(parent);

        other.subscribe(parent.other);
        source.subscribe(parent);
    }

    static final class TakeUntilMainObserver extends AtomicReference<Disposable>
    implements CompletableObserver, Disposable {

        private static final long serialVersionUID = 3533011714830024923L;

        final CompletableObserver downstream;

        final OtherObserver other;

        final AtomicBoolean once;

        TakeUntilMainObserver(CompletableObserver downstream) {
            this.downstream = downstream;
            this.other = new OtherObserver(this);
            this.once = new AtomicBoolean();
        }

        @Override
        public void dispose() {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(this);
                DisposableHelper.dispose(other);
            }
        }

        @Override
        public boolean isDisposed() {
            return once.get();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(other);
                downstream.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(other);
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        void innerComplete() {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(this);
                downstream.onComplete();
            }
        }

        void innerError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                DisposableHelper.dispose(this);
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        static final class OtherObserver extends AtomicReference<Disposable>
        implements CompletableObserver {

            private static final long serialVersionUID = 5176264485428790318L;
            final TakeUntilMainObserver parent;

            OtherObserver(TakeUntilMainObserver parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onComplete() {
                parent.innerComplete();
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(e);
            }
        }
    }
}
