/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rx.Notification;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action2;

public abstract class AbstractSubject<T> extends Subject<T, T> {
    /** Base state with lock. */
    static class BaseState {
        /** The lock to protect the other fields. */
        private final Lock lock = new ReentrantLock();
        /** Lock. */
        public void lock() {
            lock.lock();
        }
        /** Unlock. */
        public void unlock() {
            lock.unlock();
        }
        
    } 
    /** The default state of Subjects.*/
    static class DefaultState<T> extends BaseState {
        /** The currently subscribed observers. */
        private final Map<Subscription, Observer<? super T>> observers = new LinkedHashMap<Subscription, Observer<? super T>>();
        /** Indicator that the subject has completed. */
        public boolean done;
        /** If not null, the subject completed with an error. */
        public Throwable error;
        /** 
         * Add an observer to the observers and create a Subscription for it.
         * Caller should hold the lock.
         * @param obs
         * @return 
         */
        public Subscription addObserver(Observer<? super T> obs) {
            Subscription s = new Subscription() {
                final AtomicBoolean once = new AtomicBoolean();
                @Override
                public void unsubscribe() {
                    if (once.compareAndSet(false, true)) {
                        remove(this);
                    }
                }
                
            };
            observers.put(s, obs);
            return s;
        }
        /**
         * Returns a live collection of the observers.
         * <p>
         * Caller should hold the lock.
         * @return 
         */
        public Collection<Observer<? super T>> observers() {
            return new ArrayList<Observer<? super T>>(observers.values());
        }
        /**
         * Removes and returns all observers from the mapping.
         * <p>
         * Caller should hold the lock.
         * @return 
         */
        public Collection<Observer<? super T>> removeAll() {
            List<Observer<? super T>> list = new ArrayList<Observer<? super T>>(observers.values());
            observers.clear();
            return list;
        }
        /** 
         * Remove the subscription. 
         * @param s
         */
        protected void remove(Subscription s) {
            lock();
            try {
                observers.remove(s);
            } finally {
                unlock();
            }
        }
        /**
         * Set the error state and dispatch it to the observers.
         * @param e 
         */
        public void defaultOnError(Throwable e) {
            lock();
            try {
                if (done) {
                    return;
                }
                defaultDispatchError(e);
            } finally {
                unlock();
            }
        }
        /**
         * Set the completion state and dispatch it to the observers.
         */
        public void defaultOnCompleted() {
            lock();
            try {
                if (done) {
                    return;
                }
                done = true;
                for (Observer<? super T> o : removeAll()) {
                    o.onCompleted();
                }
            } finally {
                unlock();
            }
        }
        /**
         * Dispatch the value to all subscribed observers.
         * @param value 
         */
        public void defaultOnNext(T value) {
            lock();
            try {
                if (done) {
                    return;
                }
                defaultDispatch(value);
            } finally {
                unlock();
            }
        }
        /**
         * Dispatch the value to all subscribed observers.
         * <p>
         * Caller should hold the lock.
         * @param value 
         */
        public void defaultDispatch(T value) {
            for (Observer<? super T> o : observers()) {
                o.onNext(value);
            }
        }
        /**
         * Dispatch the exception to all subscribed observers and
         * remove them.
         * @param e 
         */
        public void defaultDispatchError(Throwable e) {
            done = true;
            error = e;
            for (Observer<? super T> o : removeAll()) {
                o.onError(e);
            }
        }
    }
    protected AbstractSubject(rx.Observable.OnSubscribeFunc<T> onSubscribe) {
        super(onSubscribe);
    }

    protected static class SubjectState<T> {
        protected final ConcurrentHashMap<Subscription, Observer<? super T>> observers = new ConcurrentHashMap<Subscription, Observer<? super T>>();
        protected final AtomicReference<Notification<T>> currentValue = new AtomicReference<Notification<T>>();
        protected final AtomicBoolean completed = new AtomicBoolean();
        protected final ReentrantLock SUBSCRIPTION_LOCK = new ReentrantLock();
    }

    protected static <T> OnSubscribeFunc<T> getOnSubscribeFunc(final SubjectState<T> state, final Action2<SubjectState<T>, Observer<? super T>> onEach) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                /*
                 * Subscription needs to be synchronized with terminal states to ensure
                 * race conditions are handled. When subscribing we must make sure
                 * onComplete/onError is correctly emitted to all observers, even if it
                 * comes in while the onComplete/onError is being propagated.
                 */
                state.SUBSCRIPTION_LOCK.lock();
                try {
                    if (state.completed.get()) {
                        emitNotification(state.currentValue.get(), observer);
                        if (onEach != null) {
                            onEach.call(state, observer);
                        }
                        return Subscriptions.empty();
                    } else {
                        // the subject is not completed so we subscribe
                        final SafeObservableSubscription subscription = new SafeObservableSubscription();

                        subscription.wrap(new Subscription() {
                            @Override
                            public void unsubscribe() {
                                // on unsubscribe remove it from the map of outbound observers to notify
                                state.observers.remove(subscription);
                            }
                        });

                        // on subscribe add it to the map of outbound observers to notify
                        state.observers.put(subscription, observer);

                        // invoke onSubscribe logic
                        if (onEach != null) {
                            onEach.call(state, observer);
                        }

                        return subscription;
                    }
                } finally {
                    state.SUBSCRIPTION_LOCK.unlock();
                }

            }

        };
    }

    protected static <T> void emitNotification(Notification<T> value, Observer<? super T> observer) {
        // if null that means onNext was never invoked (no Notification set)
        if (value != null) {
            if (value.isOnNext()) {
                observer.onNext(value.getValue());
            } else if (value.isOnError()) {
                observer.onError(value.getThrowable());
            } else if (value.isOnCompleted()) {
                observer.onCompleted();
            }
        }
    }

    /**
     * Emit the current value.
     * 
     * @param state
     */
    protected static <T> void emitNotification(final SubjectState<T> state, final Action2<SubjectState<T>, Observer<? super T>> onEach) {
        for (Subscription s : snapshotOfObservers(state)) {
            Observer<? super T> o = state.observers.get(s);
            // emit notifications to this observer
            emitNotification(state.currentValue.get(), o);
            // onEach action if applicable
            if (onEach != null) {
                onEach.call(state, o);
            }
        }
    }

    /**
     * Emit the current value to all observers and remove their subscription.
     * 
     * @param state
     */
    protected void emitNotificationAndTerminate(final SubjectState<T> state, final Action2<SubjectState<T>, Observer<? super T>> onEach) {
        /*
         * We can not allow new subscribers to be added while we execute the terminal state.
         */
        state.SUBSCRIPTION_LOCK.lock();
        try {
            if (state.completed.compareAndSet(false, true)) {
                for (Subscription s : snapshotOfObservers(state)) {
                    Observer<? super T> o = state.observers.get(s);
                    // emit notifications to this observer
                    emitNotification(state.currentValue.get(), o);
                    // onEach action if applicable
                    if (onEach != null) {
                        onEach.call(state, o);
                    }

                    // remove the subscription as it is completed
                    state.observers.remove(s);
                }
            }
        } finally {
            state.SUBSCRIPTION_LOCK.unlock();
        }
    }

    /**
     * Current snapshot of 'state.observers.keySet()' so that concurrent modifications aren't included.
     * 
     * This makes it behave deterministically in a single-threaded execution when nesting subscribes.
     * 
     * In multi-threaded execution it will cause new subscriptions to wait until the following onNext instead
     * of possibly being included in the current onNext iteration.
     * 
     * @return List<Observer<T>>
     */
    private static <T> Collection<Subscription> snapshotOfObservers(final SubjectState<T> state) {
        return new ArrayList<Subscription>(state.observers.keySet());
    }
}
