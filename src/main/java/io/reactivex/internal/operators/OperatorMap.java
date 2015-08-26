/*
 * Copyright 2011-2015 David Karnok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.internal.operators;

import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * 
 */
public final class OperatorMap<T, U> implements Operator<U, T> {
    final Function<? super T, ? extends U> function;
    public OperatorMap(Function<? super T, ? extends U> function) {
        this.function = function;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        return new MapperSubscriber<>(t, function);
    }
    
    static final class MapperSubscriber<T, U> implements Subscriber<T> {
        final Subscriber<? super U> actual;
        final Function<? super T, ? extends U> function;
        Subscription subscription;
        public MapperSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends U> function) {
            this.actual = actual;
            this.function = function;
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
            U u;
            try {
                u = function.apply(t);
            } catch (Throwable e) {
                subscription.cancel();
                actual.onError(e);
                return;
            }
            actual.onNext(u);
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
