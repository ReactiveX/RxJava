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
import rx.Observable.OnSubscribe;

import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.operators.NotificationLite;
import rx.subscriptions.Subscriptions;

/**
 * Represents the typical state and OnSubscribe logic for a Subject implementation.
 * @param <T> the source and return value type
 */
@SuppressWarnings({"unchecked", "rawtypes"})
/* package */final class SubjectSubscriptionManager<T> implements OnSubscribe<T> {
    /** Contains the unsubscription flag and the array of active subscribers. */
    private final AtomicReference<State<T>> state = new AtomicReference<State<T>>(State.EMPTY);
    /** Stores the latest value or the terminal value for some Subjects. */
    final AtomicReference<Object> latest = new AtomicReference<Object>();
    /** Indicates that the subject is active (cheaper than checking the state).*/
    boolean active = true;
    /** Action called when a new subscriber subscribes but before it is added to the state. */
    Action1<SubjectObserver<T>> onStart = Actions.empty();
    /** Action called after the subscriber has been added to the state. */
    Action1<SubjectObserver<T>> onAdded = Actions.empty();
    /** Action called when the subscriber wants to subscribe to a terminal state. */
    Action1<SubjectObserver<T>> onTerminated = Actions.empty();
    /** Called when the subscruber explicitly unsubscribes. */
    Action1<SubjectObserver<T>> onUnsubscribed = Actions.empty();
    @Override
    public void call(final Subscriber<? super T> child) {
        SubjectObserver<T> bo = new SubjectObserver<T>(child);
        addUnsubscriber(child, bo);
        onStart.call(bo);
        if (add(bo) && child.isUnsubscribed()) {
            remove(bo);
            onUnsubscribed.call(bo);
        }
    }
    /** Registers the unsubscribe action for the given subscriber. */
    void addUnsubscriber(Subscriber<? super T> child, final SubjectObserver<T> bo) {
        child.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                remove(bo);
                onUnsubscribed.call(bo);
            }
        }));
    }    
    /** Set the latest NotificationLite value. */
    void set(Object value) {
        this.latest.set(value);
    }
    /** @return Retrieve the latest NotificationLite value */
    Object get() {
        return latest.get();
    }
    /** @return the array of active subscribers, don't write into the array! */
    SubjectObserver<T>[] observers() {
        return state.get().observers;
    }
    /**
     * Try to atomically add a SubjectObserver to the active state.
     * @param o the SubjectObserver to add
     * @return false if the subject is already in its terminal state
     */
    boolean add(SubjectObserver<T> o) {
        do {
            State oldState = state.get();
            if (oldState.terminated) {
                onTerminated.call(o);
                return false;
            }
            State newState = oldState.add(o);
            if (state.compareAndSet(oldState, newState)) {
                onAdded.call(o);
                return true;
            }
        } while (true);
    }
    /**
     * Atomically remove the specified SubjectObserver from the active observers.
     * @param o the SubjectObserver to remove
     */
    void remove(SubjectObserver<T> o) {
        do {
            State oldState = state.get();
            if (oldState.terminated) {
                return;
            }
            State newState = oldState.remove(o);
            if (newState == oldState || state.compareAndSet(oldState, newState)) {
                return;
            }
        } while (true);
    }
    /**
     * Set a new latest NotificationLite value and return the active observers.
     * @param n the new latest value
     * @return the array of SubjectObservers, don't write into the array!
     */
    SubjectObserver<T>[] next(Object n) {
        set(n);
        return state.get().observers;
    }
    /**
     * Atomically set the terminal NotificationLite value (which could be any of the 3),
     * clear the active observers and return the last active observers.
     * @param n the terminal value
     * @return the last active SubjectObservers
     */
    SubjectObserver<T>[] terminate(Object n) {
        set(n);
        active = false;
        do {
            State<T> oldState = state.get();
            if (oldState.terminated) {
                return State.NO_OBSERVERS;
            }
            if (state.compareAndSet(oldState, State.TERMINATED)) {
                return oldState.observers;
            }
        } while (true);
    }

    /** State-machine representing the termination state and active SubjectObservers. */
    protected static final class State<T> {
        final boolean terminated;
        final SubjectObserver[] observers;
        static final SubjectObserver[] NO_OBSERVERS = new SubjectObserver[0];
        static final State TERMINATED = new State(true, NO_OBSERVERS);
        static final State EMPTY = new State(false, NO_OBSERVERS);
        
        public State(boolean terminated, SubjectObserver[] observers) {
            this.terminated = terminated;
            this.observers = observers;
        }
        public State add(SubjectObserver o) {
            SubjectObserver[] a = observers;
            int n = a.length;
            SubjectObserver[] b = new SubjectObserver[n + 1];
            System.arraycopy(observers, 0, b, 0, n);
            b[n] = o;
            return new State<T>(terminated, b);
        }
        public State remove(SubjectObserver o) {
            SubjectObserver[] a = observers;
            int n = a.length;
            if (n == 1 && a[0] == o) {
                return EMPTY;
            } else
                if (n == 0) {
                    return this;
                }
            SubjectObserver[] b = new SubjectObserver[n - 1];
            int j = 0;
            for (int i = 0; i < n; i++) {
                SubjectObserver ai = a[i];
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
                SubjectObserver[] c = new SubjectObserver[j];
                System.arraycopy(b, 0, c, 0, j);
                b = c;
            }
            return new State<T>(terminated, b);
        }
    }
    
    /**
     * Observer wrapping the actual Subscriber and providing various
     * emission facilities.
     * @param <T> the consumed value type of the actual Observer
     */
    protected static final class SubjectObserver<T> implements Observer<T> {
        /** The actual Observer. */
        final Observer<? super T> actual;
        /** The NotificationLite to avoid allocating objects for each OnNext value. */
        final NotificationLite<T> nl = NotificationLite.instance();
        /** Was the emitFirst run? Guarded by this. */
        boolean first = true;
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        List<Object> queue;
        /* volatile */boolean fastPath;
        protected volatile boolean caughtUp;
        public SubjectObserver(Observer<? super T> actual) {
            this.actual = actual;
        }
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
        @Override
        public void onCompleted() {
            actual.onCompleted();
        }
        /**
         * Emits the given NotificationLite value and
         * prevents the emitFirst to run if not already run.
         * @param n the NotificationLite value
         */
        protected void emitNext(Object n) {
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
        /**
         * Tries to emit a NotificationLite value as the first
         * value and drains the queue as long as possible.
         * @param n the NotificationLite value
         */
        protected void emitFirst(Object n) {
            synchronized (this) {
                if (!first || emitting) {
                    return;
                }
                first = false;
                emitting = n != null;
            }
            if (n != null) {
                emitLoop(null, n);
            }
        }
        /**
         * Emits the contents of the queue as long as there are values.
         * @param localQueue the initial queue contents
         * @param current the current content to emit
         */
        protected void emitLoop(List<Object> localQueue, Object current) {
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
        /**
         * Dispatches a NotificationLite value to the actual Observer.
         * @param n the value to dispatch
         */
        protected void accept(Object n) {
            if (n != null) {
                nl.accept(actual, n);
            }
        }
        
        /** @return the actual Observer. */
        protected Observer<? super T> getActual() {
            return actual;
        }
    }
}