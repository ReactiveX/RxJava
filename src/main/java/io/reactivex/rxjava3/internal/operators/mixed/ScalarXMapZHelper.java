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

package io.reactivex.rxjava3.internal.operators.mixed;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.EmptyDisposable;
import io.reactivex.rxjava3.internal.operators.maybe.MaybeToObservable;
import io.reactivex.rxjava3.internal.operators.single.SingleToObservable;

import java.util.Objects;

/**
 * Utility class to extract a value from a scalar source reactive type,
 * map it to a 0-1 type then subscribe the output type's consumer to it,
 * saving on the overhead of the regular subscription channel.
 * <p>History: 2.1.11 - experimental
 * @since 2.2
 */
final class ScalarXMapZHelper {

    private ScalarXMapZHelper() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Try subscribing to a {@link CompletableSource} mapped from
     * a scalar source (which implements {@link Supplier}).
     * @param <T> the upstream value type
     * @param source the source reactive type ({@code Flowable} or {@code Observable})
     *               possibly implementing {@link Supplier}.
     * @param mapper the function that turns the scalar upstream value into a
     *              {@link CompletableSource}
     * @param observer the consumer to subscribe to the mapped {@link CompletableSource}
     * @return true if a subscription did happen and the regular path should be skipped
     */
    static <T> boolean tryAsCompletable(Object source,
            Function<? super T, ? extends CompletableSource> mapper,
            CompletableObserver observer) {
        if (source instanceof Supplier) {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) source;
            CompletableSource cs = null;
            try {
                T item = supplier.get();
                if (item != null) {
                    cs = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null CompletableSource");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return true;
            }

            if (cs == null) {
                EmptyDisposable.complete(observer);
            } else {
                cs.subscribe(observer);
            }
            return true;
        }
        return false;
    }

    /**
     * Try subscribing to a {@link MaybeSource} mapped from
     * a scalar source (which implements {@link Supplier}).
     * @param <T> the upstream value type
     * @param source the source reactive type ({@code Flowable} or {@code Observable})
     *               possibly implementing {@link Supplier}.
     * @param mapper the function that turns the scalar upstream value into a
     *              {@link MaybeSource}
     * @param observer the consumer to subscribe to the mapped {@link MaybeSource}
     * @return true if a subscription did happen and the regular path should be skipped
     */
    static <T, R> boolean tryAsMaybe(Object source,
            Function<? super T, ? extends MaybeSource<? extends R>> mapper,
            Observer<? super R> observer) {
        if (source instanceof Supplier) {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) source;
            MaybeSource<? extends R> cs = null;
            try {
                T item = supplier.get();
                if (item != null) {
                    cs = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null MaybeSource");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return true;
            }

            if (cs == null) {
                EmptyDisposable.complete(observer);
            } else {
                cs.subscribe(MaybeToObservable.create(observer));
            }
            return true;
        }
        return false;
    }

    /**
     * Try subscribing to a {@link SingleSource} mapped from
     * a scalar source (which implements {@link Supplier}).
     * @param <T> the upstream value type
     * @param source the source reactive type ({@code Flowable} or {@code Observable})
     *               possibly implementing {@link Supplier}.
     * @param mapper the function that turns the scalar upstream value into a
     *              {@link SingleSource}
     * @param observer the consumer to subscribe to the mapped {@link SingleSource}
     * @return true if a subscription did happen and the regular path should be skipped
     */
    static <T, R> boolean tryAsSingle(Object source,
            Function<? super T, ? extends SingleSource<? extends R>> mapper,
            Observer<? super R> observer) {
        if (source instanceof Supplier) {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) source;
            SingleSource<? extends R> cs = null;
            try {
                T item = supplier.get();
                if (item != null) {
                    cs = Objects.requireNonNull(mapper.apply(item), "The mapper returned a null SingleSource");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, observer);
                return true;
            }

            if (cs == null) {
                EmptyDisposable.complete(observer);
            } else {
                cs.subscribe(SingleToObservable.create(observer));
            }
            return true;
        }
        return false;
    }
}
