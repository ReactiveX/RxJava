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

import java.util.Collection;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.subscribers.flowable.CancelledSubscriber;
import io.reactivex.internal.subscriptions.*;

public final class OperatorDistinct<T, K> implements Operator<T, T> {
    final Function<? super T, K> keySelector;
    final Supplier<? extends Predicate<? super K>> predicateSupplier;
    
    public OperatorDistinct(Function<? super T, K> keySelector, Supplier<? extends Predicate<? super K>> predicateSupplier) {
        this.predicateSupplier = predicateSupplier;
        this.keySelector = keySelector;
    }
    
    public static <T, K> OperatorDistinct<T, K> withCollection(Function<? super T, K> keySelector, final Supplier<? extends Collection<? super K>> collectionSupplier) {
        Supplier<? extends Predicate<? super K>> p = new Supplier<Predicate<K>>() {
            @Override
            public Predicate<K> get() {
                final Collection<? super K> coll = collectionSupplier.get();
                
                return new Predicate<K>() {
                    @Override
                    public boolean test(K t) {
                        if (t == null) {
                            coll.clear();
                            return true;
                        }
                        return coll.add(t);
                    }
                };
            }
        };
        
        return new OperatorDistinct<T, K>(keySelector, p);
    }
    
    static final OperatorDistinct<Object, Object> UNTIL_CHANGED;
    static {
        Supplier<? extends Predicate<? super Object>> p = new Supplier<Predicate<Object>>() {
            @Override
            public Predicate<Object> get() {
                final Object[] last = { null };
                
                return new Predicate<Object>() {
                    @Override
                    public boolean test(Object t) {
                        if (t == null) {
                            last[0] = null;
                            return true;
                        }
                        Object o = last[0];
                        last[0] = t;
                        return !Objects.equals(o, t);
                    }
                };
            }
        };
        UNTIL_CHANGED = new OperatorDistinct<Object, Object>(Functions.identity(), p);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorDistinct<T, T> untilChanged() {
        return (OperatorDistinct<T, T>)UNTIL_CHANGED;
    }

    public static <T, K> OperatorDistinct<T, K> untilChanged(Function<? super T, K> keySelector) {
        Supplier<? extends Predicate<? super K>> p = new Supplier<Predicate<K>>() {
            @Override
            public Predicate<K> get() {
                final Object[] last = { null };
                
                return new Predicate<K>() {
                    @Override
                    public boolean test(K t) {
                        if (t == null) {
                            last[0] = null;
                            return true;
                        }
                        Object o = last[0];
                        last[0] = t;
                        return !Objects.equals(o, t);
                    }
                };
            }
        };
        return new OperatorDistinct<T, K>(keySelector, p);
    }

    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        Predicate<? super K> coll;
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
        
        return new DistinctSubscriber<T, K>(t, keySelector, coll);
    }
    
    static final class DistinctSubscriber<T, K> implements Subscriber<T> {
        final Subscriber<? super T> actual;
        final Predicate<? super K> predicate;
        final Function<? super T, K> keySelector;
        
        Subscription s;

        public DistinctSubscriber(Subscriber<? super T> actual, Function<? super T, K> keySelector, Predicate<? super K> predicate) {
            this.actual = actual;
            this.keySelector = keySelector;
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
            K key;
            
            try {
                key = keySelector.apply(t);
            } catch (Throwable e) {
                s.cancel();
                actual.onError(e);
                return;
            }
            
            if (key == null) {
                s.cancel();
                actual.onError(new NullPointerException("Null key supplied"));
                return;
            }
            
            
            boolean b;
            try {
                b = predicate.test(key);
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
                actual.onError(new CompositeException(e, t));
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
