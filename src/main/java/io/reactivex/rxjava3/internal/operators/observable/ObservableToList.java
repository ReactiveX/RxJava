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

import java.util.Collection;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;

public final class ObservableToList<T, U extends Collection<? super T>>
extends AbstractObservableWithUpstream<T, U> {

    final Supplier<U> collectionSupplier;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ObservableToList(ObservableSource<T> source, final int defaultCapacityHint) {
        super(source);
        this.collectionSupplier = (Supplier)Functions.createArrayList(defaultCapacityHint);
    }

    public ObservableToList(ObservableSource<T> source, Supplier<U> collectionSupplier) {
        super(source);
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        U coll;
        try {
            coll = ExceptionHelper.nullCheck(collectionSupplier.get(), "The collectionSupplier returned a null Collection.");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }
        source.subscribe(new ToListObserver<T, U>(t, coll));
    }

    static final class ToListObserver<T, U extends Collection<? super T>> implements Observer<T>, Disposable {
        final Observer<? super U> downstream;

        Disposable upstream;

        U collection;

        ToListObserver(Observer<? super U> actual, U collection) {
            this.downstream = actual;
            this.collection = collection;
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
            collection.add(t);
        }

        @Override
        public void onError(Throwable t) {
            collection = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U c = collection;
            collection = null;
            downstream.onNext(c);
            downstream.onComplete();
        }
    }
}
