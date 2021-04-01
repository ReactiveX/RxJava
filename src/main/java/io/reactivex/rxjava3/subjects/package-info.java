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

/**
 * Classes representing so-called hot sources, aka <strong>subjects</strong>, that implement a base reactive class and
 * the respective consumer type at once to allow forms of multicasting events to multiple
 * consumers as well as consuming another base reactive type of their kind.
 * <p>
 * Available subject classes with their respective base classes and consumer interfaces:
 * <br>
 * <table border="1" style="border-collapse: collapse;" summary="The available subject classes with their respective base classes and consumer interfaces.">
 * <tr><td><b>Subject type</b></td><td><b>Base class</b></td><td><b>Consumer interface</b></td></tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava3.subjects.Subject Subject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link io.reactivex.rxjava3.subjects.AsyncSubject AsyncSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link io.reactivex.rxjava3.subjects.BehaviorSubject BehaviorSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link io.reactivex.rxjava3.subjects.PublishSubject PublishSubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link io.reactivex.rxjava3.subjects.ReplaySubject ReplaySubject}
 *     <br>&nbsp;&nbsp;&nbsp;{@link io.reactivex.rxjava3.subjects.UnicastSubject UnicastSubject}
 *     </td>
 *     <td>{@link io.reactivex.rxjava3.core.Observable Observable}</td>
 *     <td>{@link io.reactivex.rxjava3.core.Observer Observer}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava3.subjects.SingleSubject SingleSubject}</td>
 *     <td>{@link io.reactivex.rxjava3.core.Single Single}</td>
 *     <td>{@link io.reactivex.rxjava3.core.SingleObserver SingleObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava3.subjects.MaybeSubject MaybeSubject}</td>
 *     <td>{@link io.reactivex.rxjava3.core.Maybe Maybe}</td>
 *     <td>{@link io.reactivex.rxjava3.core.MaybeObserver MaybeObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava3.subjects.CompletableSubject CompletableSubject}</td>
 *     <td>{@link io.reactivex.rxjava3.core.Completable Completable}</td>
 *     <td>{@link io.reactivex.rxjava3.core.CompletableObserver CompletableObserver}</td>
 * </tr>
 * </table>
 * <p>
 * The backpressure-aware variants of the {@code Subject} class are called
 * {@link org.reactivestreams.Processor}s and reside in the {@code io.reactivex.processors} package.
 * @see io.reactivex.rxjava3.processors
 */
package io.reactivex.rxjava3.subjects;
