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

import static io.reactivex.rxjava3.internal.util.ExceptionHelper.timeoutMessage;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class CompletableTimeout extends Completable {

    final CompletableSource source;
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final CompletableSource other;

    public CompletableTimeout(CompletableSource source, long timeout,
                              TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        final CompositeDisposable set = new CompositeDisposable();
        observer.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();

        Disposable timer = scheduler.scheduleDirect(new DisposeTask(once, set, observer), timeout, unit);

        set.add(timer);

        source.subscribe(new TimeOutObserver(set, once, observer));
    }

    static final class TimeOutObserver implements CompletableObserver {

        private final CompositeDisposable set;
        private final AtomicBoolean once;
        private final CompletableObserver downstream;

        TimeOutObserver(CompositeDisposable set, AtomicBoolean once, CompletableObserver observer) {
            this.set = set;
            this.once = once;
            this.downstream = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                downstream.onComplete();
            }
        }

    }

    final class DisposeTask implements Runnable {
        private final AtomicBoolean once;
        final CompositeDisposable set;
        final CompletableObserver downstream;

        DisposeTask(AtomicBoolean once, CompositeDisposable set, CompletableObserver observer) {
            this.once = once;
            this.set = set;
            this.downstream = observer;
        }

        @Override
        public void run() {
            if (once.compareAndSet(false, true)) {
                set.clear();
                if (other == null) {
                    downstream.onError(new TimeoutException(timeoutMessage(timeout, unit)));
                } else {
                    other.subscribe(new DisposeObserver());
                }
            }
        }

        final class DisposeObserver implements CompletableObserver {

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }

            @Override
            public void onError(Throwable e) {
                set.dispose();
                downstream.onError(e);
            }

            @Override
            public void onComplete() {
                set.dispose();
                downstream.onComplete();
            }

        }
    }
}
