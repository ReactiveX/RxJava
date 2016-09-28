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
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.internal.producers.SingleProducer;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that publishes only the last item observed to each {@link Observer} once the source {@code Observable}
 * has completed.  The item is cached and published to any {@code Observer}s which subscribe after the source
 * has completed.  If the source emitted no items, {@code AsyncSubject} completes without emitting anything.
 * If the source terminated in an error, current and future subscribers will receive only the error.
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
    final SubjectSubscriptionManager<T> state;
    volatile Object lastValue;

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
                if (v == null || NotificationLite.isCompleted(v)) {
                    o.onCompleted();
                } else
                if (NotificationLite.isError(v)) {
                    o.onError(NotificationLite.getError(v));
                } else {
                    o.actual.setProducer(new SingleProducer<T>(o.actual, NotificationLite.<T>getValue(v)));
                }
            }
        };
        return new AsyncSubject<T>(state, state);
    }

    protected AsyncSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        if (state.active) {
            Object last = lastValue;
            if (last == null) {
                last = NotificationLite.completed();
            }
            for (SubjectObserver<T> bo : state.terminate(last)) {
                if (last == NotificationLite.completed()) {
                    bo.onCompleted();
                } else {
                    bo.actual.setProducer(new SingleProducer<T>(bo.actual, NotificationLite.<T>getValue(last)));
                }
            }
        }
    }

    @Override
    public void onError(final Throwable e) {
        if (state.active) {
            Object n = NotificationLite.error(e);
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
        lastValue = NotificationLite.next(v);
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
     * @since 1.2
     */
    public boolean hasValue() {
        Object v = lastValue;
        Object o = state.getLatest();
        return !NotificationLite.isError(o) && NotificationLite.isNext(v);
    }
    /**
     * Check if the Subject has terminated with an exception.
     * @return true if the subject has received a throwable through {@code onError}.
     * @since 1.2
     */
    public boolean hasThrowable() {
        Object o = state.getLatest();
        return NotificationLite.isError(o);
    }
    /**
     * Check if the Subject has terminated normally.
     * @return true if the subject completed normally via {@code onCompleted()}
     * @since 1.2
     */
    public boolean hasCompleted() {
        Object o = state.getLatest();
        return o != null && !NotificationLite.isError(o);
    }
    /**
     * Returns the current value of the Subject if there is such a value and
     * the subject hasn't terminated with an exception.
     * <p>The method can return {@code null} for various reasons. Use {@link #hasValue()}, {@link #hasThrowable()}
     * and {@link #hasCompleted()} to determine if such {@code null} is a valid value, there was an
     * exception or the Subject terminated without receiving any value.
     * @return the current value or {@code null} if the Subject doesn't have a value,
     * has terminated with an exception or has an actual {@code null} as a value.
     * @since 1.2
     */
    public T getValue() {
        Object v = lastValue;
        Object o = state.getLatest();
        if (!NotificationLite.isError(o) && NotificationLite.isNext(v)) {
            return NotificationLite.getValue(v);
        }
        return null;
    }
    /**
     * Returns the Throwable that terminated the Subject.
     * @return the Throwable that terminated the Subject or {@code null} if the
     * subject hasn't terminated yet or it terminated normally.
     * @since 1.2
     */
    public Throwable getThrowable() {
        Object o = state.getLatest();
        if (NotificationLite.isError(o)) {
            return NotificationLite.getError(o);
        }
        return null;
    }
}
