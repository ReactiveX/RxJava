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
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableCount<T> extends Flowable<Long> {

    final Publisher<T> source;

    public FlowableCount(Publisher<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Long> s) {
        source.subscribe(new CountSubscriber(s));
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
            if (SubscriptionHelper.validateRequest(n)) {
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
