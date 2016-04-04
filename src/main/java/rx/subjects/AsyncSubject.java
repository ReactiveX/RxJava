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

import java.util.*;

import rx.Observer;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.internal.producers.SingleProducer;
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
                Object v = state.getLatest();
                NotificationLite<T> nl = state.nl;
                if (v == null || nl.isCompleted(v)) {
                    o.onCompleted();
                } else
                if (nl.isError(v)) {
                    o.onError(nl.getError(v));
                } else {
                    o.actual.setProducer(new SingleProducer<T>(o.actual, nl.getValue(v)));
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
                    bo.actual.setProducer(new SingleProducer<T>(bo.actual, nl.getValue(last)));
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

            Exceptions.throwIfAny(errors);
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
    /**
     * Check if the Subject has a value.
     * <p>Use the {@link #getValue()} method to retrieve such a value.
     * <p>Note that unless {@link #hasCompleted()} or {@link #hasThrowable()} returns true, the value
     * retrieved by {@code getValue()} may get outdated.
     * @return true if and only if the subject has some value but not an error
     */
    @Beta
    public boolean hasValue() {
        Object v = lastValue;
        Object o = state.getLatest();
        return !nl.isError(o) && nl.isNext(v);
    }
    /**
     * Check if the Subject has terminated with an exception.
     * @return true if the subject has received a throwable through {@code onError}.
     */
    @Beta
    public boolean hasThrowable() {
        Object o = state.getLatest();
        return nl.isError(o);
    }
    /**
     * Check if the Subject has terminated normally.
     * @return true if the subject completed normally via {@code onCompleted()}
     */
    @Beta
    public boolean hasCompleted() {
        Object o = state.getLatest();
        return o != null && !nl.isError(o);
    }
    /**
     * Returns the current value of the Subject if there is such a value and
     * the subject hasn't terminated with an exception.
     * <p>The method can return {@code null} for various reasons. Use {@link #hasValue()}, {@link #hasThrowable()}
     * and {@link #hasCompleted()} to determine if such {@code null} is a valid value, there was an
     * exception or the Subject terminated without receiving any value. 
     * @return the current value or {@code null} if the Subject doesn't have a value,
     * has terminated with an exception or has an actual {@code null} as a value.
     */
    @Beta
    public T getValue() {
        Object v = lastValue;
        Object o = state.getLatest();
        if (!nl.isError(o) && nl.isNext(v)) {
            return nl.getValue(v);
        }
        return null;
    }
    /**
     * Returns the Throwable that terminated the Subject.
     * @return the Throwable that terminated the Subject or {@code null} if the
     * subject hasn't terminated yet or it terminated normally.
     */
    @Beta
    public Throwable getThrowable() {
        Object o = state.getLatest();
        if (nl.isError(o)) {
            return nl.getError(o);
        }
        return null;
    }
}
