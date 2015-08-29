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

import java.util.*;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscribers.CancelledSubscriber;
import io.reactivex.internal.subscriptions.*;

public final class OperatorDistinct<T> implements Operator<T, T> {
    
    final Supplier<? extends Predicate<? super T>> predicateSupplier;
    
    public OperatorDistinct(Supplier<? extends Predicate<? super T>> predicateSupplier) {
        this.predicateSupplier = predicateSupplier;
    }
    
    public static <T> OperatorDistinct<T> withCollection(Supplier<? extends Collection<? super T>> collectionSupplier) {
        Supplier<? extends Predicate<? super T>> p = () -> {
            Collection<? super T> coll = collectionSupplier.get();
            
            return t -> {
                if (t == null) {
                    coll.clear();
                    return true;
                }
                return coll.add(t);
            };
        };
        
        return new OperatorDistinct<>(p);
    }
    
    static final OperatorDistinct<Object> UNTIL_CHANGED;
    static {
        Supplier<? extends Predicate<? super Object>> p = () -> {
            Object[] last = { null };
            
            return t -> {
                if (t == null) {
                    last[0] = null;
                    return true;
                }
                Object o = last[0];
                last[0] = t;
                return !Objects.equals(o, t);
            };
        };
        UNTIL_CHANGED = new OperatorDistinct<>(p);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorDistinct<T> untilChanged() {
        return (OperatorDistinct<T>)UNTIL_CHANGED;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        Predicate<? super T> coll;
        try {
            coll = predicateSupplier.get();
        } catch (Throwable e) {
            EmptySubscription.error(e, t);
            return CancelledSubscriber.INSTANCE;
        }
        
        if (coll == null) {
            EmptySubscription.error(new NullPointerException("predicateSupplier returned null"), t);
            return CancelledSubscriber.INSTANCE;
        }
        
        return null;
    }
    
    static final class DistinctSubscriber<T> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        final Predicate<? super T> predicate;
        
        Subscription s;

        public DistinctSubscriber(Subscriber<? super T> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                s.cancel();
                actual.onError(e);
                return;
            }
            
            if (b) {
                actual.onNext(t);
            } else {
                s.request(1);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            try {
                predicate.test(null); // special case: poison pill
            } catch (Throwable e) {
                t.addSuppressed(e);
                actual.onError(t);
                return;
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            try {
                predicate.test(null); // special case: poison pill
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            actual.onComplete();
        }
    }
}
