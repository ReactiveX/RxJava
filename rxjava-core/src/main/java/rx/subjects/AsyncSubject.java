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
import rx.Observer;
import rx.Subscription;
import rx.subjects.AbstractSubject.DefaultState;
import rx.subscriptions.Subscriptions;

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
public class AsyncSubject<T> extends Subject<T, T> {
    /** The inner state. */
    protected static final class State<T> extends DefaultState<T> {
        protected boolean hasValue;
        protected T value;
    }
    /**
     * Create a new AsyncSubject
     * 
     * @return a new AsyncSubject
     */
    public static <T> AsyncSubject<T> create() {
        State<T> state = new State<T>();
        return new AsyncSubject<T>(new AsyncSubjectSubscribeFunc<T>(state), state);
    }
    /** The state. */
    protected final State<T> state;
    
    protected AsyncSubject(Observable.OnSubscribeFunc<T> osf, State<T> state) {
        super(osf);
        this.state = state;
    }

    @Override
    public void onNext(T args) {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.hasValue = true;
            state.value = args;
        } finally {
            state.unlock();
        }
    }

    @Override
    public void onError(Throwable e) {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.value = null;
            state.hasValue = false;
            
            state.defaultDispatchError(e);
        } finally {
            state.unlock();
        }
    }
    
    @Override
    public void onCompleted() {
        state.lock();
        try {
            if (state.done) {
                return;
            }
            state.done = true;
            for (Observer<? super T> o : state.removeAll()) {
                if (state.hasValue) {
                    o.onNext(state.value);
                }
                o.onCompleted();
            }
        } finally {
            state.unlock();
        }
    }
    /** The subscription function. */
    protected static final class AsyncSubjectSubscribeFunc<T> implements OnSubscribeFunc<T> {
        protected final State<T> state;
        protected AsyncSubjectSubscribeFunc(State<T> state) {
            this.state = state;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> t1) {
            Throwable error;
            boolean hasValue;
            T value;
            state.lock();
            try {
                if (!state.done) {
                    return state.addObserver(t1);
                }
                error = state.error;
                hasValue = state.hasValue;
                value = state.value;
            } finally {
                state.unlock();
            }
            if (error != null) {
                t1.onError(error);
            } else {
                if (hasValue) {
                    t1.onNext(value);
                }
                t1.onCompleted();
            }
            return Subscriptions.empty();
        }
    }
}
