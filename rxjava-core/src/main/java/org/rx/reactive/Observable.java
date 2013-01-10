package org.rx.reactive;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.functions.Func1;
import org.rx.functions.Func2;
import org.rx.functions.Functions;
import org.rx.operations.ObservableExtensions;
import org.rx.operations.OperationMaterialize;

/**
 * The Observable interface that implements the Reactive Pattern.
 * <p>
 * The documentation for this interface makes use of marble diagrams. The following legend explains
 * these diagrams:
 * <p>
 * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.legend&ceoid=27321465&key=API&pageId=27321465"><img
 * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.legend.png"></a>
 * <p>
 * It provides overloaded methods for subscribing as well as delegate methods to the ObservableExtensions.
 * 
 * @param <T>
 */
public abstract class Observable<T> {

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
    public abstract Subscription subscribe(Observer<T> observer);

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
    public Subscription subscribe(final Object onNext) {
        if (onNext instanceof Observer) {
            throw new RuntimeException("Observers are not intended to be passed to this generic method. Your generic type is most likely wrong. This method is for dynamic code to send in closures.");
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
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
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
        Functions.execute(callback, args);
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
    public Observable<T> filter(Func1<Boolean, T> predicate) {
        return ObservableExtensions.filter(this, predicate);
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
        return ObservableExtensions.filter(this, new Func1<Boolean, T>() {

            public Boolean call(T t1) {
                return Functions.execute(callback, t1);
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
        return ObservableExtensions.last(this);
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
    public <R> Observable<R> map(Func1<R, T> func) {
        return ObservableExtensions.map(this, func);
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
        return ObservableExtensions.map(this, new Func1<R, T>() {

            public R call(T t1) {
                return Functions.execute(callback, t1);
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
    public <R> Observable<R> mapMany(Func1<Observable<R>, T> func) {
        return ObservableExtensions.mapMany(this, func);
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
        return ObservableExtensions.mapMany(this, new Func1<Observable<R>, T>() {

            public Observable<R> call(T t1) {
                return Functions.execute(callback, t1);
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
        return OperationMaterialize.materialize(this);
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
    public Observable<T> onErrorResumeNext(final Func1<Observable<T>, Exception> resumeFunction) {
        return ObservableExtensions.onErrorResumeNext(this, resumeFunction);
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
        return ObservableExtensions.onErrorResumeNext(this, resumeSequence);
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
        return ObservableExtensions.onErrorResumeNext(this, new Func1<Observable<T>, Exception>() {

            public Observable<T> call(Exception e) {
                return Functions.execute(resumeFunction, e);
            }
        });
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
    public Observable<T> onErrorReturn(Func1<T, Exception> resumeFunction) {
        return ObservableExtensions.onErrorReturn(this, resumeFunction);
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
        return ObservableExtensions.onErrorReturn(this, new Func1<T, Exception>() {

            public T call(Exception e) {
                return Functions.execute(resumeFunction, e);
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
        return ObservableExtensions.reduce(this, accumulator);
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
        return ObservableExtensions.reduce(this, accumulator);
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
        return ObservableExtensions.reduce(this, initialValue, accumulator);
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
        return ObservableExtensions.reduce(this, initialValue, accumulator);
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
        return ObservableExtensions.scan(this, accumulator);
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
        return ObservableExtensions.scan(this, accumulator);
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
        return ObservableExtensions.scan(this, initialValue, accumulator);
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
        return ObservableExtensions.scan(this, initialValue, accumulator);
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
        return ObservableExtensions.skip(this, num);
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
        return ObservableExtensions.take(this, num);
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
        return ObservableExtensions.toList(this);
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
        return ObservableExtensions.toSortedList(this);
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
    public Observable<List<T>> toSortedList(Func2<Integer, T, T> sortFunction) {
        return ObservableExtensions.toSortedList(this, sortFunction);
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
        return ObservableExtensions.toSortedList(this, sortFunction);
    }

    public static class UnitTest {
        @Mock
        ScriptAssertion assertion;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testLast() {
            String script = "mockApiCall.getObservable().last().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
        }

        @Test
        public void testMap() {
            String script = "mockApiCall.getObservable().map({v -> 'say' + v}).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("sayhello_1");
        }

        @Test
        public void testScriptWithOnNext() {
            String script = "mockApiCall.getObservable().subscribe({ result -> a.received(result)})";
            runGroovyScript(script);
            verify(assertion).received("hello_1");
        }

        @Test
        public void testScriptWithMerge() {
            String script = "o.merge(mockApiCall.getObservable(), mockApiCall.getObservable()).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
            verify(assertion, times(1)).received("hello_2");
        }

        @Test
        public void testScriptWithMaterialize() {
            String script = "mockApiCall.getObservable().materialize().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            // 2 times: once for hello_1 and once for onCompleted
            verify(assertion, times(2)).received(any(Notification.class));
        }

        @Test
        public void testToSortedList() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList().subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListWithFunction() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList({a, b -> a - b}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        private void runGroovyScript(String script) {
            ClassLoader parent = getClass().getClassLoader();
            @SuppressWarnings("resource")
            GroovyClassLoader loader = new GroovyClassLoader(parent);

            Binding binding = new Binding();
            binding.setVariable("mockApiCall", new TestFactory());
            binding.setVariable("a", assertion);
            binding.setVariable("o", org.rx.operations.ObservableExtensions.class);

            /* parse the script and execute it */
            InvokerHelper.createScript(loader.parseClass(script), binding).run();
        }

        private static class TestFactory {
            int counter = 1;

            @SuppressWarnings("unused")
            public TestObservable getObservable() {
                return new TestObservable(counter++);
            }

            @SuppressWarnings("unused")
            public Observable<Integer> getNumbers() {
                return ObservableExtensions.toObservable(1, 3, 2, 5, 4);
            }
        }

        private static class TestObservable extends Observable<String> {
            private final int count;

            public TestObservable(int count) {
                this.count = count;
            }

            public Subscription subscribe(Observer<String> observer) {

                observer.onNext("hello_" + count);
                observer.onCompleted();

                return new Subscription() {

                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static interface ScriptAssertion {
            public void received(Object o);
        }
    }

}
