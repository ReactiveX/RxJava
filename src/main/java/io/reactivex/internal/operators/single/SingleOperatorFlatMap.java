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

package io.reactivex.internal.operators.single;

import java.util.function.Function;

import io.reactivex.Single;
import io.reactivex.Single.*;
import io.reactivex.disposables.*;

public final class SingleOperatorFlatMap<T, R> implements SingleOperator<R, T> {
    final Function<? super T, ? extends Single<? extends R>> mapper;

    public SingleOperatorFlatMap(Function<? super T, ? extends Single<? extends R>> mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public SingleCallback<? super T> apply(SingleCallback<? super R> t) {
        return new SingleFlatMapCallback<>(t, mapper);
    }
    
    static final class SingleFlatMapCallback<T, R> implements SingleCallback<T> {
        final SingleCallback<? super R> actual;
        final Function<? super T, ? extends Single<? extends R>> mapper;
        
        final MultipleAssignmentDisposable mad;

        public SingleFlatMapCallback(SingleCallback<? super R> actual,
                Function<? super T, ? extends Single<? extends R>> mapper) {
            this.actual = actual;
            this.mapper = mapper;
            this.mad = new MultipleAssignmentDisposable();
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            mad.set(d);
        }
        
        @Override
        public void onSuccess(T value) {
            Single<? extends R> o;
            
            try {
                o = mapper.apply(value);
            } catch (Throwable e) {
                actual.onFailure(e);
                return;
            }
            
            if (o == null) {
                actual.onFailure(new NullPointerException("The single returned by the mapper is null"));
                return;
            }
            
            if (mad.isDisposed()) {
                return;
            }
            
            o.subscribe(new SingleCallback<R>() {
                @Override
                public void onSubscribe(Disposable d) {
                    mad.set(d);
                }
                
                @Override
                public void onSuccess(R value) {
                    actual.onSuccess(value);
                }
                
                @Override
                public void onFailure(Throwable e) {
                    actual.onFailure(e);
                }
            });
        }
        
        @Override
        public void onFailure(Throwable e) {
            actual.onFailure(e);
        }
    }
}
