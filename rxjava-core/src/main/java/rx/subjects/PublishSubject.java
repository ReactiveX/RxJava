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
import rx.operators.NotificationLite;
import rx.subjects.BehaviorSubject.BehaviorOnSubscribe;
import rx.subjects.BehaviorSubject.State;

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
        State<T> state = new State<T>();
        return new PublishSubject<T>(new BehaviorOnSubscribe<T>(state), state);
    }

    final State<T> state;
    private final NotificationLite<T> nl = NotificationLite.instance();
    
    protected PublishSubject(OnSubscribe<T> onSubscribe, State<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.completed();
            for (BehaviorSubject.BehaviorObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }

    }

    @Override
    public void onError(final Throwable e) {
        Object last = state.get();
        if (last == null || state.active) {
            Object n = nl.error(e);
            for (BehaviorSubject.BehaviorObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n);
            }
        }
    }

    @Override
    public void onNext(T v) {
        for (BehaviorSubject.BehaviorObserver<T> bo : state.observers()) {
            bo.emitNext(nl.next(v));
        }
    }
}