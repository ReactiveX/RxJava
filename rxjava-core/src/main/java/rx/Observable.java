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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import rx.operators.OperationConcat;
import rx.operators.OperationFilter;
import rx.operators.OperationLast;
import rx.operators.OperationMap;
import rx.operators.OperationMaterialize;
import rx.operators.OperationMerge;
import rx.operators.OperationMergeDelayError;
import rx.operators.OperationOnErrorResumeNextViaFunction;
import rx.operators.OperationOnErrorResumeNextViaObservable;
import rx.operators.OperationOnErrorReturn;
import rx.operators.OperationScan;
import rx.operators.OperationSkip;
import rx.operators.OperationSynchronize;
import rx.operators.OperationTake;
import rx.operators.OperationToObservableFuture;
import rx.operators.OperationToObservableIterable;
import rx.operators.OperationToObservableList;
import rx.operators.OperationToObservableSortedList;
import rx.operators.OperationZip;
import rx.util.AtomicObservableSubscription;
import rx.util.AtomicObserver;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * The Observable interface that implements the Reactive Pattern.
 * <p>
 * The documentation for this interface makes use of marble diagrams. The following legend explains
 * these diagrams:
 * <p>
 * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.legend&ceoid=27321465&key=API&pageId=27321465"><img
 * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.legend.png"></a>
 * <p>
 * It provides overloaded methods for subscribing as well as delegate methods to the
 * 
 * @param <T>
 */
public class Observable<T> {

    private final Func1<Observer<T>, Subscription> onSubscribe;
    private final boolean isTrusted;

    protected Observable(Func1<Observer<T>, Subscription> onSubscribe) {
        this(onSubscribe, false);
    }

    protected Observable() {
        this(null, false);
    }

    private Observable(Func1<Observer<T>, Subscription> onSubscribe, boolean isTrusted) {
        this.onSubscribe = onSubscribe;
        this.isTrusted = isTrusted;
    }

    /**
     * A Observer must call a Observable's <code>subscribe</code> method in order to register itself
     * to receive push-based notifications from the Observable. A typical implementation of the
     * <code>subscribe</code> method does the following:
     * 
     * It stores a reference to the Observer in a collection object, such as a <code>List<T></code>
     * object.
     * 
     * It returns a reference to an <code>ObservableSubscription</code> interface. This enables
     * Observers to unsubscribe (that is, to stop receiving notifications) before the Observable has
     * finished sending them and has called the Observer's <code>OnCompleted</code> method.
     * 
     * At any given time, a particular instance of an <code>Observable<T></code> implementation is
     * responsible for accepting all subscriptions and notifying all subscribers. Unless the
     * documentation for a particular <code>Observable<T></code> implementation indicates otherwise,
     * Observers should make no assumptions about the <code>Observable<T></code> implementation, such
     * as the order of notifications that multiple Observers will receive.
     * <p>
     * For more information see
     * <a href="https://confluence.corp.netflix.com/display/API/Observers%2C+Observables%2C+and+the+Reactive+Pattern">API.Next Programmer's Guide: Observers, Observables, and the Reactive Pattern</a>
     * 
     * @param Observer
     * @return a <code>ObservableSubscription</code> reference to an interface that allows observers
     *         to stop receiving notifications before the provider has finished sending them
     */
    public Subscription subscribe(Observer<T> observer) {
        if (onSubscribe == null) {
            throw new IllegalStateException("onSubscribe function can not be null.");
            // the subscribe function can also be overridden but generally that's not the appropriate approach so I won't mention that in the exception
        }
        if (isTrusted) {
            return onSubscribe.call(observer);
        } else {
            AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            return subscription.wrap(onSubscribe.call(new AtomicObserver<T>(subscription, observer)));
        }
    };

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Map<String, Object> callbacks) {
        return subscribe(new Observer() {

            public void onCompleted() {
                Object completed = callbacks.get("onCompleted");
                if (completed != null) {
                    executeCallback(completed);
                }
            }

            public void onError(Exception e) {
                handleError(e);
                Object onError = callbacks.get("onError");
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            public void onNext(Object args) {
                Object onNext = callbacks.get("onNext");
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object o) {
        if (o instanceof Observer) {
            // in case a dynamic language is not correctly handling the overloaded methods and we receive an Observer just forward to the correct method.
            return subscribe((Observer) o);
        }

        return subscribe(new Observer() {

            public void onCompleted() {
                // do nothing
            }

            public void onError(Exception e) {
                handleError(e);
                // no callback defined
            }

            public void onNext(Object args) {
                if (o == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(o, args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext) {

        return subscribe(new Observer<T>() {

            public void onCompleted() {
                // do nothing
            }

            public void onError(Exception e) {
                handleError(e);
                // no callback defined
            }

            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError) {
        return subscribe(new Observer() {

            public void onCompleted() {
                // do nothing
            }

            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            public void onNext(Object args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError) {

        return subscribe(new Observer<T>() {

            public void onCompleted() {
                // do nothing
            }

            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    onError.call(e);
                }
            }

            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Subscription subscribe(final Object onNext, final Object onError, final Object onComplete) {
        return subscribe(new Observer() {

            public void onCompleted() {
                if (onComplete != null) {
                    executeCallback(onComplete);
                }
            }

            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            public void onNext(Object args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    public Subscription subscribe(final Action1<T> onNext, final Action1<Exception> onError, final Action0 onComplete) {

        return subscribe(new Observer<T>() {

            public void onCompleted() {
                onComplete.call();
            }

            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    onError.call(e);
                }
            }

            public void onNext(T args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                onNext.call(args);
            }

        });
    }

    /**
     * When an error occurs in any Observer we will invoke this to allow it to be handled by the global APIObservableErrorHandler
     * 
     * @param e
     */
    private static void handleError(Exception e) {
        // plugins have been removed during opensourcing but the intention is to provide these hooks again
    }

    /**
     * Execute the callback with the given arguments.
     * <p>
     * The callbacks align with the onCompleted, onError and onNext methods an an IObserver.
     * 
     * @param callback
     *            Object to be invoked. It is left to the implementing class to determine the type, such as a Groovy Closure or JRuby closure conversion.
     * @param args
     */
    private void executeCallback(final Object callback, Object... args) {
        Functions.from(callback).call(args);
    }

    /**
     * A Observable that never sends any information to a Observer.
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
                    return new NoOpObservableSubscription();
                }

            });
        }
    }

    /**
     * A disposable object that does nothing when its unsubscribe method is called.
     */
    private static class NoOpObservableSubscription implements Subscription {
        public void unsubscribe() {
        }
    }

    /**
     * A Observable that calls a Observer's <code>onError</code> closure when the Observer subscribes.
     * 
     * @param <T>
     *            the type of object returned by the Observable
     */
    private static class ThrowObservable<T> extends Observable<T> {

        public ThrowObservable(final Exception exception) {
            super(new Func1<Observer<T>, Subscription>() {

                /**
                 * Accepts a Observer and calls its <code>onError</code> method.
                 * 
                 * @param observer
                 *            a Observer of this Observable
                 * @return a reference to the subscription
                 */
                @Override
                public Subscription call(Observer<T> observer) {
                    observer.onError(exception);
                    return new NoOpObservableSubscription();
                }

            });
        }

    }

    /**
     * Creates a Observable that will execute the given function when a Observer subscribes to it.
     * <p>
     * You can create a simple Observable from scratch by using the <code>create</code> method. You pass this method a closure that accepts as a parameter the map of closures that a Observer passes to
     * a
     * Observable's <code>subscribe</code> method. Write
     * the closure you pass to <code>create</code> so that it behaves as a Observable - calling the passed-in <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     * appropriately.
     * <p>
     * A well-formed Observable must call either the Observer's <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * 
     * @param <T>
     *            the type emitted by the Observable sequence
     * @param func
     *            a closure that accepts a <code>Observer<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a <code>ObservableSubscription</code> to allow
     *            cancelling the subscription (if applicable)
     * @return a Observable that, when a Observer subscribes to it, will execute the given function
     */
    public static <T> Observable<T> create(Func1<Observer<T>, Subscription> func) {
        return new Observable<T>(func);
    }

    /*
     * Private version that creates a 'trusted' Observable to allow performance optimizations.
     */
    private static <T> Observable<T> _create(Func1<Observer<T>, Subscription> func) {
        return new Observable<T>(func, true);
    }

    /**
     * Creates a Observable that will execute the given function when a Observer subscribes to it.
     * <p>
     * You can create a simple Observable from scratch by using the <code>create</code> method. You pass this method a closure that accepts as a parameter the map of closures that a Observer passes to
     * a
     * Observable's <code>subscribe</code> method. Write
     * the closure you pass to <code>create</code> so that it behaves as a Observable - calling the passed-in <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     * appropriately.
     * <p>
     * A well-formed Observable must call either the Observer's <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * 
     * @param <T>
     *            the type of the observable sequence
     * @param func
     *            a closure that accepts a <code>Observer<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a <code>ObservableSubscription</code> to allow
     *            cancelling the subscription (if applicable)
     * @return a Observable that, when a Observer subscribes to it, will execute the given function
     */
    public static <T> Observable<T> create(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return _create(new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> t1) {
                return (Subscription) _f.call(t1);
            }

        });
    }

    /**
     * Returns a Observable that returns no data to the Observer and immediately invokes its <code>onCompleted</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.empty&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.empty.png"></a>
     * 
     * @param <T>
     *            the type of item emitted by the Observable
     * @return a Observable that returns no data to the Observer and immediately invokes the
     *         Observer's <code>onCompleted</code> method
     */
    public static <T> Observable<T> empty() {
        return toObservable(new ArrayList<T>());
    }

    /**
     * Returns a Observable that calls <code>onError</code> when a Observer subscribes to it.
     * <p>
     * Note: Maps to <code>Observable.Throw</code> in Rx - throw is a reserved word in Java.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.error&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.error.png"></a>
     * 
     * @param exception
     *            the error to throw
     * @param <T>
     *            the type of object returned by the Observable
     * @return a Observable object that calls <code>onError</code> when a Observer subscribes
     */
    public static <T> Observable<T> error(Exception exception) {
        return new ThrowObservable<T>(exception);
    }

    /**
     * Filters a Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a closure that evaluates the items emitted by the source Observable, returning <code>true</code> if they pass the filter
     * @return a Observable that emits only those items in the original Observable that the filter
     *         evaluates as true
     */
    public static <T> Observable<T> filter(Observable<T> that, Func1<T, Boolean> predicate) {
        return _create(OperationFilter.filter(that, predicate));
    }

    /**
     * Filters a Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param that
     *            the Observable to filter
     * @param predicate
     *            a closure that evaluates the items emitted by the source Observable, returning <code>true</code> if they pass the filter
     * @return a Observable that emits only those items in the original Observable that the filter
     *         evaluates as true
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
     * Converts an Iterable sequence to a Observable sequence.
     * 
     * @param iterable
     *            the source Iterable sequence
     * @param <T>
     *            the type of items in the iterable sequence and the type emitted by the resulting
     *            Observable
     * @return a Observable that emits each item in the source Iterable sequence
     * @see {@link #toObservable(Iterable)}
     */
    public static <T> Observable<T> from(Iterable<T> iterable) {
        return toObservable(iterable);
    }

    /**
     * Converts an Array sequence to a Observable sequence.
     * 
     * @param iterable
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items emitted by the resulting
     *            Observable
     * @return a Observable that emits each item in the source Array
     * @see {@link #toObservable(Object...)}
     */
    public static <T> Observable<T> from(T... items) {
        return toObservable(items);
    }

    /**
     * Returns a Observable that notifies an observer of a single value and then completes.
     * <p>
     * To convert any object into a Observable that emits that object, pass that object into the <code>just</code> method.
     * <p>
     * This is similar to the {@link toObservable} method, except that <code>toObservable</code> will convert an iterable object into a Observable that emits each of the items in the iterable, one at
     * a
     * time, while the <code>just</code> method would
     * convert the iterable into a Observable that emits the entire iterable as a single item.
     * <p>
     * This value is the equivalent of <code>Observable.Return</code> in the Reactive Extensions library.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.just&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.just.png"></a>
     * 
     * @param value
     *            the value to pass to the Observer's <code>onNext</code> method
     * @param <T>
     *            the type of the value
     * @return a Observable that notifies a Observer of a single value and then completes
     */
    public static <T> Observable<T> just(T value) {
        List<T> list = new ArrayList<T>();
        list.add(value);

        return toObservable(list);
    }

    /**
     * Takes the last item emitted by a source Observable and returns a Observable that emits only
     * that item as its sole emission.
     * <p>
     * To convert a Observable that emits a sequence of objects into one that only emits the last object in this sequence before completing, use the <code>last</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.last&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.last.png"></a>
     * 
     * @param that
     *            the source Observable
     * @return a Observable that emits a single item, which is identical to the last item emitted
     *         by the source Observable
     */
    public static <T> Observable<T> last(final Observable<T> that) {
        return _create(OperationLast.last(that));
    }

    /**
     * Applies a closure of your choosing to every notification emitted by a Observable, and returns
     * this transformation as a new Observable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a closure to apply to each item in the sequence emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items returned by map closure <code>func</code>
     * @return a Observable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Observable
     */
    public static <T, R> Observable<R> map(Observable<T> sequence, Func1<T, R> func) {
        return _create(OperationMap.map(sequence, func));
    }

    /**
     * Applies a closure of your choosing to every notification emitted by a Observable, and returns
     * this transformation as a new Observable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param sequence
     *            the source Observable
     * @param function
     *            a closure to apply to each item in the sequence emitted by the source Observable
     * @param <T>
     *            the type of items emitted by the the source Observable
     * @param <R>
     *            the type of items returned by map closure <code>function</code>
     * @return a Observable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Observable
     */
    public static <T, R> Observable<R> map(Observable<T> sequence, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
        return map(sequence, new Func1<T, R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(T t1) {
                return (R) _f.call(t1);
            }

        });
    }

    /**
     * Creates a new Observable sequence by applying a closure that you supply to each object in the
     * original Observable sequence, where that closure is itself a Observable that emits objects,
     * and then merges the results of that closure applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param sequence
     *            the source Observable
     * @param func
     *            a closure to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>func</code>
     * @return a Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of
     *         the Observables obtained from this transformation
     */
    public static <T, R> Observable<R> mapMany(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return _create(OperationMap.mapMany(sequence, func));
    }

    /**
     * Creates a new Observable sequence by applying a closure that you supply to each object in the
     * original Observable sequence, where that closure is itself a Observable that emits objects,
     * and then merges the results of that closure applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param sequence
     *            the source Observable
     * @param function
     *            a closure to apply to each item emitted by the source Observable, generating a
     *            Observable
     * @param <T>
     *            the type emitted by the source Observable
     * @param <R>
     *            the type emitted by the Observables emitted by <code>function</code>
     * @return a Observable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Observable and merging the results of the
     *         Observables obtained from this transformation
     */
    public static <T, R> Observable<R> mapMany(Observable<T> sequence, final Object function) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(function);
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
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.materialize&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.materialize.png"></a>
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
     */
    public static <T> Observable<Notification<T>> materialize(final Observable<T> sequence) {
        return _create(OperationMaterialize.materialize(sequence));
    }

    /**
     * Flattens the Observable sequences from a list of Observables into one Observable sequence
     * without any transformation. You can combine the output of multiple Observables so that they
     * act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.png"></a>
     * 
     * @param source
     *            a list of Observables that emit sequences of items
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(List<Observable<T>> source) {
        return _create(OperationMerge.merge(source));
    }

    /**
     * Flattens the Observable sequences emitted by a sequence of Observables that are emitted by a
     * Observable into one Observable sequence without any transformation. You can combine the output
     * of multiple Observables so that they act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge.W&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.W.png"></a>
     * 
     * @param source
     *            a Observable that emits Observables
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<Observable<T>> source) {
        return _create(OperationMerge.merge(source));
    }

    /**
     * Flattens the Observable sequences from a series of Observables into one Observable sequence
     * without any transformation. You can combine the output of multiple Observables so that they
     * act like a single Observable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.png"></a>
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> merge(Observable<T>... source) {
        return _create(OperationMerge.merge(source));
    }

    /**
     * Combines the objects emitted by two or more Observables, and emits the result as a single Observable,
     * by using the <code>concat</code> method.
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return a Observable that emits a sequence of elements that are the result of combining the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.concat(v=vs.103).aspx">MSDN: Observable.Concat Method</a>
     */
    public static <T> Observable<T> concat(Observable<T>... source) {
        return _create(OperationConcat.concat(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.png"></a>
     * 
     * @param source
     *            a list of Observables that emit sequences of items
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(List<Observable<T>> source) {
        return _create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError.W&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.W.png"></a>
     * 
     * @param source
     *            a Observable that emits Observables
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the Observables emitted by the <code>source</code> Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<Observable<T>> source) {
        return _create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.png"></a>
     * 
     * @param source
     *            a series of Observables that emit sequences of items
     * @return a Observable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Observables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> Observable<T> mergeDelayError(Observable<T>... source) {
        return _create(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Returns a Observable that never sends any information to a Observer.
     * 
     * This observable is useful primarily for testing purposes.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.never&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.never.png"></a>
     * 
     * @param <T>
     *            the type of item (not) emitted by the Observable
     * @return a Observable that never sends any information to a Observer
     */
    public static <T> Observable<T> never() {
        return new NeverObservable<T>();
    }

    /**
     * A ObservableSubscription that does nothing.
     * 
     * //TODO should this be moved to a Subscriptions utility class?
     * 
     * @return
     */
    public static Subscription noOpSubscription() {
        return new NoOpObservableSubscription();
    }

    /**
     * A Subscription implemented via a Func
     * 
     * //TODO should this be moved to a Subscriptions utility class?
     * 
     * @return
     */
    public static Subscription createSubscription(final Action0 unsubscribe) {
        return new Subscription() {

            @Override
            public void unsubscribe() {
                unsubscribe.call();
            }

        };
    }

    /**
     * A Subscription implemented via an anonymous function (such as closures from other languages).
     * 
     * //TODO should this be moved to a Subscriptions utility class?
     * 
     * @return
     */
    public static Subscription createSubscription(final Object unsubscribe) {
        final FuncN<?> f = Functions.from(unsubscribe);
        return new Subscription() {

            @Override
            public void unsubscribe() {
                f.call();
            }

        };
    }

    /**
     * Instruct a Observable to pass control to another Observable (the return value of a function)
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Observable encounters an error that prevents it from emitting the expected item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and then
     * quits
     * without calling any more of its Observer's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a closure that emits a Observable (<code>resumeFunction</code>) to a Observable's
     * <code>onErrorResumeNext</code> method, if the original Observable encounters
     * an error, instead of calling its Observer's <code>onError</code> closure, it will instead relinquish control to this new Observable, which will call the Observer's <code>onNext</code> method if
     * it
     * is able to do so. In such a case, because no
     * Observable necessarily invokes <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a closure that returns a Observable that will take over if the source Observable
     *            encounters an error
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorResumeNext(final Observable<T> that, final Func1<Exception, Observable<T>> resumeFunction) {
        return _create(OperationOnErrorResumeNextViaFunction.onErrorResumeNextViaFunction(that, resumeFunction));
    }

    /**
     * Instruct a Observable to emit a particular object (as returned by a closure) to its Observer
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Observable encounters an error that prevents it from emitting the expected item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and then
     * quits
     * without calling any more of its Observer's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a function that returns another Observable (<code>resumeFunction</code>) to a Observable's
     * <code>onErrorResumeNext</code> method, if the original Observable
     * encounters an error, instead of calling its Observer's <code>onError</code> closure, it will instead relinquish control to this new Observable which will call the Observer's <code>onNext</code>
     * method if it is able to do so. In such a case,
     * because no Observable necessarily invokes <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a closure that returns a Observable that will take over if the source Observable
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
     * Instruct a Observable to pass control to another Observable rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Observable encounters an error that prevents it from emitting the expected item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and then
     * quits
     * without calling any more of its Observer's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable (<code>resumeSequence</code>) to a Observable's <code>onErrorResumeNext</code> method,
     * if
     * the original Observable encounters an error,
     * instead of calling its Observer's <code>onError</code> closure, it will instead relinquish control to <code>resumeSequence</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no
     * Observable necessarily invokes <code>onError</code>, the Observer may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Observable
     * @param resumeSequence
     *            the Observable that will take over if the source Observable encounters an error
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorResumeNext(final Observable<T> that, final Observable<T> resumeSequence) {
        return _create(OperationOnErrorResumeNextViaObservable.onErrorResumeNextViaObservable(that, resumeSequence));
    }

    /**
     * Instruct a Observable to emit a particular item to its Observer's <code>onNext</code> closure
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Observable encounters an error that prevents it from emitting the expected item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and then
     * quits
     * without calling any more of its Observer's
     * closures. The <code>onErrorReturn</code> method changes this behavior. If you pass a closure (<code>resumeFunction</code>) to a Observable's <code>onErrorReturn</code> method, if the original
     * Observable encounters an error, instead of calling
     * its Observer's <code>onError</code> closure, it will instead pass the return value of <code>resumeFunction</code> to the Observer's <code>onNext</code> method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     * 
     * @param that
     *            the source Observable
     * @param resumeFunction
     *            a closure that returns a value that will be passed into a Observer's <code>onNext</code> closure if the Observable encounters an error that would
     *            otherwise cause it to call <code>onError</code>
     * @return the source Observable, with its behavior modified as described
     */
    public static <T> Observable<T> onErrorReturn(final Observable<T> that, Func1<Exception, T> resumeFunction) {
        return _create(OperationOnErrorReturn.onErrorReturn(that, resumeFunction));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce.noseed&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> Observable<T> reduce(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return last(create(OperationScan.scan(sequence, accumulator)));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce.noseed&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
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
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            a seed passed into the first execution of the accumulator closure
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> Observable<T> reduce(Observable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
        return last(create(OperationScan.scan(sequence, initialValue, accumulator)));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code>
     * method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            a seed passed into the first execution of the accumulator closure
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * @return a Observable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> Observable<T> reduce(final Observable<T> sequence, final T initialValue, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return reduce(sequence, initialValue, new Func2<T, T, T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T call(T t1, T t2) {
                return (T) _f.call(t1, t2);
            }

        });
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> Observable<T> scan(Observable<T> sequence, Func2<T, T, T> accumulator) {
        return _create(OperationScan.scan(sequence, accumulator));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
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
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> Observable<T> scan(Observable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
        return _create(OperationScan.scan(sequence, initialValue, accumulator));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Observable
     * @param sequence
     *            the source Observable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Observable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> Observable<T> scan(final Observable<T> sequence, final T initialValue, final Object accumulator) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(accumulator);
        return scan(sequence, initialValue, new Func2<T, T, T>() {

            @SuppressWarnings("unchecked")
            @Override
            public T call(T t1, T t2) {
                return (T) _f.call(t1, t2);
            }

        });
    }

    /**
     * Returns a Observable that skips the first <code>num</code> items emitted by the source
     * Observable. You can ignore the first <code>num</code> items emitted by a Observable and attend
     * only to those items that come after, by modifying the Observable with the <code>skip</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.skip&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.skip.png"></a>
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items to skip
     * @return a Observable that emits the same sequence of items emitted by the source Observable,
     *         except for the first <code>num</code> items
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx">MSDN: Observable.Skip Method</a>
     */
    public static <T> Observable<T> skip(final Observable<T> items, int num) {
        return _create(OperationSkip.skip(items, num));
    }

    /**
     * Accepts a Observable and wraps it in another Observable that ensures that the resulting
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
     * @return a Observable that is a chronologically well-behaved version of the source Observable
     */
    public static <T> Observable<T> synchronize(Observable<T> observable) {
        return _create(OperationSynchronize.synchronize(observable));
    }

    /**
     * Returns a Observable that emits the first <code>num</code> items emitted by the source
     * Observable.
     * <p>
     * You can choose to pay attention only to the first <code>num</code> values emitted by a Observable by calling its <code>take</code> method. This method returns a Observable that will call a
     * subscribing Observer's <code>onNext</code> closure a
     * maximum of <code>num</code> times before calling <code>onCompleted</code>.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.take&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.take.png"></a>
     * 
     * @param items
     *            the source Observable
     * @param num
     *            the number of items from the start of the sequence emitted by the source
     *            Observable to emit
     * @return a Observable that only emits the first <code>num</code> items emitted by the source
     *         Observable
     */
    public static <T> Observable<T> take(final Observable<T> items, final int num) {
        return _create(OperationTake.take(items, num));
    }

    /**
     * Returns a Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * <p>
     * Normally, a Observable that returns multiple items will do so by calling its Observer's <code>onNext</code> closure for each such item. You can change this behavior, instructing the Observable
     * to
     * compose a list of all of these multiple items and
     * then to call the Observer's <code>onNext</code> closure once, passing it the entire list, by calling the Observable object's <code>toList</code> method prior to calling its
     * <code>subscribe</code>
     * method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolist&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolist.png"></a>
     * 
     * @param that
     *            the source Observable
     * @return a Observable that emits a single item: a <code>List</code> containing all of the
     *         items emitted by the source Observable
     */
    public static <T> Observable<List<T>> toList(final Observable<T> that) {
        return _create(OperationToObservableList.toObservableList(that));
    }

    /**
     * Converts an Iterable sequence to a Observable sequence.
     * 
     * Any object that supports the Iterable interface can be converted into a Observable that emits
     * each iterable item in the object, by passing the object into the <code>toObservable</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.toObservable&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.toObservable.png"></a>
     * 
     * @param iterable
     *            the source Iterable sequence
     * @param <T>
     *            the type of items in the iterable sequence and the type emitted by the resulting
     *            Observable
     * @return a Observable that emits each item in the source Iterable sequence
     */
    public static <T> Observable<T> toObservable(Iterable<T> iterable) {
        return _create(OperationToObservableIterable.toObservableIterable(iterable));
    }

    /**
     * Converts an Future to a Observable sequence.
     * 
     * Any object that supports the {@link Future} interface can be converted into a Observable that emits
     * the return value of the get() method in the object, by passing the object into the <code>toObservable</code> method.
     * The subscribe method on this synchronously so the Subscription returned doesn't nothing.
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of of object that the future's returns and the type emitted by the resulting
     *            Observable
     * @return a Observable that emits the item from the source Future
     */
    public static <T> Observable<T> toObservable(Future<T> future) {
        return _create(OperationToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts an Future to a Observable sequence.
     * 
     * Any object that supports the {@link Future} interface can be converted into a Observable that emits
     * the return value of the get() method in the object, by passing the object into the <code>toObservable</code> method.
     * The subscribe method on this synchronously so the Subscription returned doesn't nothing.
     * If the future timesout the {@link TimeoutException} exception is passed to the onError.
     * 
     * @param future
     *            the source {@link Future}
     * @param time
     *            the maximum time to wait
     * @param unit
     *            the time unit of the time argument
     * @param <T>
     *            the type of of object that the future's returns and the type emitted by the resulting
     *            Observable
     * @return a Observable that emits the item from the source Future
     */
    public static <T> Observable<T> toObservable(Future<T> future, long time, TimeUnit unit) {
        return _create(OperationToObservableFuture.toObservableFuture(future, time, unit));
    }

    /**
     * Converts an Array sequence to a Observable sequence.
     * 
     * An Array can be converted into a Observable that emits each item in the Array, by passing the
     * Array into the <code>toObservable</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.toObservable&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.toObservable.png"></a>
     * 
     * @param iterable
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items emitted by the resulting
     *            Observable
     * @return a Observable that emits each item in the source Array
     */
    public static <T> Observable<T> toObservable(T... items) {
        return toObservable(Arrays.asList(items));
    }

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
     * 
     * @param sequence
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence) {
        return _create(OperationToObservableSortedList.toSortedList(sequence));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted.f&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.f.png"></a>
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, Func2<T, T, Integer> sortFunction) {
        return _create(OperationToObservableSortedList.toSortedList(sequence, sortFunction));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted.f&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.f.png"></a>
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> Observable<List<T>> toSortedList(Observable<T> sequence, final Object sortFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(sortFunction);
        return _create(OperationToObservableSortedList.toSortedList(sequence, new Func2<T, T, Integer>() {

            @Override
            public Integer call(T t1, T t2) {
                return (Integer) _f.call(t1, t2);
            }

        }));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by two other Observables, with the results of this closure becoming the
     * sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>
     * and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Observable will be the result of the closure applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.png"></a>
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param reduceFunction
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
     */
    public static <R, T0, T1> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Func2<T0, T1, R> reduceFunction) {
        return _create(OperationZip.zip(w0, w1, reduceFunction));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by two other Observables, with the results of this closure becoming the
     * sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>
     * and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Observable will be the result of the closure applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.png"></a>
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param reduceFunction
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
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
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by three other Observables, with the results of this closure becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>,
     * the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Observable will be the result of the closure applied to the second item emitted by <code>w0</code>, the second item
     * emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.3&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.3.png"></a>
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
     */
    public static <R, T0, T1, T2> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Func3<T0, T1, T2, R> function) {
        return _create(OperationZip.zip(w0, w1, w2, function));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by three other Observables, with the results of this closure becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>,
     * the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Observable will be the result of the closure applied to the second item emitted by <code>w0</code>, the second item
     * emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.3&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.3.png"></a>
     * 
     * @param w0
     *            one source Observable
     * @param w1
     *            another source Observable
     * @param w2
     *            a third source Observable
     * @param function
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
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
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by four other Observables, with the results of this closure becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>,
     * the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Observable will be the result of the closure applied to the second item
     * emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.4&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.4.png"></a>
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
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> Observable<R> zip(Observable<T0> w0, Observable<T1> w1, Observable<T2> w2, Observable<T3> w3, Func4<T0, T1, T2, T3, R> reduceFunction) {
        return _create(OperationZip.zip(w0, w1, w2, w3, reduceFunction));
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by four other Observables, with the results of this closure becoming
     * the sequence emitted by the returned Observable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Observable will be the result of the closure applied to the first item emitted by <code>w0</code>,
     * the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Observable will be the result of the closure applied to the second item
     * emitted by each of those Observables; and so forth.
     * <p>
     * The resulting <code>Observable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Observable with the
     * shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.4&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.4.png"></a>
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
     *            a closure that, when applied to an item emitted by each of the source Observables,
     *            results in a value that will be emitted by the resulting Observable
     * @return a Observable that emits the zipped results
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
     * Filters a Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param predicate
     *            a closure that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
     * @return a Observable that emits only those items in the original Observable that the filter
     *         evaluates as <code>true</code>
     */
    public Observable<T> filter(Func1<T, Boolean> predicate) {
        return filter(this, predicate);
    }

    /**
     * Filters a Observable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param callback
     *            a closure that evaluates the items emitted by the source Observable, returning
     *            <code>true</code> if they pass the filter
     * @return a Observable that emits only those items in the original Observable that the filter
     *         evaluates as "true"
     */
    public Observable<T> filter(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return filter(this, new Func1<T, Boolean>() {

            public Boolean call(T t1) {
                return (Boolean) _f.call(t1);
            }
        });
    }

    /**
     * Converts a Observable that emits a sequence of objects into one that only emits the last
     * object in this sequence before completing.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.last&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.last.png"></a>
     * 
     * @return a Observable that emits only the last item emitted by the original Observable
     */
    public Observable<T> last() {
        return last(this);
    }

    /**
     * Applies a closure of your choosing to every item emitted by a Observable, and returns this
     * transformation as a new Observable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param func
     *            a closure to apply to each item in the sequence.
     * @return a Observable that emits a sequence that is the result of applying the transformation
     *         closure to each item in the sequence emitted by the input Observable.
     */
    public <R> Observable<R> map(Func1<T, R> func) {
        return map(this, func);
    }

    /**
     * Applies a closure of your choosing to every item emitted by a Observable, and returns this
     * transformation as a new Observable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param callback
     *            a closure to apply to each item in the sequence.
     * @return a Observable that emits a sequence that is the result of applying the transformation
     *         closure to each item in the sequence emitted by the input Observable.
     */
    public <R> Observable<R> map(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return map(this, new Func1<T, R>() {

            @SuppressWarnings("unchecked")
            public R call(T t1) {
                return (R) _f.call(t1);
            }
        });
    }

    /**
     * Creates a new Observable sequence by applying a closure that you supply to each item in the
     * original Observable sequence, where that closure is itself a Observable that emits items, and
     * then merges the results of that closure applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param func
     *            a closure to apply to each item in the sequence, that returns a Observable.
     * @return a Observable that emits a sequence that is the result of applying the transformation
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
     */
    public <R> Observable<R> mapMany(Func1<T, Observable<R>> func) {
        return mapMany(this, func);
    }

    /**
     * Creates a new Observable sequence by applying a closure that you supply to each item in the
     * original Observable sequence, where that closure is itself a Observable that emits items, and
     * then merges the results of that closure applied to every item emitted by the original
     * Observable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param callback
     *            a closure to apply to each item in the sequence that returns a Observable.
     * @return a Observable that emits a sequence that is the result of applying the transformation'
     *         function to each item in the input sequence and merging the results of the
     *         Observables obtained from this transformation.
     */
    public <R> Observable<R> mapMany(final Object callback) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(callback);
        return mapMany(this, new Func1<T, Observable<R>>() {

            @SuppressWarnings("unchecked")
            public Observable<R> call(T t1) {
                return (Observable<R>) _f.call(t1);
            }
        });
    }

    /**
     * Materializes the implicit notifications of this observable sequence as explicit notification values.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.materialize&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.materialize.png"></a>
     * 
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
     */
    public Observable<Notification<T>> materialize() {
        return materialize(this);
    }

    /**
     * Instruct a Observable to pass control to another Observable rather than calling <code>onError</code> if it encounters an error.
     * 
     * By default, when a Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeFunction</code>) to a Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onErrort</code> closure, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param resumeFunction
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Func1<Exception, Observable<T>> resumeFunction) {
        return onErrorResumeNext(this, resumeFunction);
    }

    /**
     * Instruct a Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * 
     * By default, when a Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeFunction</code>) to a Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> closure, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param resumeFunction
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Object resumeFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(resumeFunction);
        return onErrorResumeNext(this, new Func1<Exception, Observable<T>>() {

            @SuppressWarnings("unchecked")
            public Observable<T> call(Exception e) {
                return (Observable<T>) _f.call(e);
            }
        });
    }

    /**
     * Instruct a Observable to pass control to another Observable rather than calling
     * <code>onError</code> if it encounters an error.
     * 
     * By default, when a Observable encounters an error that prevents it from emitting the expected
     * item to its Observer, the Observable calls its Observer's <code>onError</code> closure, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another Observable
     * (<code>resumeSequence</code>) to a Observable's <code>onErrorResumeNext</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> closure, it will instead relinquish control to
     * <code>resumeSequence</code> which will call the Observer's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Observable necessarily invokes
     * <code>onError</code>, the Observer may never know that an error happened.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param resumeSequence
     * @return the original Observable, with appropriately modified behavior
     */
    public Observable<T> onErrorResumeNext(final Observable<T> resumeSequence) {
        return onErrorResumeNext(this, resumeSequence);
    }

    /**
     * Instruct a Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * 
     * By default, when a Observable encounters an error that prevents it from emitting the expected
     * object to its Observer, the Observable calls its Observer's <code>onError</code> closure, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a closure
     * (<code>resumeFunction</code>) to a Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> closure, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Observer's <code>onNext</code> method.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     * 
     * @param resumeFunction
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorReturn(Func1<Exception, T> resumeFunction) {
        return onErrorReturn(this, resumeFunction);
    }

    /**
     * Instruct a Observable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     * 
     * By default, when a Observable encounters an error that prevents it from emitting the expected
     * object to its Observer, the Observable calls its Observer's <code>onError</code> closure, and
     * then quits without calling any more of its Observer's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a closure
     * (<code>resumeFunction</code>) to a Observable's <code>onErrorReturn</code> method, if the
     * original Observable encounters an error, instead of calling its Observer's
     * <code>onError</code> closure, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Observer's <code>onNext</code> method.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     * 
     * @param that
     * @param resumeFunction
     * @return the original Observable with appropriately modified behavior
     */
    public Observable<T> onErrorReturn(final Object resumeFunction) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(resumeFunction);
        return onErrorReturn(this, new Func1<Exception, T>() {

            @SuppressWarnings("unchecked")
            public T call(Exception e) {
                return (T) _f.call(e);
            }
        });
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose result
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
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose result
     *            will be used in the next accumulator call (if applicable).
     * 
     * @return A Observable that emits a single element from the result of accumulating the output
     *         from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public Observable<T> reduce(Object accumulator) {
        return reduce(this, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable).
     * 
     * @return A Observable that emits a single element from the result of accumulating the output
     *         from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public Observable<T> reduce(T initialValue, Func2<T, T, T> accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable).
     * @return A Observable that emits a single element from the result of accumulating the output
     *         from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public Observable<T> reduce(T initialValue, Object accumulator) {
        return reduce(this, initialValue, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations. It emits the result of
     * each of these iterations as a sequence from the returned Observable. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/showgliffyeditor.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465&t=marble.scan.noseed"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * @return A Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(Func2<T, T, T> accumulator) {
        return scan(this, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations. It emits the result of
     * each of these iterations as a sequence from the returned Observable. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/showgliffyeditor.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465&t=marble.scan.noseed"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * 
     * @return A Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(final Object accumulator) {
        return scan(this, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, and so on until all items have been emitted by the
     * source Observable, emitting the result of each of these iterations. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * @return A Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(T initialValue, Func2<T, T, T> accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * Returns a Observable that applies a closure of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that closure along with the second item emitted
     * by a Observable into the same closure, then feeds the result of that closure along with the
     * third item into the same closure, and so on, emitting the result of each of these
     * iterations. This sort of closure is sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose result
     *            will be sent via <code>onNext</code> and used in the next accumulator call (if
     *            applicable).
     * @return A Observable sequence whose elements are the result of accumulating the output from
     *         the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public Observable<T> scan(final T initialValue, final Object accumulator) {
        return scan(this, initialValue, accumulator);
    }

    /**
     * Returns a Observable that skips the first <code>num</code> items emitted by the source
     * Observable.
     * You can ignore the first <code>num</code> items emitted by a Observable and attend only to
     * those items that come after, by modifying the Observable with the <code>skip</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.skip&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.skip.png"></a>
     * 
     * @param num
     *            The number of items to skip
     * @return A Observable sequence that is identical to the source Observable except that it does
     *         not emit the first <code>num</code> items from that sequence.
     */
    public Observable<T> skip(int num) {
        return skip(this, num);
    }

    /**
     * Returns a Observable that emits the first <code>num</code> items emitted by the source
     * Observable.
     * 
     * You can choose to pay attention only to the first <code>num</code> values emitted by a
     * Observable by calling its <code>take</code> method. This method returns a Observable that will
     * call a subscribing Observer's <code>onNext</code> closure a maximum of <code>num</code> times
     * before calling <code>onCompleted</code>.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.take&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.take.png"></a>
     * 
     * @param num
     * @return a Observable that emits only the first <code>num</code> items from the source
     *         Observable, or all of the items from the source Observable if that Observable emits
     *         fewer than <code>num</code> items.
     */
    public Observable<T> take(final int num) {
        return take(this, num);
    }

    /**
     * Returns a Observable that emits a single item, a list composed of all the items emitted by
     * the source Observable.
     * 
     * Normally, a Observable that returns multiple items will do so by calling its Observer's
     * <code>onNext</code> closure for each such item. You can change this behavior, instructing
     * the Observable to compose a list of all of these multiple items and then to call the
     * Observer's <code>onNext</code> closure once, passing it the entire list, by calling the
     * Observable object's <code>toList</code> method prior to calling its <code>subscribe</code>
     * method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolist&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolist.png"></a>
     * 
     * @return a Observable that emits a single item: a List containing all of the items emitted by
     *         the source Observable.
     */
    public Observable<List<T>> toList() {
        return toList(this);
    }

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
     * 
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return
     */
    public Observable<List<T>> toSortedList() {
        return toSortedList(this);
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
     * 
     * @param sortFunction
     * @return
     */
    public Observable<List<T>> toSortedList(Func2<T, T, Integer> sortFunction) {
        return toSortedList(this, sortFunction);
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
     * 
     * @param sortFunction
     * @return
     */
    public Observable<List<T>> toSortedList(final Object sortFunction) {
        return toSortedList(this, sortFunction);
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
                    return Observable.noOpSubscription();
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

    }
}
