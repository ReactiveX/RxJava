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

package io.reactivex.rxjava3.observers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;

/**
 * An abstract {@link Observer} that allows asynchronous cancellation of its subscription and associated resources.
 *
 * <p>All pre-implemented final methods are thread-safe.
 *
 * <p>To release the associated resources, one has to call {@link #dispose()}
 * in {@code onError()} and {@code onComplete()} explicitly.
 *
 * <p>Use {@link #add(Disposable)} to associate resources (as {@link io.reactivex.rxjava3.disposables.Disposable Disposable}s)
 * with this {@code ResourceObserver} that will be cleaned up when {@link #dispose()} is called.
 * Removing previously associated resources is not possible but one can create a
 * {@link io.reactivex.rxjava3.disposables.CompositeDisposable CompositeDisposable}, associate it with this
 * {@code ResourceObserver} and then add/remove resources to/from the {@code CompositeDisposable}
 * freely.
 *
 * <p>Use the {@link #dispose()} to dispose the sequence from within an
 * {@code onNext} implementation.
 *
 * <p>Like all other consumers, {@code ResourceObserver} can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * {@link IllegalStateException} with message {@code "It is not allowed to subscribe with a(n) <class name> multiple times."}.
 *
 * <p>Implementation of {@link #onStart()}, {@link #onNext(Object)}, {@link #onError(Throwable)}
 * and {@link #onComplete()} are not allowed to throw any unchecked exceptions.
 * If for some reason this can't be avoided, use {@link io.reactivex.rxjava3.core.Observable#safeSubscribe(io.reactivex.rxjava3.core.Observer)}
 * instead of the standard {@code subscribe()} method.
 *
 * <p>Example<pre><code>
 * Disposable d =
 *     Observable.range(1, 5)
 *     .subscribeWith(new ResourceObserver&lt;Integer&gt;() {
 *         &#64;Override public void onStart() {
 *             add(Schedulers.single()
 *                 .scheduleDirect(() -&gt; System.out.println("Time!"),
 *                     2, TimeUnit.SECONDS));
 *             request(1);
 *         }
 *         &#64;Override public void onNext(Integer t) {
 *             if (t == 3) {
 *                 dispose();
 *             }
 *             System.out.println(t);
 *         }
 *         &#64;Override public void onError(Throwable t) {
 *             t.printStackTrace();
 *             dispose();
 *         }
 *         &#64;Override public void onComplete() {
 *             System.out.println("Done!");
 *             dispose();
 *         }
 *     });
 * // ...
 * d.dispose();
 * </code></pre>
 *
 * @param <T> the value type
 */
public abstract class ResourceObserver<T> implements Observer<T>, Disposable {
    /** The active subscription. */
    private final AtomicReference<Disposable> upstream = new AtomicReference<>();

    /** The resource composite, can never be null. */
    private final ListCompositeDisposable resources = new ListCompositeDisposable();

    /**
     * Adds a resource to this {@code ResourceObserver}.
     *
     * @param resource the resource to add
     *
     * @throws NullPointerException if resource is {@code null}
     */
    public final void add(@NonNull Disposable resource) {
        Objects.requireNonNull(resource, "resource is null");
        resources.add(resource);
    }

    @Override
    public final void onSubscribe(Disposable d) {
        if (EndConsumerHelper.setOnce(this.upstream, d, getClass())) {
            onStart();
        }
    }

    /**
     * Called once the upstream sets a {@link Disposable} on this {@code ResourceObserver}.
     *
     * <p>You can perform initialization at this moment. The default
     * implementation does nothing.
     */
    protected void onStart() {
    }

    /**
     * Cancels the main disposable (if any) and disposes the resources associated with
     * this {@code ResourceObserver} (if any).
     *
     * <p>This method can be called before the upstream calls {@link #onSubscribe(Disposable)} at which
     * case the main {@link Disposable} will be immediately disposed.
     */
    @Override
    public final void dispose() {
        if (DisposableHelper.dispose(upstream)) {
            resources.dispose();
        }
    }

    /**
     * Returns true if this {@code ResourceObserver} has been disposed/cancelled.
     * @return true if this {@code ResourceObserver} has been disposed/cancelled
     */
    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
    }
}
