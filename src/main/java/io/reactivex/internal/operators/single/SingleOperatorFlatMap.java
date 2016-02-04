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

import io.reactivex.Single;
import io.reactivex.Single.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.Function;

public final class SingleOperatorFlatMap<T, R> implements SingleOperator<R, T> {
    final Function<? super T, ? extends Single<? extends R>> mapper;

    public SingleOperatorFlatMap(Function<? super T, ? extends Single<? extends R>> mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public SingleSubscriber<? super T> apply(SingleSubscriber<? super R> t) {
        return new SingleFlatMapCallback<T, R>(t, mapper);
    }
    
    static final class SingleFlatMapCallback<T, R> implements SingleSubscriber<T> {
        final SingleSubscriber<? super R> actual;
        final Function<? super T, ? extends Single<? extends R>> mapper;
        
        final MultipleAssignmentDisposable mad;

        public SingleFlatMapCallback(SingleSubscriber<? super R> actual,
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
                actual.onError(e);
                return;
            }
            
            if (o == null) {
                actual.onError(new NullPointerException("The single returned by the mapper is null"));
                return;
            }
            
            if (mad.isDisposed()) {
                return;
            }
            
            o.subscribe(new SingleSubscriber<R>() {
                @Override
                public void onSubscribe(Disposable d) {
                    mad.set(d);
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
