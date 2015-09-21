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

import io.reactivex.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to create Observers from lambdas.
 */
public final class NbpObservers {
    /** Utility class with factory methods only. */
    private NbpObservers() {
        throw new IllegalStateException("No instances!");
    }
    
    public static <T> NbpObserver<T> empty() {
        return new NbpObserver<T>() {
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
    
    public static <T> NbpAsyncObserver<T> emptyAsync() {
        return new NbpAsyncObserver<T>() {
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
    
    public static <T> NbpObserver<T> create(Consumer<? super T> onNext) {
        return create(onNext, RxJavaPlugins::onError, () -> { }, () -> { });
    }

    public static <T> NbpObserver<T> create(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError) {
        return create(onNext, onError, () -> { }, () -> { });
    }

    public static <T> NbpObserver<T> create(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete) {
        return create(onNext, onError, onComplete, () -> { });
    }

    public static <T> NbpObserver<T> create(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete, Runnable onStart) {
        Objects.requireNonNull(onNext);
        Objects.requireNonNull(onError);
        Objects.requireNonNull(onComplete);
        Objects.requireNonNull(onStart);
        return new NbpObserver<T>() {
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
                    cancel();
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
    
    public static <T> NbpAsyncObserver<T> createAsync(Consumer<? super T> onNext) {
        return createAsync(onNext, RxJavaPlugins::onError, () -> { }, () -> { });
    }

    public static <T> NbpAsyncObserver<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError) {
        return createAsync(onNext, onError, () -> { }, () -> { });
    }

    public static <T> NbpAsyncObserver<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete) {
        return createAsync(onNext, onError, onComplete, () -> { });
    }
    
    public static <T> NbpAsyncObserver<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete, Runnable onStart) {
        Objects.requireNonNull(onNext);
        Objects.requireNonNull(onError);
        Objects.requireNonNull(onComplete);
        Objects.requireNonNull(onStart);
        return new NbpAsyncObserver<T>() {
            boolean done;
            @Override
            protected void onStart() {
                try {
                    onStart.run();
                } catch (Throwable e) {
                    done = true;
                    cancel();
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
                    cancel();
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
