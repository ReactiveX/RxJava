/*
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

import java.util.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Creates {@link Observable} of a number of items followed by either an error or
 * completion. Subscription status is not checked before emitting an event.
 *
 * @param <T> the value type
 */
public final class Burst<T> extends Observable<T> {

    final List<T> items;
    final Throwable error;

    Burst(Throwable error, List<T> items) {
        this.error = error;
        this.items = items;
    }

    @Override
    protected void subscribeActual(final Observer<? super T> observer) {
        observer.onSubscribe(Disposable.empty());
        for (T item: items) {
            observer.onNext(item);
        }
        if (error != null) {
            observer.onError(error);
        } else {
            observer.onComplete();
        }
    }

    public static <T> Builder<T> item(T item) {
        return items(item);
    }

    @SafeVarargs
    public static <T> Builder<T> items(T... items) {
        return new Builder<>(Arrays.asList(items));
    }

    public static final class Builder<T> {

        private final List<T> items;
        private Throwable error;

        Builder(List<T> items) {
            this.items = items;
        }

        public Observable<T> error(Throwable e) {
            this.error = e;
            return create();
        }

        public Observable<T> create() {
            return new Burst<>(error, items);
        }

    }

}
