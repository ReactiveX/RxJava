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

package io.reactivex.rxjava4.core;

import java.io.Serial;
import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.ThrowableWrapper;
import io.reactivex.rxjava4.internal.util.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

/**
 * Consist of a terminal stage and a disposable to be able to cancel a sequence.
 * @param <T> the return and element type of the various stages
 * @since 4.0.0
 */
public final class CompletionStageDisposable<T> implements AutoCloseable {

    // record classes can't have extra fields, why?
    // also I have to write out the constructor instead of declaring it in the record definition, FFS

    static final Cleaner cleaner = Cleaner.create();

    static volatile Consumer<Cleaner.Cleanable> trackAllocations;

    static final class State extends AtomicBoolean implements Runnable {

        /** */
        @Serial
        private static final long serialVersionUID = 262854674341831347L;

        Throwable allocationTrace;

        @Override
        public void run() {
            if (!get()) {
                RxJavaPlugins.onError(
                        new IllegalStateException("CompletionStageDisposable was not awaited or ignored explicitly",
                                allocationTrace));
            }
        }

    }

    final CompletionStage<T> stage;
    final Disposable disposable;
    final State state;
    final Cleaner.Cleanable cleanable;

    /**
     * Construct an instance with parameters
     * @param stage the stage to be awaited
     * @param disposable the disposable to cancel asynchronously
     */
    public CompletionStageDisposable(@NonNull CompletionStage<T> stage, @NonNull Disposable disposable) {
        Objects.requireNonNull(stage, "stage is null");
        Objects.requireNonNull(disposable, "disposable is null");
        this.stage = stage;
        this.disposable = disposable;
        this.state = new State();
        this.cleanable = cleaner.register(this, state);
        if (trackAllocations != null) {
            state.allocationTrace = new StackOverflowError("CompletionStageDisposable::AllocationTrace");
            trackAllocations.accept(this.cleanable);
        } else {
            state.allocationTrace = null;
        }
    }
    /**
     * Await the completion of the current stage.
     * <p>
     * Rethrows any original unchecked exceptions as is.
     * @throws CancellationException if the computation was cancelled
     * @throws ThrowableWrapper if the original exception was a checked exception
     */
    public void await() {
        state.lazySet(true);
        try {
            stage.toCompletableFuture().join();
        } catch (CompletionException ce) {
            throw ExceptionHelper.wrapOrThrow(ce.getCause());
        }
    }

    /**
     * Indicate this instance is deliberately not awaiting its stage.
     */
    public void ignore() {
        state.lazySet(true);
    }

    @Override
    public void close() {
        try {
            state.lazySet(true);
            disposable.dispose();
        } finally {
            cleanable.clean();
        }
    }

    /**
     * Set an allocator tracer callback to track where CompletionStageDisposables are leaking.
     * @param callback the callback to call when a new trace is being established
     */
    public static void setAllocationTrace(Consumer<Cleaner.Cleanable> callback) {
        trackAllocations = callback;
    }

    /**
     * Returns the current allocation stacktrace capturing consumer.
     * @return the current allocation stacktrace capturing consumer.
     */
    public static Consumer<Cleaner.Cleanable> getAllocationTrace() {
        return trackAllocations;
    }

    /***
     * Returns the associated completion stage value.
     * @return the associated completion stage value.
     */
    public CompletionStage<T> stage() {
        return stage;
    }

    /**
     * Returns the associated disposable value.
     * @return the associated disposable value.
     */
    public Disposable disposable() {
        return disposable;
    }
}
