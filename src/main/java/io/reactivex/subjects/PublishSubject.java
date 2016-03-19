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

package io.reactivex.subjects;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

public final class PublishSubject<T> extends Subject<T, T> {
    public static <T> PublishSubject<T> create() {
        State<T> state = new State<T>();
        return new PublishSubject<T>(state);
    }
    
    final State<T> state;
    protected PublishSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        if (state.done) {
            d.dispose();
        }
    }
    
    @Override
    public void onNext(T value) {
        state.onNext(value);
    }
    
    @Override
    public void onError(Throwable e) {
        state.onError(e);
    }
    
    @Override
    public void onComplete() {
        state.onComplete();
    }
    
    @Override
    public boolean hasSubscribers() {
        return state.subscribers.get().length != 0;
    }
    
    @Override
    public Throwable getThrowable() {
        Object o = state.get();
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }
    
    @Override
    public T getValue() {
        return null;
    }
    
    @Override
    public T[] getValues(T[] array) {
        if (array.length != 0) {
            array[0] = null;
        }
        return array;
    }
    
    @Override
    public boolean hasComplete() {
        Object o = state.get();
        return o != null && !NotificationLite.isError(o);
    }
    
    @Override
    public boolean hasThrowable() {
        return NotificationLite.isError(state.get());
    }
    
    @Override
    public boolean hasValue() {
        return false;
    }
    
    static final class State<T> extends AtomicReference<Object> implements NbpOnSubscribe<T>, Observer<T> {
        /** */
        private static final long serialVersionUID = 4876574210612691772L;

        final AtomicReference<Observer<? super T>[]> subscribers = new AtomicReference<Observer<? super T>[]>();
        
        @SuppressWarnings("rawtypes")
        static final Observer[] EMPTY = new Observer[0];
        @SuppressWarnings("rawtypes")
        static final Observer[] TERMINATED = new Observer[0];

        volatile boolean done;
        
        @SuppressWarnings("unchecked")
        public State() {
            subscribers.lazySet(EMPTY);
        }
        
        boolean add(Observer<? super T> s) {
            for (;;) {
                Observer<? super T>[] a = subscribers.get();
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                
                @SuppressWarnings("unchecked")
                Observer<? super T>[] b = new Observer[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = s;
                
                if (subscribers.compareAndSet(a, b)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void remove(Observer<? super T> s) {
            for (;;) {
                Observer<? super T>[] a = subscribers.get();
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    Observer<? super T> e = a[i];
                    if (e.equals(s)) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                Observer<? super T>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new Observer[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        Observer<? super T>[] terminate(Object notification) {
            if (compareAndSet(null, notification)) {
                Observer<? super T>[] a = subscribers.get();
                if (a != TERMINATED) {
                    return subscribers.getAndSet(TERMINATED);
                }
            }
            return TERMINATED;
        }
        
        void emit(Observer<? super T> t, Object v) {
            if (NotificationLite.isComplete(v)) {
                t.onComplete();
            } else {
                t.onError(NotificationLite.getError(v));
            }
        }
        
        @Override
        public void accept(final Observer<? super T> t) {
            Object v = get();
            if (v != null) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                emit(t, v);
                return;
            }
            BooleanDisposable bd = new BooleanDisposable(new Runnable() {
                @Override
                public void run() {
                    remove(t);
                }
            });
            t.onSubscribe(bd);
            if (add(t)) {
                if (bd.isDisposed()) {
                    remove(t);
                }
                return;
            }
            v = get();
            emit(t, v);
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            if (done) {
                d.dispose();
            }
        }
        
        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (value == null) {
                onError(new NullPointerException("The value is null"));
                return;
            }
            for (Observer<? super T> v : subscribers.get()) {
                v.onNext(value);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            if (e == null) {
                e = new NullPointerException();
            }

            for (Observer<? super T> v : terminate(NotificationLite.error(e))) {
                v.onError(e);
            }
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            for (Observer<? super T> v : terminate(NotificationLite.complete())) {
                v.onComplete();
            }
        }
    }
}