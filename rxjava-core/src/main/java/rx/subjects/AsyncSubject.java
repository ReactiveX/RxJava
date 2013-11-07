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

import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;

/**
 * Subject that publishes only the last event to each {@link Observer} that has subscribed when the
 * sequence completes.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.AsyncSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * / observer will receive no onNext events because the subject.onCompleted() isn't called.
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

        return new AsyncSubject<T>(onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<? super T>> observers;
    private final AtomicReference<T> currentValue;
    private final AtomicBoolean hasValue = new AtomicBoolean();

    protected AsyncSubject(OnSubscribeFunc<T> onSubscribe, ConcurrentHashMap<Subscription, Observer<? super T>> observers) {
        super(onSubscribe);
        this.observers = observers;
        this.currentValue = new AtomicReference<T>();
    }

    @Override
    public void onCompleted() {
        T finalValue = currentValue.get();
        for (Observer<? super T> observer : observers.values()) {
            if (hasValue.get()) {
                observer.onNext(finalValue);
            }
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
        hasValue.set(true);
        currentValue.set(args);
    }
}
