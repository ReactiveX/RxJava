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
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableAmb<T> extends Observable<T> {
    final ObservableSource<? extends T>[] sources;
    final Iterable<? extends ObservableSource<? extends T>> sourcesIterable;

    public ObservableAmb(ObservableSource<? extends T>[] sources, Iterable<? extends ObservableSource<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Observer<? super T> s) {
        ObservableSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Observable[8];
            try {
                for (ObservableSource<? extends T> p : sourcesIterable) {
                    if (p == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), s);
                        return;
                    }
                    if (count == sources.length) {
                        ObservableSource<? extends T>[] b = new ObservableSource[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = p;
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, s);
                return;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptyDisposable.complete(s);
            return;
        } else
        if (count == 1) {
            sources[0].subscribe(s);
            return;
        }

        AmbCoordinator<T> ac = new AmbCoordinator<T>(s, count);
        ac.subscribe(sources);
    }

    static final class AmbCoordinator<T> implements Disposable {
        final Observer<? super T> actual;
        final AmbInnerObserver<T>[] observers;

        final AtomicInteger winner = new AtomicInteger();

        @SuppressWarnings("unchecked")
        AmbCoordinator(Observer<? super T> actual, int count) {
            this.actual = actual;
            this.observers = new AmbInnerObserver[count];
        }

        public void subscribe(ObservableSource<? extends T>[] sources) {
            AmbInnerObserver<T>[] as = observers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new AmbInnerObserver<T>(this, i + 1, actual);
            }
            winner.lazySet(0); // release the contents of 'as'
            actual.onSubscribe(this);

            for (int i = 0; i < len; i++) {
                if (winner.get() != 0) {
                    return;
                }

                sources[i].subscribe(as[i]);
            }
        }

        public boolean win(int index) {
            int w = winner.get();
            if (w == 0) {
                if (winner.compareAndSet(0, index)) {
                    AmbInnerObserver<T>[] a = observers;
                    int n = a.length;
                    for (int i = 0; i < n; i++) {
                        if (i + 1 != index) {
                            a[i].dispose();
                        }
                    }
                    return true;
                }
                return false;
            }
            return w == index;
        }

        @Override
        public void dispose() {
            if (winner.get() != -1) {
                winner.lazySet(-1);

                for (AmbInnerObserver<T> a : observers) {
                    a.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return winner.get() == -1;
        }
    }

    static final class AmbInnerObserver<T> extends AtomicReference<Disposable> implements Observer<T> {

        private static final long serialVersionUID = -1185974347409665484L;
        final AmbCoordinator<T> parent;
        final int index;
        final Observer<? super T> actual;

        boolean won;

        AmbInnerObserver(AmbCoordinator<T> parent, int index, Observer<? super T> actual) {
            this.parent = parent;
            this.index = index;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this, s);
        }

        @Override
        public void onNext(T t) {
            if (won) {
                actual.onNext(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onNext(t);
                } else {
                    get().dispose();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (won) {
                actual.onError(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }
        }

        @Override
        public void onComplete() {
            if (won) {
                actual.onComplete();
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onComplete();
                }
            }
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
