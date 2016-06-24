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
import io.reactivex.functions.Function;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableMap<T, U> extends Flowable<U> {
    final Publisher<T> source;
    final Function<? super T, ? extends U> function;
    public FlowableMap(Publisher<T> source, Function<? super T, ? extends U> function) {
        this.source = source;
        this.function = function;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        source.subscribe(new MapperSubscriber<T, U>(s, function));
    }
    
    static final class MapperSubscriber<T, U> implements Subscriber<T> {
        final Subscriber<? super U> actual;
        final Function<? super T, ? extends U> function;
        
        Subscription subscription;
        
        boolean done;
        
        public MapperSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends U> function) {
            this.actual = actual;
            this.function = function;
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
            if (done) {
                return;
            }
            U u;
            try {
                u = function.apply(t);
            } catch (Throwable e) {
                done = true;
                subscription.cancel();
                actual.onError(e);
                return;
            }
            if (u == null) {
                done = true;
                subscription.cancel();
                actual.onError(new NullPointerException("Value returned by the function is null"));
                return;
            }
            actual.onNext(u);
        }
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
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
            actual.onComplete();
        }
    }
}
