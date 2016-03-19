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
import io.reactivex.functions.Predicate;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorSkipWhile<T> implements Operator<T, T> {
    final Predicate<? super T> predicate;
    public OperatorSkipWhile(Predicate<? super T> predicate) {
        this.predicate = predicate;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> s) {
        return new SkipWhileSubscriber<T>(s, predicate);
    }
    
    static final class SkipWhileSubscriber<T> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        final Predicate<? super T> predicate;
        Subscription s;
        boolean notSkipping;
        public SkipWhileSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
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
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            if (notSkipping) {
                actual.onNext(t);
            } else {
                boolean b;
                try {
                    b = predicate.test(t);
                } catch (Throwable e) {
                    s.cancel();
                    actual.onError(e);
                    return;
                }
                if (b) {
                    s.request(1);
                } else {
                    notSkipping = true;
                    actual.onNext(t);
                }
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
