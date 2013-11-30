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

import rx.Notification;
import rx.Observer;
import rx.Observable.OnGetSubscriptionFunc;

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
public class PublishSubject<T> extends AbstractSubject<T> {
    public static <T> PublishSubject<T> create() {
        final SubjectState<T> state = new SubjectState<T>();
        OnGetSubscriptionFunc<T> onGetSubscription = getOnGetSubscriptionFunc(state, null);
        return new PublishSubject<T>(onGetSubscription, state);
    }

    private final SubjectState<T> state;

    protected PublishSubject(OnGetSubscriptionFunc<T> onGetSubscription, SubjectState<T> state) {
        super(onGetSubscription);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        /**
         * Mark this subject as completed and emit latest value + 'onCompleted' to all Observers
         */
        state.currentValue.set(new Notification<T>());
        emitNotificationAndTerminate(state, null);
    }

    @Override
    public void onError(Throwable e) {
        /**
         * Mark this subject as completed with an error as the last value and emit 'onError' to all Observers
         */
        state.currentValue.set(new Notification<T>(e));
        emitNotificationAndTerminate(state, null);
    }

    @Override
    public void onNext(T v) {
        /**
         * Store the latest value and send it to all observers;
         */
        state.currentValue.set(new Notification<T>(v));
        emitNotification(state, null);
    }
}
