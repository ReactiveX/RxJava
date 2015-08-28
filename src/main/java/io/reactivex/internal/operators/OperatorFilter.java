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
import io.reactivex.internal.subscribers.ConditionalSubscriber;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class OperatorFilter<T> implements Operator<T, T> {
    final Predicate<? super T> predicate;
    public OperatorFilter(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> s) {
        return new FilterSubscriber<>(s, predicate);
    }
    
    static final class FilterSubscriber<T> implements ConditionalSubscriber<T> {
        final Predicate<? super T> filter;
        final Subscriber<? super T> actual;
        Subscription subscription;
        public FilterSubscriber(Subscriber<? super T> actual, Predicate<? super T> filter) {
            this.actual = actual;
            this.filter = filter;
        }
        @Override
        public void onSubscribe(Subscription s) {
            if (subscription != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            subscription = s;
            actual.onSubscribe(s);
        }
        @Override
        public void onNext(T t) {
            if (!onNextIf(t)) {
                subscription.request(1);
            }
        }
        
        @Override
        public boolean onNextIf(T t) {
            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                subscription.cancel();
                actual.onError(e);
                return true;
            }
            if (b) {
                actual.onNext(t);
            }
            return b;
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            actual.onComplete();
        }
    }
}
