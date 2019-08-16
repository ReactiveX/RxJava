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

package io.reactivex.rxjava3.internal.operators.completable;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableMergeArray extends Completable {
    final CompletableSource[] sources;

    public CompletableMergeArray(CompletableSource[] sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicBoolean once = new AtomicBoolean();

        InnerCompletableObserver shared = new InnerCompletableObserver(observer, once, set, sources.length + 1);
        observer.onSubscribe(shared);

        for (CompletableSource c : sources) {
            if (set.isDisposed()) {
                return;
            }

            if (c == null) {
                set.dispose();
                NullPointerException npe = new NullPointerException("A completable source is null");
                shared.onError(npe);
                return;
            }

            c.subscribe(shared);
        }

        shared.onComplete();
    }

    static final class InnerCompletableObserver extends AtomicInteger implements CompletableObserver, Disposable {
        private static final long serialVersionUID = -8360547806504310570L;

        final CompletableObserver downstream;

        final AtomicBoolean once;

        final CompositeDisposable set;

        InnerCompletableObserver(CompletableObserver actual, AtomicBoolean once, CompositeDisposable set, int n) {
            this.downstream = actual;
            this.once = once;
            this.set = set;
            this.lazySet(n);
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            set.dispose();
            if (once.compareAndSet(false, true)) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (decrementAndGet() == 0) {
                if (once.compareAndSet(false, true)) {
                    downstream.onComplete();
                }
            }
        }

        @Override
        public void dispose() {
            set.dispose();
            once.set(true);
        }

        @Override
        public boolean isDisposed() {
            return set.isDisposed();
        }
    }
}
