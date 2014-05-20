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


import rx.Observer;
import rx.functions.Action1;
import rx.operators.NotificationLite;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that publishes the most recent and all subsequent events to each subscribed {@link Observer}.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/S.BehaviorSubject.png">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

 * / observer will receive all events.
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.subscribe(observer);
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive only onCompleted
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.onCompleted();
  subject.subscribe(observer);
  
  // observer will receive only onError
  BehaviorSubject<Object> subject = BehaviorSubject.create("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.onError(new RuntimeException("error"));
  subject.subscribe(observer);
  } </pre>
 * 
 * @param <T>
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class BehaviorSubject<T> extends Subject<T, T> {
    /**
     * Create a {@link BehaviorSubject} without a default value.
     * @param <T> the value type
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create() {
        return create(null, false);
    }
    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each {@link Observer} that subscribes to it.
     * 
     * @param <T> the value type
     * @param defaultValue
     *            the value which will be published to any {@link Observer} as long as the {@link BehaviorSubject} has not yet received any events
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create(T defaultValue) {
        return create(defaultValue, true);
    }
    private static <T> BehaviorSubject<T> create(T defaultValue, boolean hasDefault) {
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();
        if (hasDefault) {
            state.set(NotificationLite.instance().next(defaultValue));
        }
        state.onAdded = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> o) {
                o.emitFirst(state.get());
            }
            
        };
        state.onTerminated = state.onAdded;
        return new BehaviorSubject<T>(state, state); 
    }

    private final SubjectSubscriptionManager<T> state;
    private final NotificationLite<T> nl = NotificationLite.instance();

    protected BehaviorSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.completed();
            for (SubjectObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }
    }

    @Override
    public void onError(Throwable e) {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.error(e);
            for (SubjectObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }
    }

    @Override
    public void onNext(T v) {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.next(v);
            for (SubjectObserver<T> bo : state.next(n)) {
                bo.emitNext(n);
            }
        }
    }
    
    /* test support */ int subscriberCount() {
        return state.observers().length;
    }
}
