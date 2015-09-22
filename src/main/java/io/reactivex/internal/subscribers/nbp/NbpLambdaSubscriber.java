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

package io.reactivex.internal.subscribers.nbp;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpLambdaSubscriber<T> extends AtomicReference<Disposable> implements NbpSubscriber<T>, Disposable {
    /** */
    private static final long serialVersionUID = -7251123623727029452L;
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    final Consumer<? super Disposable> onSubscribe;
    
    static final Disposable CANCELLED = () -> { };
    
    public NbpLambdaSubscriber(Consumer<? super T> onNext, Consumer<? super Throwable> onError, 
            Runnable onComplete,
            Consumer<? super Disposable> onSubscribe) {
        super();
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        if (compareAndSet(null, s)) {
            onSubscribe.accept(this);
        } else {
            s.dispose();
            if (get() != CANCELLED) {
                SubscriptionHelper.reportDisposableSet();
            }
        }
    }
    
    @Override
    public void onNext(T t) {
        try {
            onNext.accept(t);
        } catch (Throwable e) {
            onError(e);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        dispose();
        try {
            onError.accept(t);
        } catch (Throwable e) {
            e.addSuppressed(t);
            RxJavaPlugins.onError(e);
        }
    }
    
    @Override
    public void onComplete() {
        dispose();
        try {
            onComplete.run();
        } catch (Throwable e) {
            RxJavaPlugins.onError(e);
        }
    }
    
    @Override
    public void dispose() {
        Disposable o = get();
        if (o != CANCELLED) {
            o = getAndSet(CANCELLED);
            if (o != CANCELLED && o != null) {
                o.dispose();
            }
        }
    }
}
