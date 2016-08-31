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

package io.reactivex.internal.operators.completable;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableUsing<R> extends Completable {

    final Callable<R> resourceSupplier;
    final Function<? super R, ? extends CompletableSource> completableFunction;
    final Consumer<? super R> disposer;
    final boolean eager;
    
    public CompletableUsing(Callable<R> resourceSupplier,
                            Function<? super R, ? extends CompletableSource> completableFunction, Consumer<? super R> disposer,
                            boolean eager) {
        this.resourceSupplier = resourceSupplier;
        this.completableFunction = completableFunction;
        this.disposer = disposer;
        this.eager = eager;
    }



    @Override
    protected void subscribeActual(final CompletableObserver s) {
        final R resource; // NOPMD
        
        try {
            resource = resourceSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, s);
            return;
        }
        
        CompletableSource cs;
        
        try {
            cs = ObjectHelper.requireNonNull(completableFunction.apply(resource), "The completableFunction returned a null Completable");
        } catch (Throwable e) {
            try {
                disposer.accept(resource);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }
            
            EmptyDisposable.error(e, s);
            return;
        }
        
        final AtomicBoolean once = new AtomicBoolean();
        
        cs.subscribe(new CompletableObserver() {
            Disposable d;
            void disposeThis() {
                d.dispose();
                if (once.compareAndSet(false, true)) {
                    try {
                        disposer.accept(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        RxJavaPlugins.onError(ex);
                    }
                }
            }

            @Override
            public void onComplete() {
                if (eager) {
                    if (once.compareAndSet(false, true)) {
                        try {
                            disposer.accept(resource);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            s.onError(ex);
                            return;
                        }
                    }
                }
                
                s.onComplete();
                
                if (!eager) {
                    disposeThis();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (eager) {
                    if (once.compareAndSet(false, true)) {
                        try {
                            disposer.accept(resource);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            e = new CompositeException(ex, e);
                        }
                    }
                }
                
                s.onError(e);
                
                if (!eager) {
                    disposeThis();
                }
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                this.d = d;
                s.onSubscribe(Disposables.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        disposeThis();
                    }
                }));
            }
        });
    }

}
