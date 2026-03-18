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
 * Default wrappers and implementations for observer-based consumer classes and interfaces,
 * including disposable and resource-tracking variants and
 * the {@link io.reactivex.rxjava4.observers.TestObserver TestObserver} that allows unit testing
 * {@link io.reactivex.rxjava4.core.Observable Observable}-, {@link io.reactivex.rxjava4.core.Single Single}-,
 * {@link io.reactivex.rxjava4.core.Maybe Maybe}- and {@link io.reactivex.rxjava4.core.Completable Completable}-based flows.
 * <p>
 * Available observer variants
 * <br>
 * <table border="1" style="border-collapse: collapse;">
 * <caption>The available observer types.</caption>
 * <tr><td><b>Reactive type</b></td><td><b>Base interface</b></td><td><b>Simple</b></td><td><b>Disposable</b></td><td><b>Resource</b></td></tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava4.core.Observable Observable}</td>
 *     <td>{@link io.reactivex.rxjava4.core.Observer Observer}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.DefaultObserver DefaultObserver}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.DisposableObserver DisposableObserver}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.ResourceObserver DisposableObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava4.core.Maybe Maybe}</td>
 *     <td>{@link io.reactivex.rxjava4.core.MaybeObserver MaybeObserver}</td>
 *     <td>N/A</td>
 *     <td>{@link io.reactivex.rxjava4.observers.DisposableMaybeObserver DisposableMaybeObserver}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.ResourceMaybeObserver DisposableMaybeObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava4.core.Single Single}</td>
 *     <td>{@link io.reactivex.rxjava4.core.SingleObserver SingleObserver}</td>
 *     <td>N/A</td>
 *     <td>{@link io.reactivex.rxjava4.observers.DisposableSingleObserver DisposableSingleObserver}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.ResourceSingleObserver DisposableSingleObserver}</td>
 * </tr>
 * <tr>
 *     <td>{@link io.reactivex.rxjava4.core.Completable Completable}</td>
 *     <td>{@link io.reactivex.rxjava4.core.CompletableObserver CompletableObserver}</td>
 *     <td>N/A</td>
 *     <td>{@link io.reactivex.rxjava4.observers.DisposableCompletableObserver DisposableCompletableObserver}</td>
 *     <td>{@link io.reactivex.rxjava4.observers.ResourceCompletableObserver DisposableCompletableObserver}</td>
 * </tr>
 * </table>
 */
package io.reactivex.rxjava4.observers;
