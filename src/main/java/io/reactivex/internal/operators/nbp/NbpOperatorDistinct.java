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

package io.reactivex.internal.operators.nbp;

import java.util.*;
import java.util.function.*;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscribers.nbp.NbpCancelledSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorDistinct<T, K> implements NbpOperator<T, T> {
    final Function<? super T, K> keySelector;
    final Supplier<? extends Predicate<? super K>> predicateSupplier;
    
    public NbpOperatorDistinct(Function<? super T, K> keySelector, Supplier<? extends Predicate<? super K>> predicateSupplier) {
        this.predicateSupplier = predicateSupplier;
        this.keySelector = keySelector;
    }
    
    public static <T, K> NbpOperatorDistinct<T, K> withCollection(Function<? super T, K> keySelector, Supplier<? extends Collection<? super K>> collectionSupplier) {
        Supplier<? extends Predicate<? super K>> p = () -> {
            Collection<? super K> coll = collectionSupplier.get();
            
            return t -> {
                if (t == null) {
                    coll.clear();
                    return true;
                }
                return coll.add(t);
            };
        };
        
        return new NbpOperatorDistinct<>(keySelector, p);
    }
    
    static final NbpOperatorDistinct<Object, Object> UNTIL_CHANGED;
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
        UNTIL_CHANGED = new NbpOperatorDistinct<>(v -> v, p);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> NbpOperatorDistinct<T, T> untilChanged() {
        return (NbpOperatorDistinct<T, T>)UNTIL_CHANGED;
    }

    public static <T, K> NbpOperatorDistinct<T, K> untilChanged(Function<? super T, K> keySelector) {
        Supplier<? extends Predicate<? super K>> p = () -> {
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
        return new NbpOperatorDistinct<>(keySelector, p);
    }

    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        Predicate<? super K> coll;
        try {
            coll = predicateSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return NbpCancelledSubscriber.INSTANCE;
        }
        
        if (coll == null) {
            EmptyDisposable.error(new NullPointerException("predicateSupplier returned null"), t);
            return NbpCancelledSubscriber.INSTANCE;
        }
        
        return new DistinctSubscriber<>(t, keySelector, coll);
    }
    
    static final class DistinctSubscriber<T, K> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;
        final Predicate<? super K> predicate;
        final Function<? super T, K> keySelector;
        
        Disposable s;

        public DistinctSubscriber(NbpSubscriber<? super T> actual, Function<? super T, K> keySelector, Predicate<? super K> predicate) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
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
                s.dispose();
                actual.onError(e);
                return;
            }
            
            if (key == null) {
                s.dispose();
                actual.onError(new NullPointerException("Null key supplied"));
                return;
            }
            
            
            boolean b;
            try {
                b = predicate.test(key);
            } catch (Throwable e) {
                s.dispose();
                actual.onError(e);
                return;
            }
            
            if (b) {
                actual.onNext(t);
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
