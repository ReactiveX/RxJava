package org.rx.reactive;

import java.util.List;
import java.util.Map;

import org.rx.functions.Func1;
import org.rx.functions.Func2;


/**
 * The Watchable interface that implements the Reactive Pattern.
 * The documentation for this interface makes use of marble diagrams. The following legend explains
 * these diagrams:
 * <p>
 * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.legend&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.legend.png"></a>
 */

public interface IObservable<T> {

    /**
     * A Watcher must call a Watchable's <code>subscribe</code> method in order to register itself
     * to receive push-based notifications from the Watchable. A typical implementation of the
     * <code>subscribe</code> method does the following:
     * 
     * It stores a reference to the Watcher in a collection object, such as a <code>List<T></code>
     * object.
     * 
     * It returns a reference to an <code>WatchableSubscription</code> interface. This enables
     * Watchers to unsubscribe (that is, to stop receiving notifications) before the Watchable has
     * finished sending them and has called the Watcher's <code>OnCompleted</code> method.
     * 
     * At any given time, a particular instance of an <code>Watchable<T></code> implementation is
     * responsible for accepting all subscriptions and notifying all subscribers. Unless the
     * documentation for a particular <code>Watchable<T></code> implementation indicates otherwise,
     * Watchers should make no assumptions about the <code>Watchable<T></code> implementation, such
     * as the order of notifications that multiple Watchers will receive.
     * <p>
     * For more information see
     * <a href="https://confluence.corp.netflix.com/display/API/Watchers%2C+Watchables%2C+and+the+Reactive+Pattern">API.Next Programmer's Guide: Watchers, Watchables, and the Reactive Pattern</a>
     * 
     * @param watcher
     * @return a <code>WatchableSubscription</code> reference to an interface that allows observers
     * to stop receiving notifications before the provider has finished sending them
     */
    public IDisposable subscribe(IObserver<T> watcher);

    /**
     * Filters a Watchable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     *
     * @param callback
     *        a closure that evaluates the items emitted by the source watchable, returning
     *        <code>true</code> if they pass the filter
     * @return a watchable that emits only those items in the original watchable that the filter
     *         evaluates as "true"
     */
    public IObservable<T> filter(Object callback);

    /**
     * Filters a Watchable by discarding any of its emissions that do not meet some test.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.filter&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.filter.png"></a>
     * 
     * @param predicate
     *        a closure that evaluates the items emitted by the source watchable, returning
     *        <code>true</code> if they pass the filter
     * @return a watchable that emits only those items in the original watchable that the filter
     *         evaluates as <code>true</code>
     */
    public IObservable<T> filter(Func1<Boolean, T> predicate);

    /**
     * Converts a Watchable that emits a sequence of objects into one that only emits the last
     * object in this sequence before completing.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.last&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.last.png"></a>
     * 
     * @return a Watchable that emits only the last item emitted by the original watchable
     */
    public IObservable<T> last();

    /**
     * Applies a closure of your choosing to every item emitted by a Watchable, and returns this
     * transformation as a new Watchable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     *
     * @param callback
     *            a closure to apply to each item in the sequence.
     * @return a Watchable that emits a sequence that is the result of applying the transformation
     *         closure to each item in the sequence emitted by the input Watchable.
     */
    public <R> IObservable<R> map(Object callback);

    /**
     * Applies a closure of your choosing to every item emitted by a Watchable, and returns this
     * transformation as a new Watchable sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.map&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.map.png"></a>
     *
     * @param func
     *            a closure to apply to each item in the sequence.
     * @return a Watchable that emits a sequence that is the result of applying the transformation
     *         closure to each item in the sequence emitted by the input Watchable.
     */
    public <R> IObservable<R> map(Func1<R, T> func);

    /**
     * Creates a new Watchable sequence by applying a closure that you supply to each item in the
     * original Watchable sequence, where that closure is itself a Watchable that emits items, and
     * then merges the results of that closure applied to every item emitted by the original
     * Watchable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     *
     * @param callback
     *            a closure to apply to each item in the sequence that returns a Watchable.
     * @return a Watchable that emits a sequence that is the result of applying the transformation'
     *         function to each item in the input sequence and merging the results of the
     *         Watchables obtained from this transformation.
     */
    public <R> IObservable<R> mapMany(Object callback);

    /**
     * Creates a new Watchable sequence by applying a closure that you supply to each item in the
     * original Watchable sequence, where that closure is itself a Watchable that emits items, and
     * then merges the results of that closure applied to every item emitted by the original
     * Watchable, emitting these merged results as its own sequence.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.mapmany&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.mapMany.png"></a>
     *
     * @param func
     *            a closure to apply to each item in the sequence, that returns a Watchable.
     * @return a Watchable that emits a sequence that is the result of applying the transformation
     *         function to each item in the input sequence and merging the results of the
     *         Watchables obtained from this transformation.
     */
    public <R> IObservable<R> mapMany(Func1<IObservable<R>, T> func);
    
     /**
      * Materializes the implicit notifications of this observable sequence as explicit notification values.
      * <p>
      * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.materialize&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.materialize.png"></a>
      *
      * @return An observable sequence whose elements are the result of materializing the notifications of the given sequence.
      * @see http://msdn.microsoft.com/en-us/library/hh229453(v=VS.103).aspx
      */
     public IObservable<Notification<T>> materialize();

    /**
     * Instruct a Watchable to pass control to another Watchable rather than calling
     * <code>onError</code> if it encounters an error.
     *
     * By default, when a Watchable encounters an error that prevents it from emitting the expected
     * item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and
     * then quits without calling any more of its Watcher's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another watchable
     * (<code>resumeSequence</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the
     * original Watchable encounters an error, instead of calling its Watcher's
     * <code>onError</code> closure, it will instead relinquish control to
     * <code>resumeSequence</code> which will call the Watcher's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Watchable necessarily invokes
     * <code>onError</code>, the Watcher may never know that an error happened.
     *
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * @param resumeSequence
     * @return the original watchable, with appropriately modified behavior
     */
    public IObservable<T> onErrorResumeNext(IObservable<T> resumeSequence);

    /**
     * Instruct a Watchable to pass control to another Watchable rather than calling <code>onError</code> if it encounters an error.
     *
     * By default, when a Watchable encounters an error that prevents it from emitting the expected
     * item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and
     * then quits without calling any more of its Watcher's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another watchable
     * (<code>resumeFunction</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the
     * original Watchable encounters an error, instead of calling its Watcher's
     * <code>onErrort</code> closure, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Watcher's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Watchable necessarily invokes
     * <code>onError</code>, the Watcher may never know that an error happened.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * @param resumeFunction
     * @return the original watchable, with appropriately modified behavior
     */
    public IObservable<T> onErrorResumeNext(Func1<IObservable<T>, Exception> resumeFunction);

    /**
     * Instruct a Watchable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     *
     * By default, when a Watchable encounters an error that prevents it from emitting the expected
     * item to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and
     * then quits without calling any more of its Watcher's closures. The
     * <code>onErrorResumeNext</code> method changes this behavior. If you pass another watchable
     * (<code>resumeFunction</code>) to a Watchable's <code>onErrorResumeNext</code> method, if the
     * original Watchable encounters an error, instead of calling its Watcher's
     * <code>onError</code> closure, it will instead relinquish control to
     * <code>resumeFunction</code> which will call the Watcher's <code>onNext</code> method if it
     * is able to do so. In such a case, because no Watchable necessarily invokes
     * <code>onError</code>, the Watcher may never know that an error happened.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorresumenext&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorresumenext.png"></a>
     * @param resumeFunction
     * @return the original watchable with appropriately modified behavior
     */
    public IObservable<T> onErrorResumeNext(Object resumeFunction);

    /**
     * Instruct a Watchable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     *
     * By default, when a Watchable encounters an error that prevents it from emitting the expected
     * object to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and
     * then quits without calling any more of its Watcher's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a closure
     * (<code>resumeFunction</code>) to a Watchable's <code>onErrorReturn</code> method, if the
     * original Watchable encounters an error, instead of calling its Watcher's
     * <code>onError</code> closure, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Watcher's <code>onNext</code> method.
     *
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     *
     * @param resumeFunction
     * @return the original watchable with appropriately modified behavior
     */
    public IObservable<T> onErrorReturn(Func1<T, Exception> resumeFunction);

    /**
     * Instruct a Watchable to emit a particular item rather than calling <code>onError</code> if
     * it encounters an error.
     *
     * By default, when a Watchable encounters an error that prevents it from emitting the expected
     * object to its Watcher, the Watchable calls its Watcher's <code>onError</code> closure, and
     * then quits without calling any more of its Watcher's closures. The
     * <code>onErrorReturn</code> method changes this behavior. If you pass a closure
     * (<code>resumeFunction</code>) to a Watchable's <code>onErrorReturn</code> method, if the
     * original Watchable encounters an error, instead of calling its Watcher's
     * <code>onError</code> closure, it will instead call pass the return value of
     * <code>resumeFunction</code> to the Watcher's <code>onNext</code> method.
     * 
     * You can use this to prevent errors from propagating or to supply fallback data should errors
     * be encountered.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.onerrorreturn&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.onerrorreturn.png"></a>
     *
     * @param that
     * @param resumeFunction
     * @return the original watchable with appropriately modified behavior
     */
    public IObservable<T> onErrorReturn(Object resumeFunction);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     *<p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     *
     * @param accumulator
     *         An accumulator closure to be invoked on each element from the sequence, whose result
     *         will be used in the next accumulator call (if applicable).
     * 
     * @return An observable sequence with a single element from the result of accumulating the
     *         output from the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN:
     *      Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold
     *      (higher-order function)</a>
     */
    public IObservable<T> reduce(Func2<T, T, T> accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     *<p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     *
     * @param accumulator
     *         An accumulator closure to be invoked on each element from the sequence, whose result
     *         will be used in the next accumulator call (if applicable).
     * 
     * @return A Watchable that emits a single element from the result of accumulating the output
     *         from the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public IObservable<T> reduce(Object accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole 
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     *
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable).
     * 
     * @return A Watchable that emits a single element from the result of accumulating the output
     *         from the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN:
     *      Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold
     *      (higher-order function)</a>
     */
    public IObservable<T> reduce(T initialValue, Func2<T, T, T> accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the final result from the final call to your closure as its sole
     * output.
     * <p>
     * This technique, which is called "reduce" here, is sometimes called "fold," "accumulate,"
     * "compress," or "inject" in other programming contexts. Groovy, for instance, has an
     * <code>inject</code> method that does a similar operation on lists.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.reduce&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.reduce.png"></a>
     *
     * @param initialValue
     *            The initial (seed) accumulator value.
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence, whose
     *            result will be used in the next accumulator call (if applicable).
     * @return A Watchable that emits a single element from the result of accumulating the output
     *         from the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN:
     *      Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold
     *      (higher-order function)</a>
     */
    public IObservable<T> reduce(T initialValue, Object accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations. It emits the result of
     * each of these iterations as a sequence from the returned Watchable. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/showgliffyeditor.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465&t=marble.scan.noseed"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     *
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * @return A Watchable sequence whose elements are the result of accumulating the output from
     *         the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN:
     *      Observable.Scan</a>
     */
    public IObservable<T> scan(Func2<T, T, T> accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations. It emits the result of
     * each of these iterations as a sequence from the returned Watchable. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/showgliffyeditor.action?name=marble.scan.noseed&ceoid=27321465&key=API&pageId=27321465&t=marble.scan.noseed"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.noseed.png"></a>
     *
     * @param accumulator
     *            An accumulator closure to be invoked on each element from the sequence whose
     *            result will be sent via <code>onNext</code> and used in the next accumulator call
     *            (if applicable).
     * 
     * @return A Watchable sequence whose elements are the result of accumulating the output from
     *         the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN:
     *      Observable.Scan</a>
     */
    public IObservable<T> scan(Object accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, and so on until all items have been emitted by the
     * source Watchable, emitting the result of each of these iterations. This sort of closure is
     * sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     *
     * @param initialValue
     *        The initial (seed) accumulator value.
     * @param accumulator
     *        An accumulator closure to be invoked on each element from the sequence whose
     *        result will be sent via <code>onNext</code> and used in the next accumulator call
     *        (if applicable).
     * @return A Watchable sequence whose elements are the result of accumulating the output from
     *         the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN:
     *      Observable.Scan</a>
     */
    public IObservable<T> scan(T initialValue, Func2<T, T, T> accumulator);

    /**
     * Returns a Watchable that applies a closure of your choosing to the first item emitted by a
     * source Watchable, then feeds the result of that closure along with the second item emitted
     * by a Watchable into the same closure, then feeds the result of that closure along with the
     * third item into the same closure, and so on, emitting the result of each of these
     * iterations. This sort of closure is sometimes called an accumulator.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.scan&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.scan.png"></a>
     *
     * @param initialValue
     *        The initial (seed) accumulator value.
     * @param accumulator
     *        An accumulator closure to be invoked on each element from the sequence whose result
     *        will be sent via <code>onNext</code> and used in the next accumulator call (if
     *        applicable).
     * @return A Watchable sequence whose elements are the result of accumulating the output from
     *         the list of Watchables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211665(v%3Dvs.103).aspx">MSDN: Observable.Scan</a>
     */
    public IObservable<T> scan(T initialValue, Object accumulator);

    /**
     * Returns a Watchable that skips the first <code>num</code> items emitted by the source
     * Watchable.
     * You can ignore the first <code>num</code> items emitted by a watchable and attend only to
     * those items that come after, by modifying the watchable with the <code>skip</code> method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.skip&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.skip.png"></a>
     *
     * @param num
     *        The number of items to skip
     * @return A Watchable sequence that is identical to the source Watchable except that it does
     *         not emit the first <code>num</code> items from that sequence.
     */
    public IObservable<T> skip(int num);

    /**
     * Helper method that acts as a synonym to <code>execute(APIServiceCallback<T> callback)</code>
     * so that Groovy can pass in a closure without the
     * <code>as com.netflix.api.service.APIServiceCallback</code> at the end of it.
     * 
     * @param callbacks
     */
    public IDisposable subscribe(Map<String, Object> callbacks);

    public IDisposable subscribe(Object onNext);

    public IDisposable subscribe(Object onNext, Object onError);

    public IDisposable subscribe(Object onNext, Object onError, Object onComplete);

    /**
     * Returns a Watchable that emits the first <code>num</code> items emitted by the source
     * Watchable.
     *
     * You can choose to pay attention only to the first <code>num</code> values emitted by a
     * Watchable by calling its <code>take</code> method. This method returns a Watchable that will
     * call a subscribing Watcher's <code>onNext</code> closure a maximum of <code>num</code> times
     * before calling <code>onCompleted</code>.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.take&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.take.png"></a>
     *
     * @param num
     * @return a Watchable that emits only the first <code>num</code> items from the source
     *         watchable, or all of the items from the source Watchable if that Watchable emits
     *         fewer than <code>num</code> items.
     */
    public IObservable<T> take(int num);

    /**
     * Returns a Watchable that emits a single item, a list composed of all the items emitted by
     * the source Watchable.
     *
     * Normally, a Watchable that returns multiple items will do so by calling its Watcher's
     * <code>onNext</code> closure for each such item. You can change this behavior, instructing
     * the Watchable to compose a list of all of these multiple items and then to call the
     * Watcher's <code>onNext</code> closure once, passing it the entire list, by calling the
     * Watchable object's <code>toList</code> method prior to calling its <code>subscribe</code>
     * method.
     * <p>
     * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolist&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolist.png"></a>
     *
     * @return a Watchable that emits a single item: a List containing all of the items emitted by
     *         the source Watchable.
     */
    public IObservable<List<T>> toList();
    
     /**
      * Sort T objects by their natural order (object must implement Comparable).
      * <p>
      * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
      * 
      * @throws ClassCastException
      *             if T objects do not implement Comparable
      * @return
      */
     public IObservable<List<T>> toSortedList();

     /**
      * Sort T objects using the defined sort function.
      * <p>
      * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
      * 
      * @param sortFunction
      * @return
      */
     public IObservable<List<T>> toSortedList(Func2<Integer, T, T> sortFunction);

     /**
      * Sort T objects using the defined sort function.
      * <p>
      * <a href="https://confluence.corp.netflix.com/plugins/gliffy/viewlargediagram.action?name=marble.tolistsorted&ceoid=27321465&key=API&pageId=27321465"><img src="https://confluence.corp.netflix.com/download/attachments/27321465/marble.tolistsorted.png"></a>
      * 
      * @param sortFunction
      * @return
      */
     public IObservable<List<T>> toSortedList(final Object sortFunction);

}
