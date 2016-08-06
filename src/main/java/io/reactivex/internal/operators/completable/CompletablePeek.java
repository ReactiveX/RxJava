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

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletablePeek extends Completable {

    final CompletableConsumable source;
    final Consumer<? super Disposable> onSubscribe; 
    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    final Runnable onTerminate;
    final Runnable onAfterTerminate;
    final Runnable onDisposed;
    
    public CompletablePeek(CompletableConsumable source, Consumer<? super Disposable> onSubscribe,
            Consumer<? super Throwable> onError, 
            Runnable onComplete, 
            Runnable onTerminate, 
            Runnable onAfterTerminate, 
            Runnable onDisposed) {
        this.source = source;
        this.onSubscribe = onSubscribe;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onTerminate = onTerminate;
        this.onAfterTerminate = onAfterTerminate;
        this.onDisposed = onDisposed;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {

        source.subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                try {
                    onComplete.run();
                    onTerminate.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    s.onError(e);
                    return;
                }
                
                s.onComplete();
                
                try {
                    onAfterTerminate.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    RxJavaPlugins.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    onError.accept(e);
                    onTerminate.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    e = new CompositeException(ex, e);
                }
                
                s.onError(e);
                
                try {
                    onAfterTerminate.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }

            @Override
            public void onSubscribe(final Disposable d) {
                
                try {
                    onSubscribe.accept(d);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    d.dispose();
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(ex);
                    return;
                }
                
                s.onSubscribe(Disposables.from(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            onDisposed.run();
                        } catch (Throwable e) {
                            Exceptions.throwIfFatal(e);
                            RxJavaPlugins.onError(e);
                        }
                        d.dispose();
                    }
                }));
            }
            
        });
    }

    
}
