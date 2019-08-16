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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.SequentialDisposable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableOnErrorNext<T> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier;

    public ObservableOnErrorNext(ObservableSource<T> source,
                                 Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier) {
        super(source);
        this.nextSupplier = nextSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        OnErrorNextObserver<T> parent = new OnErrorNextObserver<T>(t, nextSupplier);
        t.onSubscribe(parent.arbiter);
        source.subscribe(parent);
    }

    static final class OnErrorNextObserver<T> implements Observer<T> {
        final Observer<? super T> downstream;
        final Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier;
        final SequentialDisposable arbiter;

        boolean once;

        boolean done;

        OnErrorNextObserver(Observer<? super T> actual, Function<? super Throwable, ? extends ObservableSource<? extends T>> nextSupplier) {
            this.downstream = actual;
            this.nextSupplier = nextSupplier;
            this.arbiter = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            arbiter.replace(d);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                downstream.onError(t);
                return;
            }
            once = true;

            ObservableSource<? extends T> p;

            try {
                p = nextSupplier.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(new CompositeException(t, e));
                return;
            }

            if (p == null) {
                NullPointerException npe = new NullPointerException("Observable is null");
                npe.initCause(t);
                downstream.onError(npe);
                return;
            }

            p.subscribe(this);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            once = true;
            downstream.onComplete();
        }
    }
}
