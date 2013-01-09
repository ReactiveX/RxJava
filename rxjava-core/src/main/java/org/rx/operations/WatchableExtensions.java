package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.functions.Func1;
import org.rx.functions.Func2;
import org.rx.functions.Func3;
import org.rx.functions.Func4;
import org.rx.functions.Functions;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.Notification;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * A set of methods for creating, combining, and consuming Watchables.
 * <p>
 * Includes overloaded methods that handle the generic Object types being passed in for closures so that we can support Closure and RubyProc and other such types from dynamic languages.
 * <p>
 * See Functions.execute for supported types.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.legend&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.legend.png"></a>
 * 
 * @see <a href="https://confluence.corp.netflix.com/display/API/Watchers%2C+Watchables%2C+and+the+Reactive+Pattern">API.Next Programmer's Guide: Watchers, Watchables, and the Reactive Pattern</a>
 * @see <a href="http://www.scala-lang.org/api/current/index.html#scala.collection.Seq">scala.collection: Seq</a>
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable(v=vs.103).aspx">MSDN: Observable Class</a>
 * @see <a href="http://rxwiki.wikidot.com/observable-operators">Reactive Framework Wiki: Observable Operators</a>
 */
public class WatchableExtensions {

    /**
     * A Watchable that never sends any information to a Watcher.
     * 
     * This Watchable is useful primarily for testing purposes.
     * 
     * @param <T>
     *            the type of item emitted by the Watchable
     */
    private static class NeverWatchable<T> extends AbstractIObservable<T> {
        public IDisposable subscribe(IObserver<T> observer) {
            return new NullWatchableSubscription();
        }
    }

    /**
     * A disposable object that does nothing when its unsubscribe method is called.
     */
    private static class NullWatchableSubscription implements IDisposable {
        public void unsubscribe() {
        }
    }

    /**
     * A Watchable that calls a Watcher's <code>onError</code> closure when the Watcher subscribes.
     * 
     * @param <T>
     *            the type of object returned by the Watchable
     */
    private static class ThrowWatchable<T> extends AbstractIObservable<T> {
        private Exception exception;

        public ThrowWatchable(Exception exception) {
            this.exception = exception;
        }

        /**
         * Accepts a Watcher and calls its <code>onError</code> method.
         * 
         * @param observer
         *            a Watcher of this Watchable
         * @return a reference to the subscription
         */
        public IDisposable subscribe(IObserver<T> observer) {
            observer.onError(this.exception);
            return new NullWatchableSubscription();
        }
    }

    /**
     * Creates a Watchable that will execute the given function when a Watcher subscribes to it.
     * <p>
     * You can create a simple Watchable from scratch by using the <code>create</code> method. You pass this method a closure that accepts as a parameter the map of closures that a Watcher passes to a Watchable's <code>subscribe</code> method. Write
     * the closure you pass to <code>create</code> so that it behaves as a Watchable - calling the passed-in <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods appropriately.
     * <p>
     * A well-formed Watchable must call either the Watcher's <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * 
     * @param <T>
     *            the type emitted by the Watchable sequence
     * @param func
     *            a closure that accepts a <code>Watcher<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a <code>WatchableSubscription</code> to allow
     *            cancelling the subscription (if applicable)
     * @return a Watchable that, when a Watcher subscribes to it, will execute the given function
     */
    public static <T> IObservable<T> create(Func1<IDisposable, IObserver<T>> func) {
        return wrap(new OperationToWatchableFunction<T>(func));
    }

    /**
     * Creates a Watchable that will execute the given function when a Watcher subscribes to it.
     * <p>
     * You can create a simple Watchable from scratch by using the <code>create</code> method. You pass this method a closure that accepts as a parameter the map of closures that a Watcher passes to a Watchable's <code>subscribe</code> method. Write
     * the closure you pass to <code>create</code> so that it behaves as a Watchable - calling the passed-in <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods appropriately.
     * <p>
     * A well-formed Watchable must call either the Watcher's <code>onCompleted</code> method exactly once or its <code>onError</code> method exactly once.
     * 
     * @param <T>
     *            the type of the observable sequence
     * @param func
     *            a closure that accepts a <code>Watcher<T></code> and calls its <code>onNext</code>, <code>onError</code>, and <code>onCompleted</code> methods
     *            as appropriate, and returns a <code>WatchableSubscription</code> to allow
     *            cancelling the subscription (if applicable)
     * @return a Watchable that, when a Watcher subscribes to it, will execute the given function
     */
    public static <T> IObservable<T> create(final Object callback) {
        return create(new Func1<IDisposable, IObserver<T>>() {

            @Override
            public IDisposable call(IObserver<T> t1) {
                return Functions.execute(callback, t1);
            }

        });
    }

    /**
     * Returns a Watchable that returns no data to the Watcher and immediately invokes its <code>onCompleted</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.empty&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.empty.png"></a>
     * 
     * @param <T>
     *            the type of item emitted by the Watchable
     * @return a Watchable that returns no data to the Watcher and immediately invokes the
     *         Watcher's <code>onCompleted</code> method
     */
    public static <T> IObservable<T> empty() {
        return toWatchable(new ArrayList<T>());
    }

    /**
     * Returns a Watchable that calls <code>onError</code> when a Watcher subscribes to it.
     * <p>
     * Note: Maps to <code>Observable.Throw</code> in Rx - throw is a reserved word in Java.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.error&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.error.png"></a>
     * 
     * @param exception
     *            the error to throw
     * @param <T>
     *            the type of object returned by the Watchable
     * @return a Watchable object that calls <code>onError</code> when a Watcher subscribes
     */
    public static <T> IObservable<T> error(Exception exception) {
        return wrap(new ThrowWatchable<T>(exception));
    }

    /**
     * Filters a Watchable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param that
     *            the Watchable to filter
     * @param predicate
     *            a closure that evaluates the items emitted by the source Watchable, returning <code>true</code> if they pass the filter
     * @return a Watchable that emits only those items in the original Watchable that the filter
     *         evaluates as true
     */
    public static <T> IObservable<T> filter(IObservable<T> that, Func1<Boolean, T> predicate) {
        return new OperationFilter<T>(that, predicate);
    }

    /**
     * Filters a Watchable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param that
     *            the Watchable to filter
     * @param predicate
     *            a closure that evaluates the items emitted by the source Watchable, returning <code>true</code> if they pass the filter
     * @return a Watchable that emits only those items in the original Watchable that the filter
     *         evaluates as true
     */
    public static <T> IObservable<T> filter(IObservable<T> that, final Object function) {
        return filter(that, new Func1<Boolean, T>() {

            @Override
            public Boolean call(T t1) {
                return Functions.execute(function, t1);

            }

        });
    }

    /**
     * Returns a Watchable that notifies an observer of a single value and then completes.
     * <p>
     * To convert any object into a Watchable that emits that object, pass that object into the <code>just</code> method.
     * <p>
     * This is similar to the {@link toWatchable} method, except that <code>toWatchable</code> will convert an iterable object into a Watchable that emits each of the items in the iterable, one at a time, while the <code>just</code> method would
     * convert the iterable into a Watchable that emits the entire iterable as a single item.
     * <p>
     * This value is the equivalent of <code>Observable.Return</code> in the Reactive Extensions library.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.just&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.just.png"></a>
     * 
     * @param value
     *            the value to pass to the Watcher's <code>onNext</code> method
     * @param <T>
     *            the type of the value
     * @return a Watchable that notifies a Watcher of a single value and then completes
     */
    public static <T> IObservable<T> just(T value) {
        List<T> list = new ArrayList<T>();
        list.add(value);

        return toWatchable(list);
    }

    /**
     * Takes the last item emitted by a source Watchable and returns a Watchable that emits only
     * that item as its sole emission.
     * <p>
     * To convert a Watchable that emits a sequence of objects into one that only emits the last object in this sequence before completing, use the <code>last</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.last&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.last.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @return a Watchable that emits a single item, which is identical to the last item emitted
     *         by the source Watchable
     */
    public static <T> IObservable<T> last(final IObservable<T> that) {
        return wrap(new OperationLast<T>(that));
    }

    /**
     * Applies a closure of your choosing to every notification emitted by a Watchable, and returns
     * this transformation as a new Watchable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param sequence
     *            the source Watchable
     * @param func
     *            a closure to apply to each item in the sequence emitted by the source Watchable
     * @param <T>
     *            the type of items emitted by the the source Watchable
     * @param <R>
     *            the type of items returned by map closure <code>func</code>
     * @return a Watchable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Watchable
     */
    public static <T, R> IObservable<R> map(IObservable<T> sequence, Func1<R, T> func) {
        return wrap(OperationMap.map(sequence, func));
    }

    /**
     * Applies a closure of your choosing to every notification emitted by a Watchable, and returns
     * this transformation as a new Watchable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     * 
     * @param sequence
     *            the source Watchable
     * @param function
     *            a closure to apply to each item in the sequence emitted by the source Watchable
     * @param <T>
     *            the type of items emitted by the the source Watchable
     * @param <R>
     *            the type of items returned by map closure <code>function</code>
     * @return a Watchable that is the result of applying the transformation function to each item
     *         in the sequence emitted by the source Watchable
     */
    public static <T, R> IObservable<R> map(IObservable<T> sequence, final Object function) {
        return map(sequence, new Func1<R, T>() {

            @Override
            public R call(T t1) {
                return Functions.execute(function, t1);
            }

        });
    }

    /**
     * Creates a new Watchable sequence by applying a closure that you supply to each object in the
     * original Watchable sequence, where that closure is itself a Watchable that emits objects,
     * and then merges the results of that closure applied to every item emitted by the original
     * Watchable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param sequence
     *            the source Watchable
     * @param func
     *            a closure to apply to each item emitted by the source Watchable, generating a
     *            Watchable
     * @param <T>
     *            the type emitted by the source Watchable
     * @param <R>
     *            the type emitted by the Watchables emitted by <code>func</code>
     * @return a Watchable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Watchable and merging the results of
     *         the Watchables obtained from this transformation
     */
    public static <T, R> IObservable<R> mapMany(IObservable<T> sequence, Func1<IObservable<R>, T> func) {
        return wrap(OperationMap.mapMany(sequence, func));
    }

    /**
     * Creates a new Watchable sequence by applying a closure that you supply to each object in the
     * original Watchable sequence, where that closure is itself a Watchable that emits objects,
     * and then merges the results of that closure applied to every item emitted by the original
     * Watchable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     * 
     * @param sequence
     *            the source Watchable
     * @param function
     *            a closure to apply to each item emitted by the source Watchable, generating a
     *            Watchable
     * @param <T>
     *            the type emitted by the source Watchable
     * @param <R>
     *            the type emitted by the Watchables emitted by <code>function</code>
     * @return a Watchable that emits a sequence that is the result of applying the transformation
     *         function to each item emitted by the source Watchable and merging the results of the
     *         Watchables obtained from this transformation
     */
    public static <T, R> IObservable<R> mapMany(IObservable<T> sequence, final Object function) {
        return mapMany(sequence, new Func1<R, T>() {

            @Override
            public R call(T t1) {
                return Functions.execute(function, t1);
            }

        });
    }

    /**
     * Materializes the implicit notifications of an observable sequence as explicit notification values.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.materialize&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.materialize.png"></a>
     * 
     * @param source
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
     * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
     */
    public static <T> IObservable<Notification<T>> materialize(final IObservable<T> sequence) {
        return OperationMaterialize.materialize(sequence);
    }

    /**
     * Flattens the Watchable sequences from a list of Watchables into one Watchable sequence
     * without any transformation. You can combine the output of multiple Watchables so that they
     * act like a single Watchable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.png"></a>
     * 
     * @param source
     *            a list of Watchables that emit sequences of items
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Watchables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> merge(List<IObservable<T>> source) {
        return wrap(OperationMerge.merge(source));
    }

    /**
     * Flattens the Watchable sequences from a series of Watchables into one Watchable sequence
     * without any transformation. You can combine the output of multiple Watchables so that they
     * act like a single Watchable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.png"></a>
     * 
     * @param source
     *            a series of Watchables that emit sequences of items
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Watchables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> merge(IObservable<T>... source) {
        return wrap(OperationMerge.merge(source));
    }

    /**
     * Flattens the Watchable sequences emitted by a sequence of Watchables that are emitted by a
     * Watchable into one Watchable sequence without any transformation. You can combine the output
     * of multiple Watchables so that they act like a single Watchable, by using the <code>merge</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.merge.W&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.merge.W.png"></a>
     * 
     * @param source
     *            a Watchable that emits Watchables
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the Watchables emitted by the <code>source</code> Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> merge(IObservable<IObservable<T>> source) {
        return wrap(OperationMerge.merge(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.png"></a>
     * 
     * @param source
     *            a list of Watchables that emit sequences of items
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> list of Watchables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> mergeDelayError(List<IObservable<T>> source) {
        return wrap(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.png"></a>
     * 
     * @param source
     *            a series of Watchables that emit sequences of items
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the <code>source</code> Watchables
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> mergeDelayError(IObservable<T>... source) {
        return wrap(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Same functionality as <code>merge</code> except that errors received to onError will be held until all sequences have finished (onComplete/onError) before sending the error.
     * <p>
     * Only the first onError received will be sent.
     * <p>
     * This enables receiving all successes from merged sequences without one onError from one sequence causing all onNext calls to be prevented.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mergeDelayError.W&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mergeDelayError.W.png"></a>
     * 
     * @param source
     *            a Watchable that emits Watchables
     * @return a Watchable that emits a sequence of elements that are the result of flattening the
     *         output from the Watchables emitted by the <code>source</code> Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">MSDN: Observable.Merge Method</a>
     */
    public static <T> IObservable<T> mergeDelayError(IObservable<IObservable<T>> source) {
        return wrap(OperationMergeDelayError.mergeDelayError(source));
    }

    /**
     * Returns a Watchable that never sends any information to a Watcher.
     * 
     * This observable is useful primarily for testing purposes.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.never&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.never.png"></a>
     * 
     * @param <T>
     *            the type of item (not) emitted by the Watchable
     * @return a Watchable that never sends any information to a Watcher
     */
    public static <T> IObservable<T> never() {
        return wrap(new NeverWatchable<T>());
    }

    /**
     * A WatchableSubscription that does nothing.
     * 
     * @return
     */
    public static IDisposable noOpSubscription() {
        return new NullWatchableSubscription();
    }

    /**
     * Instruct a Watchable to pass control to another Watchable rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Watchable encounters an error that prevents it from emitting the expected item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and then quits without calling any more of its Watcher's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass another Watchable (<code>resumeSequence</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the original Watchable encounters an error,
     * instead of calling its Watcher's <code>onError</code> closure, it will instead relinquish control to <code>resumeSequence</code> which will call the Watcher's <code>onNext</code> method if it is able to do so. In such a case, because no
     * Watchable necessarily invokes <code>onError</code>, the Watcher may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @param resumeSequence
     *            the Watchable that will take over if the source Watchable encounters an error
     * @return the source Watchable, with its behavior modified as described
     */
    public static <T> IObservable<T> onErrorResumeNext(final IObservable<T> that, final IObservable<T> resumeSequence) {
        return wrap(new OperationOnErrorResumeNextViaWatchable<T>(that, resumeSequence));
    }

    /**
     * Instruct a Watchable to pass control to another Watchable (the return value of a function)
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Watchable encounters an error that prevents it from emitting the expected item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and then quits without calling any more of its Watcher's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a closure that emits a Watchable (<code>resumeFunction</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the original Watchable encounters
     * an error, instead of calling its Watcher's <code>onError</code> closure, it will instead relinquish control to this new Watchable, which will call the Watcher's <code>onNext</code> method if it is able to do so. In such a case, because no
     * Watchable necessarily invokes <code>onError</code>, the Watcher may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @param resumeFunction
     *            a closure that returns a Watchable that will take over if the source Watchable
     *            encounters an error
     * @return the source Watchable, with its behavior modified as described
     */
    public static <T> IObservable<T> onErrorResumeNext(final IObservable<T> that, final Func1<IObservable<T>, Exception> resumeFunction) {
        return wrap(new OperationOnErrorResumeNextViaFunction<T>(that, resumeFunction));
    }

    /**
     * Instruct a Watchable to emit a particular object (as returned by a closure) to its Watcher
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Watchable encounters an error that prevents it from emitting the expected item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and then quits without calling any more of its Watcher's
     * closures. The <code>onErrorResumeNext</code> method changes this behavior. If you pass a function that returns another Watchable (<code>resumeFunction</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the original Watchable
     * encounters an error, instead of calling its Watcher's <code>onError</code> closure, it will instead relinquish control to this new Watchable which will call the Watcher's <code>onNext</code> method if it is able to do so. In such a case,
     * because no Watchable necessarily invokes <code>onError</code>, the Watcher may never know that an error happened.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @param resumeFunction
     *            a closure that returns a Watchable that will take over if the source Watchable
     *            encounters an error
     * @return the source Watchable, with its behavior modified as described
     */
    public static <T> IObservable<T> onErrorResumeNext(final IObservable<T> that, final Object resumeFunction) {
        return onErrorResumeNext(that, new Func1<IObservable<T>, Exception>() {

            @Override
            public IObservable<T> call(Exception e) {
                return Functions.execute(resumeFunction, e);
            }
        });
    }

    /**
     * Instruct a Watchable to emit a particular item to its Watcher's <code>onNext</code> closure
     * rather than calling <code>onError</code> if it encounters an error.
     * <p>
     * By default, when a Watchable encounters an error that prevents it from emitting the expected item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and then quits without calling any more of its Watcher's
     * closures. The <code>onErrorReturn</code> method changes this behavior. If you pass a closure (<code>resumeFunction</code>) to a Watchable's <code>onErrorReturn</code> method, if the original Watchable encounters an error, instead of calling
     * its Watcher's <code>onError</code> closure, it will instead pass the return value of <code>resumeFunction</code> to the Watcher's <code>onNext</code> method.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @param resumeFunction
     *            a closure that returns a value that will be passed into a Watcher's <code>onNext</code> closure if the Watchable encounters an error that would
     *            otherwise cause it to call <code>onError</code>
     * @return the source Watchable, with its behavior modified as described
     */
    public static <T> IObservable<T> onErrorReturn(final IObservable<T> that, Func1<T, Exception> resumeFunction) {
        return wrap(new OperationOnErrorReturn<T>(that, resumeFunction));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce.noseed&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Watchable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> IObservable<T> reduce(IObservable<T> sequence, Func2<T, T, T> accumulator) {
        return wrap(OperationScan.scan(sequence, accumulator).last());
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce.noseed&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Watchable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> IObservable<T> reduce(final IObservable<T> sequence, final Object accumulator) {
        return reduce(sequence, new Func2<T, T, T>() {

            @Override
            public T call(T t1, T t2) {
                return Functions.execute(accumulator, t1, t2);
            }

        });
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param initialValue
     *            a seed passed into the first execution of the accumulator closure
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * 
     * @return a Watchable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> IObservable<T> reduce(IObservable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
        return wrap(OperationScan.scan(sequence, initialValue, accumulator).last());
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance, has an <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param initialValue
     *            a seed passed into the first execution of the accumulator closure
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable)
     * @return a Watchable that emits a single element that is the result of accumulating the
     *         output from applying the accumulator to the sequence of items emitted by the source
     *         Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> IObservable<T> reduce(final IObservable<T> sequence, final T initialValue, final Object accumulator) {
        return reduce(sequence, initialValue, new Func2<T, T, T>() {

            @Override
            public T call(T t1, T t2) {
                return Functions.execute(accumulator, t1, t2);
            }

        });
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Watchable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> IObservable<T> scan(IObservable<T> sequence, Func2<T, T, T> accumulator) {
        return wrap(OperationScan.scan(sequence, accumulator));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Watchable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> IObservable<T> scan(final IObservable<T> sequence, final Object accumulator) {
        return scan(sequence, new Func2<T, T, T>() {

            @Override
            public T call(T t1, T t2) {
                return Functions.execute(accumulator, t1, t2);
            }

        });
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Watchable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> IObservable<T> scan(IObservable<T> sequence, T initialValue, Func2<T, T, T> accumulator) {
        return wrap(OperationScan.scan(sequence, initialValue, accumulator));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     * 
     * @param <T>
     *            the type item emitted by the source Watchable
     * @param sequence
     *            the source Watchable
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator closure to be invoked on each element from the sequence, whose
     *            result will be emitted and used in the next accumulator call (if applicable)
     * @return a Watchable that emits a sequence of items that are the result of accumulating the
     *         output from the sequence emitted by the source Watchable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public static <T> IObservable<T> scan(final IObservable<T> sequence, final T initialValue, final Object accumulator) {
        return scan(sequence, initialValue, new Func2<T, T, T>() {

            @Override
            public T call(T t1, T t2) {
                return Functions.execute(accumulator, t1, t2);
            }

        });
    }

    /**
     * Returns a Watchable that skips the first <code>num</code> items emitted by the source
     * Watchable. You can ignore the first <code>num</code> items emitted by a watchable and attend
     * only to those items that come after, by modifying the watchable with the <code>skip</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.skip&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.skip.png"></a>
     * 
     * @param items
     *            the source Watchable
     * @param num
     *            the number of items to skip
     * @return a Watchable that emits the same sequence of items emitted by the source Watchable,
     *         except for the first <code>num</code> items
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx">MSDN: Observable.Skip Method</a>
     */
    public static <T> IObservable<T> skip(final IObservable<T> items, int num) {
        return wrap(OperationSkip.skip(items, num));
    }

    /**
     * Accepts a Watchable and wraps it in another Watchable that ensures that the resulting
     * Watchable is chronologically well-behaved.
     * <p>
     * A well-behaved observable ensures <code>onNext</code>, <code>onCompleted</code>, or <code>onError</code> calls to its subscribers are not interleaved, <code>onCompleted</code> and <code>onError</code> are only called once respectively, and no
     * <code>onNext</code> calls follow <code>onCompleted</code> and <code>onError</code> calls.
     * 
     * @param observable
     *            the source Watchable
     * @param <T>
     *            the type of item emitted by the source Watchable
     * @return a watchable that is a chronologically well-behaved version of the source watchable
     */
    public static <T> IObservable<T> synchronize(IObservable<T> observable) {
        return wrap(new OperationSynchronize<T>(observable));
    }

    /**
     * Returns a Watchable that emits the first <code>num</code> items emitted by the source
     * Watchable.
     * <p>
     * You can choose to pay attention only to the first <code>num</code> values emitted by a Watchable by calling its <code>take</code> method. This method returns a Watchable that will call a subscribing Watcher's <code>onNext</code> closure a
     * maximum of <code>num</code> times before calling <code>onCompleted</code>.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.take&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.take.png"></a>
     * 
     * @param items
     *            the source Watchable
     * @param num
     *            the number of items from the start of the sequence emitted by the source
     *            Watchable to emit
     * @return a Watchable that only emits the first <code>num</code> items emitted by the source
     *         Watchable
     */
    public static <T> IObservable<T> take(final IObservable<T> items, final int num) {
        return wrap(OperationTake.take(items, num));
    }

    /**
     * Returns a Watchable that emits a single item, a list composed of all the items emitted by
     * the source Watchable.
     * <p>
     * Normally, a Watchable that returns multiple items will do so by calling its Watcher's <code>onNext</code> closure for each such item. You can change this behavior, instructing the Watchable to compose a list of all of these multiple items and
     * then to call the Watcher's <code>onNext</code> closure once, passing it the entire list, by calling the Watchable object's <code>toList</code> method prior to calling its <code>subscribe</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolist&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolist.png"></a>
     * 
     * @param that
     *            the source Watchable
     * @return a Watchable that emits a single item: a <code>List</code> containing all of the
     *         items emitted by the source Watchable
     */
    public static <T> IObservable<List<T>> toList(final IObservable<T> that) {
        return wrap(new OperationToWatchableList<T>(that));
    }

    /**
     * Sort T objects by their natural order (object must implement Comparable).
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
     * 
     * @param sequence
     * @throws ClassCastException
     *             if T objects do not implement Comparable
     * @return
     */
    public static <T> IObservable<List<T>> toSortedList(IObservable<T> sequence) {
        return wrap(OperationToWatchableSortedList.toSortedList(sequence));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted.f&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.f.png"></a>
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> IObservable<List<T>> toSortedList(IObservable<T> sequence, Func2<Integer, T, T> sortFunction) {
        return wrap(OperationToWatchableSortedList.toSortedList(sequence, sortFunction));
    }

    /**
     * Sort T objects using the defined sort function.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted.f&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.f.png"></a>
     * 
     * @param sequence
     * @param sortFunction
     * @return
     */
    public static <T> IObservable<List<T>> toSortedList(IObservable<T> sequence, final Object sortFunction) {
        return wrap(OperationToWatchableSortedList.toSortedList(sequence, new Func2<Integer, T, T>() {

            @Override
            public Integer call(T t1, T t2) {
                return Functions.execute(sortFunction, t1, t2);
            }

        }));
    }

    /**
     * Converts an Iterable sequence to a Watchable sequence.
     * 
     * Any object that supports the Iterable interface can be converted into a Watchable that emits
     * each iterable item in the object, by passing the object into the <code>toWatchable</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.towatchable&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.towatchable.png"></a>
     * 
     * @param iterable
     *            the source Iterable sequence
     * @param <T>
     *            the type of items in the iterable sequence and the type emitted by the resulting
     *            Watchable
     * @return a Watchable that emits each item in the source Iterable sequence
     */
    public static <T> IObservable<T> toWatchable(Iterable<T> iterable) {
        return wrap(new OperationToWatchableIterable<T>(iterable));
    }

    /**
     * Converts an Array sequence to a Watchable sequence.
     * 
     * An Array can be converted into a Watchable that emits each item in the Array, by passing the
     * Array into the <code>toWatchable</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.towatchable&ceoid=27321465&key=API&pageId=27321465"><img
     * src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.towatchable.png"></a>
     * 
     * @param iterable
     *            the source Array
     * @param <T>
     *            the type of items in the Array, and the type of items emitted by the resulting
     *            Watchable
     * @return a Watchable that emits each item in the source Array
     */
    public static <T> IObservable<T> toWatchable(T... items) {
        return toWatchable(Arrays.asList(items));
    }

    /**
     * Allow wrapping responses with the <code>AbstractIObservable</code> so that we have all of
     * the utility methods available for subscribing.
     * <p>
     * This is not expected to benefit Java usage, but is intended for dynamic script which are a primary target of the Observable operations.
     * <p>
     * Since they are dynamic they can execute the "hidden" methods on <code>AbstractIObservable</code> while appearing to only receive an <code>IObservable</code> without first casting.
     * 
     * @param o
     * @return
     */
    private static <T> AbstractIObservable<T> wrap(final IObservable<T> o) {
        if (o instanceof AbstractIObservable) {
            // if the Watchable is already an AbstractWatchable, don't wrap it again.
            return (AbstractIObservable<T>) o;
        }
        return new AbstractIObservable<T>() {

            @Override
            public IDisposable subscribe(IObserver<T> observer) {
                return o.subscribe(observer);
            }

        };
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by two other Watchables, with the results of this closure becoming the
     * sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code> and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param reduceFunction
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, Func2<R, T0, T1> reduceFunction) {
        return wrap(OperationZip.zip(w0, w1, reduceFunction));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by two other Watchables, with the results of this closure becoming the
     * sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code> and the first item emitted by <code>w1</code>; the
     * second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by <code>w0</code> and the second item emitted by <code>w1</code>; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param reduceFunction
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, final Object function) {
        return zip(w0, w1, new Func2<R, T0, T1>() {

            @Override
            public R call(T0 t0, T1 t1) {
                return Functions.execute(function, t0, t1);
            }

        });
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by three other Watchables, with the results of this closure becoming
     * the sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code>, the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by <code>w0</code>, the second item emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.3&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.3.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param w2
     *            a third source Watchable
     * @param function
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1, T2> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, Func3<R, T0, T1, T2> function) {
        return wrap(OperationZip.zip(w0, w1, w2, function));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by three other Watchables, with the results of this closure becoming
     * the sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code>, the first item emitted by <code>w1</code>, and the
     * first item emitted by <code>w2</code>; the second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by <code>w0</code>, the second item emitted by <code>w1</code>, and the second item
     * emitted by <code>w2</code>; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.3&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.3.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param w2
     *            a third source Watchable
     * @param function
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1, T2> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, final Object function) {
        return zip(w0, w1, w2, new Func3<R, T0, T1, T2>() {

            @Override
            public R call(T0 t0, T1 t1, T2 t2) {
                return Functions.execute(function, t0, t1, t2);
            }

        });
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by four other Watchables, with the results of this closure becoming
     * the sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code>, the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by each of those Watchables; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.4&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.4.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param w2
     *            a third source Watchable
     * @param w3
     *            a fourth source Watchable
     * @param reduceFunction
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, IObservable<T3> w3, Func4<R, T0, T1, T2, T3> reduceFunction) {
        return wrap(OperationZip.zip(w0, w1, w2, w3, reduceFunction));
    }

    /**
     * Returns a Watchable that applies a closure of your choosing to the combination of items
     * emitted, in sequence, by four other Watchables, with the results of this closure becoming
     * the sequence emitted by the returned Watchable.
     * <p>
     * <code>zip</code> applies this closure in strict sequence, so the first item emitted by the new Watchable will be the result of the closure applied to the first item emitted by <code>w0</code>, the first item emitted by <code>w1</code>, the
     * first item emitted by <code>w2</code>, and the first item emitted by <code>w3</code>; the second item emitted by the new Watchable will be the result of the closure applied to the second item emitted by each of those Watchables; and so forth.
     * <p>
     * The resulting <code>Watchable<R></code> returned from <code>zip</code> will call <code>onNext</code> as many times as the number <code>onNext</code> calls of the source Watchable with the shortest sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.zip.4&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.zip.4.png"></a>
     * 
     * @param w0
     *            one source Watchable
     * @param w1
     *            another source Watchable
     * @param w2
     *            a third source Watchable
     * @param w3
     *            a fourth source Watchable
     * @param function
     *            a closure that, when applied to an item emitted by each of the source Watchables,
     *            results in a value that will be emitted by the resulting Watchable
     * @return a Watchable that emits the zipped results
     */
    public static <R, T0, T1, T2, T3> IObservable<R> zip(IObservable<T0> w0, IObservable<T1> w1, IObservable<T2> w2, IObservable<T3> w3, final Object function) {
        return zip(w0, w1, w2, w3, new Func4<R, T0, T1, T2, T3>() {

            @Override
            public R call(T0 t0, T1 t1, T2 t2, T3 t3) {
                return Functions.execute(function, t0, t1, t2, t3);
            }

        });
    }

    public static class UnitTest {
        @Mock
        ScriptAssertion assertion;

        @Mock
        IObserver<Integer> w;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testReduce() {
            IObservable<Integer> watchable = toWatchable(1, 2, 3, 4);
            reduce(watchable, new Func2<Integer, Integer, Integer>() {

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
            IObservable<Integer> watchable = toWatchable(1, 2, 3, 4);
            reduce(watchable, 50, new Func2<Integer, Integer, Integer>() {

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
        public void testCreateViaGroovy() {
            runGroovyScript("o.create({it.onNext('hello');it.onCompleted();}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received("hello");
        }

        @Test
        public void testFilterViaGroovy() {
            runGroovyScript("o.filter(o.toWatchable(1, 2, 3), {it >= 2}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
        }

        @Test
        public void testMapViaGroovy() {
            runGroovyScript("o.map(o.toWatchable(1, 2, 3), {'hello_' + it}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received("hello_" + 1);
            verify(assertion, times(1)).received("hello_" + 2);
            verify(assertion, times(1)).received("hello_" + 3);
        }

        @Test
        public void testSkipViaGroovy() {
            runGroovyScript("o.skip(o.toWatchable(1, 2, 3), 2).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(0)).received(2);
            verify(assertion, times(1)).received(3);
        }

        @Test
        public void testTakeViaGroovy() {
            runGroovyScript("o.take(o.toWatchable(1, 2, 3), 2).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testSkipTakeViaGroovy() {
            runGroovyScript("o.skip(o.toWatchable(1, 2, 3), 1).take(1).subscribe({ result -> a.received(result)});");
            verify(assertion, times(0)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(0)).received(3);
        }

        @Test
        public void testMergeDelayErrorViaGroovy() {
            runGroovyScript("o.mergeDelayError(o.toWatchable(1, 2, 3), o.merge(o.toWatchable(6), o.error(new NullPointerException()), o.toWatchable(7)), o.toWatchable(4, 5)).subscribe({ result -> a.received(result)}, { exception -> a.error(exception)});");
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
            verify(assertion, times(1)).received(4);
            verify(assertion, times(1)).received(5);
            verify(assertion, times(1)).received(6);
            verify(assertion, times(0)).received(7);
            verify(assertion, times(1)).error(any(NullPointerException.class));
        }

        @Test
        public void testMergeViaGroovy() {
            runGroovyScript("o.merge(o.toWatchable(1, 2, 3), o.merge(o.toWatchable(6), o.error(new NullPointerException()), o.toWatchable(7)), o.toWatchable(4, 5)).subscribe({ result -> a.received(result)}, { exception -> a.error(exception)});");
            // executing synchronously so we can deterministically know what order things will come
            verify(assertion, times(1)).received(1);
            verify(assertion, times(1)).received(2);
            verify(assertion, times(1)).received(3);
            verify(assertion, times(0)).received(4); // the NPE will cause this sequence to be skipped
            verify(assertion, times(0)).received(5); // the NPE will cause this sequence to be skipped
            verify(assertion, times(1)).received(6); // this comes before the NPE so should exist
            verify(assertion, times(0)).received(7);// this comes in the sequence after the NPE
            verify(assertion, times(1)).error(any(NullPointerException.class));
        }

        @Test
        public void testMaterializeViaGroovy() {
            runGroovyScript("o.materialize(o.toWatchable(1, 2, 3)).subscribe({ result -> a.received(result)});");
            // we expect 4 onNext calls: 3 for 1, 2, 3 WatchableNotification.OnNext and 1 for WatchableNotification.OnCompleted
            verify(assertion, times(4)).received(any(Notification.class));
            verify(assertion, times(0)).error(any(Exception.class));
        }

        @Test
        public void testToSortedList() {
            runGroovyScript("o.toSortedList(o.toWatchable(1, 3, 2, 5, 4)).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListWithFunction() {
            runGroovyScript("o.toSortedList(o.toWatchable(1, 3, 2, 5, 4), {a, b -> a - b}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        private void runGroovyScript(String script) {
            ClassLoader parent = getClass().getClassLoader();
            GroovyClassLoader loader = new GroovyClassLoader(parent);

            Binding binding = new Binding();
            binding.setVariable("a", assertion);
            binding.setVariable("o", org.rx.operations.WatchableExtensions.class);

            /* parse the script and execute it */
            InvokerHelper.createScript(loader.parseClass(script), binding).run();
        }

        private static interface ScriptAssertion {
            public void received(Object o);

            public void error(Exception o);
        }
    }
}
