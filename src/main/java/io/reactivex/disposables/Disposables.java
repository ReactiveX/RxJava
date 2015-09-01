/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.disposables;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Utility class to help create disposables by wrapping
 * other types.
 */
public final class Disposables {
    /** Utility class. */
    private Disposables() {
        throw new IllegalStateException("No instances!");
    }
    
    public static Disposable from(Runnable run) {
        Objects.requireNonNull(run);
        return new RunnableDisposable(run);
    }
    
    
    public static Disposable from(Future<?> future) {
        return from(future, true);
    }
    
    public static Disposable from(Future<?> future, boolean allowInterrupt) {
        Objects.requireNonNull(future);
        return new FutureDisposable(future, allowInterrupt);
    }
    
    static final Disposable EMPTY = () -> { };
    
    public static Disposable empty() {
        return EMPTY;
    }

    // TODO there is no way to distinguish a disposed and non-disposed resource
    static final Disposable DISPOSED = () -> { };
    
    public static Disposable disposed() {
        return DISPOSED;
    }
    
    public static CompositeDisposable from(Disposable... resources) {
        return new CompositeDisposable(resources);
    }

    /** Wraps a Runnable instance. */
    static final class RunnableDisposable implements Disposable {
        volatile Runnable run;

        static final AtomicReferenceFieldUpdater<RunnableDisposable, Runnable> RUN =
                AtomicReferenceFieldUpdater.newUpdater(RunnableDisposable.class, Runnable.class, "run");
        
        static final Runnable DISPOSED = () -> { };
        
        public RunnableDisposable(Runnable run) {
            RUN.lazySet(this, run);
        }
        
        @Override
        public void dispose() {
            Runnable r = run;
            if (r != DISPOSED) {
                r = RUN.getAndSet(this, DISPOSED);
                if (r != DISPOSED) {
                    r.run();
                }
            }
        }
    }
    
    /** Wraps a Future instance. */
    static final class FutureDisposable implements Disposable {
        volatile Future<?> future;
        
        final boolean allowInterrupt;

        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<FutureDisposable, Future> RUN =
                AtomicReferenceFieldUpdater.newUpdater(FutureDisposable.class, Future.class, "future");
        
        public FutureDisposable(Future<?> run, boolean allowInterrupt) {
            this.allowInterrupt = allowInterrupt;
            RUN.lazySet(this, run);
        }
        
        @Override
        public void dispose() {
            Future<?> r = future;
            if (r != DisposedFuture.INSTANCE) {
                r = RUN.getAndSet(this, DisposedFuture.INSTANCE);
                if (r != DisposedFuture.INSTANCE) {
                    r.cancel(allowInterrupt);
                }
            }
        }
    }

    /** A singleton instance of the disposed future. */
    enum DisposedFuture implements Future<Object> {
        INSTANCE;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
