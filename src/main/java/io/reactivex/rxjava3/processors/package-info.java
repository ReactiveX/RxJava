/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Classes representing so-called hot backpressure-aware sources, aka <strong>processors</strong>,
 * that implement the {@link io.reactivex.rxjava3.processors.FlowableProcessor FlowableProcessor} class,
 * the Reactive Streams {@link org.reactivestreams.Processor Processor} interface
 * to allow forms of multicasting events to one or more subscribers as well as consuming another
 * Reactive Streams {@link org.reactivestreams.Publisher Publisher}.
 * <p>
 * Available processor implementations:
 * <br>
 * <ul>
 *     <li>{@link io.reactivex.rxjava3.processors.AsyncProcessor AsyncProcessor} - replays the very last item</li>
 *     <li>{@link io.reactivex.rxjava3.processors.BehaviorProcessor BehaviorProcessor} - remembers the latest item</li>
 *     <li>{@link io.reactivex.rxjava3.processors.MulticastProcessor MulticastProcessor} - coordinates its source with its consumers</li>
 *     <li>{@link io.reactivex.rxjava3.processors.PublishProcessor PublishProcessor} - dispatches items to current consumers</li>
 *     <li>{@link io.reactivex.rxjava3.processors.ReplayProcessor ReplayProcessor} - remembers some or all items and replays them to consumers</li>
 *     <li>{@link io.reactivex.rxjava3.processors.UnicastProcessor UnicastProcessor} - remembers or relays items to a single consumer</li>
 * </ul>
 * <p>
 * The non-backpressured variants of the {@code FlowableProcessor} class are called
 * {@link io.reactivex.rxjava3.subjects.Subject}s and reside in the {@code io.reactivex.subjects} package.
 * @see io.reactivex.rxjava3.subjects
 */
package io.reactivex.rxjava3.processors;
