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
package io.reactivex.internal.fuseable;

import java.util.Queue;

import io.reactivex.disposables.Disposable;

/**
 * An interface extending Queue and Disposable and allows negotiating
 * the fusion mode between subsequent operators of the {@code Observable} base reactive type.
 * <p>
 * The negotiation happens in subscription time when the upstream
 * calls the {@code onSubscribe} with an instance of this interface. The
 * downstream has then the obligation to call {@link #requestFusion(int)}
 * with the appropriate mode before calling {@code request()}.
 * <p>
 * In <b>synchronous fusion</b>, all upstream values are either already available or is generated
 * when {@link #poll()} is called synchronously. When the {@link #poll()} returns null,
 * that is the indication if a terminated stream. In this mode, the upstream won't call the onXXX methods.
 * <p>
 * In <b>asynchronous fusion</b>, upstream values may become available to {@link #poll()} eventually.
 * Upstream signals onError() and onComplete() as usual but onNext may not actually contain
 * the upstream value but have {@code null} instead. Downstream should treat such onNext as indication
 * that {@link #poll()} can be called.
 * <p>
 * The general rules for consuming the {@link Queue} interface:
 * <ul>
 * <li> {@link #poll()} has to be called sequentially (from within a serializing drain-loop).</li>
 * <li>In addition, callers of {@link #poll()} should be prepared to catch exceptions.</li>
 * <li>Due to how computation attaches to the {@link #poll()}, {@link #poll()} may return
 * {@code null} even if a preceding {@link #isEmpty()} returned false.</li>
 * </ul>
 * <p>
 * Implementations should only allow calling the following methods and the rest of the
 * {@link Queue} interface methods should throw {@link UnsupportedOperationException}:
 * <ul>
 * <li>{@link #poll()}</li>
 * <li>{@link #isEmpty()}</li>
 * <li>{@link #clear()}</li>
 * </ul>
 * @param <T> the value type transmitted through the queue
 */
public interface QueueDisposable<T> extends QueueFuseable<T>, Disposable {
}
