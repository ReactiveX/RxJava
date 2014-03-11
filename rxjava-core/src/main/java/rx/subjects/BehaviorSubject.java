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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observer;
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
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each
     * {@link Observer} that subscribes to it.
     * 
     * @param defaultValue
     *            the value which will be published to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet received any events
     * @return the constructed {@link BehaviorSubject}
     * @deprecated use {@link #create(T)} instead
     */
    public static <T> BehaviorSubject<T> createWithDefaultValue(T defaultValue) {
        return create(defaultValue);
    }

    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each
     * {@link Observer} that subscribes to it.
     * 
     * @param defaultValue
     *            the value which will be published to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet received any events
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create(T defaultValue) {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        // set a default value so subscriptions will immediately receive this until a new notification is received
        final AtomicReference<Notification<T>> lastNotification = new AtomicReference<Notification<T>>(new Notification<T>(defaultValue));

        OnSubscribe<T> onSubscribe = subscriptionManager.getOnSubscribeFunc(
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
                        Notification<T> n = lastNotification.get();
                        if (n.isOnNext()) {
                            n.accept(o);
                        }
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
                        lastNotification.get().accept(o);
                    }
                }, null);

        return new BehaviorSubject<T>(onSubscribe, subscriptionManager, lastNotification);
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    final AtomicReference<Notification<T>> lastNotification;

    protected BehaviorSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, AtomicReference<Notification<T>> lastNotification) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.lastNotification = lastNotification;
    }

    @Override
    public void onCompleted() {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                lastNotification.set(new Notification<T>());
                for (Observer<? super T> o : observers) {
                    o.onCompleted();
                }
            }
        });
    }

    @Override
    public void onError(final Throwable e) {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                lastNotification.set(new Notification<T>(e));
                for (Observer<? super T> o : observers) {
                    o.onError(e);
                }
            }
        });

    }

    @Override
    public void onNext(T v) {
        // do not overwrite a terminal notification
        // so new subscribers can get them
        if (lastNotification.get().isOnNext()) {
            lastNotification.set(new Notification<T>(v));
            for (Observer<? super T> o : subscriptionManager.rawSnapshot()) {
                o.onNext(v);
            }
        }
    }

}
