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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;

public final class MaybeOnErrorComplete<T> extends Maybe<T> {
    final MaybeSource<? extends T> source;

    final Callable<? extends T> valueSupplier;
    
    final T value;
    
    public MaybeOnErrorComplete(MaybeSource<? extends T> source, Callable<? extends T> valueSupplier, T value) {
        this.source = source;
        this.valueSupplier = valueSupplier;
        this.value = value;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super T> s) {

        source.subscribe(new MaybeObserver<T>() {

            @Override
            public void onError(Throwable e) {
                T v;

                if (valueSupplier != null) {
                    try {
                        v = valueSupplier.call();
                    } catch (Throwable ex) {
                        s.onError(new CompositeException(ex, e));
                        return;
                    }
                } else {
                    v = value;
                }
                
                if (v == null) {
                    NullPointerException npe = new NullPointerException("Value supplied was null");
                    npe.initCause(e);
                    s.onError(npe);
                    return;
                }
                
                s.onSuccess(v);
            }

            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }

            @Override
            public void onComplete() {
                s.onComplete();
            }

            @Override
            public void onSuccess(T value) {
                s.onSuccess(value);
            }
            
        });
    }

}
