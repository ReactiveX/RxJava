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
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.fuseable.FuseToObservable;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

public final class ObservableCollectSingle<T, U> extends Single<U> implements FuseToObservable<U> {

    final ObservableSource<T> source;

    final Supplier<? extends U> initialSupplier;
    final BiConsumer<? super U, ? super T> collector;

    public ObservableCollectSingle(ObservableSource<T> source,
            Supplier<? extends U> initialSupplier, BiConsumer<? super U, ? super T> collector) {
        this.source = source;
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super U> t) {
        U u;
        try {
            u = Objects.requireNonNull(initialSupplier.get(), "The initialSupplier returned a null value");
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return;
        }

        source.subscribe(new CollectObserver<T, U>(t, u, collector));
    }

    @Override
    public Observable<U> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableCollect<T, U>(source, initialSupplier, collector));
    }

    static final class CollectObserver<T, U> implements Observer<T>, Disposable {
        final SingleObserver<? super U> downstream;
        final BiConsumer<? super U, ? super T> collector;
        final U u;

        Disposable upstream;

        boolean done;

        CollectObserver(SingleObserver<? super U> actual, U u, BiConsumer<? super U, ? super T> collector) {
            this.downstream = actual;
            this.collector = collector;
            this.u = u;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.accept(u, t);
            } catch (Throwable e) {
                upstream.dispose();
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            downstream.onSuccess(u);
        }
    }
}
