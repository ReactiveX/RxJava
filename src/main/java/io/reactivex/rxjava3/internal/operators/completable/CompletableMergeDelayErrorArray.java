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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;

public final class CompletableMergeDelayErrorArray extends Completable {

    final CompletableSource[] sources;

    public CompletableMergeDelayErrorArray(CompletableSource[] sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);

        final AtomicThrowable errors = new AtomicThrowable();
        set.add(new TryTerminateAndReportDisposable(errors));

        observer.onSubscribe(set);

        for (CompletableSource c : sources) {
            if (set.isDisposed()) {
                return;
            }

            if (c == null) {
                Throwable ex = new NullPointerException("A completable source is null");
                errors.tryAddThrowableOrReport(ex);
                wip.decrementAndGet();
                continue;
            }

            c.subscribe(new MergeInnerCompletableObserver(observer, set, errors, wip));
        }

        if (wip.decrementAndGet() == 0) {
            errors.tryTerminateConsumer(observer);
        }
    }

    static final class TryTerminateAndReportDisposable implements Disposable {
        final AtomicThrowable errors;
        TryTerminateAndReportDisposable(AtomicThrowable errors) {
            this.errors = errors;
        }

        @Override
        public void dispose() {
            errors.tryTerminateAndReport();
        }

        @Override
        public boolean isDisposed() {
            return errors.isTerminated();
        }
    }

    static final class MergeInnerCompletableObserver
    implements CompletableObserver {
        final CompletableObserver downstream;
        final CompositeDisposable set;
        final AtomicThrowable errors;
        final AtomicInteger wip;

        MergeInnerCompletableObserver(CompletableObserver observer, CompositeDisposable set, AtomicThrowable error,
                AtomicInteger wip) {
            this.downstream = observer;
            this.set = set;
            this.errors = error;
            this.wip = wip;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            if (errors.tryAddThrowableOrReport(e)) {
                tryTerminate();
            }
        }

        @Override
        public void onComplete() {
            tryTerminate();
        }

        void tryTerminate() {
            if (wip.decrementAndGet() == 0) {
                errors.tryTerminateConsumer(downstream);
            }
        }
    }
}
