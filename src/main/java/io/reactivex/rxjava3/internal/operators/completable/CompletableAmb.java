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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableAmb extends Completable {
    private final CompletableSource[] sources;
    private final Iterable<? extends CompletableSource> sourcesIterable;

    public CompletableAmb(CompletableSource[] sources, Iterable<? extends CompletableSource> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        CompletableSource[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new CompletableSource[8];
            try {
                for (CompletableSource element : sourcesIterable) {
                    if (element == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), observer);
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
                EmptyDisposable.error(e, observer);
                return;
            }
        } else {
            count = sources.length;
        }

        final CompositeDisposable set = new CompositeDisposable();
        observer.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();

        for (int i = 0; i < count; i++) {
            CompletableSource c = sources[i];
            if (set.isDisposed()) {
                return;
            }
            if (c == null) {
                NullPointerException npe = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    observer.onError(npe);
                } else {
                    RxJavaPlugins.onError(npe);
                }
                return;
            }

            // no need to have separate subscribers because inner is stateless
            c.subscribe(new Amb(once, set, observer));
        }

        if (count == 0) {
            observer.onComplete();
        }
    }

    static final class Amb implements CompletableObserver {

        final AtomicBoolean once;

        final CompositeDisposable set;

        final CompletableObserver downstream;

        Disposable upstream;

        Amb(AtomicBoolean once, CompositeDisposable set, CompletableObserver observer) {
            this.once = once;
            this.set = set;
            this.downstream = observer;
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();
                downstream.onComplete();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onSubscribe(Disposable d) {
            upstream = d;
            set.add(d);
        }
    }
}
