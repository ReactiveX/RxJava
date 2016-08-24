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

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Combines a main sequence of values with the latest from multiple other sequences via
 * a selector function.
 *
 * @param <T> the main sequence's type
 * @param <R> the output type
 */
public class ObservableWithLatestFromMany<T, R> extends AbstractObservableWithUpstream<T, R> {

    final ObservableSource<?>[] otherArray;
    
    final Iterable<? extends ObservableSource<?>> otherIterable;
    
    final Function<? super Object[], R> combiner;
    
    public ObservableWithLatestFromMany(ObservableSource<T> source, ObservableSource<?>[] otherArray, Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = otherArray;
        this.otherIterable = null;
        this.combiner = combiner;
    }

    public ObservableWithLatestFromMany(ObservableSource<T> source, Iterable<? extends ObservableSource<?>> otherIterable, Function<? super Object[], R> combiner) {
        super(source);
        this.otherArray = null;
        this.otherIterable = otherIterable;
        this.combiner = combiner;
    }

    @Override
    protected void subscribeActual(Observer<? super R> s) {
        ObservableSource<?>[] others = otherArray;
        int n = 0;
        if (others == null) {
            others = new ObservableSource[8];
            
            try {
                for (ObservableSource<?> p : otherIterable) {
                    if (n == others.length) {
                        others = Arrays.copyOf(others, n + (n >> 1));
                    }
                    others[n++] = p;
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                EmptyDisposable.error(ex, s);
                return;
            }
            
        } else {
            n = others.length;
        }
        
        if (n == 0) {
            new ObservableMap<T, R>(source, new Function<T, R>() {
                @Override
                public R apply(T t) throws Exception {
                    return combiner.apply(new Object[] { t });
                }
            }).subscribeActual(s);
            return;
        }
        
        WithLatestFromObserver<T, R> parent = new WithLatestFromObserver<T, R>(s, combiner, n);
        s.onSubscribe(parent);
        parent.subscribe(others, n);
        
        source.subscribe(parent);
    }
    
    static final class WithLatestFromObserver<T, R> 
    extends AtomicInteger
    implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 1577321883966341961L;

        final Observer<? super R> actual;
        
        final Function<? super Object[], R> combiner;
        
        final WithLatestInnerSubscriber[] subscribers;
        
        final AtomicReferenceArray<Object> values;
        
        final AtomicReference<Disposable> d;
        
        final AtomicThrowable error;
        
        volatile boolean done;
        
        public WithLatestFromObserver(Observer<? super R> actual, Function<? super Object[], R> combiner, int n) {
            this.actual = actual;
            this.combiner = combiner;
            WithLatestInnerSubscriber[] s = new WithLatestInnerSubscriber[n];
            for (int i = 0; i < n; i++) {
                s[i] = new WithLatestInnerSubscriber(this, i);
            }
            this.subscribers = s;
            this.values = new AtomicReferenceArray<Object>(n);
            this.d = new AtomicReference<Disposable>();
            this.error = new AtomicThrowable();
        }
        
        void subscribe(ObservableSource<?>[] others, int n) {
            WithLatestInnerSubscriber[] subscribers = this.subscribers;
            AtomicReference<Disposable> s = this.d;
            for (int i = 0; i < n; i++) {
                if (DisposableHelper.isDisposed(s.get()) || done) {
                    return;
                }
                others[i].subscribe(subscribers[i]);
            }
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this.d, d);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            AtomicReferenceArray<Object> ara = values;
            int n = ara.length();
            Object[] objects = new Object[n + 1];
            objects[0] = t;
            
            for (int i = 0; i < n; i++) {
                Object o = ara.get(i);
                if (o == null) {
                    // no latest, skip this value
                    return;
                }
                objects[i + 1] = o;
            }
            
            R v;
            
            try {
                v = ObjectHelper.requireNonNull(combiner.apply(objects), "combiner returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                dispose();
                onError(ex);
                return;
            }
            
            HalfSerializer.onNext(actual, v, this, error);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            cancelAllBut(-1);
            HalfSerializer.onError(actual, t, this, error);
        }
        
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                cancelAllBut(-1);
                HalfSerializer.onComplete(actual, this, error);
            }
        }
        
        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(d.get());
        }
        
        @Override
        public void dispose() {
            DisposableHelper.dispose(d);
            for (Disposable s : subscribers) {
                s.dispose();
            }
        }
        
        void innerNext(int index, Object o) {
            values.set(index, o);
        }
        
        void innerError(int index, Throwable t) {
            done = true;
            DisposableHelper.dispose(d);
            cancelAllBut(index);
            HalfSerializer.onError(actual, t, this, error);
        }
        
        void innerComplete(int index, boolean nonEmpty) {
            if (!nonEmpty) {
                done = true;
                cancelAllBut(index);
                HalfSerializer.onComplete(actual, this, error);
            }
        }
        
        void cancelAllBut(int index) {
            WithLatestInnerSubscriber[] subscribers = this.subscribers;
            for (int i = 0; i < subscribers.length; i++) {
                if (i != index) {
                    subscribers[i].dispose();
                }
            }
        }
    }

    static final class WithLatestInnerSubscriber
    extends AtomicReference<Disposable>
    implements Observer<Object>, Disposable {
        /** */
        private static final long serialVersionUID = 3256684027868224024L;

        final WithLatestFromObserver<?, ?> parent;
        
        final int index;
        
        boolean hasValue;
        
        public WithLatestInnerSubscriber(WithLatestFromObserver<?, ?> parent, int index) {
            this.parent = parent;
            this.index = index;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }
        
        @Override
        public void onNext(Object t) {
            if (!hasValue) {
                hasValue = true;
            }
            parent.innerNext(index, t);
        }
        
        @Override
        public void onError(Throwable t) {
            parent.innerError(index, t);
        }
        
        @Override
        public void onComplete() {
            parent.innerComplete(index, hasValue);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
        
        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
