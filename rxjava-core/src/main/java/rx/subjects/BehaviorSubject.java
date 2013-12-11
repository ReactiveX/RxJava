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
import rx.subjects.AbstractSubject.DefaultState;
import rx.subscriptions.Subscriptions;

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
public class BehaviorSubject<T> extends Subject<T, T> {
    /** The inner state. */
    protected static final class State<T> extends DefaultState<T> {
        protected T value;
    }    
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
        final State<T> state = new State<T>();
        state.value = defaultValue;
        return new BehaviorSubject<T>(new BehaviorSubjectSubscribeFunc<T>(state), state);
    }

    private final State<T> state;

    protected BehaviorSubject(OnSubscribeFunc<T> onSubscribe, State<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        state.defaultOnCompleted();
    }

    @Override
    public void onError(Throwable e) {
        state.defaultOnError(e);
    }

    @Override
    public void onNext(T v) {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.value = v;
            state.defaultDispatch(v);
        } finally {
            state.unlock();
        }
    }
    /** The subscription function. */
    protected static final class BehaviorSubjectSubscribeFunc<T> implements OnSubscribeFunc<T> {
        protected final State<T> state;
        protected BehaviorSubjectSubscribeFunc(State<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            Throwable error;
            T value;
            state.lock();
            try {
                value = state.value;
                if (!state.done) {
                    t1.onNext(value);
                    return state.addObserver(t1);
                }
                error = state.error;
            } finally {
                state.unlock();
            }
            if (error != null) {
                t1.onError(error);
            } else {
                t1.onCompleted();
            }
            return Subscriptions.empty();
        }
    }
}
