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

import io.reactivex.Flowable;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class FlowableFilter<T> extends Flowable<T> {
    final Publisher<T> source;
    final Predicate<? super T> predicate;
    public FlowableFilter(Publisher<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new FilterSubscriber<T>(s, predicate));
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
            if (SubscriptionHelper.validateSubscription(this.subscription, s)) {
                subscription = s;
                actual.onSubscribe(s);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                subscription.request(1);
            }
        }
        
        @Override
        public boolean tryOnNext(T t) {
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
