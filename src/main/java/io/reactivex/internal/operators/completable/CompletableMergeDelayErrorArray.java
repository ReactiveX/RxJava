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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableMergeDelayErrorArray extends Completable {

    final CompletableSource[] sources;

    public CompletableMergeDelayErrorArray(CompletableSource[] sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(final CompletableObserver s) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);

        final AtomicThrowable error = new AtomicThrowable();

        s.onSubscribe(set);

        for (CompletableSource c : sources) {
            if (set.isDisposed()) {
                return;
            }

            if (c == null) {
                Throwable ex = new NullPointerException("A completable source is null");
                error.addThrowable(ex);
                wip.decrementAndGet();
                continue;
            }

            c.subscribe(new MergeInnerCompletableObserver(s, set, error, wip));
        }

        if (wip.decrementAndGet() == 0) {
            Throwable ex = error.terminate();
            if (ex == null) {
                s.onComplete();
            } else {
                s.onError(ex);
            }
        }
    }

    static final class MergeInnerCompletableObserver
    implements CompletableObserver {
        final CompletableObserver actual;
        final CompositeDisposable set;
        final AtomicThrowable error;
        final AtomicInteger wip;

        MergeInnerCompletableObserver(CompletableObserver s, CompositeDisposable set, AtomicThrowable error,
                AtomicInteger wip) {
            this.actual = s;
            this.set = set;
            this.error = error;
            this.wip = wip;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            if (error.addThrowable(e)) {
                tryTerminate();
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            tryTerminate();
        }

        void tryTerminate() {
            if (wip.decrementAndGet() == 0) {
                Throwable ex = error.terminate();
                if (ex == null) {
                    actual.onComplete();
                } else {
                    actual.onError(ex);
                }
            }
        }
    }
}
