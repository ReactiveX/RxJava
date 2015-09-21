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

package io.reactivex.subscribers.nbp;

import java.util.Objects;
import java.util.function.Consumer;

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscribers.nbp.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to construct various resource-holding and disposable Subscribers from lambdas.
 */
public final class NbpSubscribers {
    /** Utility class. */
    private NbpSubscribers() {
        throw new IllegalStateException("No instances!");
    }

    @SuppressWarnings("unchecked")
    public static <T> NbpSubscriber<T> empty() {
        return (NbpSubscriber<T>)NbpEmptySubscriber.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> NbpSubscriber<T> cancelled() {
        return (NbpSubscriber<T>)NbpCancelledSubscriber.INSTANCE;
    }

    public static <T> NbpDisposableSubscriber<T> emptyDisposable() {
        return new NbpDisposableSubscriber<T>() {
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

    public static <T> NbpDisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext
    ) {
        return createDisposable(onNext, RxJavaPlugins::onError, () -> { }, () -> { });
    }

    public static <T> NbpDisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError
    ) {
        return createDisposable(onNext, onError, () -> { }, () -> { });
    }

    public static <T> NbpDisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete
    ) {
        return createDisposable(onNext, onError, onComplete, () -> { });
    }
    
    public static <T> NbpDisposableSubscriber<T> createDisposable(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete,
            Runnable onStart
    ) {
        Objects.requireNonNull(onNext);
        Objects.requireNonNull(onError);
        Objects.requireNonNull(onComplete);
        Objects.requireNonNull(onStart);
        return new NbpDisposableSubscriber<T>() {
            boolean done;
            @Override
            protected void onStart() {
                super.onStart();
                try {
                    onStart.run();
                } catch (Throwable e) {
                    done = true;
                    dispose();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        ex.addSuppressed(e);
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
                    dispose();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        ex.addSuppressed(e);
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
                    ex.addSuppressed(t);
                    RxJavaPlugins.onError(ex);
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

    public static <T> NbpSubscriber<T> create(
            Consumer<? super T> onNext
    ) {
        return create(onNext, RxJavaPlugins::onError, () -> { }, s -> { });
    }

    public static <T> NbpSubscriber<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError
    ) {
        return create(onNext, onError, () -> { }, s -> { });
    }

    public static <T> NbpSubscriber<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete
    ) {
        return create(onNext, onError, onComplete, s -> { });
    }
    
    public static <T> NbpSubscriber<T> create(
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Runnable onComplete,
            Consumer<? super Disposable> onStart
    ) {
        Objects.requireNonNull(onNext);
        Objects.requireNonNull(onError);
        Objects.requireNonNull(onComplete);
        Objects.requireNonNull(onStart);
        return new NbpSubscriber<T>() {
            boolean done;
            
            Disposable s;
            @Override
            public void onSubscribe(Disposable s) {
                if (SubscriptionHelper.validateDisposable(this.s, s)) {
                    return;
                }
                this.s = s;
                try {
                    onStart.accept(s);
                } catch (Throwable e) {
                    done = true;
                    s.dispose();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        ex.addSuppressed(e);
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
                    s.dispose();
                    try {
                        onError.accept(e);
                    } catch (Throwable ex) {
                        ex.addSuppressed(e);
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
                    ex.addSuppressed(t);
                    RxJavaPlugins.onError(ex);
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
