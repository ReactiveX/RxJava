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

import java.util.ArrayDeque;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorSkipLast<T> implements Operator<T, T> {
    final int skip;
    
    public OperatorSkipLast(int skip) {
        this.skip = skip;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> s) {
        return new SkipLastSubscriber<T>(s, skip);
    }
    
    static final class SkipLastSubscriber<T> extends ArrayDeque<T> implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -3807491841935125653L;
        final Subscriber<? super T> actual;
        final int skip;
        
        Subscription s;
        
        public SkipLastSubscriber(Subscriber<? super T> actual, int skip) {
            super(skip);
            this.actual = actual;
            this.skip = skip;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            if (skip == size()) {
                actual.onNext(poll());
            } else {
                s.request(1);
            }
            offer(t);
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
