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
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class SingleFlatMap<T, R> extends Single<R> {
    final SingleSource<? extends T> source;
    
    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    public SingleFlatMap(SingleSource<? extends T> source, Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        this.mapper = mapper;
        this.source = source;
    }
    
    @Override
    protected void subscribeActual(SingleObserver<? super R> subscriber) {
        SingleFlatMapCallback<T, R> parent = new SingleFlatMapCallback<T, R>(subscriber, mapper);
        subscriber.onSubscribe(parent.sd);
        source.subscribe(parent);
    }
    
    static final class SingleFlatMapCallback<T, R> implements SingleObserver<T> {
        final SingleObserver<? super R> actual;
        final Function<? super T, ? extends SingleSource<? extends R>> mapper;
        
        final SequentialDisposable sd;

        public SingleFlatMapCallback(SingleObserver<? super R> actual,
                Function<? super T, ? extends SingleSource<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
            this.sd = new SequentialDisposable();
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            sd.replace(d);
        }
        
        @Override
        public void onSuccess(T value) {
            SingleSource<? extends R> o;
            
            try {
                o = mapper.apply(value);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }
            
            if (o == null) {
                actual.onError(new NullPointerException("The single returned by the mapper is null"));
                return;
            }
            
            if (sd.isDisposed()) {
                return;
            }
            
            o.subscribe(new SingleObserver<R>() {
                @Override
                public void onSubscribe(Disposable d) {
                    sd.replace(d);
                }
                
                @Override
                public void onSuccess(R value) {
                    actual.onSuccess(value);
                }
                
                @Override
                public void onError(Throwable e) {
                    actual.onError(e);
                }
            });
        }
        
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
    }
}
