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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableAmb extends Completable {
    private final CompletableSource[] sources;
    private final Iterable<? extends CompletableSource> sourcesIterable;

    public CompletableAmb(CompletableSource[] sources, Iterable<? extends CompletableSource> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    public void subscribeActual(final CompletableObserver s) {
        CompletableSource[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new CompletableSource[8];
            try {
                for (CompletableSource element : sourcesIterable) {
                    if (element == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), s);
                        return;
                    }
                    if (count == sources.length) {
                        CompletableSource[] b = new CompletableSource[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = element;
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, s);
                return;
            }
        } else {
            count = sources.length;
        }

        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();

        CompletableObserver inner = new Amb(once, set, s);

        for (int i = 0; i < count; i++) {
            CompletableSource c = sources[i];
            if (set.isDisposed()) {
                return;
            }
            if (c == null) {
                NullPointerException npe = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(npe);
                } else {
                    RxJavaPlugins.onError(npe);
                }
                return;
            }

            // no need to have separate subscribers because inner is stateless
            c.subscribe(inner);
        }

        if (count == 0) {
            s.onComplete();
        }
    }

    static final class Amb implements CompletableObserver {
        private final AtomicBoolean once;
        private final CompositeDisposable set;
        private final CompletableObserver s;

        Amb(AtomicBoolean once, CompositeDisposable set, CompletableObserver s) {
            this.once = once;
            this.set = set;
            this.s = s;
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                s.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                s.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

    }
}
