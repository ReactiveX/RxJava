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

import java.util.*;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.functions.Supplier;
import io.reactivex.internal.subscribers.flowable.CancelledSubscriber;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorToList<T, U extends Collection<? super T>> implements Operator<U, T> {
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final OperatorToList DEFAULT = new OperatorToList(new Supplier() {
        @Override
        public Object get() {
            return new ArrayList<Object>();
        }
    });
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorToList<T, List<T>> defaultInstance() {
        return DEFAULT;
    }
    
    final Supplier<U> collectionSupplier;
    
    public OperatorToList(Supplier<U> collectionSupplier) {
        this.collectionSupplier = collectionSupplier;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super U> t) {
        U coll;
        try {
            coll = collectionSupplier.get();
        } catch (Throwable e) {
            EmptySubscription.error(e, t);
            return CancelledSubscriber.INSTANCE;
        }
        return new ToListSubscriber<T, U>(t, coll);
    }
    
    static final class ToListSubscriber<T, U extends Collection<? super T>> implements Subscriber<T>, Subscription {
        U collection;
        final Subscriber<? super U> actual;
        
        Subscription s;
        
        public ToListSubscriber(Subscriber<? super U> actual, U collection) {
            this.actual = actual;
            this.collection = collection;
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
        public void onNext(T t) {
            collection.add(t);
        }
        
        @Override
        public void onError(Throwable t) {
            collection = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            U c = collection;
            collection = null;
            actual.onNext(c);
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > 0 required but it was " + n));
                return;
            }
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
