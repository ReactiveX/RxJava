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

import io.reactivex.Observer;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.functions.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to create Observers from lambdas.
 */
public final class Observers {
    /** Utility class with factory methods only. */
    private Observers() {
        throw new IllegalStateException("No instances!");
    }
    
    public static <T> Observer<T> empty() {
        return new Observer<T>() {
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
    
    public static <T> AsyncObserver<T> emptyAsync() {
        return new AsyncObserver<T>() {
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
    
    public static <T> Observer<T> create(Consumer<? super T> onNext) {
        return create(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> Observer<T> create(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError) {
        return create(onNext, onError, Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> Observer<T> create(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete) {
        return create(onNext, onError, onComplete, Functions.emptyRunnable());
    }

    public static <T> Observer<T> create(
            final Consumer<? super T> onNext, 
            final Consumer<? super Throwable> onError, 
            final Runnable onComplete, 
            final Runnable onStart) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new Observer<T>() {
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
    
    public static <T> AsyncObserver<T> createAsync(Consumer<? super T> onNext) {
        return createAsync(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> AsyncObserver<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError) {
        return createAsync(onNext, onError, Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    public static <T> AsyncObserver<T> createAsync(Consumer<? super T> onNext, 
            Consumer<? super Throwable> onError, Runnable onComplete) {
        return createAsync(onNext, onError, onComplete, Functions.emptyRunnable());
    }
    
    public static <T> AsyncObserver<T> createAsync(
            final Consumer<? super T> onNext, 
            final Consumer<? super Throwable> onError, 
            final Runnable onComplete, 
            final Runnable onStart) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onStart, "onStart is null");
        return new AsyncObserver<T>() {
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
