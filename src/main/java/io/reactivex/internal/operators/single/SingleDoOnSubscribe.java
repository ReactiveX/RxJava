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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleDoOnSubscribe<T> extends Single<T> {

    final SingleConsumable<T> source;
    
    final Consumer<? super Disposable> onSubscribe;
    
    public SingleDoOnSubscribe(SingleConsumable<T> source, Consumer<? super Disposable> onSubscribe) {
        this.source = source;
        this.onSubscribe = onSubscribe;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super T> s) {

        source.subscribe(new SingleSubscriber<T>() {
            boolean done;
            @Override
            public void onSubscribe(Disposable d) {
                try {
                    onSubscribe.accept(d);
                } catch (Throwable ex) {
                    done = true;
                    d.dispose();
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(ex);
                    return;
                }
                
                s.onSubscribe(d);
            }

            @Override
            public void onSuccess(T value) {
                if (done) {
                    return;
                }
                s.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                s.onError(e);
            }
            
        });
    }

}
