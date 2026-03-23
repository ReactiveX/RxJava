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
package io.reactivex.rxjava4.core.docs;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.functions.*;

/**
 * Documents a set of operators so that the main Flowable source file is not cluttered.
 * @param <T> the element type of the flow
 * @since 4.0.0
 */
public sealed interface FlowableDocBasic<T> permits Flowable {
    /**
     * Returns a {@code Flowable} that applies a specified function to each item emitted by the current {@code Flowable} and
     * emits the results of these function applications.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/map.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator doesn't interfere with backpressure which is determined by the current {@code Flowable}'s backpressure
     *  behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param <R> the output type
     * @param mapper
     *            a function to apply to each item emitted by the current {@code Flowable}
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code mapper} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     * @see Flowable#mapOptional(Function)
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    <@NonNull R> Flowable<R> map(@NonNull Function<? super T, ? extends R> mapper);

    /**
     * Filters items emitted by the current {@code Flowable} by only emitting those that satisfy a specified predicate.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/filter.v3.png" alt="">
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator doesn't interfere with backpressure which is determined by the current {@code Flowable}'s backpressure
     *  behavior.</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code filter} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param predicate
     *            a function that evaluates each item emitted by the current {@code Flowable}, returning {@code true}
     *            if it passes the filter
     * @return the new {@code Flowable} instance
     * @throws NullPointerException if {@code predicate} is {@code null}
     * @see <a href="http://reactivex.io/documentation/operators/filter.html">ReactiveX operators documentation: Filter</a>
     */
    @CheckReturnValue
    @NonNull
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerSupport.NONE)
    Flowable<T> filter(@NonNull Predicate<? super T> predicate);

}
