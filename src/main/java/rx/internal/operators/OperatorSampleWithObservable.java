/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicReference;

import rx.*;
import rx.Observable.Operator;
import rx.observers.SerializedSubscriber;

/**
 * Sample with the help of another observable.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/hh229742.aspx">MSDN: Observable.Sample</a>
 * 
 * @param <T> the source and result value type
 * @param <U> the element type of the sampler Observable
 */
public final class OperatorSampleWithObservable<T, U> implements Operator<T, T> {
    final Observable<U> sampler;
    /** Indicates that no value is available. */
    static final Object EMPTY_TOKEN = new Object();

    public OperatorSampleWithObservable(Observable<U> sampler) {
        this.sampler = sampler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
    
        final AtomicReference<Object> value = new AtomicReference<Object>(EMPTY_TOKEN);
        
        final AtomicReference<Subscription> main = new AtomicReference<Subscription>();
        
        final Subscriber<U> samplerSub = new Subscriber<U>() {
            @Override
            public void onNext(U t) {
                Object localValue = value.getAndSet(EMPTY_TOKEN);
                if (localValue != EMPTY_TOKEN) {
                    @SuppressWarnings("unchecked")
                    T v = (T)localValue;
                    s.onNext(v);
                }
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                // no need to null check, main is assigned before any of the two gets subscribed
                main.get().unsubscribe();
            }

            @Override
            public void onCompleted() {
                onNext(null);
                s.onCompleted();
                // no need to null check, main is assigned before any of the two gets subscribed
                main.get().unsubscribe();
            }
        };
        
        Subscriber<T> result = new Subscriber<T>() {
            @Override
            public void onNext(T t) {
                value.set(t);
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                
                samplerSub.unsubscribe();
            }

            @Override
            public void onCompleted() {
                samplerSub.onNext(null);
                s.onCompleted();

                samplerSub.unsubscribe();
            }
        };
        
        main.lazySet(result);
        
        child.add(result);
        child.add(samplerSub);
        
        sampler.unsafeSubscribe(samplerSub);
        
        return result;
    }
}
