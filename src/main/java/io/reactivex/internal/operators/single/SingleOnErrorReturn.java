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
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.Supplier;

public final class SingleOnErrorReturn<T> extends Single<T> {
    final SingleConsumable<? extends T> source;

    final Supplier<? extends T> valueSupplier;
    
    final T value;
    
    public SingleOnErrorReturn(SingleConsumable<? extends T> source, Supplier<? extends T> valueSupplier, T value) {
        this.source = source;
        this.valueSupplier = valueSupplier;
        this.value = value;
    }



    @Override
    protected void subscribeActual(final SingleSubscriber<? super T> s) {

        source.subscribe(new SingleSubscriber<T>() {

            @Override
            public void onError(Throwable e) {
                T v;

                if (valueSupplier != null) {
                    try {
                        v = valueSupplier.get();
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
            public void onSuccess(T value) {
                s.onSuccess(value);
            }
            
        });
    }

}
