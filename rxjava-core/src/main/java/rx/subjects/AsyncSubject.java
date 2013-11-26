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
public class AsyncSubject<T> extends AbstractSubject<T> {

    /**
     * Create a new AsyncSubject
     * 
     * @return a new AsyncSubject
     */
    public static <T> AsyncSubject<T> create() {
        final SubjectState<T> state = new SubjectState<T>();
        OnSubscribeFunc<T> onSubscribe = getOnSubscribeFunc(state, new Action2<SubjectState<T>, Observer<? super T>>() {

            @Override
            public void call(SubjectState<T> state, Observer<? super T> o) {
                // we want the last value + completed so add this extra logic 
                // to send onCompleted if the last value is an onNext
                if (state.completed.get()) {
                    Notification<T> value = state.currentValue.get();
                    if (value != null && value.isOnNext()) {
                        o.onCompleted();
                    }
                }
            }
        });
        return new AsyncSubject<T>(onSubscribe, state);
    }

    private final SubjectState<T> state;

    protected AsyncSubject(OnSubscribeFunc<T> onSubscribe, SubjectState<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        /**
         * Mark this subject as completed and emit latest value + 'onCompleted' to all Observers
         */
        emitNotificationAndTerminate(state, new Action2<SubjectState<T>, Observer<? super T>>() {

            @Override
            public void call(SubjectState<T> state, Observer<? super T> o) {
                o.onCompleted();
            }
        });
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
         * Store the latest value but do not send it. It only gets sent when 'onCompleted' occurs.
         */
        state.currentValue.set(new Notification<T>(v));
    }

}
