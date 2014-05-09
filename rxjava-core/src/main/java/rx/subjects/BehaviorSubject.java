/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subjects;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.operators.NotificationLite;
import rx.subscriptions.Subscriptions;

/**
 * Subject that publishes the most recent and all subsequent events to each subscribed {@link Observer}.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.BehaviorSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * / observer will receive all events.
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.subscribe(observer);
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive only onCompleted
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.onCompleted();
  subject.subscribe(observer);
  
  // observer will receive only onError
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.onError(new RuntimeException("error"));
  subject.subscribe(observer);
  } </pre>
 * 
 * @param <T>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class BehaviorSubject<T> extends Subject<T, T> {
    /**
     * Create a {@link BehaviorSubject} without a default value.
     * @param <T> the value type
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create() {
        return create(null, false);
    }
    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each {@link Observer} that subscribes to it.
     * 
     * @param <T> the value type
     * @param defaultValue
     *            the value which will be published to any {@link Observer} as long as the {@link BehaviorSubject} has not yet received any events
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create(T defaultValue) {
        return create(defaultValue, true);
    }
    private static <T> BehaviorSubject<T> create(T defaultValue, boolean hasDefault) {
        State<T> state = new State<T>();
        if (hasDefault) {
            state.set(NotificationLite.instance().next(defaultValue));
        }
        return new BehaviorSubject<T>(new BehaviorOnSubscribe<T>(state), state); 
    }

    static final class State<T> {
        final AtomicReference<Object> latest = new AtomicReference<Object>();
        final AtomicReference<BehaviorState> observers = new AtomicReference<BehaviorState>(BehaviorState.EMPTY);
        boolean active = true;
        void set(Object value) {
            this.latest.set(value);
        }
        Object get() {
            return latest.get();
        }
        BehaviorObserver<T>[] observers() {
            return observers.get().observers;
        }
        boolean add(BehaviorObserver<T> o) {
            do {
                BehaviorState oldState = observers.get();
                if (oldState.terminated) {
                    o.emitFirst(get());
                    return false;
                }
                BehaviorState newState = oldState.add(o);
                if (observers.compareAndSet(oldState, newState)) {
                    o.emitFirst(get());
                    return true;
                }
            } while (true);
        }
        void remove(BehaviorObserver<T> o) {
            do {
                BehaviorState oldState = observers.get();
                if (oldState.terminated) {
                    return;
                }
                BehaviorState newState = oldState.remove(o);
                if (newState == oldState || observers.compareAndSet(oldState, newState)) {
                    return;
                }
            } while (true);
        }
        BehaviorObserver<T>[] next(Object n) {
            set(n);
            return observers.get().observers;
        }
        BehaviorObserver<T>[] terminate(Object n) {
            set(n);
            active = false;
            do {
                BehaviorState oldState = observers.get();
                if (oldState.terminated) {
                    return BehaviorState.NO_OBSERVERS;
                }
                if (observers.compareAndSet(oldState, BehaviorState.TERMINATED)) {
                    return oldState.observers;
                }
            } while (true);
        }
    }
    static final class BehaviorState {
        final boolean terminated;
        final BehaviorObserver[] observers;
        static final BehaviorObserver[] NO_OBSERVERS = new BehaviorObserver[0];
        static final BehaviorState TERMINATED = new BehaviorState(true, NO_OBSERVERS);
        static final BehaviorState EMPTY = new BehaviorState(false, NO_OBSERVERS);

        public BehaviorState(boolean terminated, BehaviorObserver[] observers) {
            this.terminated = terminated;
            this.observers = observers;
        }
        public BehaviorState add(BehaviorObserver o) {
            int n = observers.length;
            BehaviorObserver[] a = new BehaviorObserver[n + 1];
            System.arraycopy(observers, 0, a, 0, n);
            a[n] = o;
            return new BehaviorState(terminated, a);
        }
        public BehaviorState remove(BehaviorObserver o) {
            BehaviorObserver[] a = observers;
            int n = a.length;
            if (n == 1 && a[0] == o) {
                return EMPTY;
            } else
            if (n == 0) {
                return this;
            }
            BehaviorObserver[] b = new BehaviorObserver[n - 1];
            int j = 0;
            for (int i = 0; i < n; i++) {
                BehaviorObserver ai = a[i];
                if (ai != o) {
                    if (j == n - 1) {
                        return this;
                    }
                    b[j++] = ai;
                }
            }
            if (j == 0) {
                return EMPTY;
            }
            if (j < n - 1) {
                BehaviorObserver[] c = new BehaviorObserver[j];
                System.arraycopy(b, 0, c, 0, j);
                b = c;
            }
            return new BehaviorState(terminated, b);
        }
    }
    
    static final class BehaviorOnSubscribe<T> implements OnSubscribe<T> {
        private final State<T> state;

        public BehaviorOnSubscribe(State<T> state) {
            this.state = state;
        }

        @Override
        public void call(final Subscriber<? super T> child) {
            BehaviorObserver<T> bo = new BehaviorObserver<T>(child);
            addUnsubscriber(child, bo);
            if (state.add(bo) && child.isUnsubscribed()) {
                state.remove(bo);
            }
        }
    
        void addUnsubscriber(Subscriber<? super T> child, final BehaviorObserver<T> bo) {
            child.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    state.remove(bo);
                }
            }));
        }
    }
    
    
    private final State<T> state;
    private final NotificationLite<T> nl = NotificationLite.instance();

    protected BehaviorSubject(OnSubscribe<T> onSubscribe, State<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.completed();
            for (BehaviorObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }
    }

    @Override
    public void onError(Throwable e) {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.error(e);
            for (BehaviorObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }
    }

    @Override
    public void onNext(T v) {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.next(v);
            for (BehaviorObserver<T> bo : state.next(n)) {
                bo.emitNext(n);
            }
        }
    }
    
    /* test support */ int subscriberCount() {
        return state.observers.get().observers.length;
    }
    
    static final class BehaviorObserver<T> {
        final Observer<? super T> actual;
        final NotificationLite<T> nl = NotificationLite.instance();
        /** Guarded by this. */
        boolean first = true;
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        List<Object> queue;
        /* volatile */boolean fastPath;
        public BehaviorObserver(Observer<? super T> actual) {
            this.actual = actual;
        }
        void emitNext(Object n) {
            if (!fastPath) {
                synchronized (this) {
                    first = false;
                    if (emitting) {
                        if (queue == null) {
                            queue = new ArrayList<Object>();
                        }
                        queue.add(n);
                        return;
                    }
                }
                fastPath = true;
            }
            nl.accept(actual, n);
        }
        void emitFirst(Object n) {
            synchronized (this) {
                if (!first || emitting) {
                    return;
                }
                first = false;
                emitting = true;
            }
            emitLoop(null, n);
        }
        void emitLoop(List<Object> localQueue, Object current) {
            boolean once = true;
            boolean skipFinal = false;
            try {
                do {
                    if (localQueue != null) {
                        for (Object n : localQueue) {
                            accept(n);
                        }
                    }
                    if (once) {
                        once = false;
                        accept(current);
                    }
                    synchronized (this) {
                        localQueue = queue;
                        queue = null;
                        if (localQueue == null) {
                            emitting = false;
                            skipFinal = true;
                            break;
                        }
                    }
                } while (true);
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
        }
        void accept(Object n) {
            if (n != null) {
                nl.accept(actual, n);
            }
        }
    }
}
