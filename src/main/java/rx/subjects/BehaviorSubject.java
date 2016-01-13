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


import java.lang.reflect.Array;
import java.util.*;

import rx.Observer;
import rx.annotations.Beta;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

/**
 * Subject that emits the most recent item it has observed and all subsequent observed items to each subscribed
 * {@link Observer}.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.BehaviorSubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  // observer will receive all events.
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
 *          the type of item expected to be observed by the Subject
 */
public final class BehaviorSubject<T> extends Subject<T, T> {
    /**
     * Creates a {@link BehaviorSubject} without a default item.
     *
     * @param <T>
     *            the type of item the Subject will emit
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create() {
        return create(null, false);
    }
    /**
     * Creates a {@link BehaviorSubject} that emits the last item it observed and all subsequent items to each
     * {@link Observer} that subscribes to it.
     * 
     * @param <T>
     *            the type of item the Subject will emit
     * @param defaultValue
     *            the item that will be emitted first to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet observed any items from its source {@code Observable}
     * @return the constructed {@link BehaviorSubject}
     */
    public static <T> BehaviorSubject<T> create(T defaultValue) {
        return create(defaultValue, true);
    }
    private static <T> BehaviorSubject<T> create(T defaultValue, boolean hasDefault) {
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();
        if (hasDefault) {
            state.setLatest(NotificationLite.instance().next(defaultValue));
        }
        state.onAdded = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> o) {
                o.emitFirst(state.getLatest(), state.nl);
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
        Object last = state.getLatest();
        if (last == null || state.active) {
            Object n = nl.completed();
            for (SubjectObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n, state.nl);
            }
        }
    }

    @Override
    public void onError(Throwable e) {
        Object last = state.getLatest();
        if (last == null || state.active) {
            Object n = nl.error(e);
            List<Throwable> errors = null;
            for (SubjectObserver<T> bo : state.terminate(n)) {
                try {
                    bo.emitNext(n, state.nl);
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
        Object last = state.getLatest();
        if (last == null || state.active) {
            Object n = nl.next(v);
            for (SubjectObserver<T> bo : state.next(n)) {
                bo.emitNext(n, state.nl);
            }
        }
    }

    /* test support */ int subscriberCount() {
        return state.observers().length;
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
     * @return true if and only if the subject has some value and hasn't terminated yet.
     */
    @Beta
    public boolean hasValue() {
        Object o = state.getLatest();
        return nl.isNext(o);
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
        return nl.isCompleted(o);
    }
    /**
     * Returns the current value of the Subject if there is such a value and
     * the subject hasn't terminated yet.
     * <p>The method can return {@code null} for various reasons. Use {@link #hasValue()}, {@link #hasThrowable()}
     * and {@link #hasCompleted()} to determine if such {@code null} is a valid value, there was an
     * exception or the Subject terminated (with or without receiving any value). 
     * @return the current value or {@code null} if the Subject doesn't have a value,
     * has terminated or has an actual {@code null} as a valid value.
     */
    @Beta
    public T getValue() {
        Object o = state.getLatest();
        if (nl.isNext(o)) {
            return nl.getValue(o);
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
    /**
     * Returns a snapshot of the currently buffered non-terminal events into 
     * the provided {@code a} array or creates a new array if it has not enough capacity.
     * @param a the array to fill in
     * @return the array {@code a} if it had enough capacity or a new array containing the available values 
     */
    @Beta
    @SuppressWarnings("unchecked")
    public T[] getValues(T[] a) {
        Object o = state.getLatest();
        if (nl.isNext(o)) {
            if (a.length == 0) {
                a = (T[])Array.newInstance(a.getClass().getComponentType(), 1);
            }
            a[0] = nl.getValue(o);
            if (a.length > 1) {
                a[1] = null;
            }
        } else
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }
    
    /** An empty array to trigger getValues() to return a new array. */
    private static final Object[] EMPTY_ARRAY = new Object[0];
    
    /**
     * Returns a snapshot of the currently buffered non-terminal events.
     * <p>The operation is threadsafe.
     *
     * @return a snapshot of the currently buffered non-terminal events.
     * @since (If this graduates from being an Experimental class method, replace this parenthetical with the release number)
     */
    @SuppressWarnings("unchecked")
    @Beta
    public Object[] getValues() {
        T[] r = getValues((T[])EMPTY_ARRAY);
        if (r == EMPTY_ARRAY) {
            return new Object[0]; // don't leak the default empty array.
        }
        return r;
    }
}
