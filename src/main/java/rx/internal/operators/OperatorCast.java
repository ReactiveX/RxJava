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
import rx.internal.util.RxJavaPluginUtils;

/**
 * Converts the elements of an observable sequence to the specified type.
 * @param <T> the input value type
 * @param <R> the output value type
 */
public class OperatorCast<T, R> implements Operator<R, T> {

    final Class<R> castClass;

    public OperatorCast(Class<R> castClass) {
        this.castClass = castClass;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        CastSubscriber<T, R> parent = new CastSubscriber<T, R>(o, castClass);
        o.add(parent);
        return parent;
    }
    
    static final class CastSubscriber<T, R> extends Subscriber<T> {
        
        final Subscriber<? super R> actual;
        
        final Class<R> castClass;

        boolean done;
        
        public CastSubscriber(Subscriber<? super R> actual, Class<R> castClass) {
            this.actual = actual;
            this.castClass = castClass;
        }
        
        @Override
        public void onNext(T t) {
            R result;
            
            try {
                result = castClass.cast(t);
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
