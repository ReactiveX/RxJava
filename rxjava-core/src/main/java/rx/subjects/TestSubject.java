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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.EventLoop;
import rx.Scheduler.Schedulable;
import rx.functions.Action1;
import rx.schedulers.TestScheduler;
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
public final class TestSubject<T> extends Subject<T, T> {

    public static <T> TestSubject<T> create(TestScheduler scheduler) {
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

        return new TestSubject<T>(onSubscribe, subscriptionManager, lastNotification, scheduler);
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    private final AtomicReference<Notification<T>> lastNotification;
    private final Scheduler.EventLoop innerScheduler;

    protected TestSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, AtomicReference<Notification<T>> lastNotification, TestScheduler scheduler) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.lastNotification = lastNotification;
        this.innerScheduler = scheduler.createEventLoop();
    }

    @Override
    public void onCompleted() {
        onCompleted(innerScheduler.now());
    }

    private void _onCompleted() {
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

    public void onCompleted(long timeInMilliseconds) {
        innerScheduler.schedule(new Action1<Schedulable>() {

            @Override
            public void call(Schedulable t1) {
                _onCompleted();
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onError(final Throwable e) {
        onError(e, innerScheduler.now());
    }

    private void _onError(final Throwable e) {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                lastNotification.set(Notification.<T> createOnError(e));
                for (Observer<? super T> o : observers) {
                    o.onError(e);
                }
            }
        });

    }

    public void onError(final Throwable e, long timeInMilliseconds) {
        innerScheduler.schedule(new Action1<Schedulable>() {

            @Override
            public void call(Schedulable t1) {
                _onError(e);
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onNext(T v) {
        onNext(v, innerScheduler.now());
    }

    private void _onNext(T v) {
        for (Observer<? super T> o : subscriptionManager.rawSnapshot()) {
            o.onNext(v);
        }
    }

    public void onNext(final T v, long timeInMilliseconds) {
        innerScheduler.schedule(new Action1<Schedulable>() {

            @Override
            public void call(Schedulable t1) {
                _onNext(v);
            }

        }, timeInMilliseconds, TimeUnit.MILLISECONDS);
    }
}
