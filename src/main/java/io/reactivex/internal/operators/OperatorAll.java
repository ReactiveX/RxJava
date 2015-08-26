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

import java.util.function.Predicate;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorAll<T> implements Operator<Boolean, T> {
    final Predicate<? super T> predicate;
    public OperatorAll(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Boolean> t) {
        return new AnySubscriber<>(t, predicate);
    }
    
    static final class AnySubscriber<T> implements Subscriber<T>, Subscription {
        final Subscriber<? super Boolean> actual;
        final Predicate<? super T> predicate;
        
        Subscription s;
        
        boolean done;
        
        public AnySubscriber(Subscriber<? super Boolean> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
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
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                done = true;
                s.cancel();
                actual.onError(e);
                return;
            }
            if (!b) {
                done = true;
                s.cancel();
                actual.onNext(false);
                actual.onComplete();
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                actual.onError(t);
            }
        }
        
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onNext(true);
                actual.onComplete();
            }
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
