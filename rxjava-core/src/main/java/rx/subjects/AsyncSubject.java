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
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();
        state.onTerminated = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                Object v = state.get();
                o.accept(v);
                NotificationLite<T> nl = NotificationLite.instance();
                if (v == null || (!nl.isCompleted(v) && !nl.isError(v))) {
                    o.onCompleted();
                }
            }
        };
        return new AsyncSubject<T>(state, state);
    }

    final SubjectSubscriptionManager<T> state;
    volatile Object lastValue;
    private final NotificationLite<T> nl = NotificationLite.instance();


    protected AsyncSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        if (state.active) {
            Object last = lastValue;
            if (last == null) {
                last = nl.completed();
            }
            for (SubjectObserver<T> bo : state.terminate(last)) {
                if (last == nl.completed()) {
                    bo.onCompleted();
                } else {
                    bo.onNext(nl.getValue(last));
                    bo.onCompleted();
                }
            }
        }
    }

    @Override
    public void onError(final Throwable e) {
        if (state.active) {
            Object n = nl.error(e);
            for (SubjectObserver<T> bo : state.terminate(n)) {
                bo.onError(e);
            }
        }
    }

    @Override
    public void onNext(T v) {
        lastValue = nl.next(v);
    }

}