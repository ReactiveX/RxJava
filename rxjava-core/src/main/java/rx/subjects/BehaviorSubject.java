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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

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
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        // set a default value so subscriptions will immediately receive this until a new notification is received
        final State<T> state = new State<T>();
        if (hasDefault) {
            state.lastNotification.set(Notification.createOnNext(defaultValue));
        }

        final OnSubscribe<T> onSubscribeBase = subscriptionManager.getOnSubscribeFunc(
                /**
                 * This function executes at beginning of subscription.
                 * 
                 * This will always run, even if Subject is in terminal state.
                 */
                new Action1<SubjectObserver<? super T>>() {
                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        /*
                         * When we subscribe we always emit the latest value to the observer.
                         * 
                         * Here we only emit if it's an onNext as terminal states are handled in the next function.
                         */
                        state.addPending(o);
                    }
                },
                /**
                 * This function executes if the Subject is terminated before subscription occurs.
                 */
                new Action1<SubjectObserver<? super T>>() {
                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        /*
                         * If we are already terminated, or termination happens while trying to subscribe
                         * this will be invoked and we emit whatever the last terminal value was.
                         */
                        state.removePending(o);
                    }
                }, new Action1<SubjectObserver<? super T>>() {
                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        state.removePending(o);
                    }
                    
                });
        OnSubscribe<T> onSubscribe = new OnSubscribe<T>() {

            @Override
            public void call(Subscriber<? super T> t1) {
                onSubscribeBase.call(t1);
                state.removePendingSubscriber(t1);
            }
        };
        return new BehaviorSubject<T>(onSubscribe, subscriptionManager, state);
    }

    static final class State<T> {
        final AtomicReference<Notification<T>> lastNotification;
        /** Guarded by this. */
        List<Object> pendingSubscriptions;
        public State() {
            this.lastNotification = new AtomicReference<Notification<T>>();
        }
        public void addPending(SubjectObserver<? super T> subscriber) {
            synchronized (this) {
                if (pendingSubscriptions == null) {
                    pendingSubscriptions = new ArrayList<Object>(4);
                }
                pendingSubscriptions.add(subscriber);
                List<Notification<T>> list = new ArrayList<Notification<T>>(4);
                list.add(lastNotification.get());
                pendingSubscriptions.add(list);
            }
        }
        public void bufferValue(Notification<T> value) {
            synchronized (this) {
                if (pendingSubscriptions == null) {
                    return;
                }
                for (int i = 1; i < pendingSubscriptions.size(); i += 2) {
                    @SuppressWarnings("unchecked")
                    List<Notification<T>> list = (List<Notification<T>>)pendingSubscriptions.get(i);
                    list.add(value);
                }
            }
        }
        public void removePending(SubjectObserver<? super T> subscriber) {
            List<Notification<T>> toCatchUp = null;
            synchronized (this) {
                if (pendingSubscriptions == null) {
                    return;
                }
                int idx = pendingSubscriptions.indexOf(subscriber);
                if (idx >= 0) {
                    pendingSubscriptions.remove(idx);
                    @SuppressWarnings("unchecked")
                    List<Notification<T>> list = (List<Notification<T>>)pendingSubscriptions.remove(idx);
                    toCatchUp = list;
                    subscriber.caughtUp = true;
                    if (pendingSubscriptions.isEmpty()) {
                        pendingSubscriptions = null;
                    }
                }
            }
            if (toCatchUp != null) {
                for (Notification<T> n : toCatchUp) {
                    if (n != null) {
                        n.accept(subscriber);
                    }
                }
            }
        }
        public void removePendingSubscriber(Subscriber<? super T> subscriber) {
            List<Notification<T>> toCatchUp = null;
            synchronized (this) {
                if (pendingSubscriptions == null) {
                    return;
                }
                for (int i = 0; i < pendingSubscriptions.size(); i += 2) {
                    @SuppressWarnings("unchecked")
                    SubjectObserver<? super T> so = (SubjectObserver<? super T>)pendingSubscriptions.get(i);
                    if (so.getActual() == subscriber && !so.caughtUp) {
                        @SuppressWarnings("unchecked")
                        List<Notification<T>> list = (List<Notification<T>>)pendingSubscriptions.get(i + 1);
                        toCatchUp = list;
                        so.caughtUp = true;
                        pendingSubscriptions.remove(i);
                        pendingSubscriptions.remove(i);
                        if (pendingSubscriptions.isEmpty()) {
                            pendingSubscriptions = null;
                        }
                        break;
                    }
                }
            }
            if (toCatchUp != null) {
                for (Notification<T> n : toCatchUp) {
                    if (n != null) {
                        n.accept(subscriber);
                    }
                }
            }
        }
        public void replayAllPending() {
            List<Object> localPending;
            synchronized (this) {
                localPending = pendingSubscriptions;
                pendingSubscriptions = null;
            }
            if (localPending != null) {
                for (int i = 0; i < localPending.size(); i += 2) {
                    @SuppressWarnings("unchecked")
                    SubjectObserver<? super T> so = (SubjectObserver<? super T>)localPending.get(i);
                    if (!so.caughtUp) {
                        @SuppressWarnings("unchecked")
                        List<Notification<T>> list = (List<Notification<T>>)localPending.get(i + 1);
                        for (Notification<T> v : list) {
                            if (v != null) {
                                v.accept(so);
                            }
                        }
                        so.caughtUp = true;
                    }
                }
            }
        }
    }
    
    private final State<T> state;
    private final SubjectSubscriptionManager<T> subscriptionManager;

    protected BehaviorSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, 
            State<T> state) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.state = state;
    }

    @Override
    public void onCompleted() {
        Collection<SubjectObserver<? super T>> observers = subscriptionManager.terminate(new Action0() {

            @Override
            public void call() {
                final Notification<T> ne = Notification.<T>createOnCompleted();
                state.bufferValue(ne);
                state.lastNotification.set(ne);
            }
        });
        if (observers != null) {
            state.replayAllPending();
            for (Observer<? super T> o : observers) {
                o.onCompleted();
            }
        }
    }

    @Override
    public void onError(final Throwable e) {
        Collection<SubjectObserver<? super T>> observers = subscriptionManager.terminate(new Action0() {

            @Override
            public void call() {
                final Notification<T> ne = Notification.<T>createOnError(e);
                state.bufferValue(ne);
                state.lastNotification.set(ne);
            }
        });
        if (observers != null) {
            state.replayAllPending();
            for (Observer<? super T> o : observers) {
                o.onError(e);
            }
        }
    }

    @Override
    public void onNext(T v) {
        // do not overwrite a terminal notification
        // so new subscribers can get them
        Notification<T> last = state.lastNotification.get();
        if (last == null || last.isOnNext()) {
            Notification<T> n = Notification.createOnNext(v);
            state.bufferValue(n);
            state.lastNotification.set(n);
            
            for (SubjectObserver<? super T> o : subscriptionManager.rawSnapshot()) {
                if (o.caughtUp) {
                    o.onNext(v);
                } else {
                    state.removePending(o);
                }
            }
        }
    }
}
