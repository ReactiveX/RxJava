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
package rx.operators;

import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.observers.SerializedSubscriber;

/**
 * Sample with the help of another observable.
 * 
 * @see <a href='http://msdn.microsoft.com/en-us/library/hh229742.aspx'>MSDN: Observable.Sample</a>
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
        
        Subscriber<U> samplerSub = new Subscriber<U>(child) {
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
                unsubscribe();
            }

            @Override
            public void onCompleted() {
                s.onCompleted();
                unsubscribe();
            }
            
        };
        
        Subscriber<T> result = new Subscriber<T>(child) {
            @Override
            public void onNext(T t) {
                value.set(t);
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
                unsubscribe();
            }

            @Override
            public void onCompleted() {
                s.onCompleted();
                unsubscribe();
            }
        };
        
        sampler.unsafeSubscribe(samplerSub);
        
        return result;
    }
}
