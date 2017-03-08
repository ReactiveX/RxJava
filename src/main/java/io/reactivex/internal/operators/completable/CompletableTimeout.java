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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

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
    public void subscribeActual(final CompletableObserver s) {
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();

        Disposable timer = scheduler.scheduleDirect(new TimeoutTask(once, set, s), timeout, unit);

        set.add(timer);

        source.subscribe(new TimeoutObserverObserver(set, once, s));
    }

    static final class TimeoutObserverObserver implements CompletableObserver {

        private final CompositeDisposable disposable;
        private final AtomicBoolean once;
        private final CompletableObserver observer;

        public TimeoutObserverObserver(CompositeDisposable disposable, AtomicBoolean once, CompletableObserver observer) {
            this.disposable = disposable;
            this.once = once;
            this.observer = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposable.add(d);
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                disposable.dispose();
                observer.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                disposable.dispose();
                observer.onComplete();
            }
        }

    }

    private class TimeoutTask implements Runnable {

        private final AtomicBoolean once;
        private final CompositeDisposable disposable;
        private final CompletableObserver completableObserver;

        public TimeoutTask(AtomicBoolean once, CompositeDisposable set, CompletableObserver s) {
            this.once = once;
            this.disposable = set;
            this.completableObserver = s;
        }

        @Override
        public void run() {
            if (once.compareAndSet(false, true)) {
                disposable.clear();
                if (other == null) {
                    completableObserver.onError(new TimeoutException());
                } else {
                    other.subscribe(new DisposeObserver());
                }
            }
        }

        private class DisposeObserver implements CompletableObserver {

            @Override
            public void onSubscribe(Disposable d) {
                disposable.add(d);
            }

            @Override
            public void onError(Throwable e) {
                disposable.dispose();
                completableObserver.onError(e);
            }

            @Override
            public void onComplete() {
                disposable.dispose();
                completableObserver.onComplete();
            }

        }
    }
}
