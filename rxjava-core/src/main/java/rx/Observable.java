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
package rx;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import rx.observables.BlockingObservable;
import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
import rx.operators.AtomicObservableSubscription;
import rx.operators.AtomicObserver;
import rx.operators.OperationAll;
import rx.operators.OperationCache;
import rx.operators.OperationConcat;
import rx.operators.OperationDefer;
import rx.operators.OperationDematerialize;
import rx.operators.OperationFilter;
import rx.operators.OperationFinally;
import rx.operators.OperationGroupBy;
import rx.operators.OperationMap;
import rx.operators.OperationMaterialize;
import rx.operators.OperationMerge;
import rx.operators.OperationMergeDelayError;
import rx.operators.OperationMulticast;
import rx.operators.OperationObserveOn;
import rx.operators.OperationOnErrorResumeNextViaFunction;
import rx.operators.OperationOnErrorResumeNextViaObservable;
import rx.operators.OperationOnErrorReturn;
import rx.operators.OperationSample;
import rx.operators.OperationScan;
import rx.operators.OperationSkip;
import rx.operators.OperationSubscribeOn;
import rx.operators.OperationSwitch;
import rx.operators.OperationSynchronize;
import rx.operators.OperationTake;
import rx.operators.OperationTakeLast;
import rx.operators.OperationTakeUntil;
import rx.operators.OperationTakeWhile;
import rx.operators.OperationTimestamp;
import rx.operators.OperationToObservableFuture;
import rx.operators.OperationToObservableIterable;
import rx.operators.OperationToObservableList;
import rx.operators.OperationToObservableSortedList;
import rx.operators.OperationWhere;
import rx.operators.OperationZip;
import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaObservableExecutionHook;
import rx.plugins.RxJavaPlugins;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.OnErrorNotImplementedException;
import rx.util.Range;
import rx.util.Timestamped;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FuncN;
import rx.util.functions.Function;
import rx.util.functions.FunctionLanguageAdaptor;
import rx.util.functions.Functions;

/**
 * The Observable interface that implements the Reactive Pattern.
 * <p>
 * This interface provides overloaded methods for subscribing as well as delegate methods to the
 * various operators.
 * <p>
 * The documentation for this interface makes use of marble diagrams. The following legend explains
 * these diagrams:
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/legend.png">
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava
 * Wiki</a>
 * 
 * @param <T>
 */
public class Observable<T> {

//TODO use a consistent parameter naming scheme (for example: for all operators that modify a source Observable, the parameter representing that source Observable should have the same name, e.g. "source" -- currently such parameters are named any of "sequence", "that", "source", "items", or "observable")

    private final static RxJavaObservableExecutionHook hook = RxJavaPlugins.getInstance().getObservableExecutionHook();

    private final Func1<Observer<T>, Subscription> onSubscribe;

    /**
     * Observable with Function to execute when subscribed to.
     * <p>
     * NOTE: Use {@link #create(Func1)} to create an Observable instead of this method unless you
     * specifically have a need for inheritance.
     * 
     * @param onSubscribe
     *            {@link Func1} to be executed when {@link #subscribe(Observer)} is called.
     */
    protected Observable(Func1<Observer<T>, Subscription> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    protected Observable() {
        this(null);
        //TODO should this be made private to prevent it? It really serves no good purpose and only confuses things. Unit tests are incorrectly using it today
    }

    /**
     * An {@link Observer} must call an Observable's <code>subscribe</code> method in order to
     * receive items and notifications from the Observable. A typical implementation of
     * <code>subscribe</code> does the following:
     * <p>
     * It stores a reference to the Observer in a collection object, such as a
     * <code>List&lt;T&gt;</code> object.
     * <p>
     * It returns a reference to the {@link Subscription} interface. This enables Observers to
     * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
     * sending them, which also invokes the Observer's {@link Observer#onCompleted onCompleted}
     * method.
     * <p>
     * An <code>Observable&lt;T&gt;</code> instance is responsible for accepting all subscriptions
     * and notifying all Observers. Unless the documentation for a particular
     * <code>Observable&lt;T&gt;</code> implementation indicates otherwise, Observers should make no
     * assumptions about the order in which multiple Observers will receive their notifications.
     * <p>
     * For more information see the
     * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * @param observer
     * @return a {@link Subscription} reference with which the Observer can stop receiving items
     *         before the Observable has finished sending them
     * @throws IllegalArgumentException
     *             if the {@link Observer} provided as the argument to <code>subscribe()</code> is
     *             <code>null</code>
     */
    public Subscription subscribe(Observer<T> observer) {
        // allow the hook to intercept and/or decorate
        Func1<Observer<T>, Subscription> onSubscribeFunction = hook.onSubscribeStart(this, onSubscribe);
        // validate and proceed
        if (observer == null) {
            throw new IllegalArgumentException("observer can not be null");
        }
        if (onSubscribeFunction == null) {
            throw new IllegalStateException("onSubscribe function can not be null.");
            // the subscribe function can also be overridden but generally that's not the appropriate approach so I won't mention that in the exception
        }
        try {
            /**
             * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
             */
            if (isInternalImplementation(observer)) {
                Subscription s = onSubscribeFunction.call(observer);
                if (s == null) {
                    // this generally shouldn't be the case on a 'trusted' onSubscribe but in case it happens
                    // we want to gracefully handle it the same as AtomicObservableSubscription does
                    return hook.onSubscribeReturn(this, Subscriptions.empty());
                } else {
                    return hook.onSubscribeReturn(this, s);
                }
            } else {
                AtomicObservableSubscription subscription = new AtomicObservableSubscription();
                subscription.wrap(onSubscribeFunction.call(new AtomicObserver<T>(subscription, observer)));
                return hook.onSubscribeReturn(this, subscription);
            }
        } catch (OnErrorNotImplementedException e) {
            // special handling when onError is not implemented ... we just rethrow
            throw e;
        } catch (Exception e) {
            // if an unhandled error occurs executing the onSubscribe we will propagate it
            try {
                observer.onError(hook.onSubscribeError(this, e));
            } catch (OnErrorNotImplementedException e2) {
                // special handling when onError is not implemented ... we just rethrow
                throw e2;
            } catch (Exception e2) {
                // if this happens it means the onError itself failed (perhaps an invalid function implementation)
                // so we are unable to propagate the error correctly and will just throw
                RuntimeException r = new RuntimeException("Error occurred attempting to subscribe [" + e.getMessage() + "] and then again while trying to pass to onError.", e2);
                hook.onSubscribeError(this, r);
                throw r;
            }
            return Subscriptions.empty();
        }
    }

    /**
     * An {@link Observer} must call an Observable's <code>subscribe</code> method in order to
     * receive items and notifications from the Observable. A typical implementation of
     * <code>subscribe</code> method does the following:
     * <p>
     * It stores a reference to the Observer in a collection object, such as a
     * <code>List&lt;T&gt;</code> object.
     * <p>
     * It returns a reference to the {@link Subscription} interface. This enables Observers to
     * unsubscribe, that is, to stop receiving items and notifications before the Observable stops
     * sending them, which also invokes the Observer's {@link Observer#onCompleted onCompleted}
     * method.
     * <p>
     * An <code>Observable&lt;T&gt;</code> instance is responsible for accepting all subscriptions
     * and notifying all Observers. Unless the documentation for a particular
     * <code>Observable&lt;T&gt;</code> implementation indicates otherwise, Observers should make no
     * assumptions about the order in which multiple Observers will receive their notifications.
     * <p>
     * For more information see the
     * <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * @param observer
     * @param scheduler
     *            the {@link Scheduler} on which Observers subscribe to the Observable
     * @return a {@link Subscription} reference with which Observers can stop receiving items and
     *         notifications before the Observable has finished sending them
     * @throws IllegalArgumentException
     *             if an argument to <code>subscribe()</code> is <code>null</code>
     */
    public Subscription subscribe(Observer<T> observer, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(observer);
    }

    /**
     * Used for protecting against errors being thrown from Observer implementations and ensuring onNext/onError/onCompleted contract compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
     */
    private Subscription protectivelyWrapAndSubscribe(Observer<T> o) {
        AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        return subscription.wrap(subscribe(new AtomicObserver<T>(subscription, o)));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Map<String, Object> callbacks) {
        if (callbacks == null) {
            throw new RuntimeException("callbacks map can not be null");
        }
        Object _onNext = callbacks.get("onNext");
        if (_onNext == null) {
            throw new RuntimeException("'onNext' key must contain an implementation");
        }
        // lookup and memoize onNext
        final FuncN onNext = Functions.from(_onNext);

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer() {

            @Override
            public void onCompleted() {
                Object onComplete = callbacks.get("onCompleted");
                if (onComplete != null) {
                    Functions.from(onComplete).call();
                }
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                Object onError = callbacks.get("onError");
                if (onError != null) {
                    Functions.from(onError).call(e);
                } else {
                    throw new OnErrorNotImplementedException(e);
                }
            }

            @Override
            public void onNext(Object args) {
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Map<String, Object> callbacks, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(callbacks);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object o) {
        if (o instanceof Observer) {
            // in case a dynamic language is not correctly handling the overloaded methods and we receive an Observer just forward to the correct method.
            return subscribe((Observer) o);
        }

        if (o == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        // lookup and memoize onNext
        final FuncN onNext = Functions.from(o);

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(Object args) {
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Object o, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(o);
    }

    public Subscription subscribe(final Action1<T> onNext) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        // lookup and memoize onNext
        final FuncN onNextFunction = Functions.from(onNext);

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                Functions.from(onError).call(e);
            }

            @Override
            public void onNext(Object args) {
                onNextFunction.call(args);
            }

        });
    }

    public Subscription subscribe(final Object onNext, final Object onError, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError);
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer<T>() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                onError.call(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError, final Object onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        // lookup and memoize onNext        
        final FuncN onNextFunction = Functions.from(onNext);

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer() {

            @Override
            public void onCompleted() {
                Functions.from(onComplete).call();
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                Functions.from(onError).call(e);
            }

            @Override
            public void onNext(Object args) {
                onNextFunction.call(args);
            }

        });
    }

    public Subscription subscribe(final Object onNext, final Object onError, final Object onComplete, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError, onComplete);
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, final Action0 onComplete) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }
        if (onComplete == null) {
            throw new IllegalArgumentException("onComplete can not be null");
        }

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer<T>() {

            @Override
            public void onCompleted() {
                onComplete.call();
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                onError.call(e);
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, final Action0 onComplete, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError, onComplete);
    }

    /**
     * Returns a {@link ConnectableObservable} that upon connection causes the source Observable to
     * push results into the specified subject.
     * 
     * @param subject
     *            the {@link Subject} for the {@link ConnectableObservable} to push source items
     *            into
     * @param <R>
     *            result type
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         push results into the specified {@link Subject}
     */
    public <R> ConnectableObservable<R> multicast(Subject<T, R> subject) {
        return multicast(this, subject);
    }

    /**
     * Allow the {@link RxJavaErrorHandler} to receive the exception from onError.
     * 
     * @param e
     */
    private void handleError(Exception e) {
        // onError should be rare so we'll only fetch when needed
        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
    }

    /**
     * An Observable that never sends any information to an {@link Observer}.
     * 
     * This Observable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of item emitted by the Observable
     */
    private static class NeverObservable<T> extends Observable<T> {
        public NeverObservable() {
            super(new Func1<Observer<T>, Subscription>() {

                @Override
                public Subscription call(Observer<T> t1) {
                    return Subscriptions.empty();
                }

            });
        }
    }

    /**
     * an Observable that invokes {@link Observer#onError onError} when the {@link Observer}
     * subscribes to it.
     * 
     * @param <T>
     *            the type of item emitted by the Observable
     */
    private static class ThrowObservable<T> extends Observable<T> {

        public ThrowObservable(final Exception exception) {
            super(new Func1<Observer<T>, Subscription>() {

                /**
                 * Accepts an {@link Observer} and calls its {@link Observer#onError onError} method.
                 * 
                 * @param observer
                 *            an {@link Observer} of this Observable
                 * @return a reference to the subscription
                 */
                @Override
                public Subscription call(Observer<T> observer) {
                    observer.onError(exception);
                    return Subscriptions.empty();
                }

            });
        }

    }

    /**
     * Creates an Observable that will execute the given function when an {@link Observer}
     * subscribes to it.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/create.png">
     * <p>
     * Write the function you pass to <code>create</code> so that it behaves as an Observable: It
     * should invoke the Observer's {@link Observer#onNext onNext},
     * {@link Observer#onError onError}, and {@link Observer#onCompleted onCompleted} methods
     * appropriately.
     * <p>
     * A well-formed Observable must invoke either the Observer's <code>onCompleted</code> method
     * exactly once or its <code>onError</code> method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
     * for detailed information.
     * 
     * @param <T>
     *            the type of the items that this Observable emits
     * @param func
     *            a function that accepts an <code>Observer&lt;T&gt;</code>, invokes its
     *            <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a {@link Subscription} to allow the Observer to
     *            canceling the subscription
     * @return an Observable that, when an {@link Observer} subscribes to it, will execute the given
     *         function
     */
    public static <T> Observable<T> create(Func1<Observer<T>, Subscription> func) {
        return new Observable<T>(func);
    }

    /**
     * Creates an Observable that will execute the given function when an {@link Observer}
     * subscribes to it.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/create.png">
     * <p>
     * This method accepts {@link Object} to allow different languages to pass in methods using
     * {@link FunctionLanguageAdaptor}.
     * <p>
     * Write the function you pass to <code>create</code> so that it behaves as an Observable: It
     * should invoke the Observer's {@link Observer#onNext onNext},
     * {@link Observer#onError onError}, and {@link Observer#onCompleted onCompleted} methods
     * appropriately.
     * <p>
     * A well-formed Observable must invoke either the Observer's <code>onCompleted</code> method
     * exactly once or its <code>onError</code> method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a>
     * for detailed information.
     * 
     * @param <T>
     *            the type of the items that this Observable emits
     * @param func
     *            a function that accepts an <code>Observer&lt;T&gt;</code>, invokes its
     *            <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a {@link Subscription} that allows the Observer to
     *            cancel the subscription
     * @return an Observable that, when an {@link Observer} subscribes to it, will execute the given
     *         function
     */
    public static <T> Observable<T> create(final Object func) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(func);
        return create(new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> t1) {
                return (Subscription) _f.call(t1);
            }

        });
    }

    /**
     * Returns an Observable that emits no data to the {@link Observer} and immediately invokes
     * its {@link Observer#onCompleted onCompleted} method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.png">
     * 
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that returns no data to the {@link Observer} and immediately invokes
     *         the {@link Observer}'s {@link Observer#onCompleted() onCompleted} method
     */
    public static <T> Observable<T> empty() {
        return toObservable(new ArrayList<T>());
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError}
     * method when the Observer subscribes to it
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/error.png">
     * 
     * @param exception
     *            the particular error to report
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that invokes the {@link Observer}'s
     *         {@link Observer#onError onError} method when the Observer subscribes to it
     */
    public static <T> Observable<T> error(Exception exception) {
        return new ThrowObservable<T>(exception);
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a function that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
     * @return an Observable that emits only those items emitted by the source Observable that the
     *         filter evaluates as <code>true</code>
     */
    public static <T> Observable<T> filter(Observable<T> that, Func1<T, Boolean> predicate) {
        return create(OperationFilter.filter(that, predicate));
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param that
     *            the Observable to filter
     * @param function
     *            a function that evaluates an item emitted by the source Observable, and
     *            returns <code>true</code> if it passes the filter
     * @return an Observable that emits only those items emitted by the source Observable that the
     *         filter evaluates as <code>true</code>
     */
    public static <T> Observable<T> filter(Observable<T> that, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
        return filter(that, new Func1<T, Boolean>() {

            @Override
            public Boolean call(T t1) {
                return (Boolean) _f.call(t1);

            }

        });
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/where.png">
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a function that evaluates an item emitted by the source Observable, and
     *            returns <code>true</code> if it passes the filter
     * @return an Observable that emits only those items emitted by the source Observable that the
     *         filter evaluates as <code>true</code>
     */
    public static <T> Observable<T> where(Observable<T> that, Func1<T, Boolean> predicate) {
        return create(OperationWhere.where(that, predicate));
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.png">
     * 
     * @param iterable
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type of items to be
     *            emitted by the resulting Observable
     * @return an Observable that emits each item in the source {@link Iterable} sequence
     * @see #toObservable(Iterable)
     */
    public static <T> Observable<T> from(Iterable<T> iterable) {
        return toObservable(iterable);
    }

    /**
     * Converts an Array into an Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.png">
     * 
     * @param items
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items to be emitted by the
     *            resulting Observable
     * @return an Observable that emits each item in the source Array
     * @see #toObservable(Object...)
     */
    public static <T> Observable<T> from(T... items) {
        return toObservable(items);
    }

    /**
     * Generates an Observable that emits a sequence of integers within a specified range.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/range.png">
     * 
     * @param start
     *            the value of the first integer in the sequence
     * @param count
     *            the number of sequential integers to generate
     * 
     * @return an Observable that emits a range of sequential integers
     * 
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229460(v=vs.103).aspx">Observable.Range Method (Int32, Int32)</a>
     */
    public static Observable<Integer> range(int start, int count) {
        return from(Range.createWithCount(start, count));
    }

    /**
     * Asynchronously subscribes and unsubscribes Observers on the specified {@link Scheduler}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
     * 
     * @param source
     *            the source Observable
     * @param scheduler
     *            the {@link Scheduler} to perform subscription and unsubscription actions on
     * @param <T>
     *            the type of the items emitted by the Observable
     * @return the source Observable modified so that its subscriptions and unsubscriptions happen
     *         on the specified {@link Scheduler}
     */
    public static <T> Observable<T> subscribeOn(Observable<T> source, Scheduler scheduler) {
        return create(OperationSubscribeOn.subscribeOn(source, scheduler));
    }

    /**
     * Asynchronously notify Observers on the specified {@link Scheduler}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
     * 
     * @param source
     *            the source Observable
     * @param scheduler
     *            the {@link Scheduler} to notify Observers on
     * @param <T>
     *            the type of the items emitted by the Observable
     * @return the source Observable modified so that its Observers are notified on the specified
     *         {@link Scheduler}
     */
    public static <T> Observable<T> observeOn(Observable<T> source, Scheduler scheduler) {
        return create(OperationObserveOn.observeOn(source, scheduler));
    }

    /**
     * Returns an Observable that calls an Observable factory to create its Observable for each
     * new Observer that subscribes.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
     * <p>
     * The defer operator allows you to defer or delay emitting items from an Observable until such
     * time as an Observer subscribes to the Observable. This allows an {@link Observer} to easily
     * obtain updates or a refreshed version of the sequence.
     * 
     * @param observableFactory
     *            the Observable factory function to invoke for each {@link Observer} that
     *            subscribes to the resulting Observable
     * @param <T>
     *            the type of the items emitted by the Observable
     * @return an Observable whose {@link Observer}s trigger an invocation of the given Observable
     *         factory function
     */
    public static <T> Observable<T> defer(Func0<Observable<T>> observableFactory) {
        return create(OperationDefer.defer(observableFactory));
    }

    /**
     * Returns an Observable that calls an Observable factory to create its Observable for each
     * new Observer that subscribes.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/defer.png">
     * <p>
     * The defer operator allows you to defer or delay emitting items from an Observable
     * until such time as an {@link Observer} subscribes to the Observable. This allows an Observer
     * to easily obtain an updates or refreshed version of the sequence.
     * 
     * @param observableFactory
     *            the Observable factory function to invoke for each {@link Observer} that
     *            subscribes to the resulting Observable
     * @param <T>
     *            the type of the items emitted by the Observable
     * @return an Observable whose {@link Observer}s trigger an invocation of the given Observable
     *         factory function
     */
    public static <T> Observable<T> defer(Object observableFactory) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(observableFactory);

        return create(OperationDefer.defer(new Func0<Observable<T>>() {

            @Override
            @SuppressWarnings("unchecked")
            public Observable<T> call() {
                return (Observable<T>) _f.call();
            }

        }));
    }

    /**
     * Returns an Observable that emits a single item and then completes.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/just.png">
     * <p>
     * To convert any object into an Observable that emits that object, pass that object into the
     * <code>just</code> method.
     * <p>
     * This is similar to the {@link #from(java.lang.Object[])} method, except that
     * <code>from()</code> will convert an {@link Iterable} object into an Observable that emits
     * each of the items in the Iterable, one at a time, while the <code>just()</code> method
     * converts an Iterable into an Observable that emits the entire Iterable as a single item.
     * 
     * @param value
     *            the item to pass to the {@link Observer}'s {@link Observer#onNext onNext} method
     * @param <T>
     *            the type of that item
     * @return an Observable that emits a single item and then completes
     */
    public static <T> Observable<T> just(T value) {
        List<T> list = new ArrayList<T>();
        list.add(value);

        return toObservable(list);
    }

    /**
     * Applies a function of your choosing to every item emitted by an Observable, and emits the
     * results of these transformations as its own Observable items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items to be emitted by the resulting Observable
     * @return an Observable that emits the items from the source Observable as transformed by the
     *         given function
     */
    public static <T, R> Observable<R> map(Observable<T> sequence, Func1<T, R> func) {
        return create(OperationMap.map(sequence, func));
    }

    /**
     * Applies a function of your choosing to every item emitted by an Observable, and emits the
     * results of these transformations as its own Observable items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items to be emitted by the resulting Observable
     * @return an Observable that emits the items from the source Observable as transformed by the
     *         given function
     */
    public static <T, R> Observable<R> map(Observable<T> sequence, final Object func) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(func);
        return map(sequence, new Func1<T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T t1) {
                return (R) _f.call(t1);
            }

        });
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function that, when applied to an item emitted by the source Observable,
     *            returns an Observable
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items emitted by the Observables that are returned from
     *            <code>func</code>
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation
     * @see #flatMap(Observable, Func1)
     */
    public static <T, R> Observable<R> mapMany(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return create(OperationMap.mapMany(sequence, func));
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function that, when applied to each item emitted by the source Observable,
     *            generates an Observable
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items emitted by the Observables that are returned from
     *            <code>func</code>
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation
     */
    public static <T, R> Observable<R> mapMany(Observable<T> sequence, final Object func) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(func);
        return mapMany(sequence, new Func1<T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T t1) {
                return (R) _f.call(t1);
            }

        });
    }

    /**
     * Turns all of the notifications from a source Observable into {@link Observer#onNext onNext}
     * emissions, and marks them with their original notification types within {@link Notification}
     * objects.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
     * 
     * @param sequence
     *            the Observable you want to materialize in this way
     * @return an Observable that emits items that are the result of materializing the
     *         notifications of the source Observable.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">MSDN: Observable.Materialize</a>
     */
    public static <T> Observable<Notification<T>> materialize(final Observable<T> sequence) {
        return create(OperationMaterialize.materialize(sequence));
    }

    /**
     * Reverses the effect of {@link #materialize materialize} by transforming the
     * {@link Notification} objects emitted by a source Observable into the items or notifications
     * they represent.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
     * 
     * @param sequence
     *            an Observable that emits {@link Notification} objects that represent the items and
     *            notifications emitted by an Observable
     * @return an Observable that emits the items and notifications embedded in the
     *         {@link Notification} objects emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">MSDN: Observable.Dematerialize</a>
     */
    public static <T> Observable<T> dematerialize(final Observable<Notification<T>> sequence) {
        return create(OperationDematerialize.dematerialize(sequence));
    }

    /**
     * Flattens a list of Observables into one Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine the items emitted by multiple Observables so that they act like a single
     * Observable, by using the <code>merge</code> method.
     * 
     * @param source
     *            a list of Observables
     * @return an Observable that emits items that are the result of flattening the
     *         <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge</a>
     */
    public static <T> Observable<T> merge(List<Observable<T>> source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Flattens a sequence of Observables emitted by an Observable into one Observable, without any
     * transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine the items emitted by multiple Observables so that they act like a single
     * Observable, by using the <code>merge</code> method.
     * 
     * @param source
     *            an Observable that emits Observables
     * @return an Observable that emits items that are the result of flattening the items emitted
     *         by the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<Observable<T>> source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Flattens a series of Observables into one Observable, without any transformation.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * <p>
     * You can combine items emitted by multiple Observables so that they act like a single
     * Observable, by using the <code>merge</code> method.
     * 
     * @param source
     *            a series of Observables
     * @return an Observable that emits items that are the result of flattening the items emitted
     *         by the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<T>... source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Returns an Observable that emits the items from the <code>source</code> Observable until
     * the <code>other</code> Observable emits an item.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
     * 
     * @param source
     *            the source Observable
     * @param other
     *            the Observable whose first emitted item will cause <code>takeUntil</code> to stop
     *            emitting items from the <code>source</code> Observable
     * @param <T>
     *            the type of items emitted by <code>source</code>
     * @param <E>
     *            the type of items emitted by <code>other</code>
     * @return an Observable that emits the items emitted by <code>source</code> until such time as
     *         <code>other</code> emits its first item
     */
    public static <T, E> Observable<T> takeUntil(final Observable<T> source, final Observable<E> other) {
        return OperationTakeUntil.takeUntil(source, other);
    }

    /**
     * Returns an Observable that emits the items emitted by two or more Observables, one after the
     * other.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/concat.png">
     * 
     * @param source
     *            a series of Observables
     * @return an Observable that emits items that are the result of combining the items emitted by
     *         the <code>source</code> Observables, one after the other
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.concat(v=vs.103).aspx">MSDN: Observable.Concat Method</a>
     */
    public static <T> Observable<T> concat(Observable<T>... source) {
        return create(OperationConcat.concat(source));
    }

    /**
     * Returns an Observable that emits the same items as the source Observable, and then calls
     * the given Action after the Observable completes.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/finallyDo.png">
     * 
     * @param source
     *            an Observable
     * @param action
     *            an {@link Action0} to be invoked when the <code>source</code> Observable completes
     *            or errors
     * @return an Observable that emits the same items as the source, then invokes the action
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx">MSDN:
     *      Observable.Finally Method</a>
     */
    public static <T> Observable<T> finallyDo(Observable<T> source, Action0 action) {
        return create(OperationFinally.finallyDo(source, action));
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function that, when applied to each item emitted by the source Observable,
     *            generates an Observable
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items emitted by the Observables that are returned from
     *            <code>func</code>
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation
     * @see #mapMany(Observable, Func1)
     */
    public static <T, R> Observable<R> flatMap(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return mapMany(sequence, func);
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function that, when applied to each item emitted by the source Observable,
     *            generates an Observable
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items emitted by the Observables that are returned from
     *            <code>func</code>
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation
     * @see #mapMany(Observable, Func1)
     */
    public static <T, R> Observable<R> flatMap(Observable<T> sequence, final Object func) {
        return mapMany(sequence, func);
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param source
     *            an Observable whose items you want to group
     * @param keySelector
     *            a function that extracts the key for each item omitted by the source Observable
     * @param elementSelector
     *            a function to map each item emitted by the source Observable to an item emitted
     *            by a {@link GroupedObservable}
     * @param <K>
     *            the key type
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items to be emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public static <K, T, R> Observable<GroupedObservable<K, R>> groupBy(Observable<T> source, final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {
        return create(OperationGroupBy.groupBy(source, keySelector, elementSelector));
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param source
     *            an Observable whose items you want to group
     * @param keySelector
     *            a function that extracts the key for each item omitted by the source Observable
     * @param elementSelector
     *            a function to map each item emitted by the source Observable to an item emitted
     *            by a {@link GroupedObservable}
     * @param <K>
     *            the key type
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of items to be emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    @SuppressWarnings("rawtypes")
    public static <K, T, R> Observable<GroupedObservable<K, R>> groupBy(Observable<T> source, final Object keySelector, final Object elementSelector) {
        final FuncN _k = Functions.from(keySelector);
        final FuncN _e = Functions.from(elementSelector);

        return groupBy(source, new Func1<T, K>() {

            @SuppressWarnings("unchecked")
            @Override
            public K call(T t1) {
                return (K) _k.call(t1);
            }
        }, new Func1<T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T t1) {
                return (R) _e.call(t1);
            }
        });
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param source
     *            an Observable whose items you want to group
     * @param keySelector
     *            a function that extracts the key for each item emitted by the source Observable
     * @param <K>
     *            the key type
     * @param <T>
     *            the type of items to be emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public static <K, T> Observable<GroupedObservable<K, T>> groupBy(Observable<T> source, final Func1<T, K> keySelector) {
        return create(OperationGroupBy.groupBy(source, keySelector));
    }
    
    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param source
     *            an Observable whose items you want to group
     * @param keySelector
     *            a function that extracts the key for each item emitted by the source Observable
     * @param <K>
     *            the key type
     * @param <T>
     *            the type of items to be emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Observable<GroupedObservable<K, T>> groupBy(Observable<T> source, final Object keySelector) {
        final FuncN _k = Functions.from(keySelector);

        return groupBy(source, new Func1<T, K>() {

            @SuppressWarnings("unchecked")
            @Override
            public K call(T t1) {
                return (K) _k.call(t1);
            }
        });
    }

    /**
     * This behaves like {@link #merge(java.util.List)} except that if any of the merged Observables
     * notify of an error via {@link Observer#onError onError}, <code>mergeDelayError</code> will
     * refrain from propagating that error notification until all of the merged Observables have
     * finished emitting items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * <p>
     * Even if multiple merged Observables send <code>onError</code> notifications,
     * <code>mergeDelayError</code> will only invoke the <code>onError</code> method of its
     * Observers once.
     * <p>
     * This method allows an Observer to receive all successfully emitted items from all of the
     * source Observables without being interrupted by an error notification from one of them.
     * 
     * @param source
     *            a list of Observables
     * @return an Observable that emits items that are the result of flattening the items emitted by
     *         the <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(List<Observable<T>> source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * This behaves like {@link #merge(Observable)} except that if any of the merged Observables
     * notify of an error via {@link Observer#onError onError}, <code>mergeDelayError</code> will
     * refrain from propagating that error notification until all of the merged Observables have
     * finished emitting items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * <p>
     * Even if multiple merged Observables send <code>onError</code> notifications,
     * <code>mergeDelayError</code> will only invoke the <code>onError</code> method of its
     * Observers once.
     * <p>
     * This method allows an Observer to receive all successfully emitted items from all of the
     * source Observables without being interrupted by an error notification from one of them.
     * 
     * @param source
     *            an Observable that emits Observables
     * @return an Observable that emits items that are the result of flattening the items emitted by
     *         the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<Observable<T>> source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * This behaves like {@link #merge(Observable...)} except that if any of the merged Observables
     * notify of an error via {@link Observer#onError onError}, <code>mergeDelayError</code> will
     * refrain from propagating that error notification until all of the merged Observables have
     * finished emitting items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * <p>
     * Even if multiple merged Observables send <code>onError</code> notifications,
     * <code>mergeDelayError</code> will only invoke the <code>onError</code> method of its
     * Observers once.
     * <p>
     * This method allows an Observer to receive all successfully emitted items from all of the
     * source Observables without being interrupted by an error notification from one of them.
     * 
     * @param source
     *            a series of Observables
     * @return an Observable that emits items that are the result of flattening the items emitted by
     *         the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<T>... source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Returns an Observable that never sends any items or notifications to an {@link Observer}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/never.png">
     * <p> 
     * This Observable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of items (not) emitted by the Observable
     * @return an Observable that never sends any items or notifications to an {@link Observer}
     */
    public static <T> Observable<T> never() {
        return new NeverObservable<T>();
    }

    /**
     * Instruct an Observable to pass control to another Observable (the return value of a function)
     * rather than invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its Observer, the Observable invokes its {@link Observer}'s
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that returns an Observable (<code>resumeFunction</code>) to
     * <code>onErrorResumeNext</code>, if the source Observable encounters an error, instead of
     * invoking its Observer's <code>onError</code> function, it will instead relinquish control to
     * this new Observable, which will invoke the Observer's {@link Observer#onNext onNext} method
     * if it is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a function that returns an Observable that will take over if the source Observable
     *            encounters an error
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorResumeNext(final Observable<T> that, final Func1<Exception, Observable<T>> resumeFunction) {
        return create(OperationOnErrorResumeNextViaFunction.onErrorResumeNextViaFunction(that, resumeFunction));
    }

    /**
     * Instruct an Observable to pass control to another Observable (the return value of a function)
     * rather than invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its Observer, the Observable invokes its {@link Observer}'s
     * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that returns an Observable (<code>resumeFunction</code>) to
     * <code>onErrorResumeNext</code>, if the source Observable encounters an error, instead of
     * invoking its Observer's <code>onError</code> function, it will instead relinquish control to
     * this new Observable, which will invoke the Observer's {@link Observer#onNext onNext} method
     * if it is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a function that returns an Observable that will take over if the source Observable
     *            encounters an error
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorResumeNext(final Observable<T> that, final Object resumeFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(resumeFunction);
        return onErrorResumeNext(that, new Func1<Exception, Observable<T>>() {

            @SuppressWarnings("unchecked")
            @Override
            public Observable<T> call(Exception e) {
                return (Observable<T>) _f.call(e);
            }
        });
    }

    /**
     * Instruct an Observable to pass control to another Observable rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its Observer, the Observable invokes its {@link Observer}'s
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass an
     * Observable (<code>resumeSequence</code>) to <code>onErrorResumeNext</code>, if the original
     * Observable encounters an error, instead of invoking its Observer's <code>onError</code>
     * method, it will instead relinquish control to this new Observable, which will invoke the
     * Observer's {@link Observer#onNext onNext} method if it is able to do so. In such a case,
     * because no Observable necessarily invokes <code>onError</code>, the Observer may never know
     * that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param that
     *            the source Observable
     * @param resumeSequence
     *            a function that returns an Observable that will take over if the source Observable
     *            encounters an error
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorResumeNext(final Observable<T> that, final Observable<T> resumeSequence) {
        return create(OperationOnErrorResumeNextViaObservable.onErrorResumeNextViaObservable(that, resumeSequence));
    }

    /**
     * Instruct an Observable to emit a particular item to its Observer's <code>onNext</code>
     * function rather than invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to <code>onErrorReturn</code>, if the source Observable
     * encounters an error, instead of invoking its Observer's <code>onError</code> method, it will
     * instead pass the return value of <code>resumeFunction</code> to the Observer's
     * {@link Observer#onNext onNext} method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a function that returns an item that will be passed into an {@link Observer}'s
     *            {@link Observer#onNext onNext} method if the Observable encounters an error that
     *            would otherwise cause it to invoke {@link Observer#onError onError}
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorReturn(final Observable<T> that, Func1<Exception, T> resumeFunction) {
        return create(OperationOnErrorReturn.onErrorReturn(that, resumeFunction));
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the underlying
     * Observable that will replay all of its items and notifications to any future
     * {@link Observer}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.png">
     * @param that
     *            the source Observable
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         emit items to its {@link Observer}s
     */
    public static <T> ConnectableObservable<T> replay(final Observable<T> that) {
        return OperationMulticast.multicast(that, ReplaySubject.<T> create());
    }

    /**
     * This method has similar behavior to {@link #replay} except that this auto-subscribes to
     * the source Observable rather than returning a {@link ConnectableObservable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
     * <p>
     * This is useful when you want an Observable to cache responses and you can't control the
     * subscribe/unsubscribe behavior of all the {@link Observer}s.
     * <p>
     * NOTE: You sacrifice the ability to unsubscribe from the origin when you use the
     * <code>cache()</code> operator so be careful not to use this operator on Observables that
     * emit an infinite or very large number of items that will use up memory.
     * 
     * @return an Observable that when first subscribed to, caches all of the items and
     *         notifications it emits so it can replay them for subsequent subscribers.
     */
    public static <T> Observable<T> cache(final Observable<T> that) {
        return create(OperationCache.cache(that));
    }

    /**
     * Returns a {@link ConnectableObservable}, which waits until its
     * {@link ConnectableObservable#connect} method is called before it begins emitting items to
     * those {@link Observer}s that have subscribed to it.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
     * 
     * @param that
     *            the source Observable
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         emit items to its {@link Observer}
     */
    public static <T> ConnectableObservable<T> publish(final Observable<T> that) {
        return OperationMulticast.multicast(that, PublishSubject.<T> create());
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, and emits the final result from the final call to your function as
     * its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     * 
     * @param <T>
     *            the type of item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of applying the
     *         accumulator function to the sequence of items emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> Observable<T> reduce(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return takeLast(create(OperationScan.scan(sequence, accumulator)), 1);
    }

    /**
     * A version of <code>reduce()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @see #reduce(Observable, Func2)
     */
    public static <T> Observable<T> reduce(final Observable<T> sequence, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return reduce(sequence, new Func2<T, T, T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T call(T t1, T t2) {
                return (T) _f.call(t1, t2);
            }

        });
    }

    /**
     * Synonymous with <code>reduce()</code>
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregate.png">
     *
     * @see #reduce(Observable, Func2)
     */
    public static <T> Observable<T> aggregate(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return reduce(sequence, accumulator);
    }

    /**
     * A version of <code>aggregate()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregate.png">
     * 
     * @see #reduce(Observable, Func2)
     */
    public static <T> Observable<T> aggregate(Observable<T> sequence, Object accumulator) {
        return reduce(sequence, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, emitting the final result from the final call to your function as
     * its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     * 
     * @param <T>
     *            the type of item emitted by the source Observable
     * @param <R>
     *            the type returned by the accumulator function, and the type of the seed
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            a seed to pass in to the first execution of the accumulator function
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of applying the
     *         accumulator function to the sequence of items emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T, R> Observable<R> reduce(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return takeLast(create(OperationScan.scan(sequence, initialValue, accumulator)), 1);
    }

    /**
     * A version of <code>reduce()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * 
     * @see #reduce(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> reduce(final Observable<T> sequence, final R initialValue, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return reduce(sequence, initialValue, new Func2<R, T, R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R call(R r, T t) {
                return (R) _f.call(r, t);
            }
        });
    }

    /**
     * Synonymous with <code>reduce()</code>.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregateSeed.png">
     *
     * @see #reduce(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> aggregate(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return reduce(sequence, initialValue, accumulator);
    }

    /**
     * A version of <code>aggregate()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregateSeed.png">
     * 
     * @see #reduce(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> aggregate(Observable<T> sequence, R initialValue, Object accumulator) {
        return reduce(sequence, initialValue, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @param <T>
     *            the type of item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be emitted and used in the next accumulator
     *            call
     * @return an Observable that emits items that are the result of accumulating the items from
     *         the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> Observable<T> scan(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return create(OperationScan.scan(sequence, accumulator));
    }

    /**
     * A version of <code>scan()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @see #scan(Observable, Func2)
     */
    public static <T> Observable<T> scan(final Observable<T> sequence, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return scan(sequence, new Func2<T, T, T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T call(T t1, T t2) {
                return (T) _f.call(t1, t2);
            }

        });
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
     * <p>
     * Note that when you pass a seed to <code>scan()</code>, that seed will be the first item
     * emitted by the resulting Observable.
     * 
     * @param <T>
     *            the type of item emitted by the source Observable
     * @param <R>
     *            the type returned by the accumulator function, and the type of the seed
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be emitted and used in the next accumulator
     *            call
     * @return an Observable that emits items that are the result of accumulating the items emitted
     *         by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T, R> Observable<R> scan(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return create(OperationScan.scan(sequence, initialValue, accumulator));
    }

    /**
     * A version of <code>scan()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
     * 
     * @see #scan(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> scan(final Observable<T> sequence, final R initialValue, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return scan(sequence, initialValue, new Func2<R, T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(R r, T t) {
                return (R) _f.call(r, t);
            }
        });
    }

    /**
     * Returns an Observable that emits a Boolean that indicates whether all items emitted by a
     * source Observable satisfy a condition.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
     * 
     * @param sequence
     *            an Observable whose emitted items you are evaluating
     * @param predicate
     *            a function that evaluates each emitted item and returns a Boolean
     * @param <T>
     *            the type of items emitted by the source Observable
     * @return an Observable that emits <code>true</code> if all of the items emitted by the source
     *         Observable satisfy the predicate; otherwise, <code>false</code>
     */
    public static <T> Observable<Boolean> all(final Observable<T> sequence, final Func1<T, Boolean> predicate) {
        return create(OperationAll.all(sequence, predicate));
    }

    /**
     * Returns an Observable that emits a Boolean that indicates whether all items emitted by a
     * source Observable satisfy a condition.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
     * 
     * @param sequence
     *            an Observable whose emitted items you are evaluating
     * @param predicate
     *            a function that evaluates each emitted item and returns a Boolean
     * @param <T>
     *            the type of items emitted by the source Observable
     * @return an Observable that emits <code>true</code> if all items emitted by the source
     *         Observable satisfy the predicate; otherwise, <code>false</code>
     */
    public static <T> Observable<Boolean> all(final Observable<T> sequence, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return all(sequence, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    /**
     * Returns an Observable that skips the first <code>num</code> items emitted by the source
     * Observable and emits the remaining items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
     * <p>
     * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
     * those items that come after, by modifying the Observable with the <code>skip</code> method.
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items to skip
     * @return an Observable that emits the same items emitted by the source Observable, except for
     *         the first <code>num</code> items
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx">MSDN: Observable.Skip Method</a>
     */
    public static <T> Observable<T> skip(final Observable<T> items, int num) {
        return create(OperationSkip.skip(items, num));
    }

    /**
     * Accepts an Observable that emits Observables, and transforms it into a single Observable that
     * emits the items emitted by the most recently published of those Observables.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/switchDo.png">
     *  
     * @param sequenceOfSequences
     *            the source Observable that emits Observables
     * @return an Observable that emits only the items emitted by the most recently published
     *         Observable
     */
    public static <T> Observable<T> switchDo(Observable<Observable<T>> sequenceOfSequences) {
        return create(OperationSwitch.switchDo(sequenceOfSequences));
    }

    /**
     * Accepts an Observable and wraps it in another Observable that ensures that the resulting
     * Observable is chronologically well-behaved.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
     * <p>
     * A well-behaved Observable does not interleave its invocations of the
     * {@link Observer#onNext onNext}, {@link Observer#onCompleted onCompleted}, and
     * {@link Observer#onError onError} methods of its {@link Observer}s; it invokes
     * <code>onCompleted</code> or <code>onError</code> only once; and it never invokes
     * <code>onNext</code> after invoking either <code>onCompleted</code> or <code>onError</code>.
     * <code>synchronize</code> enforces this, and the Observable it returns invokes
     * <code>onNext</code> and <code>onCompleted</code> or <code>onError</code> synchronously.
     * 
     * @param observable
     *            the source Observable
     * @param <T>
     *            the type of item emitted by the source Observable
     * @return an Observable that is a chronologically well-behaved version of the source
     *         Observable, and that synchronously notifies its {@link Observer}s
     */
    public static <T> Observable<T> synchronize(Observable<T> observable) {
        return create(OperationSynchronize.synchronize(observable));
    }

    /**
     * Returns an Observable that emits the first <code>num</code> items emitted by the source
     * Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
     * <p>
     * This method returns an Observable that will invoke a subscribing {@link Observer}'s
     * {@link Observer#onNext onNext} method a maximum of <code>num</code> times before invoking
     * {@link Observer#onCompleted onCompleted}.
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items to emit from the start of the sequence emitted by the source
     *            Observable
     * @return an Observable that emits only the first <code>num</code> items emitted by the source
     *         Observable
     */
    public static <T> Observable<T> take(final Observable<T> items, final int num) {
        return create(OperationTake.take(items, num));
    }

    /**
     * Returns an Observable that emits the last <code>count</code> items emitted by the source
     * Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
     * 
     * @param items
     *            the source Observable
     * @param count
     *            the number of items to emit from the end of the sequence emitted by the source
     *            Observable
     * @return an Observable that emits only the last <code>count</code> items emitted by the source
     *         Observable
     */
    public static <T> Observable<T> takeLast(final Observable<T> items, final int count) {
        return create(OperationTakeLast.takeLast(items, count));
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate, operating on the items emitted, remains true.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
     * 
     * @param items
     *            the source Observable
     * @param predicate
     *            a function to test each item emitted by the source Observable for a condition
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public static <T> Observable<T> takeWhile(final Observable<T> items, Func1<T, Boolean> predicate) {
        return create(OperationTakeWhile.takeWhile(items, predicate));
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate, operating on the items emitted, remains true.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
     * 
     * @param items
     *            the source Observable
     * @param predicate
     *            a function to test each item emitted by the source Observable for a condition
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public static <T> Observable<T> takeWhile(final Observable<T> items, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return takeWhile(items, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate remains true, where the predicate can operate on both the item and its index
     * relative to the complete sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhileWithIndex.png">
     * 
     * @param items
     *            the source Observable
     * @param predicate
     *            a function to test each item emitted by the source Observable for a condition;
     *            the second parameter of the function represents the index of the source item
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public static <T> Observable<T> takeWhileWithIndex(final Observable<T> items, Func2<T, Integer, Boolean> predicate) {
        return create(OperationTakeWhile.takeWhileWithIndex(items, predicate));
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate remains true, where the predicate can operate on both the item and its index
     * relative to the complete sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhileWithIndex.png">
     * 
     * @param items
     *            the source Observable
     * @param predicate
     *            a function to test each item emitted by the source Observable for a condition;
     *            the second parameter of the function represents the index of the source item
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public static <T> Observable<T> takeWhileWithIndex(final Observable<T> items, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return create(OperationTakeWhile.takeWhileWithIndex(items, new Func2<T, Integer, Boolean>()
        {
            @Override
            public Boolean call(T t, Integer integer)
            {
                return (Boolean) _f.call(t, integer);
            }
        }));
    }

    /**
     * Wraps each item emitted by a source Observable in a {@link Timestamped} object.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/timestamp.png">
     * 
     * @return an Observable that emits timestamped items from the source Observable
     */
    public Observable<Timestamped<T>> timestamp() {
        return create(OperationTimestamp.timestamp(this));
    }

    /**
     * Returns an Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
     * <p>
     * Normally, an Observable that emits multiple items will do so by invoking its
     * {@link Observer}'s {@link Observer#onNext onNext} method for each such item. You can change
     * this behavior, instructing the Observable to compose a List of all of the items and then to
     * invoke the Observer's <code>onNext</code> function once, passing it the entire list, by
     * calling <code>toList</code>.
     * <p>
     * Be careful not to use this operator on Observables that emit an infinite or very large
     * number of items, as you do not have the option to unsubscribe.
     * 
     * @param that
     *            the source Observable
     * @return an Observable that emits a single item: a <code>List</code> containing all of the
     *         items emitted by the source Observable
     */
    public static <T> Observable<List<T>> toList(final Observable<T> that) {
        return create(OperationToObservableList.toObservableList(that));
    }

    /**
     * Returns a {@link ConnectableObservable} that upon connection causes the source Observable to
     * emit items into the specified {@link Subject}.
     * 
     * @param source
     *            the source Observable whose emitted items will be pushed into the specified
     *            {@link Subject}
     * @param subject
     *            the {@link Subject} to push source items into
     * @param <T>
     *            the type of items emitted by the source Observable
     * @param <R>
     *            the type of the {@link Subject}
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         push items into the specified {@link Subject}
     */
    public static <T, R> ConnectableObservable<R> multicast(Observable<T> source, final Subject<T, R> subject) {
        return OperationMulticast.multicast(source, subject);
    }

    /**
     * Converts an {@link Iterable} sequence into an Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toObservable.png">
     * <p>
     * You can convert any object that supports the Iterable interface into an Observable that
     * emits each item in the Iterable, by passing the Iterable into the <code>toObservable</code>
     * method.
     * 
     * @param iterable
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type of items to be
     *            emitted by the resulting Observable
     * @return an Observable that emits each item in the source {@link Iterable} sequence
     */
    public static <T> Observable<T> toObservable(Iterable<T> iterable) {
        return create(OperationToObservableIterable.toObservableIterable(iterable));
    }

    /**
     * Converts a {@link Future} into an Observable.
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of of object that the {@link Future} returns, and also the type of the
     *            item emitted by the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     * @deprecated Replaced by {@link #from(Future)}
     */
    public static <T> Observable<T> toObservable(Future<T> future) {
        return create(OperationToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts a {@link Future} into an Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.Future.png">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an Observable that
     * emits the return value of the {@link Future#get} method of that object, by passing the
     * object into the <code>from</code> method.
     * <p>
     * This Observable is blocking; you cannot unsubscribe from it.
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to
     *            be emitted by the resulting Observable
     * @return an Observable that emits the item from the source Future
     */
    public static <T> Observable<T> from(Future<T> future) {
        return create(OperationToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts a {@link Future} into an Observable.
     * 
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the {@link TimeUnit} of the time argument
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to
     *            be emitted by the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     * @deprecated Replaced by {@link #from(Future, long, TimeUnit)}
     */
    public static <T> Observable<T> toObservable(Future<T> future, long timeout, TimeUnit unit) {
        return create(OperationToObservableFuture.toObservableFuture(future, timeout, unit));
    }

    /**
     * Converts a {@link Future} into an Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/from.Future.png">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an Observable that
     * emits the return value of the {link Future#get} method of that object, by passing the
     * object into the <code>from</code> method.
     * <p>
     * This Observable is blocking; you cannot unsubscribe from it.
     * 
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling <code>get()</code>
     * @param unit
     *            the {@link TimeUnit} of the time argument
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to
     *            be emitted by the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     */
    public static <T> Observable<T> from(Future<T> future, long timeout, TimeUnit unit) {
        return create(OperationToObservableFuture.toObservableFuture(future, timeout, unit));
    }

    /**
     * Converts an array sequence into an Observable.
     * 
     * @param items
     *            the source array
     * @param <T>
     *            the type of items in the array, and also the type of items emitted by the
     *            resulting Observable
     * @return an Observable that emits each item in the source array
     * @deprecated Use {@link #from(Object...)}
     */
    public static <T> Observable<T> toObservable(T... items) {
        return toObservable(Arrays.asList(items));
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order (each item emitted by the source Observable must implement {@link Comparable} with
     * respect to all other items emitted by the source Observable).
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sequence
     *            the source Observable
     * @throws ClassCastException
     *             if any emitted item does not implement {@link Comparable} with respect to all
     *             other emitted items
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence) {
        return create(OperationToObservableSortedList.toSortedList(sequence));
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order based on a specified comparison function
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.f.png">
     * 
     * @param sequence
     *            the source Observable
     * @param sortFunction
     *            a function that compares two items emitted by the source Observable and returns
     *            an Integer that indicates their sort order
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, Func2<T, T, Integer> sortFunction) {
        return create(OperationToObservableSortedList.toSortedList(sequence, sortFunction));
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order based on a specified comparison function
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.f.png">
     * 
     * @param sequence
     *            the source Observable
     * @param sortFunction
     *            a function that compares two items emitted by the source Observable and returns
     *            an Integer that indicates their sort order
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, final Object sortFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(sortFunction);
        return create(OperationToObservableSortedList.toSortedList(sequence, new Func2<T, T, Integer>() {

            @Override
            public Integer call(T t1, T t2) {
                return (Integer) _f.call(t1, t2);
            }

        }));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to pairs
     * of items emitted, in sequence, by two other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code> and the first item emitted by <code>w1</code>; the second item emitted by
     * the new Observable will be the result of the function applied to the second item emitted by
     * <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param reduceFunction
     *            a function that, when applied to a pair of items, each emitted by one of the two
     *            source Observables, results in an item that will be emitted by the resulting
     *            Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> reduceFunction) {
        return create(OperationZip.zip(w0, w1, reduceFunction));
    }

    /**
     * Returns an Observable that emits Boolean values that indicate whether the pairs of items
     * emitted by two source Observables are equal.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sequenceEqual.png">
     * 
     * @param first
     *            one Observable to compare
     * @param second
     *            the second Observable to compare
     * @param <T>
     *            the type of items emitted by each Observable
     * @return an Observable that emits Booleans that indicate whether the corresponding items
     *         emitted by the source Observables are equal
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> first, Observable<T> second) {
        return sequenceEqual(first, second, new Func2<T, T, Boolean>() {
            @Override
            public Boolean call(T first, T second) {
                return first.equals(second);
            }
        });
    }

    /**
     * Returns an Observable that emits Boolean values that indicate whether the pairs of items
     * emitted by two source Observables are equal based on the results of a specified equality
     * function.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sequenceEqual.png">
     * 
     * @param first
     *            one Observable to compare
     * @param second
     *            the second Observable to compare
     * @param equality
     *            a function used to compare items emitted by both Observables
     * @param <T>
     *            the type of items emitted by each Observable
     * @return an Observable that emits Booleans that indicate whether the corresponding items
     *         emitted by the source Observables are equal
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> first, Observable<T> second, Func2<T, T, Boolean> equality) {
        return zip(first, second, equality);
    }

    /**
     * Returns an Observable that emits Boolean values that indicate whether the pairs of items
     * emitted by two source Observables are equal based on the results of a specified equality
     * function.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sequenceEqual.png">
     * 
     * @param first
     *            one Observable to compare
     * @param second
     *            the second Observable to compare
     * @param equality
     *            a function used to compare items emitted by both Observables
     * @param <T>
     *            the type of items emitted by each Observable
     * @return an Observable that emits Booleans that indicate whether the corresponding items
     *         emitted by the source Observables are equal
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> first, Observable<T> second, Object equality) {
        return zip(first, second, equality);
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to pairs
     * of items emitted, in sequence, by two other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code> and the first item emitted by <code>w1</code>; the second item emitted by
     * the new Observable will be the result of the function applied to the second item emitted by
     * <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param function
     *            a function that, when applied to a pair of items, each emitted by one of the two
     *            source Observables, results in an item that will be emitted by the resulting
     *            Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
        return zip(w0, w1, new Func2<T0, T1, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T0 t0, T1 t1) {
                return (R) _f.call(t0, t1);
            }

        });
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of three items emitted, in sequence, by three other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>, the first item emitted by <code>w1</code>, and the first item emitted by
     * <code>w2</code>; the second item emitted by the new Observable will be the result of the
     * function applied to the second item emitted by <code>w0</code>, the second item emitted by
     * <code>w1</code>, and the second item emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> function) {
        return create(OperationZip.zip(w0, w1, w2, function));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of three items emitted, in sequence, by three other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>, the first item emitted by <code>w1</code>, and the first item emitted by
     * <code>w2</code>; the second item emitted by the new Observable will be the result of the
     * function applied to the second item emitted by <code>w0</code>, the second item emitted by
     * <code>w1</code>, and the second item emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
        return zip(w0, w1, w2, new Func3<T0, T1, T2, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T0 t0, T1 t1, T2 t2) {
                return (R) _f.call(t0, t1, t2);
            }

        });
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>, the first item emitted by <code>w1</code>, the first item emitted by
     * <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by
     * the new Observable will be the result of the function applied to the second item emitted by
     * each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param w3
     *            a fourth source Observable
     * @param reduceFunction
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> reduceFunction) {
        return create(OperationZip.zip(w0, w1, w2, w3, reduceFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>, the first item emitted by <code>w1</code>, the first item emitted by
     * <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by
     * the new Observable will be the result of the function applied to the second item emitted by
     * each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable&lt;R&gt;</code> returned from <code>zip</code> will invoke
     * {@link Observer#onNext onNext} as many times as the number of <code>onNext</code> invocations
     * of the source Observable that emits the fewest items.
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param w3
     *            a fourth source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
        return zip(w0, w1, w2, w3, new Func4<T0, T1, T2, T3, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T0 t0, T1 t1, T2 t2, T3 t3) {
                return (R) _f.call(t0, t1, t2, t3);
            }

        });
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * all of the Observalbes; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will invoke
     * <code>onNext</code> as many times as the number of <code>onNext</code> invokations of the
     * source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param ws
     *            An Observable of source Observables
     * @param reduceFunction
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R> Observable<R> zip(Observable<Observable<?>> ws, final FuncN<R> reduceFunction) {
        return ws.toList().mapMany(new Func1<List<Observable<?>>, Observable<R>>() {
            @Override
            public Observable<R> call(List<Observable<?>> wsList) {
                return create(OperationZip.zip(wsList, reduceFunction));
            }
        });
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * all of the Observalbes; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will invoke
     * <code>onNext</code> as many times as the number of <code>onNext</code> invocations of the
     * source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param ws
     *            An Observable of source Observables
     * @param function
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R> Observable<R> zip(Observable<Observable<?>> ws, final Object function) {
        @SuppressWarnings({ "unchecked" })
        final FuncN<R> _f = Functions.from(function);
        return zip(ws, _f);
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * all of the Observalbes; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will invoke
     * <code>onNext</code> as many times as the number of <code>onNext</code> invokations of the
     * source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param ws
     *            A collection of source Observables
     * @param reduceFunction
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R> Observable<R> zip(Collection<Observable<?>> ws, FuncN<R> reduceFunction) {
        return create(OperationZip.zip(ws, reduceFunction));
    }

    /**
     * Returns an Observable that emits the results of a function of your choosing applied to
     * combinations of four items emitted, in sequence, by four other Observables.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the
     * new Observable will be the result of the function applied to the first item emitted by
     * all of the Observalbes; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will invoke
     * <code>onNext</code> as many times as the number of <code>onNext</code> invocations of the
     * source Observable that emits the fewest items.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param ws
     *            A collection of source Observables
     * @param function
     *            a function that, when applied to an item emitted by each of the source
     *            Observables, results in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R> Observable<R> zip(Collection<Observable<?>> ws, final Object function) {
        @SuppressWarnings({ "unchecked" })
        final FuncN<R> _f = Functions.from(function);
        return zip(ws, _f);
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param predicate
     *            a function that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
     * @return an Observable that emits only those items in the original Observable that the filter
     *         evaluates as <code>true</code>
     */
    public Observable<T> filter(Func1<T, Boolean> predicate) {
        return filter(this, predicate);
    }

    /**
     * Registers an {@link Action0} to be called when this Observable invokes
     * {@link Observer#onCompleted onCompleted} or {@link Observer#onError onError}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/finallyDo.png">
     * 
     * @param action
     *            an {@link Action0} to be invoked when the source Observable finishes
     * @return an Observable that emits the same items as the source Observable, then invokes the
     *         {@link Action0}
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx">MSDN: Observable.Finally Method</a>
     */
    public Observable<T> finallyDo(Action0 action) {
        return create(OperationFinally.finallyDo(this, action));
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param callback
     *            a function that evaluates an item emitted by the source Observable, returning
     *            <code>true</code> if it passes the filter
     * @return an Observable that emits only those items in the original Observable that the filter
     *         evaluates as "true"
     */
    public Observable<T> filter(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return filter(this, new Func1<T, Boolean>() {

            @Override
            public Boolean call(T t1) {
                return (Boolean) _f.call(t1);
            }
        });
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns
     *            an Observable
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation.
     * @see #mapMany(Func1)
     */
    public <R> Observable<R> flatMap(Func1<T, Observable<R>> func) {
        return mapMany(func);
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/flatMap.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param callback
     *            a function that, when applied to an item emitted by the source Observable, returns
     *            an Observable
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation.
     * @see #mapMany(Object)
     */
    public <R> Observable<R> flatMap(final Object callback) {
        return mapMany(callback);
    }

    /**
     * Filters an Observable by discarding any items it emits that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/where.png">
     * 
     * @param predicate
     *            a function that evaluates an item emitted by the source Observable, returning
     *            <code>true</code> if it passes the filter
     * @return an Observable that emits only those items in the original Observable that the filter
     *         evaluates as <code>true</code>
     */
    public Observable<T> where(Func1<T, Boolean> predicate) {
        return where(this, predicate);
    }

    /**
     * Applies a function of your choosing to each item emitted by an Observable, and returns this
     * transformation as a new Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param func
     *            a function to apply to each item emitted by the Observable
     * @return an Observable that emits the items from the source Observable, transformed by the
     *         given function
     */
    public <R> Observable<R> map(Func1<T, R> func) {
        return map(this, func);
    }

    /**
     * Applies a function of your choosing to each item emitted by an Observable, and returns this
     * transformation as a new Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param callback
     *            a function to apply to each item emitted by the Observable
     * @return an Observable that emits the items from the source Observable, transformed by the
     *         given function
     */
    public <R> Observable<R> map(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return map(this, new Func1<T, R>() {

            @Override
            @SuppressWarnings("unchecked")
            public R call(T t1) {
                return (R) _f.call(t1);
            }
        });
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns
     *            an Observable
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation.
     * @see #flatMap(Func1)
     */
    public <R> Observable<R> mapMany(Func1<T, Observable<R>> func) {
        return mapMany(this, func);
    }

    /**
     * Creates a new Observable by applying a function that you supply to each item emitted by
     * the source Observable, where that function returns an Observable, and then merging those
     * resulting Observables and emitting the results of this merger.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * <p>
     * Note: <code>mapMany</code> and <code>flatMap</code> are equivalent.
     * 
     * @param callback
     *            a function that, when applied to an item emitted by the source Observable, returns
     *            an Observable
     * @return an Observable that emits the result of applying the transformation function to each
     *         item emitted by the source Observable and merging the results of the Observables
     *         obtained from this transformation.
     * @see #flatMap(Object)
     */
    public <R> Observable<R> mapMany(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return mapMany(this, new Func1<T, Observable<R>>() {

            @Override
            @SuppressWarnings("unchecked")
            public Observable<R> call(T t1) {
                return (Observable<R>) _f.call(t1);
            }
        });
    }

    /**
     * Turns all of the notifications from a source Observable into {@link Observer#onNext onNext}
     * emissions, and marks them with their original notification types within {@link Notification}
     * objects.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
     * 
     * @return an Observable whose items are the result of materializing the items and
     *         notifications of the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">MSDN: Observable.materialize</a>
     */
    public Observable<Notification<T>> materialize() {
        return materialize(this);
    }

    /**
     * Asynchronously subscribes and unsubscribes Observers on the specified {@link Scheduler}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
     * 
     * @param scheduler
     *            the {@link Scheduler} to perform subscription and unsubscription actions on
     * @return the source Observable modified so that its subscriptions and unsubscriptions happen
     *         on the specified {@link Scheduler}
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return subscribeOn(this, scheduler);
    }

    /**
     * Asynchronously notify {@link Observer}s on the specified {@link Scheduler}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
     * 
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @return the source Observable modified so that its {@link Observer}s are notified on the
     *         specified {@link Scheduler}
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return observeOn(this, scheduler);
    }

    /**
     * Returns an Observable that reverses the effect of {@link #materialize materialize} by
     * transforming the {@link Notification} objects emitted by the source Observable into the items
     * or notifications they represent.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
     * 
     * @return an Observable that emits the items and notifications embedded in the
     *         {@link Notification} objects emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">MSDN: Observable.dematerialize</a>
     * @throws Exception
     *             if the source Observable is not of type {@code Observable<Notification<T>>}.
     */
    @SuppressWarnings("unchecked")
    public <T2> Observable<T2> dematerialize() {
        return dematerialize((Observable<Notification<T2>>) this);
    }

    /**
     * Instruct an Observable to pass control to another Observable rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that returns an Observable (<code>resumeFunction</code>) to
     * <code>onErrorResumeNext</code>, if the original Observable encounters an error, instead of
     * invoking its Observer's <code>onError</code> method, it will instead relinquish control to
     * the Observable returned from <code>resumeFunction</code>, which will invoke the Observer's
     * {@link Observer#onNext onNext} method if it is able to do so. In such a case, because no
     * Observable necessarily invokes <code>onError</code>, the Observer may never know that an
     * error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
     *            a function that returns an Observable that will take over if the source Observable
     *            encounters an error
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Func1<Exception, Observable<T>> resumeFunction) {
        return onErrorResumeNext(this, resumeFunction);
    }

    /**
     * Instruct an Observable to emit an item (returned by a specified function) rather than 
     * invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of invoking its Observer's
     * <code>onError</code> function, it will instead pass the return value of
     * <code>resumeFunction</code> to the Observer's {@link Observer#onNext onNext} method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
     *            a function that returns an item that the Observable will emit if the source
     *            Observable encounters an error
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Object resumeFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(resumeFunction);
        return onErrorResumeNext(this, new Func1<Exception, Observable<T>>() {

            @Override
            @SuppressWarnings("unchecked")
            public Observable<T> call(Exception e) {
                return (Observable<T>) _f.call(e);
            }
        });
    }

    /**
     * Instruct an Observable to pass control to another Observable rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorResumeNext</code> method changes this behavior. If you pass
     * another Observable (<code>resumeSequence</code>) to an Observable's
     * <code>onErrorResumeNext</code> method, if the original Observable encounters an error,
     * instead of invoking its Observer's <code>onError</code> method, it will instead relinquish
     * control to <code>resumeSequence</code> which will invoke the Observer's
     * {@link Observer#onNext onNext} method if it is able to do so. In such a case, because no
     * Observable necessarily invokes <code>onError</code>, the Observer may never know that an
     * error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeSequence
     *            a function that returns an Observable that will take over if the source Observable
     *            encounters an error
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Observable<T> resumeSequence) {
        return onErrorResumeNext(this, resumeSequence);
    }

    /**
     * Instruct an Observable to emit an item (returned by a specified function) rather than
     * invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of invoking its Observer's
     * <code>onError</code> method, it will instead pass the return value of
     * <code>resumeFunction</code> to the Observer's {@link Observer#onNext onNext} method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
     *            a function that returns an item that the new Observable will emit if the source
     *            Observable encounters an error
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorReturn(Func1<Exception, T> resumeFunction) {
        return onErrorReturn(this, resumeFunction);
    }

    /**
     * Instruct an Observable to emit a particular item rather than invoking
     * {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/onErrorReturn.png">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the
     * expected item to its {@link Observer}, the Observable invokes its Observer's
     * <code>onError</code> method, and then quits without invoking any more of its Observer's
     * methods. The <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of invoking its Observer's
     * <code>onError</code> function, it will instead pass the return value of
     * <code>resumeFunction</code> to the Observer's {@link Observer#onNext onNext} method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
     *            a function that returns an item that the new Observable will emit if the source
     *            Observable encounters an error
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorReturn(final Object resumeFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(resumeFunction);
        return onErrorReturn(this, new Func1<Exception, T>() {

            @Override
            @SuppressWarnings("unchecked")
            public T call(Exception e) {
                return (T) _f.call(e);
            }
        });
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, and emits the final result from the final call to your function as
     * its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     * 
     * @param accumulator
     *            An accumulator function to be invoked on each item emitted by the source
     *            Observable, whose result will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the
     *         output from the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public Observable<T> reduce(Func2<T, T, T> accumulator) {
        return reduce(this, accumulator);
    }

    /**
     * Returns a {@link ConnectableObservable} that shares a single subscription to the underlying
     * Observable that will replay all of its items and notifications to any future
     * {@link Observer}.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/replay.png">
     * 
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         emit items to its {@link Observer}s
     */
    public ConnectableObservable<T> replay() {
        return replay(this);
    }

    /**
     * This method has similar behavior to {@link #replay} except that this auto-subscribes to
     * the source Observable rather than returning a {@link ConnectableObservable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/cache.png">
     * <p>
     * This is useful when you want an Observable to cache responses and you can't control the
     * subscribe/unsubscribe behavior of all the {@link Observer}s.
     * <p>
     * NOTE: You sacrifice the ability to unsubscribe from the origin when you use the
     * <code>cache()</code> operator so be careful not to use this operator on Observables that
     * emit an infinite or very large number of items that will use up memory.
     * 
     * @return an Observable that when first subscribed to, caches all of its notifications for
     *         the benefit of subsequent subscribers.
     */
    public Observable<T> cache() {
        return cache(this);
    }

    /**
     * Returns a {@link ConnectableObservable}, which waits until its
     * {@link ConnectableObservable#connect connect} method is called before it begins emitting
     * items to those {@link Observer}s that have subscribed to it.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/publishConnect.png">
     * 
     * @return a {@link ConnectableObservable} that upon connection causes the source Observable to
     *         emit items to its {@link Observer}s 
     */
    public ConnectableObservable<T> publish() {
        return publish(this);
    }

    /**
     * A version of <code>reduce()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @see #reduce(Func2)
     */
    public Observable<T> reduce(Object accumulator) {
        return reduce(this, accumulator);
    }

    /**
     * Synonymous with <code>reduce()</code>.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregate.png">
     *
     * @see #reduce(Func2)
     */
    public Observable<T> aggregate(Func2<T, T, T> accumulator) {
        return aggregate(this, accumulator);
    }

    /**
     * A version of <code>aggregate()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregate.png">
     * 
     * @see #reduce(Func2)
     */
    public Observable<T> aggregate(Object accumulator) {
        return aggregate(this, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole
     * item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     * 
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the output
     *         from the items emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public <R> Observable<R> reduce(R initialValue, Func2<R, T, R> accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * A version of <code>reduce()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * 
     * @see #reduce(Object, Func2)
     */
    public <R> Observable<R> reduce(R initialValue, Object accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * Synonymous with <code>reduce()</code>.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregateSeed.png">
     *
     * @see #reduce(Object, Func2)
     */
    public <R> Observable<R> aggregate(R initialValue, Func2<R, T, R> accumulator) {
        return aggregate(this, initialValue, accumulator);
    }

    /**
     * A version of <code>aggregate()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/aggregateSeed.png">
     * 
     * @see #reduce(Object, Func2)
     */
    public <R> Observable<R> aggregate(R initialValue, Object accumulator) {
        return aggregate(this, initialValue, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that when you pass a seed to <code>scan()</code> the resulting Observable will emit
     * that seed as its first emitted item.
     * 
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, whose result will be emitted to {@link Observer}s via
     *            {@link Observer#onNext onNext} and used in the next accumulator call.
     * @return an Observable that emits the results of each call to the accumulator function
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(Func2<T, T, T> accumulator) {
        return scan(this, accumulator);
    }

    /**
     * Returns an Observable that emits the results of sampling the items emitted by the source
     * Observable at a specified time interval.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
     * 
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which <code>period</code> is defined
     * @return an Observable that emits the results of sampling the items emitted by the source
     *         Observable at the specified time interval
     */
    public Observable<T> sample(long period, TimeUnit unit) {
        return create(OperationSample.sample(this, period, unit));
    }

    /**
     * Returns an Observable that emits the results of sampling the items emitted by the source
     * Observable at a specified time interval.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/sample.png">
     * 
     * @param period
     *            the sampling rate
     * @param unit
     *            the {@link TimeUnit} in which <code>period</code> is defined
     * @param scheduler
     *            the {@link Scheduler} to use when sampling
     * @return an Observable that emits the results of sampling the items emitted by the source
     *         Observable at the specified time interval
     */
    public Observable<T> sample(long period, TimeUnit unit, Scheduler scheduler) {
        return create(OperationSample.sample(this, period, unit, scheduler));
    }

    /**
     * A version of <code>scan()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @see #scan(Func2)
     */
    public Observable<T> scan(final Object accumulator) {
        return scan(this, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
     * <p>
     * This sort of function is sometimes called an accumulator.
     * <p>
     * Note that when you pass a seed to <code>scan()</code> the resulting Observable will emit
     * that seed as its first emitted item.
     * 
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, whose result will be emitted to {@link Observer}s via
     *            {@link Observer#onNext onNext} and used in the next accumulator call.
     * @return an Observable that emits the results of each call to the accumulator function
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN:
     *      Observable.Scan</a>
     */
    public <R> Observable<R> scan(R initialValue, Func2<R, T, R> accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * A version of <code>scan()</code> for use by dynamic languages.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scanSeed.png">
     * 
     * @see #scan(Object, Func2)
     */
    public <R> Observable<R> scan(final R initialValue, final Object accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * Returns an Observable that emits a Boolean that indicates whether all of the items emitted by
     * the source Observable satisfy a condition.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
     * 
     * @param predicate
     *            a function that evaluates an item and returns a Boolean
     * @return an Observable that emits <code>true</code> if all items emitted by the source
     *         Observable satisfy the predicate; otherwise, <code>false</code>
     */
    public Observable<Boolean> all(Func1<T, Boolean> predicate) {
        return all(this, predicate);
    }

    /**
     * Returns an Observable that emits a Boolean that indicates whether all of the items emitted by
     * the source Observable satisfy a condition.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
     * 
     * @param predicate
     *            a function that evaluates an item and returns a Boolean
     * @return an Observable that emits <code>true</code> if all items emitted by the source
     *         Observable satisfy the predicate; otherwise, <code>false</code>
     */
    public Observable<Boolean> all(Object predicate) {
        return all(this, predicate);
    }

    /**
     * Returns an Observable that skips the first <code>num</code> items emitted by the source
     * Observable and emits the remainder.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
     * <p>
     * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
     * those items that come after, by modifying the Observable with the <code>skip</code> method.
     * 
     * @param num
     *            the number of items to skip
     * @return an Observable that is identical to the source Observable except that it does not
     *         emit the first <code>num</code> items that the source emits
     */
    public Observable<T> skip(int num) {
        return skip(this, num);
    }

    /**
     * Returns an Observable that emits only the first <code>num</code> items emitted by the source
     * Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
     * <p>
     * This method returns an Observable that will invoke a subscribing {@link Observer}'s
     * {@link Observer#onNext onNext} function a maximum of <code>num</code> times before invoking
     * {@link Observer#onCompleted onCompleted}.
     * 
     * @param num
     *            the number of items to take
     * @return an Observable that emits only the first <code>num</code> items from the source
     *         Observable, or all of the items from the source Observable if that Observable emits
     *         fewer than <code>num</code> items
     */
    public Observable<T> take(final int num) {
        return take(this, num);
    }

    /**
     * Returns an Observable that emits items emitted by the source Observable so long as a
     * specified condition is true.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
     * 
     * @param predicate
     *            a function that evaluates an item emitted by the source Observable and returns a
     *            Boolean
     * @return an Observable that emits the items from the source Observable so long as each item
     *         satisfies the condition defined by <code>predicate</code>
     */
    public Observable<T> takeWhile(final Func1<T, Boolean> predicate) {
        return takeWhile(this, predicate);
    }

    /**
     * Returns an Observable that emits items emitted by the source Observable so long as a
     * specified condition is true.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhile.png">
     * 
     * @param predicate
     *            a function that evaluates an item emitted by the source Observable and returns a
     *            Boolean
     * @return an Observable that emits the items from the source Observable so long as each item
     *         satisfies the condition defined by <code>predicate</code>
     */
    public Observable<T> takeWhile(final Object predicate) {
        return takeWhile(this, predicate);
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate remains true, where the predicate can operate on both the item and its index
     * relative to the complete sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhileWithIndex.png">
     * 
     * @param predicate
     *            a function to test each item emitted by the source Observable for a condition;
     *            the second parameter of the function represents the index of the source item
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public Observable<T> takeWhileWithIndex(final Func2<T, Integer, Boolean> predicate) {
        return takeWhileWithIndex(this, predicate);
    }

    /**
     * Returns an Observable that emits the items emitted by a source Observable so long as a given
     * predicate remains true, where the predicate can operate on both the item and its index
     * relative to the complete sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeWhileWithIndex.png">
     * 
     * @param predicate
     *            a function that evaluates an item emitted by the source Observable and returns a
     *            Boolean; the second parameter of the function represents the index of the source
     *            item
     * @return an Observable that emits items from the source Observable so long as the predicate
     *         continues to return <code>true</code> for each item, then completes
     */
    public Observable<T> takeWhileWithIndex(final Object predicate) {
        return takeWhileWithIndex(this, predicate);
    }

    /**
     * Returns an Observable that emits only the last <code>count</code> items emitted by the source
     * Observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
     * 
     * @param count
     *            the number of items to emit from the end of the sequence emitted by the source
     *            Observable
     * @return an Observable that emits only the last <code>count</code> items emitted by the source
     *         Observable
     */
    public Observable<T> takeLast(final int count) {
        return takeLast(this, count);
    }

    /**
     * Returns an Observable that emits the items from the source Observable only until the
     * <code>other</code> Observable emits an item.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
     * 
     * @param other
     *            the Observable whose first emitted item will cause <code>takeUntil</code> to stop
     *            emitting items from the source Observable
     * @param <E>
     *            the type of items emitted by <code>other</code>
     * @return an Observable that emits the items of the source Observable until such time as
     *         <code>other</code> emits its first item
     */
    public <E> Observable<T> takeUntil(Observable<E> other) {
        return takeUntil(this, other);
    }

    /**
     * Returns an Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
     * <p>
     * Normally, an Observable that returns multiple items will do so by invoking its
     * {@link Observer}'s {@link Observer#onNext onNext} method for each such item. You can change
     * this behavior, instructing the Observable to compose a list of all of these items and then to
     * invoke the Observer's <code>onNext</code> function once, passing it the entire list, by
     * calling the Observable's <code>toList</code> method prior to calling its {@link #subscribe}
     * method.
     * <p>
     * Be careful not to use this operator on Observables that emit infinite or very large numbers
     * of items, as you do not have the option to unsubscribe.
     * 
     * @return an Observable that emits a single item: a List containing all of the items emitted by
     *         the source Observable.
     */
    public Observable<List<T>> toList() {
        return toList(this);
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order (each item emitted by the Observable must implement {@link Comparable} with respect to
     * all other items in the sequence).
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @throws ClassCastException
     *             if any item emitted by the Observable does not implement {@link Comparable} with
     *             respect to all other items emitted by the Observable
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public Observable<List<T>> toSortedList() {
        return toSortedList(this);
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order based on a specified comparison function
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.f.png">
     * 
     * @param sortFunction
     *            a function that compares two items emitted by the source Observable and returns
     *            an Integer that indicates their sort order
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public Observable<List<T>> toSortedList(Func2<T, T, Integer> sortFunction) {
        return toSortedList(this, sortFunction);
    }

    /**
     * Return an Observable that emits the items emitted by the source Observable, in a sorted
     * order based on a specified comparison function
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.f.png">
     * 
     * @param sortFunction
     *            a function that compares two items emitted by the source Observable and returns
     *            an Integer that indicates their sort order
     * @return an Observable that emits the items from the source Observable in sorted order
     */
    public Observable<List<T>> toSortedList(final Object sortFunction) {
        return toSortedList(this, sortFunction);
    }

    /**
     * Emit a specified set of items before beginning to emit items from the source Observable.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/startWith.png">
     * 
     * @param values
     *            the items you want the modified Observable to emit first
     * @return an Observable that exhibits the modified behavior
     */
    @SuppressWarnings("unchecked")
    public Observable<T> startWith(T... values) {
        return concat(Observable.<T> from(values), this);
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param keySelector
     *            a function that extracts the key from an item
     * @param elementSelector
     *            a function to map a source item to an item in a {@link GroupedObservable}
     * @param <K>
     *            the key type
     * @param <R>
     *            the type of items emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public <K, R> Observable<GroupedObservable<K, R>> groupBy(final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {
        return groupBy(this, keySelector, elementSelector);
    }
    
    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param keySelector
     *            a function that extracts the key from an item
     * @param elementSelector
     *            a function to map a source item to an item in a {@link GroupedObservable}
     * @param <K>
     *            the key type
     * @param <R>
     *            the type of items emitted by the resulting {@link GroupedObservable}s
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public <K, R> Observable<GroupedObservable<K, R>> groupBy(final Object keySelector, final Object elementSelector) {
        return groupBy(this, keySelector, elementSelector);
    }

    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public <K> Observable<GroupedObservable<K, T>> groupBy(final Func1<T, K> keySelector) {
        return groupBy(this, keySelector);
    }
    
    /**
     * Groups the items emitted by an Observable according to a specified criterion, and emits these
     * grouped items as {@link GroupedObservable}s, one GroupedObservable per group.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/groupBy.png">
     * 
     * @param keySelector
     *            a function that extracts the key for each item
     * @param <K>
     *            the key type
     * @return an Observable that emits {@link GroupedObservable}s, each of which corresponds to a
     *         unique key value and emits items representing items from the source Observable that
     *         share that key value
     */
    public <K> Observable<GroupedObservable<K, T>> groupBy(final Object keySelector) {
        return groupBy(this, keySelector);
    }

    /**
     * Converts an Observable into a {@link BlockingObservable} (an Observable with blocking
     * operators).
     *
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking
     *      Observable Operators</a>
     */
    public BlockingObservable<T> toBlockingObservable() {
        return BlockingObservable.from(this);
    }

    /**
     * Whether a given {@link Function} is an internal implementation inside rx.* packages or not.
     * <p>
     * For why this is being used see https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
     * 
     * NOTE: If strong reasons for not depending on package names comes up then the implementation of this method can change to looking for a marker interface.
     * 
     * @param f
     * @return {@code true} if the given function is an internal implementation, and {@code false} otherwise.
     */
    private boolean isInternalImplementation(Object o) {
        if (o == null) {
            return true;
        }
        // prevent double-wrapping (yeah it happens)
        if (o instanceof AtomicObserver)
            return true;
        // we treat the following package as "internal" and don't wrap it
        Package p = o.getClass().getPackage(); // it can be null
        return p != null && p.getName().startsWith("rx.operators");
    }

    public static class UnitTest {

        @Mock
        Observer<Integer> w;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testCreate() {

            Observable<String> observable = create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> Observer) {
                    Observer.onNext("one");
                    Observer.onNext("two");
                    Observer.onNext("three");
                    Observer.onCompleted();
                    return Subscriptions.empty();
                }

            });

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testReduce() {
            Observable<Integer> Observable = toObservable(1, 2, 3, 4);
            reduce(Observable, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t1 + t2;
                }

            }).subscribe(w);
            // we should be called only once
            verify(w, times(1)).onNext(anyInt());
            verify(w).onNext(10);
        }

        @Test
        public void testReduceWithInitialValue() {
            Observable<Integer> Observable = toObservable(1, 2, 3, 4);
            reduce(Observable, 50, new Func2<Integer, Integer, Integer>() {

                @Override
                public Integer call(Integer t1, Integer t2) {
                    return t1 + t2;
                }

            }).subscribe(w);
            // we should be called only once
            verify(w, times(1)).onNext(anyInt());
            verify(w).onNext(60);
        }

        @Test
        public void testSequenceEqual() {
            Observable<Integer> first = toObservable(1, 2, 3);
            Observable<Integer> second = toObservable(1, 2, 4);
            @SuppressWarnings("unchecked")
            Observer<Boolean> result = mock(Observer.class);
            sequenceEqual(first, second).subscribe(result);
            verify(result, times(2)).onNext(true);
            verify(result, times(1)).onNext(false);
        }

        @Test
        public void testOnSubscribeFails() {
            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            final RuntimeException re = new RuntimeException("bad impl");
            Observable<String> o = Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> t1) {
                    throw re;
                }

            });
            o.subscribe(observer);
            verify(observer, times(0)).onNext(anyString());
            verify(observer, times(0)).onCompleted();
            verify(observer, times(1)).onError(re);
        }

        @Test
        public void testMaterializeDematerializeChaining() {
            Observable<Integer> obs = Observable.just(1);
            Observable<Integer> chained = obs.materialize().dematerialize();

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = mock(Observer.class);
            chained.subscribe(observer);

            verify(observer, times(1)).onNext(1);
            verify(observer, times(1)).onCompleted();
            verify(observer, times(0)).onError(any(Exception.class));
        }

        /**
         * The error from the user provided Observer is not handled by the subscribe method try/catch.
         * 
         * It is handled by the AtomicObserver that wraps the provided Observer.
         * 
         * Result: Passes (if AtomicObserver functionality exists)
         */
        @Test
        public void testCustomObservableWithErrorInObserverAsynchronous() throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicInteger count = new AtomicInteger();
            final AtomicReference<Exception> error = new AtomicReference<Exception>();
            Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(final Observer<String> observer) {
                    final BooleanSubscription s = new BooleanSubscription();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                if (!s.isUnsubscribed()) {
                                    observer.onNext("1");
                                    observer.onNext("2");
                                    observer.onNext("three");
                                    observer.onNext("4");
                                    observer.onCompleted();
                                }
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                    return s;
                }
            }).subscribe(new Observer<String>() {
                @Override
                public void onCompleted() {
                    System.out.println("completed");
                }

                @Override
                public void onError(Exception e) {
                    error.set(e);
                    System.out.println("error");
                    e.printStackTrace();
                }

                @Override
                public void onNext(String v) {
                    int num = Integer.parseInt(v);
                    System.out.println(num);
                    // doSomething(num);
                    count.incrementAndGet();
                }

            });

            // wait for async sequence to complete
            latch.await();

            assertEquals(2, count.get());
            assertNotNull(error.get());
            if (!(error.get() instanceof NumberFormatException)) {
                fail("It should be a NumberFormatException");
            }
        }

        /**
         * The error from the user provided Observer is handled by the subscribe try/catch because this is synchronous
         * 
         * Result: Passes
         */
        @Test
        public void testCustomObservableWithErrorInObserverSynchronous() {
            final AtomicInteger count = new AtomicInteger();
            final AtomicReference<Exception> error = new AtomicReference<Exception>();
            Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("1");
                    observer.onNext("2");
                    observer.onNext("three");
                    observer.onNext("4");
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            }).subscribe(new Observer<String>() {

                @Override
                public void onCompleted() {
                    System.out.println("completed");
                }

                @Override
                public void onError(Exception e) {
                    error.set(e);
                    System.out.println("error");
                    e.printStackTrace();
                }

                @Override
                public void onNext(String v) {
                    int num = Integer.parseInt(v);
                    System.out.println(num);
                    // doSomething(num);
                    count.incrementAndGet();
                }

            });
            assertEquals(2, count.get());
            assertNotNull(error.get());
            if (!(error.get() instanceof NumberFormatException)) {
                fail("It should be a NumberFormatException");
            }
        }

        /**
         * The error from the user provided Observable is handled by the subscribe try/catch because this is synchronous
         * 
         * 
         * Result: Passes
         */
        @Test
        public void testCustomObservableWithErrorInObservableSynchronous() {
            final AtomicInteger count = new AtomicInteger();
            final AtomicReference<Exception> error = new AtomicReference<Exception>();
            Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("1");
                    observer.onNext("2");
                    throw new NumberFormatException();
                }
            }).subscribe(new Observer<String>() {

                @Override
                public void onCompleted() {
                    System.out.println("completed");
                }

                @Override
                public void onError(Exception e) {
                    error.set(e);
                    System.out.println("error");
                    e.printStackTrace();
                }

                @Override
                public void onNext(String v) {
                    System.out.println(v);
                    count.incrementAndGet();
                }

            });
            assertEquals(2, count.get());
            assertNotNull(error.get());
            if (!(error.get() instanceof NumberFormatException)) {
                fail("It should be a NumberFormatException");
            }
        }

        @Test
        public void testPublish() throws InterruptedException {
            final AtomicInteger counter = new AtomicInteger();
            ConnectableObservable<String> o = Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(final Observer<String> observer) {
                    final BooleanSubscription subscription = new BooleanSubscription();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            observer.onNext("one");
                            observer.onCompleted();
                        }
                    }).start();
                    return subscription;
                }
            }).publish();

            final CountDownLatch latch = new CountDownLatch(2);

            // subscribe once
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            // subscribe again
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            Subscription s = o.connect();
            try {
                if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                    fail("subscriptions did not receive values");
                }
                assertEquals(1, counter.get());
            } finally {
                s.unsubscribe();
            }
        }

        @Test
        public void testReplay() throws InterruptedException {
            final AtomicInteger counter = new AtomicInteger();
            ConnectableObservable<String> o = Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(final Observer<String> observer) {
                    final BooleanSubscription subscription = new BooleanSubscription();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            observer.onNext("one");
                            observer.onCompleted();
                        }
                    }).start();
                    return subscription;
                }
            }).replay();

            // we connect immediately and it will emit the value
            Subscription s = o.connect();
            try {

                // we then expect the following 2 subscriptions to get that same value
                final CountDownLatch latch = new CountDownLatch(2);

                // subscribe once
                o.subscribe(new Action1<String>() {

                    @Override
                    public void call(String v) {
                        assertEquals("one", v);
                        latch.countDown();
                    }
                });

                // subscribe again
                o.subscribe(new Action1<String>() {

                    @Override
                    public void call(String v) {
                        assertEquals("one", v);
                        latch.countDown();
                    }
                });

                if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                    fail("subscriptions did not receive values");
                }
                assertEquals(1, counter.get());
            } finally {
                s.unsubscribe();
            }
        }

        @Test
        public void testCache() throws InterruptedException {
            final AtomicInteger counter = new AtomicInteger();
            Observable<String> o = Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(final Observer<String> observer) {
                    final BooleanSubscription subscription = new BooleanSubscription();
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            counter.incrementAndGet();
                            observer.onNext("one");
                            observer.onCompleted();
                        }
                    }).start();
                    return subscription;
                }
            }).cache();

            // we then expect the following 2 subscriptions to get that same value
            final CountDownLatch latch = new CountDownLatch(2);

            // subscribe once
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            // subscribe again
            o.subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    assertEquals("one", v);
                    latch.countDown();
                }
            });

            if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("subscriptions did not receive values");
            }
            assertEquals(1, counter.get());
        }

        /**
         * https://github.com/Netflix/RxJava/issues/198
         * 
         * Rx Design Guidelines 5.2
         * 
         * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
         * to rethrow the exception on the thread that the message comes out from the Observable.
         * The OnCompleted behavior in this case is to do nothing."
         */
        @Test
        public void testErrorThrownWithoutErrorHandlerSynchronous() {
            try {
                error(new RuntimeException("failure")).subscribe(new Action1<Object>() {

                    @Override
                    public void call(Object t1) {
                        // won't get anything
                    }

                });
                fail("expected exception");
            } catch (Exception e) {
                assertEquals("failure", e.getMessage());
            }
        }

        /**
         * https://github.com/Netflix/RxJava/issues/198
         * 
         * Rx Design Guidelines 5.2
         * 
         * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
         * to rethrow the exception on the thread that the message comes out from the Observable.
         * The OnCompleted behavior in this case is to do nothing."
         * 
         * @throws InterruptedException
         */
        @Test
        public void testErrorThrownWithoutErrorHandlerAsynchronous() throws InterruptedException {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Exception> exception = new AtomicReference<Exception>();
            Observable.create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(final Observer<String> observer) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                observer.onError(new RuntimeException("failure"));
                            } catch (Exception e) {
                                // without an onError handler it has to just throw on whatever thread invokes it
                                exception.set(e);
                            }
                            latch.countDown();
                        }
                    }).start();
                    return Subscriptions.empty();
                }
            }).subscribe(new Action1<Object>() {

                @Override
                public void call(Object t1) {

                }

            });
            // wait for exception
            latch.await(3000, TimeUnit.MILLISECONDS);
            assertNotNull(exception.get());
            assertEquals("failure", exception.get().getMessage());
        }
    }

}
