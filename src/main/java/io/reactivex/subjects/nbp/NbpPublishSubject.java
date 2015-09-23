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

import java.util.concurrent.atomic.*;

import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpPublishSubject<T> extends NbpSubject<T, T> {
    public static <T> NbpPublishSubject<T> create() {
        State<T> state = new State<>();
        return new NbpPublishSubject<>(state);
    }
    
    final State<T> state;
    protected NbpPublishSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Disposable d) {
        // NbpSubjects never cancel the disposable
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
            if (compareAndSet(null, notification)) {
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
            } else {
                t.onError(NotificationLite.getError(v));
            }
        }
        
        @Override
        public void accept(NbpSubscriber<? super T> t) {
            Object v = get();
            if (v != null) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                emit(t, v);
                return;
            }
            BooleanDisposable bd = new BooleanDisposable(() -> remove(t));
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
            for (NbpSubscriber<? super T> v : subscribers) {
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
            for (NbpSubscriber<? super T> v : terminate(NotificationLite.complete())) {
                v.onComplete();
            }
        }
    }
}
