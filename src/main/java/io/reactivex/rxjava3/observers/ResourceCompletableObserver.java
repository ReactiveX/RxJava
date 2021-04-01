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

package io.reactivex.rxjava3.observers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.util.EndConsumerHelper;

/**
 * An abstract {@link CompletableObserver} that allows asynchronous cancellation of its subscription and associated resources.
 *
 * <p>All pre-implemented final methods are thread-safe.
 *
 * <p>Override the protected {@link #onStart()} to perform initialization when this
 * {@code ResourceCompletableObserver} is subscribed to a source.
 *
 * <p>Use the public {@link #dispose()} method to dispose the sequence externally and release
 * all resources.
 *
 * <p>To release the associated resources, one has to call {@link #dispose()}
 * in {@code onError()} and {@code onComplete()} explicitly.
 *
 * <p>Use {@link #add(Disposable)} to associate resources (as {@link io.reactivex.rxjava3.disposables.Disposable Disposable}s)
 * with this {@code ResourceCompletableObserver} that will be cleaned up when {@link #dispose()} is called.
 * Removing previously associated resources is not possible but one can create a
 * {@link io.reactivex.rxjava3.disposables.CompositeDisposable CompositeDisposable}, associate it with this
 * {@code ResourceCompletableObserver} and then add/remove resources to/from the {@code CompositeDisposable}
 * freely.
 *
 * <p>Like all other consumers, {@code ResourceCompletableObserver} can be subscribed only once.
 * Any subsequent attempt to subscribe it to a new source will yield an
 * {@link IllegalStateException} with message {@code "It is not allowed to subscribe with a(n) <class name> multiple times."}.
 *
 * <p>Implementation of {@link #onStart()}, {@link #onError(Throwable)}
 * and {@link #onComplete()} are not allowed to throw any unchecked exceptions.
 *
 * <p>Example<pre><code>
 * Disposable d =
 *     Completable.complete().delay(1, TimeUnit.SECONDS)
 *     .subscribeWith(new ResourceCompletableObserver() {
 *         &#64;Override public void onStart() {
 *             add(Schedulers.single()
 *                 .scheduleDirect(() -&gt; System.out.println("Time!"),
 *                     2, TimeUnit.SECONDS));
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
 */
public abstract class ResourceCompletableObserver implements CompletableObserver, Disposable {
    /** The active subscription. */
    private final AtomicReference<Disposable> upstream = new AtomicReference<>();

    /** The resource composite, can never be null. */
    private final ListCompositeDisposable resources = new ListCompositeDisposable();

    /**
     * Adds a resource to this {@code ResourceCompletableObserver}.
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
    public final void onSubscribe(@NonNull Disposable d) {
        if (EndConsumerHelper.setOnce(this.upstream, d, getClass())) {
            onStart();
        }
    }

    /**
     * Called once the upstream sets a {@link Disposable} on this {@code ResourceCompletableObserver}.
     *
     * <p>You can perform initialization at this moment. The default
     * implementation does nothing.
     */
    protected void onStart() {
    }

    /**
     * Cancels the main disposable (if any) and disposes the resources associated with
     * this {@code ResourceCompletableObserver} (if any).
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
     * Returns true if this {@code ResourceCompletableObserver} has been disposed/cancelled.
     * @return true if this {@code ResourceCompletableObserver} has been disposed/cancelled
     */
    @Override
    public final boolean isDisposed() {
        return DisposableHelper.isDisposed(upstream.get());
    }
}
