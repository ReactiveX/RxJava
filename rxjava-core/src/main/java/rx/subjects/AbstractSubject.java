package rx.subjects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import rx.Notification;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action2;

public abstract class AbstractSubject<T> extends Subject<T, T> {

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
