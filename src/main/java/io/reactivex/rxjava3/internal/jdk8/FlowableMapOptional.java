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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscribers.*;

/**
 * Map the upstream values into an Optional and emit its value if any.
 * @param <T> the upstream element type
 * @param <R> the output element type
 * @since 3.0.0
 */
public final class FlowableMapOptional<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final Function<? super T, Optional<? extends R>> mapper;

    public FlowableMapOptional(Flowable<T> source, Function<? super T, Optional<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new MapOptionalConditionalSubscriber<>((ConditionalSubscriber<? super R>)s, mapper));
        } else {
            source.subscribe(new MapOptionalSubscriber<>(s, mapper));
        }
    }

    static final class MapOptionalSubscriber<T, R> extends BasicFuseableSubscriber<T, R>
    implements ConditionalSubscriber<T> {

        final Function<? super T, Optional<? extends R>> mapper;

        MapOptionalSubscriber(Subscriber<? super R> downstream, Function<? super T, Optional<? extends R>> mapper) {
            super(downstream);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return true;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return true;
            }

            Optional<? extends R> result;
            try {
                result = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Optional");
            } catch (Throwable ex) {
                fail(ex);
                return true;
            }

            if (result.isPresent()) {
                downstream.onNext(result.get());
                return true;
            }
            return false;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public R poll() throws Throwable {
            for (;;) {
                T item = qs.poll();
                if (item == null) {
                    return null;
                }
                Optional<? extends R> result = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null Optional");
                if (result.isPresent()) {
                    return result.get();
                }
                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }

    static final class MapOptionalConditionalSubscriber<T, R> extends BasicFuseableConditionalSubscriber<T, R> {

        final Function<? super T, Optional<? extends R>> mapper;

        MapOptionalConditionalSubscriber(ConditionalSubscriber<? super R> downstream, Function<? super T, Optional<? extends R>> mapper) {
            super(downstream);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return true;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return true;
            }

            Optional<? extends R> result;
            try {
                result = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Optional");
            } catch (Throwable ex) {
                fail(ex);
                return true;
            }

            if (result.isPresent()) {
                return downstream.tryOnNext(result.get());
            }
            return false;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public R poll() throws Throwable {
            for (;;) {
                T item = qs.poll();
                if (item == null) {
                    return null;
                }
                Optional<? extends R> result = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null Optional");
                if (result.isPresent()) {
                    return result.get();
                }
                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }
}
