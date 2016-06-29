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

package io.reactivex.internal.operators.observable;

import java.util.Collection;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.*;

public final class ObservableDistinct<T, K> extends ObservableSource<T, T> {
    final Function<? super T, K> keySelector;
    final Supplier<? extends Predicate<? super K>> predicateSupplier;

    public ObservableDistinct(ObservableConsumable<T> source, Function<? super T, K> keySelector, Supplier<? extends Predicate<? super K>> predicateSupplier) {
        super(source);
        this.predicateSupplier = predicateSupplier;
        this.keySelector = keySelector;
    }
    
    public static <T, K> ObservableDistinct<T, K> withCollection(ObservableConsumable<T> source, final Function<? super T, K> keySelector, final Supplier<? extends Collection<? super K>> collectionSupplier) {
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
        
        return new ObservableDistinct<T, K>(source, keySelector, p);
    }
    
    public static <T> ObservableDistinct<T, T> untilChanged(ObservableConsumable<T> source) {
        Supplier<? extends Predicate<? super T>> p = new Supplier<Predicate<T>>() {
            @Override
            public Predicate<T> get() {
                final Object[] last = { null };
                
                return new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
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
        return new ObservableDistinct<T, T>(source, Functions.<T>identity(), p);
    }

    public static <T, K> ObservableDistinct<T, K> untilChanged(ObservableConsumable<T> source, Function<? super T, K> keySelector) {
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
        return new ObservableDistinct<T, K>(source, keySelector, p);
    }

    
    @Override
    public void subscribeActual(Observer<? super T> t) {
        Predicate<? super K> coll;
        try {
            coll = predicateSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return;
        }
        
        if (coll == null) {
            EmptyDisposable.error(new NullPointerException("predicateSupplier returned null"), t);
            return;
        }
        
        source.subscribe(new DistinctSubscriber<T, K>(t, keySelector, coll));
    }
    
    static final class DistinctSubscriber<T, K> implements Observer<T> {
        final Observer<? super T> actual;
        final Predicate<? super K> predicate;
        final Function<? super T, K> keySelector;
        
        Disposable s;

        public DistinctSubscriber(Observer<? super T> actual, Function<? super T, K> keySelector, Predicate<? super K> predicate) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(s);
            }
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
