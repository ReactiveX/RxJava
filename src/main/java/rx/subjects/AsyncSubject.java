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

import java.util.ArrayList;
import java.util.List;

import rx.Observer;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that publishes only the last item observed to each {@link Observer} that has subscribed, when the
 * source {@code Observable} completes.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.AsyncSubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  // observer will receive no onNext events because the subject.onCompleted() isn't called.
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
 *          the type of item expected to be observed by the Subject
 */
public final class AsyncSubject<T> extends Subject<T, T> {

    /**
     * Creates and returns a new {@code AsyncSubject}.
     * @param <T> the result value type
     * @return the new {@code AsyncSubject}
     */
    public static <T> AsyncSubject<T> create() {
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();
        state.onTerminated = new Action1<SubjectObserver<T>>() {
            @Override
            public void call(SubjectObserver<T> o) {
                Object v = state.get();
                NotificationLite<T> nl = state.nl;
                o.accept(v, nl);
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
            List<Throwable> errors = null;
            for (SubjectObserver<T> bo : state.terminate(n)) {
                try {
                    bo.onError(e);
                } catch (Throwable e2) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(e2);
                }
            }

            if (errors != null) {
                if (errors.size() == 1) {
                    Exceptions.propagate(errors.get(0));
                } else {
                    throw new CompositeException("Errors while emitting AsyncSubject.onError", errors);
                }
            }
        }
    }

    @Override
    public void onNext(T v) {
        lastValue = nl.next(v);
    }

    @Override
    public boolean hasObservers() {
        return state.observers().length > 0;
    }
}
