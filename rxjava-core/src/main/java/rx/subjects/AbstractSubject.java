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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import rx.Notification;
import rx.Observer;
import rx.util.functions.Action2;

public abstract class AbstractSubject<T> extends Subject<T, T> {

    protected AbstractSubject(OnGetSubscriptionFunc<T> onGetSubscription) {
        super(onGetSubscription);
    }

    protected static class SubjectState<T> {
        protected final ConcurrentHashMap<Object, Observer<? super T>> observers = new ConcurrentHashMap<Object, Observer<? super T>>();
        protected final AtomicReference<Notification<T>> currentValue = new AtomicReference<Notification<T>>();
        protected final AtomicBoolean completed = new AtomicBoolean();
        protected final ReentrantLock SUBSCRIPTION_LOCK = new ReentrantLock();
    }

    protected static <T> OnGetSubscriptionFunc<T> getOnGetSubscriptionFunc(final SubjectState<T> state, final Action2<SubjectState<T>, Observer<? super T>> onEach) {
        return new OnGetSubscriptionFunc<T>() {
            @Override
            public PartialSubscription<T> onGetSubscription() {
                final Object marker = new Object();
                return PartialSubscription.create(new OnPartialSubscribeFunc<T>() {
                    @Override
                    public void onSubscribe(Observer<? super T> observer) {
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
                            } else {
                                // on subscribe add it to the map of outbound observers to notify
                                state.observers.put(marker, observer);

                                // invoke onSubscribe logic
                                if (onEach != null) {
                                    onEach.call(state, observer);
                                }
                            }
                        } finally {
                            state.SUBSCRIPTION_LOCK.unlock();
                        }
                    }
                }, new OnPartialUnsubscribeFunc() {
                    @Override
                    public void onUnsubscribe() {
                        state.observers.remove(marker);
                    }
                });
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
        for (Object s : snapshotOfObservers(state)) {
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
                for (Object s : snapshotOfObservers(state)) {
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
    private static <T> Collection<Object> snapshotOfObservers(final SubjectState<T> state) {
        return new ArrayList<Object>(state.observers.keySet());
    }
}
