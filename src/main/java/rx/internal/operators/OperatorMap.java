/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import rx.*;
import rx.Observable.Operator;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Applies a function of your choosing to every item emitted by an {@code Observable}, and emits the results of
 * this transformation as a new {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
 * 
 * @param <T> the input value type
 * @param <R> the return value type
 */
public final class OperatorMap<T, R> implements Operator<R, T> {

    final Func1<? super T, ? extends R> transformer;

    public OperatorMap(Func1<? super T, ? extends R> transformer) {
        this.transformer = transformer;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        MapSubscriber<T, R> parent = new MapSubscriber<T, R>(o, transformer);
        o.add(parent);
        return parent;
    }
    
    static final class MapSubscriber<T, R> extends Subscriber<T> {
        
        final Subscriber<? super R> actual;
        
        final Func1<? super T, ? extends R> mapper;

        boolean done;
        
        public MapSubscriber(Subscriber<? super R> actual, Func1<? super T, ? extends R> mapper) {
            this.actual = actual;
            this.mapper = mapper;
        }
        
        @Override
        public void onNext(T t) {
            R result;
            
            try {
                result = mapper.call(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(OnErrorThrowable.addValueAsLastCause(ex, t));
                return;
            }
            
            actual.onNext(result);
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPluginUtils.handleException(e);
                return;
            }
            done = true;
            
            actual.onError(e);
        }
        
        
        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            actual.onCompleted();
        }
        
        @Override
        public void setProducer(Producer p) {
            actual.setProducer(p);
        }
    }

}

