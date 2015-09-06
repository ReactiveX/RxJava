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

package io.reactivex.internal.operators.nbp;

import java.util.function.Function;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;

public final class NbpOperatorMap<T, R> implements NbpOperator<R, T> {
    final Function<? super T, ? extends R> mapper;
    
    public NbpOperatorMap(Function<? super T, ? extends R> mapper) {
        this.mapper = mapper;
    }

    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super R> t) {
        return new NbpMapSubscriber<>(t, mapper);
    }
    
    static final class NbpMapSubscriber<T, R> implements NbpSubscriber<T> {
        final NbpSubscriber<? super R> actual;
        final Function<? super T, ? extends R> mapper;

        public NbpMapSubscriber(NbpSubscriber<? super R> actual, Function<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }

        @Override
        public void onSubscribe(Disposable d) {
            actual.onSubscribe(d);
        }
        
        @Override
        public void onNext(T value) {
            R v;
            try {
                v = mapper.apply(value);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            
            actual.onNext(v);
        }
        
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
