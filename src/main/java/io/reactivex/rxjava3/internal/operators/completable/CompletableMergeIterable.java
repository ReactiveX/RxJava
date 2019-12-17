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

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableMergeIterable extends Completable {
    final Iterable<? extends CompletableSource> sources;

    public CompletableMergeIterable(Iterable<? extends CompletableSource> sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicInteger wip = new AtomicInteger(1);

        MergeCompletableObserver shared = new MergeCompletableObserver(observer, set, wip);

        observer.onSubscribe(shared);

        Iterator<? extends CompletableSource> iterator;

        try {
            iterator = Objects.requireNonNull(sources.iterator(), "The source iterator returned is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            observer.onError(e);
            return;
        }

        for (;;) {
            if (set.isDisposed()) {
                return;
            }

            boolean b;
            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                set.dispose();
                shared.onError(e);
                return;
            }

            if (!b) {
                break;
            }

            if (set.isDisposed()) {
                return;
            }

            CompletableSource c;

            try {
                c = Objects.requireNonNull(iterator.next(), "The iterator returned a null CompletableSource");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                set.dispose();
                shared.onError(e);
                return;
            }

            if (set.isDisposed()) {
                return;
            }

            wip.getAndIncrement();

            c.subscribe(shared);
        }

        shared.onComplete();
    }

    static final class MergeCompletableObserver extends AtomicBoolean implements CompletableObserver, Disposable {

        private static final long serialVersionUID = -7730517613164279224L;

        final CompositeDisposable set;

        final CompletableObserver downstream;

        final AtomicInteger wip;

        MergeCompletableObserver(CompletableObserver actual, CompositeDisposable set, AtomicInteger wip) {
            this.downstream = actual;
            this.set = set;
            this.wip = wip;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            set.dispose();
            if (compareAndSet(false, true)) {
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (wip.decrementAndGet() == 0) {
                if (compareAndSet(false, true)) {
                    downstream.onComplete();
                }
            }
        }

        @Override
        public void dispose() {
            set.dispose();
            set(true);
        }

        @Override
        public boolean isDisposed() {
            return set.isDisposed();
        }
    }
}
