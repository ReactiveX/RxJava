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

import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Subject that publishes the most recent and all subsequent events to each subscribed {@link Observer}.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.BehaviorSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  // observer will receive all events.
  BehaviorSubject<Object> subject = BehaviorSubject.createWithDefaultValue("default");
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorSubject<Object> subject = BehaviorSubject.createWithDefaultValue("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.subscribe(observer);
  subject.onNext("two");
  subject.onNext("three");

  } </pre>
 *
 * @param <T>
 */
public class BehaviorSubject<T> extends Subject<T, T> {

    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each
     * {@link Observer} that subscribes to it.
     *
     * @param defaultValue
     *            The value which will be published to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet received any events.
     * @return the constructed {@link BehaviorSubject}.
     */
    public static <T> BehaviorSubject<T> createWithDefaultValue(T defaultValue) {
        final ConcurrentHashMap<Subscription, Observer<? super T>> observers = new ConcurrentHashMap<Subscription, Observer<? super T>>();

        final AtomicReference<T> currentValue = new AtomicReference<T>(defaultValue);

        OnSubscribeFunc<T> onSubscribe = new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                final SafeObservableSubscription subscription = new SafeObservableSubscription();

                subscription.wrap(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // on unsubscribe remove it from the map of outbound observers to notify
                        observers.remove(subscription);
                    }
                });

                observer.onNext(currentValue.get());

                // on subscribe add it to the map of outbound observers to notify
                observers.put(subscription, observer);
                return subscription;
            }
        };

        return new BehaviorSubject<T>(currentValue, onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<? super T>> observers;
    private final AtomicReference<T> currentValue;

    protected BehaviorSubject(AtomicReference<T> currentValue, OnSubscribeFunc<T> onSubscribe, ConcurrentHashMap<Subscription, Observer<? super T>> observers) {
        super(onSubscribe);
        this.currentValue = currentValue;
        this.observers = observers;
    }

    @Override
    public void onCompleted() {
        for (Observer<? super T> observer : observers.values()) {
            observer.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (Observer<? super T> observer : observers.values()) {
            observer.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        currentValue.set(args);
        for (Observer<? super T> observer : observers.values()) {
            observer.onNext(args);
        }
    }
}
