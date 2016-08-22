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
import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.*;

public final class ObservableDistinct<T, K> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super T, K> keySelector;
    final Callable<? extends Predicate<? super K>> predicateSupplier;

    public ObservableDistinct(ObservableSource<T> source, Function<? super T, K> keySelector, Callable<? extends Predicate<? super K>> predicateSupplier) {
        super(source);
        this.predicateSupplier = predicateSupplier;
        this.keySelector = keySelector;
    }
    
    public static <T, K> ObservableDistinct<T, K> withCollection(ObservableSource<T> source, final Function<? super T, K> keySelector, final Callable<? extends Collection<? super K>> collectionSupplier) {
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
        
        return new ObservableDistinct<T, K>(source, keySelector, p);
    }
    
    public static <T> ObservableDistinct<T, T> untilChanged(ObservableSource<T> source) {
        Callable<? extends Predicate<? super T>> p = new Callable<Predicate<T>>() {
            @Override
            public Predicate<T> call() {
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
                        return !ObjectHelper.equals(o, t);
                    }
                };
            }
        };
        return new ObservableDistinct<T, T>(source, Functions.<T>identity(), p);
    }

    public static <T, K> ObservableDistinct<T, K> untilChanged(ObservableSource<T> source, Function<? super T, K> keySelector) {
        Callable<? extends Predicate<? super K>> p = new Callable<Predicate<K>>() {
            @Override
            public Predicate<K> call() {
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
                        return !ObjectHelper.equals(o, t);
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
            coll = predicateSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }
        
        if (coll == null) {
            EmptyDisposable.error(new NullPointerException("predicateSupplier returned null"), t);
            return;
        }
        
        source.subscribe(new DistinctSubscriber<T, K>(t, keySelector, coll));
    }
    
    static final class DistinctSubscriber<T, K> implements Observer<T>, Disposable {
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
                actual.onSubscribe(this);
            }
        }
        

        @Override
        public void dispose() {
            s.dispose();
        }
        
        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }

        @Override
        public void onNext(T t) {
            K key;
            
            try {
                key = keySelector.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
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
                Exceptions.throwIfFatal(e);
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
    }
}
