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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;

public enum OperatorIgnoreElements implements Operator<Object, Object> {
    INSTANCE;
    @SuppressWarnings("unchecked")
    public static <T> Operator<T, T> instance() {
        return (Operator<T, T>)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Object> apply(final Subscriber<? super Object> t) {
        return new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                t.onSubscribe(s);
                s.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(Object v) {
                // deliberately ignored
            }
            
            @Override
            public void onError(Throwable e) {
                t.onError(e);
            }
            
            @Override
            public void onComplete() {
                t.onComplete();
            }
        };
    }
}
