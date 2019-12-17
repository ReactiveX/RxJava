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

package io.reactivex.rxjava3.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

/**
 * Utility classes to work with scalar-sourced XMap operators (where X == { flat, concat, switch }).
 */
public final class FlowableScalarXMap {

    /** Utility class. */
    private FlowableScalarXMap() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Tries to subscribe to a possibly Supplier source's mapped Publisher.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param source the source Publisher
     * @param subscriber the subscriber
     * @param mapper the function mapping a scalar value into a Publisher
     * @return true if successful, false if the caller should continue with the regular path.
     */
    @SuppressWarnings("unchecked")
    public static <T, R> boolean tryScalarXMapSubscribe(Publisher<T> source,
            Subscriber<? super R> subscriber,
            Function<? super T, ? extends Publisher<? extends R>> mapper) {
        if (source instanceof Supplier) {
            T t;

            try {
                t = ((Supplier<T>)source).get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, subscriber);
                return true;
            }

            if (t == null) {
                EmptySubscription.complete(subscriber);
                return true;
            }

            Publisher<? extends R> r;

            try {
                r = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptySubscription.error(ex, subscriber);
                return true;
            }

            if (r instanceof Supplier) {
                R u;

                try {
                    u = ((Supplier<R>)r).get();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    EmptySubscription.error(ex, subscriber);
                    return true;
                }

                if (u == null) {
                    EmptySubscription.complete(subscriber);
                    return true;
                }
                subscriber.onSubscribe(new ScalarSubscription<R>(subscriber, u));
            } else {
                r.subscribe(subscriber);
            }

            return true;
        }
        return false;
    }

    /**
     * Maps a scalar value into a Publisher and emits its values.
     *
     * @param <T> the scalar value type
     * @param <U> the output value type
     * @param value the scalar value to map
     * @param mapper the function that gets the scalar value and should return
     * a Publisher that gets streamed
     * @return the new Flowable instance
     */
    public static <T, U> Flowable<U> scalarXMap(final T value, final Function<? super T, ? extends Publisher<? extends U>> mapper) {
        return RxJavaPlugins.onAssembly(new ScalarXMapFlowable<T, U>(value, mapper));
    }

    /**
     * Maps a scalar value to a Publisher and subscribes to it.
     *
     * @param <T> the scalar value type
     * @param <R> the mapped Publisher's element type.
     */
    static final class ScalarXMapFlowable<T, R> extends Flowable<R> {

        final T value;

        final Function<? super T, ? extends Publisher<? extends R>> mapper;

        ScalarXMapFlowable(T value,
                Function<? super T, ? extends Publisher<? extends R>> mapper) {
            this.value = value;
            this.mapper = mapper;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void subscribeActual(Subscriber<? super R> s) {
            Publisher<? extends R> other;
            try {
                other = Objects.requireNonNull(mapper.apply(value), "The mapper returned a null Publisher");
            } catch (Throwable e) {
                EmptySubscription.error(e, s);
                return;
            }
            if (other instanceof Supplier) {
                R u;

                try {
                    u = ((Supplier<R>)other).get();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    EmptySubscription.error(ex, s);
                    return;
                }

                if (u == null) {
                    EmptySubscription.complete(s);
                    return;
                }
                s.onSubscribe(new ScalarSubscription<R>(s, u));
            } else {
                other.subscribe(s);
            }
        }
    }
}
