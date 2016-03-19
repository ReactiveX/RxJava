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

public final class OperatorSkip<T> implements Operator<T, T> {
    final long n;
    public OperatorSkip(long n) {
        this.n = n;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> s) {
        return new SkipSubscriber<T>(s, n);
    }
    
    static final class SkipSubscriber<T> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        long remaining;
        public SkipSubscriber(Subscriber<? super T> actual, long n) {
            this.actual = actual;
            this.remaining = n;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            long n = remaining;
            actual.onSubscribe(s);
            s.request(n);
        }
        
        @Override
        public void onNext(T t) {
            if (remaining != 0L) {
                remaining--;
            } else {
                actual.onNext(t);
            }
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
