/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.Objects;

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
        Objects.requireNonNull(run, "run is null");
        return new RunnableDisposable(run);
    }
    
    
    public static Disposable from(Future<?> future) {
        return from(future, true);
    }
    
    public static Disposable from(final Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription is null");
        return new Disposable() {
            @Override
            public void dispose() {
                subscription.cancel();
            }
        };
    }
    
    public static Disposable from(Future<?> future, boolean allowInterrupt) {
        Objects.requireNonNull(future, "future is null");
        return new FutureDisposable(future, allowInterrupt);
    }
    
    static final Disposable EMPTY = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    public static Disposable empty() {
        return EMPTY;
    }

    // TODO there is no way to distinguish a disposed and non-disposed resource
    static final Disposable DISPOSED = new Disposable() {
        @Override
        public void dispose() { }
    };
    
    public static Disposable disposed() {
        return DISPOSED;
    }
    
    public static CompositeDisposable from(Disposable... resources) {
        return new CompositeDisposable(resources);
    }

    /** Wraps a Runnable instance. */
    static final class RunnableDisposable 
    extends AtomicReference<Runnable>
    implements Disposable {
        
        /** */
        private static final long serialVersionUID = 4892876354773733738L;
        
        static final Runnable DISPOSED = new Runnable() {
            @Override
            public void run() { }
        };
        
        public RunnableDisposable(Runnable run) {
            super(run);
        }
        
        @Override
        public void dispose() {
            Runnable r = get();
            if (r != DISPOSED) {
                r = getAndSet(DISPOSED);
                if (r != DISPOSED) {
                    r.run();
                }
            }
        }
    }
    
    /** Wraps a Future instance. */
    static final class FutureDisposable
    extends AtomicReference<Future<?>>
    implements Disposable {
        /** */
        private static final long serialVersionUID = -569898983717900525L;

        final boolean allowInterrupt;

        public FutureDisposable(Future<?> run, boolean allowInterrupt) {
            super(run);
            this.allowInterrupt = allowInterrupt;
        }
        
        @Override
        public void dispose() {
            Future<?> r = get();
            if (r != DisposedFuture.INSTANCE) {
                r = getAndSet(DisposedFuture.INSTANCE);
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
    
    static final Consumer<Disposable> DISPOSER = new Consumer<Disposable>() {
        @Override
        public void accept(Disposable d) {
            d.dispose();
        }
    };
    
    /**
     * Returns a consumer that calls dispose on the received Disposable.
     * @return the consumer that calls dispose on the received Disposable.
     */
    public static Consumer<Disposable> consumeAndDispose() {
        return DISPOSER;
    }
}
