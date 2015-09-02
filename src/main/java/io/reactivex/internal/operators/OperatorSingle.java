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

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorSingle<T> implements Operator<T, T> {
    
    static final OperatorSingle<Object> NO_DEFAULT = new OperatorSingle<>(null);
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorSingle<T> instanceNoDefault() {
        return (OperatorSingle<T>)NO_DEFAULT;
    }
    
    final T defaultValue;
    public OperatorSingle(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new SingleElementSubscriber<>(t, defaultValue);
    }
    
    static final class SingleElementSubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super T> actual;
        final T defaultValue;
        
        Subscription s;
        
        T value;
        
        boolean done;
        
        public SingleElementSubscriber(Subscriber<? super T> actual, T defaultValue) {
            this.actual = actual;
            this.defaultValue = defaultValue;
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
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (value != null) {
                done = true;
                s.cancel();
                actual.onError(new IllegalArgumentException("Sequence contains more than one element!"));
                return;
            }
            value = t;
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            T v = value;
            value = null;
            if (v == null) {
                v = defaultValue;
            }
            if (v == null) {
                actual.onError(new NoSuchElementException());
            } else {
                actual.onNext(v);
                actual.onComplete();
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
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
