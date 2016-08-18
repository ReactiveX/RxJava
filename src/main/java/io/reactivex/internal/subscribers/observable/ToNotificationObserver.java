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

package io.reactivex.internal.subscribers.observable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class ToNotificationObserver<T> implements Observer<T> {
    final Consumer<? super Notification<Object>> consumer;
    
    Disposable s;
    
    public ToNotificationObserver(Consumer<? super Notification<Object>> consumer) {
        this.consumer = consumer;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        if (DisposableHelper.validate(this.s, s)) {
            this.s = s;
        }
    }
    
    @Override
    public void onNext(T t) {
        if (t == null) {
            s.dispose();
            onError(new NullPointerException());
        } else {
            try {
                consumer.accept(Notification.<Object>createOnNext(t));
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.dispose();
                onError(ex);
            }
        }
    }
    
    @Override
    public void onError(Throwable t) {
        try {
            consumer.accept(Notification.<Object>createOnError(t));
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }
    
    @Override
    public void onComplete() {
        try {
            consumer.accept(Notification.createOnComplete());
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }
}