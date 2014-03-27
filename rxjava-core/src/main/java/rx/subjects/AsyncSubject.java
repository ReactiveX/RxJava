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
 * Subject that publishes only the last event to each {@link Observer} that has subscribed when the
 * sequence completes.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.AsyncSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code
 * 
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
public final class AsyncSubject<T> extends Subject<T, T> {

    public static <T> AsyncSubject<T> create() {
        final SubjectSubscriptionManager<T> subscriptionManager = new SubjectSubscriptionManager<T>();
        final AtomicReference<Notification<T>> lastNotification = new AtomicReference<Notification<T>>(new Notification<T>());

        OnSubscribe<T> onSubscribe = subscriptionManager.getOnSubscribeFunc(
                /**
                 * This function executes at beginning of subscription.
                 * 
                 * This will always run, even if Subject is in terminal state.
                 */
                new Action1<SubjectObserver<? super T>>() {

                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        // nothing to do if not terminated
                    }
                },
                /**
                 * This function executes if the Subject is terminated.
                 */
                new Action1<SubjectObserver<? super T>>() {

                    @Override
                    public void call(SubjectObserver<? super T> o) {
                        // we want the last value + completed so add this extra logic 
                        // to send onCompleted if the last value is an onNext
                        emitValueToObserver(lastNotification.get(), o);
                    }
                }, null);

        return new AsyncSubject<T>(onSubscribe, subscriptionManager, lastNotification);
    }

    protected static <T> void emitValueToObserver(Notification<T> n, Observer<? super T> o) {
        n.accept(o);
        if (n.isOnNext()) {
            o.onCompleted();
        }
    }

    private final SubjectSubscriptionManager<T> subscriptionManager;
    final AtomicReference<Notification<T>> lastNotification;

    protected AsyncSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> subscriptionManager, AtomicReference<Notification<T>> lastNotification) {
        super(onSubscribe);
        this.subscriptionManager = subscriptionManager;
        this.lastNotification = lastNotification;
    }

    @Override
    public void onCompleted() {
        subscriptionManager.terminate(new Action1<Collection<SubjectObserver<? super T>>>() {

            @Override
            public void call(Collection<SubjectObserver<? super T>> observers) {
                for (Observer<? super T> o : observers) {
                    emitValueToObserver(lastNotification.get(), o);
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
                    emitValueToObserver(lastNotification.get(), o);
                }
            }
        });

    }

    @Override
    public void onNext(T v) {
        lastNotification.set(new Notification<T>(v));
    }

}