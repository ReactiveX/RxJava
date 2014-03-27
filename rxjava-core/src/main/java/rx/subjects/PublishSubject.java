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
 * Subject that, once and {@link Observer} has subscribed, publishes all subsequent events to the subscriber.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.PublishSubject.png">
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
public final class PublishSubject<T> extends Subject<T, T> {

    public static <T> PublishSubject<T> create() {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        // set a default value so subscriptions will immediately receive this until a new notification is received
        final AtomicReference<Notification<T>> lastNotification = new AtomicReference<Notification<T>>();

        OnSubscribe<T> onSubscribe = subscriptionManager.getOnSubscribeFunc(
                /**
                 * This function executes at beginning of subscription.
                 * 
                 * This will always run, even if Subject is in terminal state.
                 */
                new Action1<SubjectObserver<? super T>>() {

                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        // nothing onSubscribe unless in terminal state which is the next function
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

        return new PublishSubject<T>(onSubscribe, subscriptionManager, lastNotification);
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    final AtomicReference<Notification<T>> lastNotification;

    protected PublishSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, AtomicReference<Notification<T>> lastNotification) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.lastNotification = lastNotification;
    }

    @Override
    public void onCompleted() {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                lastNotification.set(Notification.<T> createOnCompleted());
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
                lastNotification.set(Notification.<T>createOnError(e));
                for (Observer<? super T> o : observers) {
                    o.onError(e);
                }
            }
        });

    }

    @Override
    public void onNext(T v) {
        for (Observer<? super T> o : subscriptionManager.rawSnapshot()) {
            o.onNext(v);
        }
    }
}