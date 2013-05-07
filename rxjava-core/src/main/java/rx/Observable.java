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
import java.util.Iterator;
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

import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
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
import rx.operators.OperationMostRecent;
import rx.operators.OperationMulticast;
import rx.operators.OperationNext;
import rx.operators.OperationObserveOn;
import rx.operators.OperationOnErrorResumeNextViaFunction;
import rx.operators.OperationOnErrorResumeNextViaObservable;
import rx.operators.OperationOnErrorReturn;
import rx.operators.OperationSample;
import rx.operators.OperationScan;
import rx.operators.OperationSkip;
import rx.operators.OperationSubscribeOn;
import rx.operators.OperationSynchronize;
import rx.operators.OperationTake;
import rx.operators.OperationTakeLast;
import rx.operators.OperationTakeUntil;
import rx.operators.OperationTakeWhile;
import rx.operators.OperationTimestamp;
import rx.operators.OperationToFuture;
import rx.operators.OperationToIterator;
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
import rx.util.AtomicObservableSubscription;
import rx.util.AtomicObserver;
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
 * It provides overloaded methods for subscribing as well as delegate methods to the various operators.
 * <p>
 * The documentation for this interface makes use of marble diagrams. The following legend explains
 * these diagrams:
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/legend.png">
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
 * 
 * @param <T>
 */
public class Observable<T> {

    private final static RxJavaObservableExecutionHook hook = RxJavaPlugins.getInstance().getObservableExecutionHook();

    private final Func1<Observer<T>, Subscription> onSubscribe;

    protected Observable() {
        this(null);
    }

    /**
     * Construct an Observable with Function to execute when subscribed to.
     * <p>
     * NOTE: Generally you're better off using {@link #create(Func1)} to create an Observable instead of using inheritance.
     * 
     * @param onSubscribe
     *            {@link Func1} to be executed when {@link #subscribe(Observer)} is called.
     */
    protected Observable(Func1<Observer<T>, Subscription> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    /**
     * an {@link Observer} must call an Observable's <code>subscribe</code> method in order to register itself
     * to receive push-based notifications from the Observable. A typical implementation of the
     * <code>subscribe</code> method does the following:
     * <p>
     * It stores a reference to the Observer in a collection object, such as a <code>List<T></code>
     * object.
     * <p>
     * It returns a reference to the {@link Subscription} interface. This enables
     * Observers to unsubscribe (that is, to stop receiving notifications) before the Observable has
     * finished sending them and has called the Observer's {@link Observer#onCompleted()} method.
     * <p>
     * At any given time, a particular instance of an <code>Observable<T></code> implementation is
     * responsible for accepting all subscriptions and notifying all subscribers. Unless the
     * documentation for a particular <code>Observable<T></code> implementation indicates otherwise,
     * Observers should make no assumptions about the <code>Observable<T></code> implementation, such
     * as the order of notifications that multiple Observers will receive.
     * <p>
     * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * 
     * @param observer
     * @return a {@link Subscription} reference that allows observers
     *         to stop receiving notifications before the provider has finished sending them
     */
    public Subscription subscribe(Observer<T> observer) {
        // allow the hook to intercept and/or decorate
        Func1<Observer<T>, Subscription> onSubscribeFunction = hook.onSubscribeStart(this, onSubscribe);
        // validate and proceed
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
        } catch (Exception e) {
            // if an unhandled error occurs executing the onSubscribe we will propagate it
            try {
                observer.onError(hook.onSubscribeError(this, e));
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
     * an {@link Observer} must call an Observable's <code>subscribe</code> method in order to register itself
     * to receive push-based notifications from the Observable. A typical implementation of the
     * <code>subscribe</code> method does the following:
     * <p>
     * It stores a reference to the Observer in a collection object, such as a <code>List<T></code>
     * object.
     * <p>
     * It returns a reference to the {@link Subscription} interface. This enables
     * Observers to unsubscribe (that is, to stop receiving notifications) before the Observable has
     * finished sending them and has called the Observer's {@link Observer#onCompleted()} method.
     * <p>
     * At any given time, a particular instance of an <code>Observable<T></code> implementation is
     * responsible for accepting all subscriptions and notifying all subscribers. Unless the
     * documentation for a particular <code>Observable<T></code> implementation indicates otherwise,
     * Observers should make no assumptions about the <code>Observable<T></code> implementation, such
     * as the order of notifications that multiple Observers will receive.
     * <p>
     * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava Wiki</a>
     * 
     * 
     * @param observer
     * @param scheduler
     *            The {@link Scheduler} that the sequence is subscribed to on.
     * @return a {@link Subscription} reference that allows observers
     *         to stop receiving notifications before the provider has finished sending them
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
        // lookup and memoize onNext
        Object _onNext = callbacks.get("onNext");
        if (_onNext == null) {
            throw new RuntimeException("onNext must be implemented");
        }
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

        // lookup and memoize onNext
        if (o == null) {
            throw new RuntimeException("onNext must be implemented");
        }
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
                // no callback defined
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
                // no callback defined
            }

            @Override
            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError) {
        // lookup and memoize onNext
        if (onNext == null) {
            throw new RuntimeException("onNext must be implemented");
        }
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
                if (onError != null) {
                    Functions.from(onError).call(e);
                }
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
                if (onError != null) {
                    onError.call(e);
                }
            }

            @Override
            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError, final Object onComplete) {
        // lookup and memoize onNext
        if (onNext == null) {
            throw new RuntimeException("onNext must be implemented");
        }
        final FuncN onNextFunction = Functions.from(onNext);

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        return protectivelyWrapAndSubscribe(new Observer() {

            @Override
            public void onCompleted() {
                if (onComplete != null) {
                    Functions.from(onComplete).call();
                }
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    Functions.from(onError).call(e);
                }
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
                if (onError != null) {
                    onError.call(e);
                }
            }

            @Override
            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, final Action0 onComplete, Scheduler scheduler) {
        return subscribeOn(scheduler).subscribe(onNext, onError, onComplete);
    }

    /**
     * Invokes an action for each element in the observable sequence, and blocks until the sequence is terminated.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link #subscribe(Observer)} but blocks. Because it blocks it does not need the {@link Observer#onCompleted()} or {@link Observer#onError(Exception)} methods.
     * 
     * @param onNext
     *            {@link Action1}
     * @throws RuntimeException
     *             if error occurs
     */
    public void forEach(final Action1<T> onNext) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exceptionFromOnError = new AtomicReference<Exception>();

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        protectivelyWrapAndSubscribe(new Observer<T>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                /*
                 * If we receive an onError event we set the reference on the outer thread
                 * so we can git it and throw after the latch.await().
                 * 
                 * We do this instead of throwing directly since this may be on a different thread and the latch is still waiting.
                 */
                exceptionFromOnError.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }
        });
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/Netflix/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (exceptionFromOnError.get() != null) {
            if (exceptionFromOnError.get() instanceof RuntimeException) {
                throw (RuntimeException) exceptionFromOnError.get();
            } else {
                throw new RuntimeException(exceptionFromOnError.get());
            }
        }
    }

    /**
     * Invokes an action for each element in the observable sequence, and blocks until the sequence is terminated.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link #subscribe(Observer)} but blocks. Because it blocks it does not need the {@link Observer#onCompleted()} or {@link Observer#onError(Exception)} methods.
     * 
     * @param o
     *            onNext {@link Action1 action}
     * @throws RuntimeException
     *             if error occurs
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void forEach(final Object o) {
        if (o instanceof Action1) {
            // in case a dynamic language is not correctly handling the overloaded methods and we receive an Action1 just forward to the correct method.
            forEach((Action1) o);
        }

        // lookup and memoize onNext
        if (o == null) {
            throw new RuntimeException("onNext must be implemented");
        }
        final FuncN onNext = Functions.from(o);

        forEach(new Action1() {

            @Override
            public void call(Object args) {
                onNext.call(args);
            }

        });
    }

    /**
     * Returns a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     * 
     * @param subject
     *            the subject to push source elements into.
     * @param <R>
     *            result type
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public <R> ConnectableObservable<R> multicast(Subject<T, R> subject) {
        return multicast(this, subject);
    }

    /**
     * Returns the only element of an observable sequence and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @return The single element in the observable sequence.
     */
    public T single() {
        return single(this);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     */
    public T single(Func1<T, Boolean> predicate) {
        return single(this, predicate);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     */
    public T single(Object predicate) {
        return single(this, predicate);
    }

    /**
     * Returns the only element of an observable sequence, or a default value if the observable sequence is empty.
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue) {
        return singleOrDefault(this, defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return singleOrDefault(this, defaultValue, predicate);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue, Object predicate) {
        return singleOrDefault(this, defaultValue, predicate);
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
     * an Observable that calls {@link Observer#onError(Exception)} when the Observer subscribes.
     * 
     * @param <T>
     *            the type of object returned by the Observable
     */
    private static class ThrowObservable<T> extends Observable<T> {

        public ThrowObservable(final Exception exception) {
            super(new Func1<Observer<T>, Subscription>() {

                /**
                 * Accepts an {@link Observer} and calls its <code>onError</code> method.
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
     * Creates an Observable that will execute the given function when a {@link Observer} subscribes to it.
     * <p>
     * Write the function you pass to <code>create</code> so that it behaves as an Observable - calling the passed-in
     * <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods appropriately.
     * <p>
     * A well-formed Observable must call either the {@link Observer}'s <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed information.
     * 
     * @param <T>
     *            the type emitted by the Observable sequence
     * @param func
     *            a function that accepts an <code>Observer<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a {@link Subscription} to allow canceling the subscription (if applicable)
     * @return an Observable that, when an {@link Observer} subscribes to it, will execute the given function
     */
    public static <T> Observable<T> create(Func1<Observer<T>, Subscription> func) {
        return new Observable<T>(func);
    }

    /**
     * Creates an Observable that will execute the given function when a {@link Observer} subscribes to it.
     * <p>
     * This method accept {@link Object} to allow different languages to pass in closures using {@link FunctionLanguageAdaptor}.
     * <p>
     * Write the function you pass to <code>create</code> so that it behaves as an Observable - calling the passed-in
     * <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods appropriately.
     * <p>
     * A well-formed Observable must call either the {@link Observer}'s <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * <p>
     * See <a href="http://go.microsoft.com/fwlink/?LinkID=205219">Rx Design Guidelines (PDF)</a> for detailed information.
     * 
     * @param <T>
     *            the type emitted by the Observable sequence
     * @param func
     *            a function that accepts an <code>Observer<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a {@link Subscription} to allow canceling the subscription (if applicable)
     * @return an Observable that, when an {@link Observer} subscribes to it, will execute the given function
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
     * Returns an Observable that returns no data to the {@link Observer} and immediately invokes its <code>onCompleted</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/empty.png">
     * 
     * @param <T>
     *            the type of item emitted by the Observable
     * @return an Observable that returns no data to the {@link Observer} and immediately invokes the {@link Observer}'s <code>onCompleted</code> method
     */
    public static <T> Observable<T> empty() {
        return toObservable(new ArrayList<T>());
    }

    /**
     * Returns an Observable that calls <code>onError</code> when an {@link Observer} subscribes to it.
     * <p>
     * 
     * @param exception
     *            the error to throw
     * @param <T>
     *            the type of object returned by the Observable
     * @return an Observable object that calls <code>onError</code> when an {@link Observer} subscribes
     */
    public static <T> Observable<T> error(Exception exception) {
        return new ThrowObservable<T>(exception);
    }

    /**
     * Filters an Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a function that evaluates the items emitted by the source Observable, returning <code>true</code> if they pass the filter
     * @return an Observable that emits only those items in the original Observable that the filter evaluates as true
     */
    public static <T> Observable<T> filter(Observable<T> that, Func1<T, Boolean> predicate) {
        return create(OperationFilter.filter(that, predicate));
    }

    /**
     * Filters an Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param that
     *            the Observable to filter
     * @param function
     *            a function that evaluates the items emitted by the source Observable, returning <code>true</code> if they pass the filter
     * @return an Observable that emits only those items in the original Observable that the filter evaluates as true
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
     * Filters an Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a function that evaluates the items emitted by the source Observable, returning <code>true</code> if they pass the filter
     * @return an Observable that emits only those items in the original Observable that the filter evaluates as true
     */
    public static <T> Observable<T> where(Observable<T> that, Func1<T, Boolean> predicate) {
        return create(OperationWhere.where(that, predicate));
    }

    /**
     * Converts an {@link Iterable} sequence to an Observable sequence.
     * 
     * @param iterable
     *            the source {@link Iterable} sequence
     * @param <T>
     *            the type of items in the {@link Iterable} sequence and the type emitted by the resulting Observable
     * @return an Observable that emits each item in the source {@link Iterable} sequence
     * @see #toObservable(Iterable)
     */
    public static <T> Observable<T> from(Iterable<T> iterable) {
        return toObservable(iterable);
    }

    /**
     * Converts an Array to an Observable sequence.
     * 
     * @param items
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items emitted by the resulting Observable
     * @return an Observable that emits each item in the source Array
     * @see #toObservable(Object...)
     */
    public static <T> Observable<T> from(T... items) {
        return toObservable(items);
    }

    /**
     * Generates an observable sequence of integral numbers within a specified range.
     * 
     * @param start
     *            The value of the first integer in the sequence
     * @param count
     *            The number of sequential integers to generate.
     * 
     * @return An observable sequence that contains a range of sequential integral numbers.
     * 
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229460(v=vs.103).aspx">Observable.Range Method (Int32, Int32)</a>
     */
    public static Observable<Integer> range(int start, int count) {
        return from(Range.createWithCount(start, count));
    }

    /**
     * Asynchronously subscribes and unsubscribes observers on the specified scheduler.
     * 
     * @param source
     *            the source observable.
     * @param scheduler
     *            the scheduler to perform subscription and unsubscription actions on.
     * @param <T>
     *            the type of observable.
     * @return the source sequence whose subscriptions and unsubscriptions happen on the specified scheduler.
     */
    public static <T> Observable<T> subscribeOn(Observable<T> source, Scheduler scheduler) {
        return create(OperationSubscribeOn.subscribeOn(source, scheduler));
    }

    /**
     * Asynchronously notify observers on the specified scheduler.
     * 
     * @param source
     *            the source observable.
     * @param scheduler
     *            the scheduler to notify observers on.
     * @param <T>
     *            the type of observable.
     * @return the source sequence whose observations happen on the specified scheduler.
     */
    public static <T> Observable<T> observeOn(Observable<T> source, Scheduler scheduler) {
        return create(OperationObserveOn.observeOn(source, scheduler));
    }

    /**
     * Returns an observable sequence that invokes the observable factory whenever a new observer subscribes.
     * The Defer operator allows you to defer or delay the creation of the sequence until the time when an observer
     * subscribes to the sequence. This is useful to allow an observer to easily obtain an updates or refreshed version
     * of the sequence.
     * 
     * @param observableFactory
     *            the observable factory function to invoke for each observer that subscribes to the resulting sequence.
     * @param <T>
     *            the type of the observable.
     * @return the observable sequence whose observers trigger an invocation of the given observable factory function.
     */
    public static <T> Observable<T> defer(Func0<Observable<T>> observableFactory) {
        return create(OperationDefer.defer(observableFactory));
    }

    /**
     * Returns an observable sequence that invokes the observable factory whenever a new observer subscribes.
     * The Defer operator allows you to defer or delay the creation of the sequence until the time when an observer
     * subscribes to the sequence. This is useful to allow an observer to easily obtain an updates or refreshed version
     * of the sequence.
     * 
     * @param observableFactory
     *            the observable factory function to invoke for each observer that subscribes to the resulting sequence.
     * @param <T>
     *            the type of the observable.
     * @return the observable sequence whose observers trigger an invocation of the given observable factory function.
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
     * Returns an Observable that notifies an {@link Observer} of a single value and then completes.
     * <p>
     * To convert any object into an Observable that emits that object, pass that object into the <code>just</code> method.
     * <p>
     * This is similar to the {@link #toObservable} method, except that <code>toObservable</code> will convert
     * an {@link Iterable} object into an Observable that emits each of the items in the {@link Iterable}, one
     * at a time, while the <code>just</code> method would convert the {@link Iterable} into an Observable
     * that emits the entire {@link Iterable} as a single item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/just.png">
     * 
     * @param value
     *            the value to pass to the Observer's <code>onNext</code> method
     * @param <T>
     *            the type of the value
     * @return an Observable that notifies an {@link Observer} of a single value and then completes
     */
    public static <T> Observable<T> just(T value) {
        List<T> list = new ArrayList<T>();
        list.add(value);

        return toObservable(list);
    }

    /**
     * Returns the last element of an observable sequence with a specified source.
     * 
     * @param that
     *            the source Observable
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> that) {
        T result = null;
        for (T value : that.toIterable()) {
            result = value;
        }
        return result;
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * 
     * @param that
     *            the source Observable
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> that, final Func1<T, Boolean> predicate) {
        return last(that.filter(predicate));
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * 
     * @param that
     *            the source Observable
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> that, final Object predicate) {
        return last(that.filter(predicate));
    }

    /**
     * Returns the last element of an observable sequence, or a default value if no value is found.
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue) {
        boolean found = false;
        T result = null;

        for (T value : source.toIterable()) {
            found = true;
            result = value;
        }

        if (!found) {
            return defaultValue;
        }

        return result;
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue, Func1<T, Boolean> predicate) {
        return lastOrDefault(source.filter(predicate), defaultValue);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return lastOrDefault(source, defaultValue, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T args) {
                return (Boolean) _f.call(args);
            }
        });
    }

    /**
     * Applies a function of your choosing to every notification emitted by an Observable, and returns
     * this transformation as a new Observable sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item in the sequence emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items returned by map function
     * @return an Observable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Observable
     */
    public static <T, R> Observable<R> map(Observable<T> sequence, Func1<T, R> func) {
        return create(OperationMap.map(sequence, func));
    }

    /**
     * Applies a function of your choosing to every notification emitted by an Observable, and returns
     * this transformation as a new Observable sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item in the sequence emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items returned by map function
     * @return an Observable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Observable
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
     * Creates a new Observable sequence by applying a function that you supply to each object in the
     * original Observable sequence, where that function is itself an Observable that emits objects,
     * and then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>func</code>
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of
     *         the Observables obtained from this transformation
     * @see #flatMap(Observable, Func1)
     */
    public static <T, R> Observable<R> mapMany(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return create(OperationMap.mapMany(sequence, func));
    }

    /**
     * Creates a new Observable sequence by applying a function that you supply to each object in the
     * original Observable sequence, where that function is itself an Observable that emits objects,
     * and then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>func</code>
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of
     *         the Observables obtained from this transformation
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
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
     * 
     * @param sequence
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">MSDN: Observable.Materialize</a>
     */
    public static <T> Observable<Notification<T>> materialize(final Observable<T> sequence) {
        return create(OperationMaterialize.materialize(sequence));
    }

    /**
     * Dematerializes the explicit notification values of an observable sequence as implicit notifications.
     * 
     * @param sequence
     *            An observable sequence containing explicit notification values which have to be turned into implicit notifications.
     * @return An observable sequence exhibiting the behavior corresponding to the source sequence's notification values.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">MSDN: Observable.Dematerialize</a>
     */
    public static <T> Observable<T> dematerialize(final Observable<Notification<T>> sequence) {
        return create(OperationDematerialize.dematerialize(sequence));
    }

    /**
     * Flattens the Observable sequences from a list of Observables into one Observable sequence
     * without any transformation. You can combine the output of multiple Observables so that they
     * act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * 
     * @param source
     *            a list of Observables that emit sequences of items
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge</a>
     */
    public static <T> Observable<T> merge(List<Observable<T>> source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Flattens the Observable sequences emitted by a sequence of Observables that are emitted by a
     * Observable into one Observable sequence without any transformation. You can combine the output
     * of multiple Observables so that they act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * 
     * @param source
     *            an Observable that emits Observables
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<Observable<T>> source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Flattens the Observable sequences from a series of Observables into one Observable sequence
     * without any transformation. You can combine the output of multiple Observables so that they
     * act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<T>... source) {
        return create(OperationMerge.merge(source));
    }

    /**
     * Returns the values from the source observable sequence until the other observable sequence produces a value.
     * 
     * @param source
     *            the source sequence to propagate elements for.
     * @param other
     *            the observable sequence that terminates propagation of elements of the source sequence.
     * @param <T>
     *            the type of source.
     * @param <E>
     *            the other type.
     * @return An observable sequence containing the elements of the source sequence up to the point the other sequence interrupted further propagation.
     */
    public static <T, E> Observable<T> takeUntil(final Observable<T> source, final Observable<E> other) {
        return OperationTakeUntil.takeUntil(source, other);
    }

    /**
     * Combines the objects emitted by two or more Observables, and emits the result as a single Observable,
     * by using the <code>concat</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/concat.png">
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return an Observable that emits a sequence of elements that are the result of combining the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.concat(v=vs.103).aspx">MSDN: Observable.Concat Method</a>
     */
    public static <T> Observable<T> concat(Observable<T>... source) {
        return create(OperationConcat.concat(source));
    }

    /**
     * Emits the same objects as the given Observable, calling the given action
     * when it calls <code>onComplete</code> or <code>onError</code>.
     * 
     * @param source
     *            an observable
     * @param action
     *            an action to be called when the source completes or errors.
     * @return an Observable that emits the same objects, then calls the action.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx">MSDN: Observable.Finally Method</a>
     */
    public static <T> Observable<T> finallyDo(Observable<T> source, Action0 action) {
        return create(OperationFinally.finallyDo(source, action));
    }

    /**
     * Creates a new Observable sequence by applying a function that you supply to each object in the
     * original Observable sequence, where that function is itself an Observable that emits objects,
     * and then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>func</code>
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of
     *         the Observables obtained from this transformation
     * @see #mapMany(Observable, Func1)
     */
    public static <T, R> Observable<R> flatMap(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return mapMany(sequence, func);
    }

    /**
     * Creates a new Observable sequence by applying a function that you supply to each object in the
     * original Observable sequence, where that function is itself an Observable that emits objects,
     * and then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a function to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>func</code>
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of
     *         the Observables obtained from this transformation
     * @see #mapMany(Observable, Func1)
     */
    public static <T, R> Observable<R> flatMap(Observable<T> sequence, final Object func) {
        return mapMany(sequence, func);
    }

    /**
     * Groups the elements of an observable and selects the resulting elements by using a specified function.
     * 
     * @param source
     *            an observable whose elements to group.
     * @param keySelector
     *            a function to extract the key for each element.
     * @param elementSelector
     *            a function to map each source element to an element in an observable group.
     * @param <K>
     *            the key type.
     * @param <T>
     *            the source type.
     * @param <R>
     *            the resulting observable type.
     * @return an observable of observable groups, each of which corresponds to a unique key value, containing all elements that share that same key value.
     */
    public static <K, T, R> Observable<GroupedObservable<K, R>> groupBy(Observable<T> source, final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {
        return create(OperationGroupBy.groupBy(source, keySelector, elementSelector));
    }

    /**
     * Groups the elements of an observable according to a specified key selector function and
     * 
     * @param source
     *            an observable whose elements to group.
     * @param keySelector
     *            a function to extract the key for each element.
     * @param <K>
     *            the key type.
     * @param <T>
     *            the source type.
     * @return an observable of observable groups, each of which corresponds to a unique key value, containing all elements that share that same key value.
     */
    public static <K, T> Observable<GroupedObservable<K, T>> groupBy(Observable<T> source, final Func1<T, K> keySelector) {
        return create(OperationGroupBy.groupBy(source, keySelector));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * 
     * @param source
     *            a list of Observables that emit sequences of items
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(List<Observable<T>> source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * 
     * @param source
     *            an Observable that emits Observables
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<Observable<T>> source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mergeDelayError.png">
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return an Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<T>... source) {
        return create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Returns an Observable that never sends any information to an {@link Observer}.
     * 
     * This observable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of item (not) emitted by the Observable
     * @return an Observable that never sends any information to an {@link Observer}
     */
    public static <T> Observable<T> never() {
        return new NeverObservable<T>();
    }

    /**
     * Instruct an Observable to pass control to another Observable (the return value of a function)
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected item to its Observer,
     * the Observable calls its {@link Observer}'s <code>onError</code> function, and then quits without calling any more
     * of its {@link Observer}'s closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that emits an Observable (<code>resumeFunction</code>) to an Observable's <code>onErrorResumeNext</code> method,
     * if the original Observable encounters an error, instead of calling its {@link Observer}'s <code>onError</code> function, it
     * will instead relinquish control to this new Observable, which will call the {@link Observer}'s <code>onNext</code> method if
     * it is able to do so. In such a case, because no Observable necessarily invokes <code>onError</code>, the Observer may
     * never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
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
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected item to its Observer,
     * the Observable calls its {@link Observer}'s <code>onError</code> function, and then quits without calling any more
     * of its {@link Observer}'s closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that emits an Observable (<code>resumeFunction</code>) to an Observable's <code>onErrorResumeNext</code> method,
     * if the original Observable encounters an error, instead of calling its {@link Observer}'s <code>onError</code> function, it
     * will instead relinquish control to this new Observable, which will call the {@link Observer}'s <code>onNext</code> method if
     * it is able to do so. In such a case, because no Observable necessarily invokes <code>onError</code>, the Observer may
     * never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
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
     * Instruct an Observable to pass control to another Observable rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected item to its Observer,
     * the Observable calls its {@link Observer}'s <code>onError</code> function, and then quits without calling any more
     * of its {@link Observer}'s closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a
     * function that emits an Observable (<code>resumeFunction</code>) to an Observable's <code>onErrorResumeNext</code> method,
     * if the original Observable encounters an error, instead of calling its {@link Observer}'s <code>onError</code> function, it
     * will instead relinquish control to this new Observable, which will call the {@link Observer}'s <code>onNext</code> method if
     * it is able to do so. In such a case, because no Observable necessarily invokes <code>onError</code>, the Observer may
     * never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
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
     * Instruct an Observable to emit a particular item to its Observer's <code>onNext</code> function
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected item to its {@link Observer}, the Observable calls its {@link Observer}'s <code>onError</code>
     * function, and then quits
     * without calling any more of its {@link Observer}'s closures. The <code>onErrorReturn</code> method changes
     * this behavior. If you pass a function (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code>
     * method, if the original Observable encounters an error, instead of calling its {@link Observer}'s
     * <code>onError</code> function, it will instead pass the return value of <code>resumeFunction</code> to the {@link Observer}'s <code>onNext</code> method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a function that returns a value that will be passed into an {@link Observer}'s <code>onNext</code> function if the Observable encounters an error that would
     *            otherwise cause it to call <code>onError</code>
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorReturn(final Observable<T> that, Func1<Exception, T> resumeFunction) {
        return create(OperationOnErrorReturn.onErrorReturn(that, resumeFunction));
    }

    /**
     * Returns a connectable observable sequence that shares a single subscription to the underlying sequence replaying all notifications.
     * 
     * @param that
     *            the source Observable
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public static <T> ConnectableObservable<T> replay(final Observable<T> that) {
        return OperationMulticast.multicast(that, ReplaySubject.<T> create());
    }

    /**
     * Similar to {@link #replay()} except that this auto-subscribes to the source sequence.
     * <p>
     * This is useful when returning an Observable that you wish to cache responses but can't control the
     * subscribe/unsubscribe behavior of all the Observers.
     * <p>
     * NOTE: You sacrifice the ability to unsubscribe from the origin with this operator so be careful to not
     * use this on infinite or very large sequences that will use up memory. This is similar to
     * the {@link Observable#toList()} operator in this caution.
     * 
     * @return an observable sequence that upon first subscription caches all events for subsequent subscriptions.
     */
    public static <T> Observable<T> cache(final Observable<T> that) {
        return create(OperationCache.cache(that));
    }

    /**
     * Returns a connectable observable sequence that shares a single subscription to the underlying sequence.
     * 
     * @param that
     *            the source Observable
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public static <T> ConnectableObservable<T> publish(final Observable<T> that) {
        return OperationMulticast.multicast(that, PublishSubject.<T> create());
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return an Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> Observable<T> reduce(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return takeLast(create(OperationScan.scan(sequence, accumulator)), 1);
    }

    /**
     * Used by dynamic languages.
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
     * @see #reduce(Observable, Func2)
     */
    public static <T> Observable<T> aggregate(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return reduce(sequence, accumulator);
    }

    /**
     * Used by dynamic languages.
     * 
     * @see #reduce(Observable, Func2)
     */
    public static <T> Observable<T> aggregate(Observable<T> sequence, Object accumulator) {
        return reduce(sequence, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param <R>
     *            the type returned for each item of the target observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            a seed passed into the first execution of the accumulator function
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return an Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T, R> Observable<R> reduce(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return takeLast(create(OperationScan.scan(sequence, initialValue, accumulator)), 1);
    }

    /**
     * Used by dynamic languages.
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
     * @see #reduce(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> aggregate(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return reduce(sequence, initialValue, accumulator);
    }

    /**
     * Used by dynamic languages.
     * 
     * @see #reduce(Observable, Object, Func2)
     */
    public static <T, R> Observable<R> aggregate(Observable<T> sequence, R initialValue, Object accumulator) {
        return reduce(sequence, initialValue, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return an Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> Observable<T> scan(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return create(OperationScan.scan(sequence, accumulator));
    }

    /**
     * Used by dynamic languages.
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
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param <R>
     *            the type returned for each item of the target observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return an Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T, R> Observable<R> scan(Observable<T> sequence, R initialValue, Func2<R, T, R> accumulator) {
        return create(OperationScan.scan(sequence, initialValue, accumulator));
    }

    /**
     * Used by dynamic languages.
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
     * Determines whether all elements of an observable sequence satisfies a condition.
     * 
     * @param sequence
     *            an observable sequence whose elements to apply the predicate to.
     * @param predicate
     *            a function to test each element for a condition.
     * @param <T>
     *            the type of observable.
     * @return true if all elements of an observable sequence satisfies a condition; otherwise, false.
     */
    public static <T> Observable<Boolean> all(final Observable<T> sequence, final Func1<T, Boolean> predicate) {
        return create(OperationAll.all(sequence, predicate));
    }

    /**
     * Determines whether all elements of an observable sequence satisfies a condition.
     * 
     * @param sequence
     *            an observable sequence whose elements to apply the predicate to.
     * @param predicate
     *            a function to test each element for a condition.
     * @param <T>
     *            the type of observable.
     * @return true if all elements of an observable sequence satisfies a condition; otherwise, false.
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
     * Observable. You can ignore the first <code>num</code> items emitted by an Observable and attend
     * only to those items that come after, by modifying the Observable with the <code>skip</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items to skip
     * @return an Observable that emits the same sequence of items emitted by the source Observable,
     *         except for the first <code>num</code> items
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx">MSDN: Observable.Skip Method</a>
     */
    public static <T> Observable<T> skip(final Observable<T> items, int num) {
        return create(OperationSkip.skip(items, num));
    }

    /**
     * Accepts an Observable and wraps it in another Observable that ensures that the resulting
     * Observable is chronologically well-behaved.
     * <p>
     * A well-behaved observable ensures <code>onNext</code>, <code>onCompleted</code>, or <code>onError</code> calls to its subscribers are not interleaved, <code>onCompleted</code> and
     * <code>onError</code> are only called once respectively, and no
     * <code>onNext</code> calls follow <code>onCompleted</code> and <code>onError</code> calls.
     * 
     * @param observable
     *            the source Observable
     * @param <T>
     *            the type of item emitted by the source Observable
     * @return an Observable that is a chronologically well-behaved version of the source Observable
     */
    public static <T> Observable<T> synchronize(Observable<T> observable) {
        return create(OperationSynchronize.synchronize(observable));
    }

    /**
     * Returns an Observable that emits the first <code>num</code> items emitted by the source
     * Observable.
     * <p>
     * You can choose to pay attention only to the first <code>num</code> values emitted by an Observable by calling its <code>take</code> method. This method returns an Observable that will call a
     * subscribing Observer's <code>onNext</code> function a
     * maximum of <code>num</code> times before calling <code>onCompleted</code>.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items from the start of the sequence emitted by the source
     *            Observable to emit
     * @return an Observable that only emits the first <code>num</code> items emitted by the source
     *         Observable
     */
    public static <T> Observable<T> take(final Observable<T> items, final int num) {
        return create(OperationTake.take(items, num));
    }

    /**
     * Returns an Observable that emits the last <code>count</code> items emitted by the source
     * Observable.
     * 
     * @param items
     *            the source Observable
     * @param count
     *            the number of items from the end of the sequence emitted by the source
     *            Observable to emit
     * @return an Observable that only emits the last <code>count</code> items emitted by the source
     *         Observable
     */
    public static <T> Observable<T> takeLast(final Observable<T> items, final int count) {
        return create(OperationTakeLast.takeLast(items, count));
    }

    /**
     * Returns the values from the start of an observable sequence while a given predicate remains true.
     * 
     * @param items
     * @param predicate
     *            a function to test each source element for a condition
     * @return the values from the start of the given sequence
     */
    public static <T> Observable<T> takeWhile(final Observable<T> items, Func1<T, Boolean> predicate) {
        return create(OperationTakeWhile.takeWhile(items, predicate));
    }

    /**
     * Returns the values from the start of an observable sequence while a given predicate remains true.
     * 
     * @param items
     * @param predicate
     *            a function to test each source element for a condition
     * @return the values from the start of the given sequence
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
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param items
     * @param predicate
     *            a function to test each element for a condition; the second parameter of the function represents the index of the source element; otherwise, false.
     * @return the values from the start of the given sequence
     */
    public static <T> Observable<T> takeWhileWithIndex(final Observable<T> items, Func2<T, Integer, Boolean> predicate) {
        return create(OperationTakeWhile.takeWhileWithIndex(items, predicate));
    }

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
     * Adds a timestamp to each item emitted by this observable.
     * 
     * @return An observable sequence of timestamped items.
     */
    public Observable<Timestamped<T>> timestamp() {
        return create(OperationTimestamp.timestamp(this));
    }

    /**
     * Return a Future representing a single value of the Observable.
     * <p>
     * This will throw an exception if the Observable emits more than 1 value. If more than 1 are expected then use <code>toList().toFuture()</code>.
     * 
     * @param that
     *            the source Observable
     * @return a Future that expects a single item emitted by the source Observable
     */
    public static <T> Future<T> toFuture(final Observable<T> that) {
        return OperationToFuture.toFuture(that);
    }

    /**
     * Returns an Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * <p>
     * Normally, an Observable that returns multiple items will do so by calling its Observer's <code>onNext</code> function for each such item. You can change this behavior, instructing the
     * Observable
     * to
     * compose a list of all of these multiple items and
     * then to call the Observer's <code>onNext</code> function once, passing it the entire list, by calling the Observable object's <code>toList</code> method prior to calling its
     * <code>subscribe</code>
     * method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
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
     * Converts an observable sequence to an Iterable.
     * 
     * @param that
     *            the source Observable
     * @return Observable converted to Iterable.
     */
    public static <T> Iterable<T> toIterable(final Observable<T> that) {

        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return getIterator(that);
            }
        };
    }

    /**
     * Returns an iterator that iterates all values of the observable.
     * 
     * @param that
     *            an observable sequence to get an iterator for.
     * @param <T>
     *            the type of source.
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public static <T> Iterator<T> getIterator(Observable<T> that) {
        return OperationToIterator.toIterator(that);
    }

    /**
     * Samples the next value (blocking without buffering) from in an observable sequence.
     * 
     * @param items
     *            the source observable sequence.
     * @param <T>
     *            the type of observable.
     * @return iterable that blocks upon each iteration until the next element in the observable source sequence becomes available.
     */
    public static <T> Iterable<T> next(Observable<T> items) {
        return OperationNext.next(items);
    }

    /**
     * Samples the most recent value in an observable sequence.
     * 
     * @param source
     *            the source observable sequence.
     * @param <T>
     *            the type of observable.
     * @param initialValue
     *            the initial value that will be yielded by the enumerable sequence if no element has been sampled yet.
     * @return the iterable that returns the last sampled element upon each iteration.
     */
    public static <T> Iterable<T> mostRecent(Observable<T> source, T initialValue) {
        return OperationMostRecent.mostRecent(source, initialValue);
    }

    /**
     * Returns a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     * 
     * @param source
     *            the source sequence whose elements will be pushed into the specified subject.
     * @param subject
     *            the subject to push source elements into.
     * @param <T>
     *            source type
     * @param <R>
     *            result type
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public static <T, R> ConnectableObservable<R> multicast(Observable<T> source, final Subject<T, R> subject) {
        return OperationMulticast.multicast(source, subject);
    }

    /**
     * Returns the only element of an observable sequence and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @param that
     *            the source Observable
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence
     */
    public static <T> T single(Observable<T> that) {
        return singleOrDefault(that, false, null);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @param that
     *            the source Observable
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence that matches the predicate
     */
    public static <T> T single(Observable<T> that, Func1<T, Boolean> predicate) {
        return single(that.filter(predicate));
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @param that
     *            the source Observable
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence that matches the predicate
     */
    public static <T> T single(Observable<T> that, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return single(that, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    /**
     * Returns the only element of an observable sequence, or a default value if the observable sequence is empty.
     * 
     * @param that
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> that, T defaultValue) {
        return singleOrDefault(that, true, defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param that
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> that, T defaultValue, Func1<T, Boolean> predicate) {
        return singleOrDefault(that.filter(predicate), defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * 
     * @param that
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> that, T defaultValue, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return singleOrDefault(that, defaultValue, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    private static <T> T singleOrDefault(Observable<T> that, boolean hasDefault, T defaultVal) {
        Iterator<T> it = that.toIterable().iterator();

        if (!it.hasNext()) {
            if (hasDefault) {
                return defaultVal;
            }
            throw new IllegalStateException("Expected single entry. Actually empty stream.");
        }

        T result = it.next();

        if (it.hasNext()) {
            throw new IllegalStateException("Expected single entry. Actually more than one entry.");
        }

        return result;
    }

    /**
     * Converts an Iterable sequence to an Observable sequence.
     * 
     * Any object that supports the Iterable interface can be converted into an Observable that emits
     * each iterable item in the object, by passing the object into the <code>toObservable</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toObservable.png">
     * 
     * @param iterable
     *            the source Iterable sequence
     * @param <T>
     *            the type of items in the iterable sequence and the type emitted by the resulting
     *            Observable
     * @return an Observable that emits each item in the source Iterable sequence
     */
    public static <T> Observable<T> toObservable(Iterable<T> iterable) {
        return create(OperationToObservableIterable.toObservableIterable(iterable));
    }

    /**
     * Converts an Future to an Observable sequence.
     * 
     * Any object that supports the {@link Future} interface can be converted into an Observable that emits
     * the return value of the get() method in the object, by passing the object into the <code>toObservable</code> method.
     * <p>
     * This is blocking so the Subscription returned when calling {@link #subscribe(Observer)} does nothing.
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of of object that the future's returns and the type emitted by the resulting
     *            Observable
     * @return an Observable that emits the item from the source Future
     */
    public static <T> Observable<T> toObservable(Future<T> future) {
        return create(OperationToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts an Future to an Observable sequence.
     * 
     * Any object that supports the {@link Future} interface can be converted into an Observable that emits
     * the return value of the get() method in the object, by passing the object into the <code>toObservable</code> method.
     * The subscribe method on this synchronously so the Subscription returned doesn't nothing.
     * <p>
     * This is blocking so the Subscription returned when calling {@link #subscribe(Observer)} does nothing.
     * 
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait
     * @param unit
     *            the time unit of the time argument
     * @param <T>
     *            the type of of object that the future's returns and the type emitted by the resulting
     *            Observable
     * @return an Observable that emits the item from the source Future
     */
    public static <T> Observable<T> toObservable(Future<T> future, long timeout, TimeUnit unit) {
        return create(OperationToObservableFuture.toObservableFuture(future, timeout, unit));
    }

    /**
     * Converts an Array sequence to an Observable sequence.
     * 
     * An Array can be converted into an Observable that emits each item in the Array, by passing the
     * Array into the <code>toObservable</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toObservable.png">
     * 
     * @param items
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items emitted by the resulting
     *            Observable
     * @return an Observable that emits each item in the source Array
     */
    public static <T> Observable<T> toObservable(T... items) {
        return toObservable(Arrays.asList(items));
    }

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sequence
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return an observable containing the sorted list
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence) {
        return create(OperationToObservableSortedList.toSortedList(sequence));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sequence
     * @param sortFunction
     * @return an observable containing the sorted list
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, Func2<T, T, Integer> sortFunction) {
        return create(OperationToObservableSortedList.toSortedList(sequence, sortFunction));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sequence
     * @param sortFunction
     * @return an observable containing the sorted list
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
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by two other Observables, with the results of this function becoming the
     * sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>
     * and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Observable will be the result of the function applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param reduceFunction
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> reduceFunction) {
        return create(OperationZip.zip(w0, w1, reduceFunction));
    }

    /**
     * Determines whether two sequences are equal by comparing the elements pairwise.
     * 
     * @param first
     *            observable to compare
     * @param second
     *            observable to compare
     * @param <T>
     *            type of sequence
     * @return sequence of booleans, true if two sequences are equal by comparing the elements pairwise; otherwise, false.
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
     * Determines whether two sequences are equal by comparing the elements pairwise using a specified equality function.
     * 
     * @param first
     *            observable sequence to compare
     * @param second
     *            observable sequence to compare
     * @param equality
     *            a function used to compare elements of both sequences
     * @param <T>
     *            type of sequence
     * @return sequence of booleans, true if two sequences are equal by comparing the elements pairwise; otherwise, false.
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> first, Observable<T> second, Func2<T, T, Boolean> equality) {
        return zip(first, second, equality);
    }

    /**
     * Determines whether two sequences are equal by comparing the elements pairwise using a specified equality function.
     * 
     * @param first
     *            observable sequence to compare
     * @param second
     *            observable sequence to compare
     * @param equality
     *            a function used to compare elements of both sequences
     * @param <T>
     *            type of sequence
     * @return sequence of booleans, true if two sequences are equal by comparing the elements pairwise; otherwise, false.
     */
    public static <T> Observable<Boolean> sequenceEqual(Observable<T> first, Observable<T> second, Object equality) {
        return zip(first, second, equality);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by two other Observables, with the results of this function becoming the
     * sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>
     * and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Observable will be the result of the function applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
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
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by three other Observables, with the results of this function becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>,
     * the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Observable will be the result of the function applied to the second item emitted by <code>w0</code>, the second item
     * emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> function) {
        return create(OperationZip.zip(w0, w1, w2, function));
    }

    /**
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by three other Observables, with the results of this function becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>,
     * the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Observable will be the result of the function applied to the second item emitted by <code>w0</code>, the second item
     * emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
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
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by four other Observables, with the results of this function becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>,
     * the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
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
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> reduceFunction) {
        return create(OperationZip.zip(w0, w1, w2, w3, reduceFunction));
    }

    /**
     * Returns an Observable that applies a function of your choosing to the combination of items
     * emitted, in sequence, by four other Observables, with the results of this function becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this function in strict sequence, so the first item emitted by the new Observable will be the result of the function applied to the first item emitted by
     * <code>w0</code>,
     * the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/zip.png">
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
     *            a function that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
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
     * Filters an Observable by discarding any of its emissions that do not meet some test.
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
     * Registers an action to be called when this observable calls
     * <code>onComplete</code> or <code>onError</code>.
     * 
     * @param action
     *            an action to be called when this observable completes or errors.
     * @return an Observable that emits the same objects as this observable, then calls the action.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212133(v=vs.103).aspx">MSDN: Observable.Finally Method</a>
     */
    public Observable<T> finallyDo(Action0 action) {
        return create(OperationFinally.finallyDo(this, action));
    }

    /**
     * Filters an Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param callback
     *            a function that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
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
     * Creates a new Observable sequence by applying a function that you supply to each item in the
     * original Observable sequence, where that function is itself an Observable that emits items, and
     * then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param func
     *            a function to apply to each item in the sequence, that returns an Observable.
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
     * @see #mapMany(Func1)
     */
    public <R> Observable<R> flatMap(Func1<T, Observable<R>> func) {
        return mapMany(func);
    }

    /**
     * Creates a new Observable sequence by applying a function that you supply to each item in the
     * original Observable sequence, where that function is itself an Observable that emits items, and
     * then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param callback
     *            a function to apply to each item in the sequence that returns an Observable.
     * @return an Observable that emits a sequence that is the result of applying the transformation'
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
     * @see #mapMany(Object)
     */
    public <R> Observable<R> flatMap(final Object callback) {
        return mapMany(callback);
    }

    /**
     * Filters an Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/filter.png">
     * 
     * @param predicate
     *            a function that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
     * @return an Observable that emits only those items in the original Observable that the filter
     *         evaluates as <code>true</code>
     */
    public Observable<T> where(Func1<T, Boolean> predicate) {
        return where(this, predicate);
    }

    /**
     * Returns the last element of an observable sequence with a specified source.
     * 
     * @return the last element in the observable sequence.
     */
    public T last() {
        return last(this);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * 
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public T last(final Func1<T, Boolean> predicate) {
        return last(this, predicate);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * 
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public T last(final Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return last(this, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T args) {
                return (Boolean) _f.call(args);
            }
        });
    }

    /**
     * Returns the last element, or a default value if no value is found.
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue) {
        return lastOrDefault(this, defaultValue);
    }

    /**
     * Returns the last element that matches the predicate, or a default value if no value is found.
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return lastOrDefault(this, defaultValue, predicate);
    }

    /**
     * Returns the last element that matches the predicate, or a default value if no value is found.
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue, Object predicate) {
        return lastOrDefault(this, defaultValue, predicate);
    }

    /**
     * Applies a function of your choosing to every item emitted by an Observable, and returns this
     * transformation as a new Observable sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param func
     *            a function to apply to each item in the sequence.
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item in the sequence emitted by the input Observable.
     */
    public <R> Observable<R> map(Func1<T, R> func) {
        return map(this, func);
    }

    /**
     * Applies a function of your choosing to every item emitted by an Observable, and returns this
     * transformation as a new Observable sequence.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/map.png">
     * 
     * @param callback
     *            a function to apply to each item in the sequence.
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item in the sequence emitted by the input Observable.
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
     * Creates a new Observable sequence by applying a function that you supply to each item in the
     * original Observable sequence, where that function is itself an Observable that emits items, and
     * then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param func
     *            a function to apply to each item in the sequence, that returns an Observable.
     * @return an Observable that emits a sequence that is the result of applying the transformation
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
     * @see #flatMap(Func1)
     */
    public <R> Observable<R> mapMany(Func1<T, Observable<R>> func) {
        return mapMany(this, func);
    }

    /**
     * Creates a new Observable sequence by applying a function that you supply to each item in the
     * original Observable sequence, where that function is itself an Observable that emits items, and
     * then merges the results of that function applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * Note: mapMany and flatMap are equivalent.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/mapMany.png">
     * 
     * @param callback
     *            a function to apply to each item in the sequence that returns an Observable.
     * @return an Observable that emits a sequence that is the result of applying the transformation'
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
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
     * Materializes the implicit notifications of this observable sequence as explicit notification values.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/materialize.png">
     * 
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx">MSDN: Observable.materialize</a>
     */
    public Observable<Notification<T>> materialize() {
        return materialize(this);
    }

    /**
     * Asynchronously subscribes and unsubscribes observers on the specified scheduler.
     * 
     * @param scheduler
     *            the scheduler to perform subscription and unsubscription actions on.
     * @return the source sequence whose subscriptions and unsubscriptions happen on the specified scheduler.
     */
    public Observable<T> subscribeOn(Scheduler scheduler) {
        return subscribeOn(this, scheduler);
    }

    /**
     * Asynchronously notify observers on the specified scheduler.
     * 
     * @param scheduler
     *            the scheduler to notify observers on.
     * @return the source sequence whose observations happen on the specified scheduler.
     */
    public Observable<T> observeOn(Scheduler scheduler) {
        return observeOn(this, scheduler);
    }

    /**
     * Dematerializes the explicit notification values of an observable sequence as implicit notifications.
     * 
     * @return An observable sequence exhibiting the behavior corresponding to the source sequence's notification values.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">MSDN: Observable.dematerialize</a>
     * @throws Exception
     *             if attempted on Observable not of type {@code Observable<Notification<T>>}.
     */
    @SuppressWarnings("unchecked")
    public <T2> Observable<T2> dematerialize() {
        return dematerialize((Observable<Notification<T2>>) this);
    }

    /**
     * Instruct an Observable to pass control to another Observable rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> function, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onErrort</code> function, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * 
     * @param resumeFunction
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Func1<Exception, Observable<T>> resumeFunction) {
        return onErrorResumeNext(this, resumeFunction);
    }

    /**
     * Instruct an Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> function, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> function, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * 
     * @param resumeFunction
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
     * Instruct an Observable to pass control to another Observable rather than calling
     * <code>onError</code> if it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> function, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeSequence</code>) to an Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> function, it will instead relinquish control to
     * <code>resumeSequence</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/onErrorResumeNext.png">
     * 
     * @param resumeSequence
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Observable<T> resumeSequence) {
        return onErrorResumeNext(this, resumeSequence);
    }

    /**
     * Instruct an Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected
     * object to its Observer, the Observable calls its Observer's <code>onError</code> function, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> function, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Observer's <code>onNext</code> method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorReturn(Func1<Exception, T> resumeFunction) {
        return onErrorReturn(this, resumeFunction);
    }

    /**
     * Instruct an Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected
     * object to its Observer, the Observable calls its Observer's <code>onError</code> function, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a function
     * (<code>resumeFunction</code>) to an Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> function, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Observer's <code>onNext</code> method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * 
     * @param resumeFunction
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
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence, whose result
     *            will be used in the next accumulator call (if applicable).
     * 
     * @return An observable sequence with a single element from the result of accumulating the
     *         output from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public Observable<T> reduce(Func2<T, T, T> accumulator) {
        return reduce(this, accumulator);
    }

    /**
     * Returns a connectable observable sequence that shares a single subscription to the underlying sequence replaying all notifications.
     * 
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public ConnectableObservable<T> replay() {
        return replay(this);
    }

    /**
     * Similar to {@link #replay()} except that this auto-subscribes to the source sequence.
     * <p>
     * This is useful when returning an Observable that you wish to cache responses but can't control the
     * subscribe/unsubscribe behavior of all the Observers.
     * <p>
     * NOTE: You sacrifice the ability to unsubscribe from the origin with this operator so be careful to not
     * use this on infinite or very large sequences that will use up memory. This is similar to
     * the {@link Observable#toList()} operator in this caution.
     * 
     * @return an observable sequence that upon first subscription caches all events for subsequent subscriptions.
     */
    public Observable<T> cache() {
        return cache(this);
    }

    /**
     * Returns a connectable observable sequence that shares a single subscription to the underlying sequence.
     * 
     * @return a connectable observable sequence that upon connection causes the source sequence to push results into the specified subject.
     */
    public ConnectableObservable<T> publish() {
        return publish(this);
    }

    /**
     * Used by dynamic languages.
     * 
     * @see #reduce(Func2)
     */
    public Observable<T> reduce(Object accumulator) {
        return reduce(this, accumulator);
    }

    /**
     * @see #reduce(Func2)
     */
    public Observable<T> aggregate(Func2<T, T, T> accumulator) {
        return aggregate(this, accumulator);
    }

    /**
     * Used by dynamic languages.
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
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable).
     * 
     * @return an Observable that emits a single element from the result of accumulating the output
     *         from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public <R> Observable<R> reduce(R initialValue, Func2<R, T, R> accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * Used by dynamic languages.
     * 
     * @see #reduce(Object, Func2)
     */
    public <R> Observable<R> reduce(R initialValue, Object accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * @see #reduce(Object, Func2)
     */
    public <R> Observable<R> aggregate(R initialValue, Func2<R, T, R> accumulator) {
        return aggregate(this, initialValue, accumulator);
    }

    /**
     * Used by dynamic languages.
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
     * source Observable, emitting the result of each of these iterations. It emits the result of
     * each of these iterations as a sequence from the returned Observable. This sort of function is
     * sometimes called an accumulator.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * @return an Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(Func2<T, T, T> accumulator) {
        return scan(this, accumulator);
    }

    /**
     * Samples the observable sequence at each interval.
     * 
     * @param period
     *            The period of time that defines the sampling rate.
     * @param unit
     *            The time unit for the sampling rate time period.
     * @return An observable sequence whose elements are the results of sampling the current observable sequence.
     */
    public Observable<T> sample(long period, TimeUnit unit) {
        return create(OperationSample.sample(this, period, unit));
    }

    /**
     * Samples the observable sequence at each interval.
     * 
     * @param period
     *            The period of time that defines the sampling rate.
     * @param unit
     *            The time unit for the sampling rate time period.
     * @param scheduler
     *            The scheduler to use for sampling.
     * @return An observable sequence whose elements are the results of sampling the current observable sequence.
     */
    public Observable<T> sample(long period, TimeUnit unit, Scheduler scheduler) {
        return create(OperationSample.sample(this, period, unit, scheduler));
    }

    /**
     * Used by dynamic languages.
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
     * source Observable, emitting the result of each of these iterations. This sort of function is
     * sometimes called an accumulator.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/scan.png">
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator function to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * @return an Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public <R> Observable<R> scan(R initialValue, Func2<R, T, R> accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * Used by dynamic languages.
     * 
     * @see #scan(Object, Func2)
     */
    public <R> Observable<R> scan(final R initialValue, final Object accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * Determines whether all elements of an observable sequence satisfies a condition.
     * 
     * @param predicate
     *            a function to test each element for a condition.
     * @return true if all elements of an observable sequence satisfies a condition; otherwise, false.
     */
    public Observable<Boolean> all(Func1<T, Boolean> predicate) {
        return all(this, predicate);
    }

    /**
     * Determines whether all elements of an observable sequence satisfies a condition.
     * 
     * @param predicate
     *            a function to test each element for a condition.
     * @return true if all elements of an observable sequence satisfies a condition; otherwise, false.
     */
    public Observable<Boolean> all(Object predicate) {
        return all(this, predicate);
    }

    /**
     * Returns an Observable that skips the first <code>num</code> items emitted by the source
     * Observable.
     * You can ignore the first <code>num</code> items emitted by an Observable and attend only to
     * those items that come after, by modifying the Observable with the <code>skip</code> method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/skip.png">
     * 
     * @param num
     *            The number of items to skip
     * @return an Observable sequence that is identical to the source Observable except that it does
     *         not emit the first <code>num</code> items from that sequence.
     */
    public Observable<T> skip(int num) {
        return skip(this, num);
    }

    /**
     * Returns an Observable that emits the first <code>num</code> items emitted by the source
     * Observable.
     * 
     * You can choose to pay attention only to the first <code>num</code> values emitted by a
     * Observable by calling its <code>take</code> method. This method returns an Observable that will
     * call a subscribing Observer's <code>onNext</code> function a maximum of <code>num</code> times
     * before calling <code>onCompleted</code>.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/take.png">
     * 
     * @param num
     * @return an Observable that emits only the first <code>num</code> items from the source
     *         Observable, or all of the items from the source Observable if that Observable emits
     *         fewer than <code>num</code> items.
     */
    public Observable<T> take(final int num) {
        return take(this, num);
    }

    /**
     * Returns an Observable that items emitted by the source Observable as long as a specified condition is true.
     * 
     * @param predicate
     *            a function to test each source element for a condition
     * @return the values from the start of the given sequence
     */
    public Observable<T> takeWhile(final Func1<T, Boolean> predicate) {
        return takeWhile(this, predicate);
    }

    /**
     * Returns an Observable that items emitted by the source Observable as long as a specified condition is true.
     * 
     * @param predicate
     *            a function to test each source element for a condition
     * @return the values from the start of the given sequence
     */
    public Observable<T> takeWhile(final Object predicate) {
        return takeWhile(this, predicate);
    }

    /**
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param predicate
     *            a function to test each element for a condition; the second parameter of the function represents the index of the source element; otherwise, false.
     * @return the values from the start of the given sequence
     */
    public Observable<T> takeWhileWithIndex(final Func2<T, Integer, Boolean> predicate) {
        return takeWhileWithIndex(this, predicate);
    }

    /**
     * Returns values from an observable sequence as long as a specified condition is true, and then skips the remaining values.
     * 
     * @param predicate
     *            a function to test each element for a condition; the second parameter of the function represents the index of the source element; otherwise, false.
     * @return the values from the start of the given sequence
     */
    public Observable<T> takeWhileWithIndex(final Object predicate) {
        return takeWhileWithIndex(this, predicate);
    }

    /**
     * Returns an Observable that emits the last <code>count</code> items emitted by the source
     * Observable.
     * 
     * @param count
     *            the number of items from the end of the sequence emitted by the source
     *            Observable to emit
     * @return an Observable that only emits the last <code>count</code> items emitted by the source
     *         Observable
     */
    public Observable<T> takeLast(final int count) {
        return takeLast(this, count);
    }

    /**
     * Returns the values from the source observable sequence until the other observable sequence produces a value.
     * 
     * @param other
     *            the observable sequence that terminates propagation of elements of the source sequence.
     * @param <E>
     *            the other type.
     * @return An observable sequence containing the elements of the source sequence up to the point the other sequence interrupted further propagation.
     */
    public <E> Observable<T> takeUntil(Observable<E> other) {
        return takeUntil(this, other);
    }

    /**
     * Return a Future representing a single value of the Observable.
     * <p>
     * This will throw an exception if the Observable emits more than 1 value. If more than 1 are expected then use <code>toList().toFuture()</code>.
     * 
     * @return a Future that expects a single item emitted by the source Observable
     */
    public Future<T> toFuture() {
        return toFuture(this);
    }

    /**
     * Returns an Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * 
     * Normally, an Observable that returns multiple items will do so by calling its Observer's
     * <code>onNext</code> function for each such item. You can change this behavior, instructing
     * the Observable to compose a list of all of these multiple items and then to call the
     * Observer's <code>onNext</code> function once, passing it the entire list, by calling the
     * Observable object's <code>toList</code> method prior to calling its <code>subscribe</code>
     * method.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toList.png">
     * 
     * @return an Observable that emits a single item: a List containing all of the items emitted by
     *         the source Observable.
     */
    public Observable<List<T>> toList() {
        return toList(this);
    }

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return an observable containing the sorted list
     */
    public Observable<List<T>> toSortedList() {
        return toSortedList(this);
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sortFunction
     * @return an observable containing the sorted list
     */
    public Observable<List<T>> toSortedList(Func2<T, T, Integer> sortFunction) {
        return toSortedList(this, sortFunction);
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/toSortedList.png">
     * 
     * @param sortFunction
     * @return an observable containing the sorted list
     */
    public Observable<List<T>> toSortedList(final Object sortFunction) {
        return toSortedList(this, sortFunction);
    }

    /**
     * Converts an observable sequence to an Iterable.
     * 
     * @return Observable converted to Iterable.
     */
    public Iterable<T> toIterable() {
        return toIterable(this);
    }

    @SuppressWarnings("unchecked")
    public Observable<T> startWith(T... values) {
        return concat(Observable.<T> from(values), this);
    }

    /**
     * Groups the elements of an observable and selects the resulting elements by using a specified function.
     * 
     * @param keySelector
     *            a function to extract the key for each element.
     * @param elementSelector
     *            a function to map each source element to an element in an observable group.
     * @param <K>
     *            the key type.
     * @param <R>
     *            the resulting observable type.
     * @return an observable of observable groups, each of which corresponds to a unique key value, containing all elements that share that same key value.
     */
    public <K, R> Observable<GroupedObservable<K, R>> groupBy(final Func1<T, K> keySelector, final Func1<T, R> elementSelector) {
        return groupBy(this, keySelector, elementSelector);
    }

    /**
     * Groups the elements of an observable according to a specified key selector function and
     * 
     * @param keySelector
     *            a function to extract the key for each element.
     * @param <K>
     *            the key type.
     * @return an observable of observable groups, each of which corresponds to a unique key value, containing all elements that share that same key value.
     */
    public <K> Observable<GroupedObservable<K, T>> groupBy(final Func1<T, K> keySelector) {
        return groupBy(this, keySelector);
    }

    /**
     * Returns an iterator that iterates all values of the observable.
     * 
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public Iterator<T> getIterator() {
        return getIterator(this);
    }

    /**
     * Samples the next value (blocking without buffering) from in an observable sequence.
     * 
     * @return iterable that blocks upon each iteration until the next element in the observable source sequence becomes available.
     */
    public Iterable<T> next() {
        return next(this);
    }

    /**
     * Samples the most recent value in an observable sequence.
     * 
     * @param initialValue
     *            the initial value that will be yielded by the enumerable sequence if no element has been sampled yet.
     * @return the iterable that returns the last sampled element upon each iteration.
     */
    public Iterable<T> mostRecent(T initialValue) {
        return mostRecent(this, initialValue);
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
        return o.getClass().getPackage().getName().startsWith("rx.operators");
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
        public void testToIterable() {
            Observable<String> obs = toObservable("one", "two", "three");

            Iterator<String> it = obs.toIterable().iterator();

            assertEquals(true, it.hasNext());
            assertEquals("one", it.next());

            assertEquals(true, it.hasNext());
            assertEquals("two", it.next());

            assertEquals(true, it.hasNext());
            assertEquals("three", it.next());

            assertEquals(false, it.hasNext());

        }

        @Test(expected = TestException.class)
        public void testToIterableWithException() {
            Observable<String> obs = create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onError(new TestException());
                    return Subscriptions.empty();
                }
            });

            Iterator<String> it = obs.toIterable().iterator();

            assertEquals(true, it.hasNext());
            assertEquals("one", it.next());

            assertEquals(true, it.hasNext());
            it.next();

        }

        @Test
        public void testLastOrDefault1() {
            Observable<String> observable = toObservable("one", "two", "three");
            assertEquals("three", observable.lastOrDefault("default"));
        }

        @Test
        public void testLastOrDefault2() {
            Observable<String> observable = toObservable();
            assertEquals("default", observable.lastOrDefault("default"));
        }

        @Test
        public void testLastOrDefault() {
            Observable<Integer> observable = toObservable(1, 0, -1);
            int last = observable.lastOrDefault(-100, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args >= 0;
                }
            });
            assertEquals(0, last);
        }

        @Test
        public void testLastOrDefaultWrongPredicate() {
            Observable<Integer> observable = toObservable(-1, -2, -3);
            int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args >= 0;
                }
            });
            assertEquals(0, last);
        }

        @Test
        public void testLastOrDefaultWithPredicate() {
            Observable<Integer> observable = toObservable(1, 0, -1);
            int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args < 0;
                }
            });

            assertEquals(-1, last);
        }

        public void testSingle() {
            Observable<String> observable = toObservable("one");
            assertEquals("one", observable.single());
        }

        @Test
        public void testSingleDefault() {
            Observable<String> observable = toObservable();
            assertEquals("default", observable.singleOrDefault("default"));
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleDefaultWithMoreThanOne() {
            Observable<String> observable = toObservable("one", "two", "three");
            observable.singleOrDefault("default");
        }

        @Test
        public void testSingleWithPredicateDefault() {
            Observable<String> observable = toObservable("one", "two", "four");
            assertEquals("four", observable.single(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 4;
                }
            }));
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleWrong() {
            Observable<Integer> observable = toObservable(1, 2);
            observable.single();
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleWrongPredicate() {
            Observable<Integer> observable = toObservable(-1);
            observable.single(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args > 0;
                }
            });
        }

        @Test
        public void testSingleDefaultPredicateMatchesNothing() {
            Observable<String> observable = toObservable("one", "two");
            String result = observable.singleOrDefault("default", new Func1<String, Boolean>() {
                @Override
                public Boolean call(String args) {
                    return args.length() == 4;
                }
            });
            assertEquals("default", result);
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleDefaultPredicateMatchesMoreThanOne() {
            toObservable("one", "two").singleOrDefault("default", new Func1<String, Boolean>() {
                @Override
                public Boolean call(String args) {
                    return args.length() == 3;
                }
            });
        }

        @Test
        public void testLast() {
            Observable<String> obs = Observable.toObservable("one", "two", "three");

            assertEquals("three", obs.last());
        }

        @Test
        public void testLastWithPredicate() {
            Observable<String> obs = Observable.toObservable("one", "two", "three");

            assertEquals("two", obs.last(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            }));
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
        public void testLastEmptyObservable() {
            Observable<String> obs = Observable.toObservable();

            assertNull(obs.last());
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
        public void testForEachWithError() {
            try {
                Observable.create(new Func1<Observer<String>, Subscription>() {

                    @Override
                    public Subscription call(final Observer<String> observer) {
                        final BooleanSubscription subscription = new BooleanSubscription();
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                observer.onNext("one");
                                observer.onNext("two");
                                observer.onNext("three");
                                observer.onCompleted();
                            }
                        }).start();
                        return subscription;
                    }
                }).forEach(new Action1<String>() {

                    @Override
                    public void call(String t1) {
                        throw new RuntimeException("fail");
                    }
                });
                fail("we expect an exception to be thrown");
            } catch (Exception e) {
                // do nothing as we expect this
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

        private static class TestException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }

    }

}
