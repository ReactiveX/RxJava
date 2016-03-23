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

package io.reactivex.internal.subscribers.flowable;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

public final class LambdaSubscriber<T> extends AtomicReference<Object> implements Subscriber<T>, Subscription, Disposable {
    /** */
    private static final long serialVersionUID = -7251123623727029452L;
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Runnable onComplete;
    final Consumer<? super Subscription> onSubscribe;
    
    static final Object CANCELLED = new Object();
    
    public LambdaSubscriber(Consumer<? super T> onNext, Consumer<? super Throwable> onError, 
            Runnable onComplete,
            Consumer<? super Subscription> onSubscribe) {
        super();
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onSubscribe = onSubscribe;
    }
    
    @Override
    public void onSubscribe(Subscription s) {
        if (compareAndSet(null, s)) {
            onSubscribe.accept(this);
        } else {
            s.cancel();
            if (get() != CANCELLED) {
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
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
        cancel();
        try {
            onError.accept(t);
        } catch (Throwable e) {
            RxJavaPlugins.onError(e);
            RxJavaPlugins.onError(t);
        }
    }
    
    @Override
    public void onComplete() {
        cancel();
        try {
            onComplete.run();
        } catch (Throwable e) {
            RxJavaPlugins.onError(e);
        }
    }
    
    @Override
    public void dispose() {
        cancel();
    }
    
    @Override
    public void request(long n) {
        Object o = get();
        if (o != CANCELLED) {
            ((Subscription)o).request(n);
        }
    }
    
    @Override
    public void cancel() {
        Object o = get();
        if (o != CANCELLED) {
            o = getAndSet(CANCELLED);
            if (o != CANCELLED && o != null) {
                ((Subscription)o).cancel();
            }
        }
    }
}
