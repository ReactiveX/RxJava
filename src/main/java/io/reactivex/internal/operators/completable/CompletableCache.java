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
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;

/**
 * Consume the upstream source exactly once and cache its terminal event.
 * 
 * @since 2.0.4 - experimental
 */
@Experimental
public final class CompletableCache extends Completable implements CompletableObserver {

    static final InnerCompletableCache[] EMPTY = new InnerCompletableCache[0];

    static final InnerCompletableCache[] TERMINATED = new InnerCompletableCache[0];

    final CompletableSource source;

    final AtomicReference<InnerCompletableCache[]> observers;

    final AtomicBoolean once;

    Throwable error;

    public CompletableCache(CompletableSource source) {
        this.source = source;
        this.observers = new AtomicReference<InnerCompletableCache[]>(EMPTY);
        this.once = new AtomicBoolean();
    }

    @Override
    protected void subscribeActual(CompletableObserver s) {
        InnerCompletableCache inner = new InnerCompletableCache(s);
        s.onSubscribe(inner);

        if (add(inner)) {
            if (inner.isDisposed()) {
                remove(inner);
            }

            if (once.compareAndSet(false, true)) {
                source.subscribe(this);
            }
        } else {
            Throwable ex = error;
            if (ex != null) {
                s.onError(ex);
            } else {
                s.onComplete();
            }
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        // not used
    }

    @Override
    public void onError(Throwable e) {
        error = e;
        for (InnerCompletableCache inner : observers.getAndSet(TERMINATED)) {
            if (!inner.get()) {
                inner.actual.onError(e);
            }
        }
    }

    @Override
    public void onComplete() {
        for (InnerCompletableCache inner : observers.getAndSet(TERMINATED)) {
            if (!inner.get()) {
                inner.actual.onComplete();
            }
        }
    }

    boolean add(InnerCompletableCache inner) {
        for (;;) {
            InnerCompletableCache[] a = observers.get();
            if (a == TERMINATED) {
                return false;
            }
            int n = a.length;
            InnerCompletableCache[] b = new InnerCompletableCache[n + 1];
            System.arraycopy(a, 0, b, 0, n);
            b[n] = inner;
            if (observers.compareAndSet(a, b)) {
                return true;
            }
        }
    }

    void remove(InnerCompletableCache inner) {
        for (;;) {
            InnerCompletableCache[] a = observers.get();
            int n = a.length;
            if (n == 0) {
                return;
            }

            int j = -1;

            for (int i = 0; i < n; i++) {
                if (a[i] == inner) {
                    j = i;
                    break;
                }
            }

            if (j < 0) {
                return;
            }

            InnerCompletableCache[] b;

            if (n == 1) {
                b = EMPTY;
            } else {
                b = new InnerCompletableCache[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }

            if (observers.compareAndSet(a, b)) {
                break;
            }
        }
    }

    final class InnerCompletableCache
    extends AtomicBoolean
    implements Disposable {

        private static final long serialVersionUID = 8943152917179642732L;

        final CompletableObserver actual;

        InnerCompletableCache(CompletableObserver actual) {
            this.actual = actual;
        }

        @Override
        public boolean isDisposed() {
            return get();
        }

        @Override
        public void dispose() {
            if (compareAndSet(false, true)) {
                remove(this);
            }
        }
    }
}
