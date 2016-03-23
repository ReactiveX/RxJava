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
import io.reactivex.internal.subscriptions.SubscriptionArbiter;

public final class OperatorSwitchIfEmpty<T> implements Operator<T, T> {
    final Publisher<? extends T> other;
    public OperatorSwitchIfEmpty(Publisher<? extends T> other) {
        this.other = other;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        // TODO Auto-generated method stub
        SwitchIfEmptySubscriber<T> parent = new SwitchIfEmptySubscriber<T>(t, other);
        t.onSubscribe(parent.arbiter);
        return parent;
    }
    
    static final class SwitchIfEmptySubscriber<T> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        final Publisher<? extends T> other;
        final SubscriptionArbiter arbiter;
        
        boolean empty;
        
        public SwitchIfEmptySubscriber(Subscriber<? super T> actual, Publisher<? extends T> other) {
            this.actual = actual;
            this.other = other;
            this.empty = true;
            this.arbiter = new SubscriptionArbiter();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            arbiter.setSubscription(s);
        }
        
        @Override
        public void onNext(T t) {
            if (empty) {
                empty = false;
            }
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (empty) {
                empty = false;
                other.subscribe(this);
            } else {
                actual.onComplete();
            }
        }
    }
}
