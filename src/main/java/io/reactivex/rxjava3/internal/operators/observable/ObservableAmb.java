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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableAmb<T> extends Observable<T> {
    final ObservableSource<? extends T>[] sources;
    final Iterable<? extends ObservableSource<? extends T>> sourcesIterable;

    public ObservableAmb(ObservableSource<? extends T>[] sources, Iterable<? extends ObservableSource<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Observer<? super T> observer) {
        ObservableSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new ObservableSource[8];
            try {
                for (ObservableSource<? extends T> p : sourcesIterable) {
                    if (p == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), observer);
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
                EmptyDisposable.error(e, observer);
                return;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptyDisposable.complete(observer);
            return;
        } else
        if (count == 1) {
            sources[0].subscribe(observer);
            return;
        }

        AmbCoordinator<T> ac = new AmbCoordinator<T>(observer, count);
        ac.subscribe(sources);
    }

    static final class AmbCoordinator<T> implements Disposable {
        final Observer<? super T> downstream;
        final AmbInnerObserver<T>[] observers;

        final AtomicInteger winner = new AtomicInteger();

        @SuppressWarnings("unchecked")
        AmbCoordinator(Observer<? super T> actual, int count) {
            this.downstream = actual;
            this.observers = new AmbInnerObserver[count];
        }

        public void subscribe(ObservableSource<? extends T>[] sources) {
            AmbInnerObserver<T>[] as = observers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new AmbInnerObserver<T>(this, i + 1, downstream);
            }
            winner.lazySet(0); // release the contents of 'as'
            downstream.onSubscribe(this);

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
        final Observer<? super T> downstream;

        boolean won;

        AmbInnerObserver(AmbCoordinator<T> parent, int index, Observer<? super T> downstream) {
            this.parent = parent;
            this.index = index;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onNext(T t) {
            if (won) {
                downstream.onNext(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onNext(t);
                } else {
                    get().dispose();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (won) {
                downstream.onError(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onError(t);
                } else {
                    RxJavaPlugins.onError(t);
                }
            }
        }

        @Override
        public void onComplete() {
            if (won) {
                downstream.onComplete();
            } else {
                if (parent.win(index)) {
                    won = true;
                    downstream.onComplete();
                }
            }
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
