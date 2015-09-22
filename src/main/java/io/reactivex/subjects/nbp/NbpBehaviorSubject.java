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
import java.util.Objects;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.StampedLock;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpBehaviorSubject<T> extends NbpSubject<T, T> {

    public static <T> NbpBehaviorSubject<T> create() {
        State<T> state = new State<>();
        return new NbpBehaviorSubject<>(state);
    }
    
    // TODO a plain create() would create a method ambiguity with Observable.create with javac
    public static <T> NbpBehaviorSubject<T> createDefault(T defaultValue) {
        Objects.requireNonNull(defaultValue);
        State<T> state = new State<>();
        state.lazySet(defaultValue);
        return new NbpBehaviorSubject<>(state);
    }
    
    final State<T> state;
    protected NbpBehaviorSubject(State<T> state) {
        super(state);
        this.state = state;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        state.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
        if (t == null) {
            onError(new NullPointerException());
            return;
        }
        state.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        if (t == null) {
            t = new NullPointerException();
        }
        state.onError(t);
    }

    @Override
    public void onComplete() {
        state.onComplete();
    }

    @Override
    public boolean hasSubscribers() {
        return state.subscribers.length != 0;
    }
    
    
    /* test support*/ int subscriberCount() {
        return state.subscribers.length;
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
        if (NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            return null;
        }
        return NotificationLite.getValue(o);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public T[] getValues(T[] array) {
        Object o = state.get();
        if (o == null || NotificationLite.isComplete(o) || NotificationLite.isError(o)) {
            if (array.length != 0) {
                array[0] = null;
            }
            return array;
        }
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
        return array;
    }
    
    @Override
    public boolean hasComplete() {
        Object o = state.get();
        return NotificationLite.isComplete(o);
    }
    
    @Override
    public boolean hasThrowable() {
        Object o = state.get();
        return NotificationLite.isError(o);
    }
    
    @Override
    public boolean hasValue() {
        Object o = state.get();
        return o != null && !NotificationLite.isComplete(o) && !NotificationLite.isError(o);
    }
    
    static final class State<T> extends AtomicReference<Object> implements NbpOnSubscribe<T>, NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = -4311717003288339429L;

        boolean done;
        
        volatile BehaviorDisposable<T>[] subscribers;
        
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<State, BehaviorDisposable[]> SUBSCRIBERS =
                AtomicReferenceFieldUpdater.newUpdater(State.class, BehaviorDisposable[].class, "subscribers");
        
        @SuppressWarnings("rawtypes")
        static final BehaviorDisposable[] EMPTY = new BehaviorDisposable[0];

        @SuppressWarnings("rawtypes")
        static final BehaviorDisposable[] TERMINATED = new BehaviorDisposable[0];

        long index;
        
        final StampedLock lock;
        
        public State() {
            this.lock = new StampedLock();
            SUBSCRIBERS.lazySet(this, EMPTY);
        }
        
        public boolean add(BehaviorDisposable<T> rs) {
            for (;;) {
                BehaviorDisposable<T>[] a = subscribers;
                if (a == TERMINATED) {
                    return false;
                }
                int len = a.length;
                @SuppressWarnings("unchecked")
                BehaviorDisposable<T>[] b = new BehaviorDisposable[len + 1];
                System.arraycopy(a, 0, b, 0, len);
                b[len] = rs;
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public void remove(BehaviorDisposable<T> rs) {
            for (;;) {
                BehaviorDisposable<T>[] a = subscribers;
                if (a == TERMINATED || a == EMPTY) {
                    return;
                }
                int len = a.length;
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (a[i] == rs) {
                        j = i;
                        break;
                    }
                }
                
                if (j < 0) {
                    return;
                }
                BehaviorDisposable<T>[] b;
                if (len == 1) {
                    b = EMPTY;
                } else {
                    b = new BehaviorDisposable[len - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, len - j - 1);
                }
                if (SUBSCRIBERS.compareAndSet(this, a, b)) {
                    return;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        public BehaviorDisposable<T>[] terminate(Object terminalValue) {
            
            BehaviorDisposable<T>[] a = subscribers;
            if (a != TERMINATED) {
                a = SUBSCRIBERS.getAndSet(this, TERMINATED);
                if (a != TERMINATED) {
                    // either this or atomics with lots of allocation
                    setCurrent(terminalValue);
                }
            }
            
            return a;
        }
        
        @Override
        public void accept(NbpSubscriber<? super T> s) {
            BehaviorDisposable<T> bs = new BehaviorDisposable<>(s, this);
            s.onSubscribe(bs);
            if (!bs.cancelled) {
                if (add(bs)) {
                    if (bs.cancelled) {
                        remove(bs);
                    } else {
                        bs.emitFirst();
                    }
                } else {
                    Object o = get();
                    if (NotificationLite.isComplete(o)) {
                        s.onComplete();
                    } else {
                        s.onError(NotificationLite.getError(o));
                    }
                }
            }
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (done) {
                s.dispose();
                return;
            }
        }
        
        void setCurrent(Object o) {
            long stamp = lock.writeLock();
            try {
                index++;
                lazySet(o);
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            Object o = NotificationLite.next(t);
            setCurrent(o);
            for (BehaviorDisposable<T> bs : subscribers) {
                bs.emitNext(o, index);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            Object o = NotificationLite.error(t);
            for (BehaviorDisposable<T> bs : terminate(o)) {
                bs.emitNext(o, index);
            }
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            Object o = NotificationLite.complete();
            for (BehaviorDisposable<T> bs : terminate(o)) {
                bs.emitNext(o, index);  // relaxed read okay since this is the only mutator thread
            }
        }
    }
    
    static final class BehaviorDisposable<T> implements Disposable {
        
        final NbpSubscriber<? super T> actual;
        final State<T> state;
        
        boolean next;
        boolean emitting;
        AppendOnlyLinkedArrayList<Object> queue;
        
        boolean fastPath;
        
        volatile boolean cancelled;
        
        long index;

        public BehaviorDisposable(NbpSubscriber<? super T> actual, State<T> state) {
            this.actual = actual;
            this.state = state;
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                
                state.remove(this);
            }
        }

        void emitFirst() {
            if (cancelled) {
                return;
            }
            Object o;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                if (next) {
                    return;
                }
                
                State<T> s = state;
                StampedLock lock = s.lock;
                
                long stamp = lock.tryOptimisticRead();
                index = s.index;
                o = s.get();
                if (!lock.validate(stamp)) {
                    stamp = lock.readLock();
                    try {
                        index = s.index;
                        o = s.get();
                    } finally {
                        lock.unlockRead(stamp);
                    }
                }
                
                emitting = o != null;
                next = true;
            }
            
            if (o != null) {
                if (emit(o)) {
                    return;
                }
            
                emitLoop();
            }
        }
        
        void emitNext(Object value, long stateIndex) {
            if (cancelled) {
                return;
            }
            if (!fastPath) {
                synchronized (this) {
                    if (cancelled) {
                        return;
                    }
                    if (index == stateIndex) {
                        return;
                    }
                    if (emitting) {
                        AppendOnlyLinkedArrayList<Object> q = queue;
                        if (q == null) {
                            q = new AppendOnlyLinkedArrayList<>(4);
                            queue = q;
                        }
                        q.add(value);
                        return;
                    }
                    next = true;
                }
                fastPath = true;
            }

            emit(value);
        }

        boolean emit(Object o) {
            if (cancelled) {
                return true;
            }
            return NotificationLite.accept(o, actual);
        }
        
        void emitLoop() {
            for (;;) {
                if (cancelled) {
                    return;
                }
                AppendOnlyLinkedArrayList<Object> q;
                synchronized (this) {
                    q = queue;
                    if (q == null) {
                        emitting = false;
                        return;
                    }
                    queue = null;
                }
                
                q.forEachWhile(this::emit);
            }
        }
    }
}
