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

import java.util.Optional;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.Try;

public enum OperatorDematerialize implements Operator<Object, Try<Optional<Object>>> {
    INSTANCE;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Operator<T, Try<Optional<T>>> instance() {
        return (Operator)INSTANCE;
    }
    
    @Override
    public Subscriber<? super Try<Optional<Object>>> apply(Subscriber<? super Object> t) {
        return new DematerializeSubscriber<>(t);
    }
    
    static final class DematerializeSubscriber<T> implements Subscriber<Try<Optional<T>>> {
        final Subscriber<? super T> actual;
        
        boolean done;
        
        public DematerializeSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(Try<Optional<T>> t) {
            if (done) {
                return;
            }
            if (t.hasError()) {
                onError(t.error());
            } else {
                Optional<T> o = t.value();
                if (o.isPresent()) {
                    actual.onNext(o.get());
                } else {
                    onComplete();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
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
