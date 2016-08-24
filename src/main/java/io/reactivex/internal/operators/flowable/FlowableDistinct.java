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
import java.util.concurrent.Callable;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableDistinct<T, K> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super T, K> keySelector;
    final Callable<? extends Predicate<? super K>> predicateSupplier;
    
    public FlowableDistinct(Publisher<T> source, Function<? super T, K> keySelector, Callable<? extends Predicate<? super K>> predicateSupplier) {
        super(source);
        this.predicateSupplier = predicateSupplier;
        this.keySelector = keySelector;
    }
    
    public static <T, K> Flowable<T> withCollection(Publisher<T> source, Function<? super T, K> keySelector, final Callable<? extends Collection<? super K>> collectionSupplier) {
        Callable<? extends Predicate<? super K>> p = new Callable<Predicate<K>>() {
            @Override
            public Predicate<K> call() throws Exception {
                final Collection<? super K> coll = collectionSupplier.call();
                
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
        
        return RxJavaPlugins.onAssembly(new FlowableDistinct<T, K>(source, keySelector, p));
    }
    
    public static <T> Flowable<T> untilChanged(Publisher<T> source) {
        Callable<? extends Predicate<? super T>> p = new Callable<Predicate<T>>() {
            Object last;
            @Override
            public Predicate<T> call() {
                
                return new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        if (t == null) {
                            last = null;
                            return true;
                        }
                        Object o = last;
                        last = t;
                        return !ObjectHelper.equals(o, t);
                    }
                };
            }
        };
        return RxJavaPlugins.onAssembly(new FlowableDistinct<T, T>(source, Functions.<T>identity(), p));
    }

    public static <T, K> Flowable<T> untilChanged(Publisher<T> source, Function<? super T, K> keySelector) {
        Callable<? extends Predicate<? super K>> p = new Callable<Predicate<K>>() {
            Object last;
            @Override
            public Predicate<K> call() {
                
                return new Predicate<K>() {
                    @Override
                    public boolean test(K t) {
                        if (t == null) {
                            last = null;
                            return true;
                        }
                        Object o = last;
                        last = t;
                        return !ObjectHelper.equals(o, t);
                    }
                };
            }
        };
        return RxJavaPlugins.onAssembly(new FlowableDistinct<T, K>(source, keySelector, p));
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        Predicate<? super K> coll;
        try {
            coll = predicateSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }
        
        if (coll == null) {
            EmptySubscription.error(new NullPointerException("predicateSupplier returned null"), s);
            return;
        }
        
        source.subscribe(new DistinctSubscriber<T, K>(s, keySelector, coll));
    }
    
    static final class DistinctSubscriber<T, K> implements Subscriber<T>, Subscription {
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
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            K key;
            
            try {
                key = keySelector.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
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
                Exceptions.throwIfFatal(e);
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
                Exceptions.throwIfFatal(e);
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
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }
            actual.onComplete();
        }
        
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
