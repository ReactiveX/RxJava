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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;

/**
 * Subject that, once and {@link Observer} has subscribed, publishes all subsequent events to the subscriber.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.PublishSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * PublishSubject<Object> subject = PublishSubject.create();
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1);
  subject.onNext("one");
  subject.onNext("two");
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2);
  subject.onNext("three");
  subject.onCompleted();

  } </pre>
 * 
 * @param <T>
 */
public class PublishSubject<T> extends Subject<T, T> {
    public static <T> PublishSubject<T> create() {
        final ConcurrentHashMap<Subscription, Observer<? super T>> observers = new ConcurrentHashMap<Subscription, Observer<? super T>>();

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

                // on subscribe add it to the map of outbound observers to notify
                observers.put(subscription, observer);

                return subscription;
            }

        };

        return new PublishSubject<T>(onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<? super T>> observers;

    protected PublishSubject(OnSubscribeFunc<T> onSubscribe, ConcurrentHashMap<Subscription, Observer<? super T>> observers) {
        super(onSubscribe);
        this.observers = observers;
    }

    @Override
    public void onCompleted() {
        for (Observer<? super T> observer : snapshotOfValues()) {
            observer.onCompleted();
        }
        observers.clear();
    }

    @Override
    public void onError(Throwable e) {
        for (Observer<? super T> observer : snapshotOfValues()) {
            observer.onError(e);
        }
        observers.clear();
    }

    @Override
    public void onNext(T args) {
        for (Observer<? super T> observer : snapshotOfValues()) {
            observer.onNext(args);
        }
    }

    /**
     * Current snapshot of 'values()' so that concurrent modifications aren't included.
     * 
     * This makes it behave deterministically in a single-threaded execution when nesting subscribes.
     * 
     * In multi-threaded execution it will cause new subscriptions to wait until the following onNext instead
     * of possibly being included in the current onNext iteration.
     * 
     * @return List<Observer<T>>
     */
    private Collection<Observer<? super T>> snapshotOfValues() {
        return new ArrayList<Observer<? super T>>(observers.values());
    }
}
