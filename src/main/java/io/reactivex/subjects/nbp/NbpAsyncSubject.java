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

package io.reactivex.subjects.nbp;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.*;

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

public final class NbpAsyncSubject<T> extends NbpSubject<T, T> {
    public static <T> NbpAsyncSubject<T> create() {
        State<T> state = new State<>();
        return new NbpAsyncSubject<>(state);
    }
    
    final State<T> state;
    protected NbpAsyncSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        if (state.subscribers == State.TERMINATED) {
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
        return state.subscribers.length != 0;
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
        return state.subscribers == State.TERMINATED && !NotificationLite.isError(state.get());
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
    
    static final class State<T> extends AtomicReference<Object> implements NbpOnSubscribe<T>, NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = 4876574210612691772L;

        volatile NbpSubscriber<? super T>[] subscribers;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<State, NbpSubscriber[]> SUBSCRIBERS =
                AtomicReferenceFieldUpdater.newUpdater(State.class, NbpSubscriber[].class, "subscribers");
        
        @SuppressWarnings("rawtypes")
        static final NbpSubscriber[] EMPTY = new NbpSubscriber[0];
        @SuppressWarnings("rawtypes")
        static final NbpSubscriber[] TERMINATED = new NbpSubscriber[0];

        boolean done;
        
        public State() {
            SUBSCRIBERS.lazySet(this, EMPTY);
        }
        
        boolean add(NbpSubscriber<? super T> s) {
            for (;;) {
                NbpSubscriber<? super T>[] a = subscribers;
                if (a == TERMINATED) {
                    return false;
                }
                int n = a.length;
                
                @SuppressWarnings("unchecked")
                NbpSubscriber<? super T>[] b = new NbpSubscriber[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = s;
                
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void remove(NbpSubscriber<? super T> s) {
            for (;;) {
                NbpSubscriber<? super T>[] a = subscribers;
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    NbpSubscriber<? super T> e = a[i];
                    if (e.equals(s)) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                NbpSubscriber<? super T>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new NbpSubscriber[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        NbpSubscriber<? super T>[] terminate(Object notification) {
            if (compareAndSet(get(), notification)) {
                NbpSubscriber<? super T>[] a = subscribers;
                if (a != TERMINATED) {
                    return SUBSCRIBERS.getAndSet(this, TERMINATED);
                }
            }
            return TERMINATED;
        }
        
        void emit(NbpSubscriber<? super T> t, Object v) {
            if (NotificationLite.isComplete(v)) {
                t.onComplete();
            } else
            if (NotificationLite.isError(v)) {
                t.onError(NotificationLite.getError(v));
            } else {
                t.onNext(NotificationLite.getValue(v));
                t.onComplete();
            }
        }
        
        @Override
        public void accept(NbpSubscriber<? super T> t) {
            BooleanDisposable bd = new BooleanDisposable(() -> remove(t));
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
            lazySet(value);
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            for (NbpSubscriber<? super T> v : terminate(NotificationLite.error(e))) {
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
                for (NbpSubscriber<? super T> v : terminate(NotificationLite.complete())) {
                    v.onComplete();
                }
            } else {
                for (NbpSubscriber<? super T> v : terminate(NotificationLite.next(value))) {
                    v.onNext(value);
                    v.onComplete();
                }
            }
        }
    }
}
