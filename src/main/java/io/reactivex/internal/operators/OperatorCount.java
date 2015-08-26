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

package io.reactivex.internal.operators;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.plugins.RxJavaPlugins;

public enum OperatorCount implements Operator<Long, Object> {
    INSTANCE;
    
    @SuppressWarnings("unchecked")
    public static <T> Operator<Long, T> instance() {
        return (Operator<Long, T>)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Object> apply(Subscriber<? super Long> t) {
        return new CountSubscriber(t);
    }
    
    static final class CountSubscriber implements Subscriber<Object>, Subscription {
        final Subscriber<? super Long> actual;
        
        Subscription s;
        
        long count;
        
        public CountSubscriber(Subscriber<? super Long> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(Object t) {
            count++;
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            actual.onNext(count);
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > required but it was " + n));
                return;
            }
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
