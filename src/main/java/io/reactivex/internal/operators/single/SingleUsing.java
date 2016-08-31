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

package io.reactivex.internal.operators.single;

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleUsing<T, U> extends Single<T> {

    final Callable<U> resourceSupplier;
    final Function<? super U, ? extends SingleSource<? extends T>> singleFunction;
    final Consumer<? super U> disposer; 
    final boolean eager;
    
    public SingleUsing(Callable<U> resourceSupplier,
                       Function<? super U, ? extends SingleSource<? extends T>> singleFunction, Consumer<? super U> disposer,
                       boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.singleFunction = singleFunction;
        this.disposer = disposer;
        this.eager = eager;
    }



    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        final U resource; // NOPMD
        
        try {
            resource = resourceSupplier.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, s);
            return;
        }
        
        SingleSource<? extends T> s1;
        
        try {
            s1 = ObjectHelper.requireNonNull(singleFunction.apply(resource), "The singleFunction returned a null SingleSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, s);
            return;
        }
        
        s1.subscribe(new SingleObserver<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                if (eager) {
                    CompositeDisposable set = new CompositeDisposable();
                    set.add(d);
                    set.add(Disposables.fromRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable e) {
                                Exceptions.throwIfFatal(e);
                                RxJavaPlugins.onError(e);
                            }
                        }
                    }));
                } else {
                    s.onSubscribe(d);
                }
            }

            @Override
            public void onSuccess(T value) {
                if (eager) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        s.onError(e);
                        return;
                    }
                }
                s.onSuccess(value);
                if (!eager) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);
                        RxJavaPlugins.onError(e);
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                if (eager) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        e = new CompositeException(ex, e);
                    }
                }
                s.onError(e);
                if (!eager) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
            
        });
    }

}
