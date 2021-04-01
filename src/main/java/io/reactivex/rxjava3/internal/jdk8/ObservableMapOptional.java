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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.observers.BasicFuseableObserver;

/**
 * Map the upstream values into an Optional and emit its value if any.
 * @param <T> the upstream element type
 * @param <R> the output element type
 * @since 3.0.0
 */
public final class ObservableMapOptional<T, R> extends Observable<R> {

    final Observable<T> source;

    final Function<? super T, Optional<? extends R>> mapper;

    public ObservableMapOptional(Observable<T> source, Function<? super T, Optional<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new MapOptionalObserver<>(observer, mapper));
    }

    static final class MapOptionalObserver<T, R> extends BasicFuseableObserver<T, R> {

        final Function<? super T, Optional<? extends R>> mapper;

        MapOptionalObserver(Observer<? super R> downstream, Function<? super T, Optional<? extends R>> mapper) {
            super(downstream);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            Optional<? extends R> result;
            try {
                result = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Optional");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }

            if (result.isPresent()) {
                downstream.onNext(result.get());
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public R poll() throws Throwable {
            for (;;) {
                T item = qd.poll();
                if (item == null) {
                    return null;
                }
                Optional<? extends R> result = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null Optional");
                if (result.isPresent()) {
                    return result.get();
                }
            }
        }
    }
}
