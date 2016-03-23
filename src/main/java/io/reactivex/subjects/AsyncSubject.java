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

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * An NbpSubject that emits the very last value followed by a completion event or the received error to NbpSubscribers.
 *
 * <p>The implementation of onXXX methods are technically thread-safe but non-serialized calls
 * to them may lead to undefined state in the currently subscribed NbpSubscribers.
 * 
 * <p>Due to the nature Observables are constructed, the NbpAsyncSubject can't be instantiated through
 * {@code new} but must be created via the {@link #create()} method.
 *
 * @param <T> the value type
 */

public final class AsyncSubject<T> extends Subject<T, T> {
    public static <T> AsyncSubject<T> create() {
        State<T> state = new State<T>();
        return new AsyncSubject<T>(state);
    }
    
    final State<T> state;
    protected AsyncSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        if (state.subscribers.get() == State.TERMINATED) {
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
        Object o = state.get();
        if (o != null) {
            if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                return null;
            }
            return NotificationLite.getValue(o);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public T[] getValues(T[] array) {
        Object o = state.get();
        if (o != null) {
            if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
                if (array.length != 0) {
                    array[0] = null;
                }
            } else {
                T v = NotificationLite.getValue(o);
                if (array.length != 0) {
                    array[0] = v;
                    if (array.length != 1) {
                        array[1] = null;
                    }
                } else {
                    array = (T[])Array.newInstance(array.getClass().getComponentType(), 1);
                    array[0] = v;
                }
            }
        } else {
            if (array.length != 0) {
                array[0] = null;
            }
        }
        return array;
    }
    
    @Override
    public boolean hasComplete() {
        return state.subscribers.get() == State.TERMINATED && !NotificationLite.isError(state.get());
    }
    
    @Override
    public boolean hasThrowable() {
        return NotificationLite.isError(state.get());
    }
    
    @Override
    public boolean hasValue() {
        Object o = state.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }
    
    static final class State<T> extends AtomicReference<Object> implements NbpOnSubscribe<T>, Observer<T> {
        /** */
        private static final long serialVersionUID = 4876574210612691772L;

        final AtomicReference<Observer<? super T>[]> subscribers;
        
        @SuppressWarnings("rawtypes")
        static final Observer[] EMPTY = new Observer[0];
        @SuppressWarnings("rawtypes")
        static final Observer[] TERMINATED = new Observer[0];

        boolean done;
        
        @SuppressWarnings("unchecked")
        public State() {
            subscribers = new AtomicReference<Observer<? super T>[]>(EMPTY);
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
            if (compareAndSet(get(), notification)) {
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
            } else
            if (NotificationLite.isError(v)) {
                t.onError(NotificationLite.getError(v));
            } else {
                t.onNext(NotificationLite.<T>getValue(v));
                t.onComplete();
            }
        }
        
        @Override
        public void accept(final Observer<? super T> t) {
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
            Object v = get();
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
                onError(new NullPointerException());
                return;
            }
            lazySet(value);
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
            T value = NotificationLite.getValue(get());
            if (value == null) {
                for (Observer<? super T> v : terminate(NotificationLite.complete())) {
                    v.onComplete();
                }
            } else {
                for (Observer<? super T> v : terminate(NotificationLite.next(value))) {
                    v.onNext(value);
                    v.onComplete();
                }
            }
        }
    }
}