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

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subjects.AbstractSubject.DefaultState;
import rx.subscriptions.Subscriptions;

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
public class PublishSubject<T> extends Subject<T, T> {
    /** The inner state. */
    protected static final class State<T> extends DefaultState<T> {
    }
    
    public static <T> PublishSubject<T> create() {
        State<T> state = new State<T>();
        return new PublishSubject<T>(new PublishSubjectSubscribeFunc<T>(state), state);
    }
/** The state. */
    protected final State<T> state;
    
    protected PublishSubject(Observable.OnSubscribeFunc<T> osf, State<T> state) {
        super(osf);
        this.state = state;
    }

    @Override
    public void onNext(T args) {
        state.defaultOnNext(args);
    }

    @Override
    public void onError(Throwable e) {
        state.defaultOnError(e);
    }
    
    @Override
    public void onCompleted() {
        state.defaultOnCompleted();
    }
    /** The subscription function. */
    protected static final class PublishSubjectSubscribeFunc<T> implements OnSubscribeFunc<T> {
        protected final State<T> state;
        protected PublishSubjectSubscribeFunc(State<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            Throwable error;
            state.lock();
            try {
                if (!state.done) {
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
