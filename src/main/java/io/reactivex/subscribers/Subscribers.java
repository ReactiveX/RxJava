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

package io.reactivex.subscribers;

import org.reactivestreams.*;

import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.subscribers.flowable.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to construct various resource-holding and disposable Subscribers from lambdas.
 */
public final class Subscribers {
    /** Utility class. */
    private Subscribers() {
        throw new IllegalStateException("No instances!");
    }

    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> empty() {
        return (Subscriber<T>)EmptySubscriber.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Subscriber<T> cancelled() {
        return (Subscriber<T>)CancelledSubscriber.INSTANCE;
    }

    public static <T> DisposableSubscriber<T> emptyDisposable() {
        return new DisposableSubscriber<T>() {
            @Override
            public void onNext(T t) {
                
            }
            
            @Override
            public void onError(Throwable t) {
                
            }
            
            @Override
            public void onComplete() {
                
            }
        };
    }

    public static <T> DisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext
    ) {
        return createDisposable(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> DisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError
    ) {
        return createDisposable(onNext, onError, Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> DisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete
    ) {
        return createDisposable(onNext, onError, onComplete, Functions.emptyRunnable());
    }
    
    public static <T> DisposableSubscriber<T> createDisposable(
            final Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError,
            final Runnable onComplete,
            final Runnable onStart
    ) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new DisposableSubscriber<T>() {
            boolean done;
            @Override
            protected void onStart() {
                super.onStart();
                try {
                    onStart.run();
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                try {
                    onNext.accept(t);
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                try {
                    onError.accept(t);
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    RxJavaPlugins.onError(t);
                }
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    RxJavaPlugins.onError(e);
                }
            }
        };
    }

    static final Consumer<Subscription> REQUEST_ALL = new Consumer<Subscription>() {
        @Override
        public void accept(Subscription t) {
            t.request(Long.MAX_VALUE);
        }
    };
    
    public static <T> Subscriber<T> create(
            Consumer<? super T> onNext
    ) {
        return create(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), REQUEST_ALL);
    }

    public static <T> Subscriber<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError
    ) {
        return create(onNext, onError, Functions.emptyRunnable(), REQUEST_ALL);
    }

    public static <T> Subscriber<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete
    ) {
        return create(onNext, onError, onComplete, REQUEST_ALL);
    }
    
    public static <T> Subscriber<T> create(
            final Consumer<? super T> onNext,
            final Consumer<? super Throwable> onError,
            final Runnable onComplete,
            final Consumer<? super Subscription> onStart
    ) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new Subscriber<T>() {
            boolean done;
            
            Subscription s;
            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.validateSubscription(this.s, s)) {
                    return;
                }
                this.s = s;
                try {
                    onStart.accept(s);
                } catch (Throwable e) {
                    done = true;
                    s.cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                try {
                    onNext.accept(t);
                } catch (Throwable e) {
                    done = true;
                    s.cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                try {
                    onError.accept(t);
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    RxJavaPlugins.onError(t);
                }
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    RxJavaPlugins.onError(e);
                }
            }
        };
    }
    public static <T> AsyncSubscriber<T> emptyAsync() {
        return new AsyncSubscriber<T>() {
            @Override
            public void onNext(T t) {
                
            }
            
            @Override
            public void onError(Throwable t) {
                RxJavaPlugins.onError(t);
            }
            
            @Override
            public void onComplete() {
                
            }
        };
    }
    
    public static <T> DefaultObserver<T> create(
            final Consumer<? super T> onNext, 
            final Consumer<? super Throwable> onError, 
            final Runnable onComplete, 
            final Runnable onStart) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new DefaultObserver<T>() {
            boolean done;
            @Override
            protected void onStart() {
                super.onStart();
                try {
                    onStart.run();
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(e);
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                try {
                    onNext.accept(t);
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(e);
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                try {
                    onError.accept(t);
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    RxJavaPlugins.onError(t);
                }
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    RxJavaPlugins.onError(e);
                }
            }
        };
    }
    
    public static <T> AsyncSubscriber<T> createAsync(Consumer<? super T> onNext) {
        return createAsync(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> AsyncSubscriber<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError) {
        return createAsync(onNext, onError, Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> AsyncSubscriber<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete) {
        return createAsync(onNext, onError, onComplete, Functions.emptyRunnable());
    }
    
    public static <T> AsyncSubscriber<T> createAsync(
            final Consumer<? super T> onNext, 
            final Consumer<? super Throwable> onError, 
            final Runnable onComplete, 
            final Runnable onStart) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new AsyncSubscriber<T>() {
            boolean done;
            @Override
            protected void onStart() {
                super.onStart();
                try {
                    onStart.run();
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                try {
                    onNext.accept(t);
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        RxJavaPlugins.onError(ex);
                        RxJavaPlugins.onError(e);
                    }
                }
            }
            
            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                try {
                    onError.accept(t);
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    RxJavaPlugins.onError(t);
                }
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    RxJavaPlugins.onError(e);
                }
            }
        };
    }
}
