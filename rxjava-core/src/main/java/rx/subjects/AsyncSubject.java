/**
 * Copyright 2013 Netflix, Inc.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import rx.Notification;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Subject that publishes only the last event to each {@link Observer} that has subscribed when the
 * sequence completes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.AsyncSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * // observer will receive no onNext events because the subject.onCompleted() isn't called.
  AsyncSubject<Object> subject = AsyncSubject.create();
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive "three" as the only onNext event.
  AsyncSubject<Object> subject = AsyncSubject.create();
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onCompleted();

  } </pre>
 * 
 * @param <T>
 */
public class AsyncSubject<T> extends Subject<T, T> {

    /**
     * Create a new AsyncSubject
     * 
     * @return a new AsyncSubject
     */
    public static <T> AsyncSubject<T> create() {
        final AsyncSubjectState<T> state = new AsyncSubjectState<T>();

        OnSubscribeFunc<T> onSubscribe = new OnSubscribeFunc<T>() {
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
                        emitNotificationToObserver(state, observer);
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

                        return subscription;
                    }
                } finally {
                    state.SUBSCRIPTION_LOCK.unlock();
                }

            }

        };

        return new AsyncSubject<T>(onSubscribe, state);
    }

    private static <T> void emitNotificationToObserver(final AsyncSubjectState<T> state, Observer<? super T> observer) {
        Notification<T> finalValue = state.currentValue.get();

        // if null that means onNext was never invoked (no Notification set)
        if (finalValue != null) {
            if (finalValue.isOnNext()) {
                observer.onNext(finalValue.getValue());
            } else if (finalValue.isOnError()) {
                observer.onError(finalValue.getThrowable());
            }
        }
        observer.onCompleted();
    }

    /**
     * State externally constructed and passed in so the onSubscribe function has access to it.
     * 
     * @param <T>
     */
    private static class AsyncSubjectState<T> {
        private final ConcurrentHashMap<Subscription, Observer<? super T>> observers = new ConcurrentHashMap<Subscription, Observer<? super T>>();
        private final AtomicReference<Notification<T>> currentValue = new AtomicReference<Notification<T>>();
        private final AtomicBoolean completed = new AtomicBoolean();
        private final ReentrantLock SUBSCRIPTION_LOCK = new ReentrantLock();
    }

    private final AsyncSubjectState<T> state;

    protected AsyncSubject(OnSubscribeFunc<T> onSubscribe, AsyncSubjectState<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        terminalState();
    }

    @Override
    public void onError(Throwable e) {
        state.currentValue.set(new Notification<T>(e));
        terminalState();
    }

    @Override
    public void onNext(T v) {
        state.currentValue.set(new Notification<T>(v));
    }

    private void terminalState() {
        /*
         * We can not allow new subscribers to be added while we execute the terminal state.
         */
        state.SUBSCRIPTION_LOCK.lock();
        try {
            if (state.completed.compareAndSet(false, true)) {
                for (Subscription s : state.observers.keySet()) {
                    // emit notifications to this observer
                    emitNotificationToObserver(state, state.observers.get(s));
                    // remove the subscription as it is completed
                    state.observers.remove(s);
                }
            }
        } finally {
            state.SUBSCRIPTION_LOCK.unlock();
        }
    }
}
