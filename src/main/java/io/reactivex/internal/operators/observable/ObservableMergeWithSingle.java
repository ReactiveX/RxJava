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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Merges an Observable and a Single by emitting the items of the Observable and the success
 * value of the Single and waiting until both the Observable and Single terminate normally.
 * <p>History: 2.1.10 - experimental
 * @param <T> the element type of the Observable
 * @since 2.2
 */
public final class ObservableMergeWithSingle<T> extends AbstractObservableWithUpstream<T, T> {

    final SingleSource<? extends T> other;

    public ObservableMergeWithSingle(Observable<T> source, SingleSource<? extends T> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        MergeWithObserver<T> parent = new MergeWithObserver<T>(observer);
        observer.onSubscribe(parent);
        source.subscribe(parent);
        other.subscribe(parent.otherObserver);
    }

    static final class MergeWithObserver<T> extends AtomicInteger
    implements Observer<T>, Disposable {

        private static final long serialVersionUID = -4592979584110982903L;

        final Observer<? super T> actual;

        final AtomicReference<Disposable> mainDisposable;

        final OtherObserver<T> otherObserver;

        final AtomicThrowable error;

        volatile SimplePlainQueue<T> queue;

        T singleItem;

        volatile boolean disposed;

        volatile boolean mainDone;

        volatile int otherState;

        static final int OTHER_STATE_HAS_VALUE = 1;

        static final int OTHER_STATE_CONSUMED_OR_EMPTY = 2;

        MergeWithObserver(Observer<? super T> actual) {
            this.actual = actual;
            this.mainDisposable = new AtomicReference<Disposable>();
            this.otherObserver = new OtherObserver<T>(this);
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(mainDisposable, d);
        }

        @Override
        public void onNext(T t) {
            if (compareAndSet(0, 1)) {
                actual.onNext(t);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimplePlainQueue<T> q = getOrCreateQueue();
                q.offer(t);
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable ex) {
            if (error.addThrowable(ex)) {
                DisposableHelper.dispose(mainDisposable);
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void onComplete() {
            mainDone = true;
            drain();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(mainDisposable.get());
        }

        @Override
        public void dispose() {
            disposed = true;
            DisposableHelper.dispose(mainDisposable);
            DisposableHelper.dispose(otherObserver);
            if (getAndIncrement() == 0) {
                queue = null;
                singleItem = null;
            }
        }

        void otherSuccess(T value) {
            if (compareAndSet(0, 1)) {
                actual.onNext(value);
                otherState = OTHER_STATE_CONSUMED_OR_EMPTY;
            } else {
                singleItem = value;
                otherState = OTHER_STATE_HAS_VALUE;
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        void otherError(Throwable ex) {
            if (error.addThrowable(ex)) {
                DisposableHelper.dispose(mainDisposable);
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        SimplePlainQueue<T> getOrCreateQueue() {
            SimplePlainQueue<T> q = queue;
            if (q == null) {
                q = new SpscLinkedArrayQueue<T>(bufferSize());
                queue = q;
            }
            return q;
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            Observer<? super T> actual = this.actual;
            int missed = 1;
            for (;;) {

                for (;;) {
                    if (disposed) {
                        singleItem = null;
                        queue = null;
                        return;
                    }

                    if (error.get() != null) {
                        singleItem = null;
                        queue = null;
                        actual.onError(error.terminate());
                        return;
                    }

                    int os = otherState;
                    if (os == OTHER_STATE_HAS_VALUE) {
                        T v = singleItem;
                        singleItem = null;
                        otherState = OTHER_STATE_CONSUMED_OR_EMPTY;
                        os = OTHER_STATE_CONSUMED_OR_EMPTY;
                        actual.onNext(v);
                    }

                    boolean d = mainDone;
                    SimplePlainQueue<T> q = queue;
                    T v = q != null ? q.poll() : null;
                    boolean empty = v == null;

                    if (d && empty && os == OTHER_STATE_CONSUMED_OR_EMPTY) {
                        queue = null;
                        actual.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    actual.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class OtherObserver<T> extends AtomicReference<Disposable>
        implements SingleObserver<T> {

            private static final long serialVersionUID = -2935427570954647017L;

            final MergeWithObserver<T> parent;

            OtherObserver(MergeWithObserver<T> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(T t) {
                parent.otherSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                parent.otherError(e);
            }

        }
    }
}
