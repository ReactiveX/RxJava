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
import rx.util.functions.Action2;

/**
 * Subject that publishes the most recent and all subsequent events to each subscribed {@link Observer}.
 * <p>
 * <img src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.BehaviorSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * / observer will receive all events.
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
public class BehaviorSubject<T> extends AbstractSubject<T> {

    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each {@link Observer} that subscribes to it.
     * 
     * @param defaultValue
     *            The value which will be published to any {@link Observer} as long as the {@link BehaviorSubject} has not yet received any events.
     * @return the constructed {@link BehaviorSubject}.
     * @deprecated Use {@link create()} instead.
     */
    public static <T> BehaviorSubject<T> createWithDefaultValue(T defaultValue) {
        return create(defaultValue);
    }

    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each {@link Observer} that subscribes to it.
     * 
     * @param defaultValue
     *            The value which will be published to any {@link Observer} as long as the {@link BehaviorSubject} has not yet received any events.
     * @return the constructed {@link BehaviorSubject}.
     */
    public static <T> BehaviorSubject<T> create(T defaultValue) {
        final SubjectState<T> state = new SubjectState<T>();
        // set a default value so subscriptions will immediately receive this until a new notification is received
        state.currentValue.set(new Notification<T>(defaultValue));
        OnSubscribeFunc<T> onSubscribe = getOnSubscribeFunc(state, new Action2<SubjectState<T>, Observer<? super T>>() {

            @Override
            public void call(SubjectState<T> state, Observer<? super T> o) {
                /**
                 * When we subscribe we always emit the latest value to the observer, including
                 * terminal states which are recorded as the last value.
                 */
                emitNotification(state.currentValue.get(), o);
            }
        });
        return new BehaviorSubject<T>(onSubscribe, state);
    }

    private final SubjectState<T> state;

    protected BehaviorSubject(OnSubscribeFunc<T> onSubscribe, SubjectState<T> state) {
        super(onSubscribe);
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
