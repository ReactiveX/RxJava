/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package rx;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable.Operator;
import rx.annotations.Experimental;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorNotImplementedException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.internal.operators.OnSubscribeToObservableFuture;
import rx.internal.operators.OperatorMap;
import rx.internal.operators.OperatorObserveOn;
import rx.internal.operators.OperatorOnErrorReturn;
import rx.internal.operators.OperatorSubscribeOn;
import rx.internal.operators.OperatorTimeout;
import rx.internal.operators.OperatorZip;
import rx.internal.producers.SingleDelayedProducer;
import rx.observers.SafeSubscriber;
import rx.plugins.RxJavaObservableExecutionHook;
import rx.plugins.RxJavaPlugins;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 * The Single class that implements the Reactive Pattern for a single value response. See {@link Observable} for a stream or vector of values.
 * <p>
 * This behaves the same as an {@link Observable} except that it can only emit either a single successful value, or an error.
 * <p>
 * Like an {@link Observable} it is lazy, can be either "hot" or "cold", synchronous or asynchronous.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="605" height="285" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.legend.png" alt="">
 * <p>
 * For more information see the <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation</a>.
 * 
 * @param <T>
 *            the type of the item emitted by the Single
 */
@Experimental
public class Single<T> {

    final Observable.OnSubscribe<T> onSubscribe;

    /**
     * Creates a Single with a Function to execute when it is subscribed to (executed).
     * <p>
     * <em>Note:</em> Use {@link #create(OnExecute)} to create a Single, instead of this constructor,
     * unless you specifically have a need for inheritance.
     * 
     * @param f
     *            {@link OnExecute} to be executed when {@link #execute(SingleSubscriber)} or {@link #subscribe(Subscriber)} is called
     */
    protected Single(final OnSubscribe<T> f) {
        // bridge between OnSubscribe (which all Operators and Observables use) and OnExecute (for Single)
        this.onSubscribe = new Observable.OnSubscribe<T>() {

            @Override
            public void call(final Subscriber<? super T> child) {
                final SingleDelayedProducer<T> producer = new SingleDelayedProducer<T>(child);
                child.setProducer(producer);
                SingleSubscriber<T> ss = new SingleSubscriber<T>() {

                    @Override
                    public void onSuccess(T value) {
                        producer.setValue(value);
                    }

                    @Override
                    public void onError(Throwable error) {
                        child.onError(error);
                    }

                };
                child.add(ss);
                f.call(ss);
            }

        };
    }

    private Single(final Observable.OnSubscribe<T> f) {
        this.onSubscribe = f;
    }

    private static final RxJavaObservableExecutionHook hook = RxJavaPlugins.getInstance().getObservableExecutionHook();

    /**
     * Returns a Single that will execute the specified function when a {@link SingleSubscriber} executes it or {@link Subscriber} subscribes to it.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.create.png" alt="">
     * <p>
     * Write the function you pass to {@code create} so that it behaves as a Single: It should invoke the
     * SingleSubscriber {@link SingleSubscriber#onSuccess onSuccess} and {@link SingleSubscriber#onError onError} methods appropriately.
     * <p>
     * A well-formed Single must invoke either the SingleSubscriber's {@code onSuccess} method exactly once or
     * its {@code onError} method exactly once.
     * <p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code create} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T>
     *            the type of the item that this Single emits
     * @param f
     *            a function that accepts an {@code SingleSubscriber<T>}, and invokes its {@code onSuccess} or {@code onError} methods as appropriate
     * @return a Single that, when a {@link Subscriber} subscribes to it, will execute the specified function
     * @see <a href="http://reactivex.io/documentation/operators/create.html">ReactiveX operators documentation: Create</a>
     */
    public final static <T> Single<T> create(OnSubscribe<T> f) {
        return new Single<T>(f); // TODO need hook 
    }

    /**
     * Invoked when Single.execute is called.
     */
    public static interface OnSubscribe<T> extends Action1<SingleSubscriber<? super T>> {
        // cover for generics insanity
    }

    /**
     * Lifts a function to the current Single and returns a new Single that when subscribed to will pass
     * the values of the current Single through the Operator function.
     * <p>
     * In other words, this allows chaining TaskExecutors together on a Single for acting on the values within
     * the Single.
     * <p> {@code
     * task.map(...).filter(...).lift(new OperatorA()).lift(new OperatorB(...)).subscribe()
     * } <p>
     * If the operator you are creating is designed to act on the item emitted by a source
     * Single, use {@code lift}. If your operator is designed to transform the source Single as a whole
     * (for instance, by applying a particular set of existing RxJava operators to it) use {@link #compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code lift} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param lift
     *            the Operator that implements the Single-operating function to be applied to the source Single
     * @return a Single that is the result of applying the lifted Operator to the source Single
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    private final <R> Single<R> lift(final Operator<? extends R, ? super T> lift) {
        // This method is private because not sure if we want to expose the Observable.Operator in this public API rather than a Single.Operator 

        return new Single<R>(new Observable.OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> o) {
                try {
                    final Subscriber<? super T> st = hook.onLift(lift).call(o);
                    try {
                        // new Subscriber created and being subscribed with so 'onStart' it
                        st.onStart();
                        onSubscribe.call(st);
                    } catch (Throwable e) {
                        // localized capture of errors rather than it skipping all operators 
                        // and ending up in the try/catch of the subscribe method which then
                        // prevents onErrorResumeNext and other similar approaches to error handling
                        if (e instanceof OnErrorNotImplementedException) {
                            throw (OnErrorNotImplementedException) e;
                        }
                        st.onError(e);
                    }
                } catch (Throwable e) {
                    if (e instanceof OnErrorNotImplementedException) {
                        throw (OnErrorNotImplementedException) e;
                    }
                    // if the lift function failed all we can do is pass the error to the final Subscriber
                    // as we don't have the operator available to us
                    o.onError(e);
                }
            }
        });
    }

    /**
     * Transform an Observable by applying a particular Transformer function to it.
     * <p>
     * This method operates on the Observable itself whereas {@link #lift} operates on the Observable's
     * Subscribers or Observers.
     * <p>
     * If the operator you are creating is designed to act on the individual items emitted by a source
     * Observable, use {@link #lift}. If your operator is designed to transform the source Observable as a whole
     * (for instance, by applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param transformer
     *            implements the function that transforms the source Observable
     * @return the source Observable, transformed by the transformer function
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Implementing-Your-Own-Operators">RxJava wiki: Implementing Your Own Operators</a>
     */
    @SuppressWarnings("unchecked")
    public <R> Single<R> compose(Transformer<? super T, ? extends R> transformer) {
        return ((Transformer<T, R>) transformer).call(this);
    }

    /**
     * Transformer function used by {@link #compose}.
     * 
     * @warn more complete description needed
     */
    public static interface Transformer<T, R> extends Func1<Single<T>, Single<R>> {
        // cover for generics insanity
    }

    /**
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toObservable.png" alt="">
     */
    private static <T> Observable<T> toObservable(Single<T> t) {
        // is this sufficient, or do I need to keep the outer Single and subscribe to it?
        return Observable.create(t.onSubscribe);
    }

    /**
     * INTERNAL: Used with lift and operators.
     * 
     * Converts the source {@code Single<T>} into an {@code Single<Observable<T>>} that emits the
     * source Observable as its single emission.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.nest.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code nest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return an Observable that emits a single item: the source Observable
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    private final Single<Observable<T>> nest() {
        return Single.just(toObservable(this));
    }

    /* *********************************************************************************************************
     * Operators Below Here
     * *********************************************************************************************************
     */

    /**
     * Returns an Observable that emits the items emitted by two Tasks, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param t1
     *            an Single to be concatenated
     * @param t2
     *            an Single to be concatenated
     * @return an Observable that emits items emitted by the two source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2) {
        return Observable.concat(toObservable(t1), toObservable(t2));
    }

    /**
     * Returns an Observable that emits the items emitted by three Tasks, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the three source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3));
    }

    /**
     * Returns an Observable that emits the items emitted by four Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the four source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4));
    }

    /**
     * Returns an Observable that emits the items emitted by five Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @param t5
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the five source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5));
    }

    /**
     * Returns an Observable that emits the items emitted by six Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @param t5
     *            a Single to be concatenated
     * @param t6
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the six source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6));
    }

    /**
     * Returns an Observable that emits the items emitted by seven Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @param t5
     *            a Single to be concatenated
     * @param t6
     *            a Single to be concatenated
     * @param t7
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the seven source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7));
    }

    /**
     * Returns an Observable that emits the items emitted by eight Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @param t5
     *            a Single to be concatenated
     * @param t6
     *            a Single to be concatenated
     * @param t7
     *            a Single to be concatenated
     * @param t8
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the eight source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7), toObservable(t8));
    }

    /**
     * Returns an Observable that emits the items emitted by nine Observables, one after the other, without
     * interleaving them.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concat.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be concatenated
     * @param t2
     *            a Single to be concatenated
     * @param t3
     *            a Single to be concatenated
     * @param t4
     *            a Single to be concatenated
     * @param t5
     *            a Single to be concatenated
     * @param t6
     *            a Single to be concatenated
     * @param t7
     *            a Single to be concatenated
     * @param t8
     *            a Single to be concatenated
     * @param t9
     *            a Single to be concatenated
     * @return an Observable that emits items emitted by the nine source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8, Single<? extends T> t9) {
        return Observable.concat(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7), toObservable(t8), toObservable(t9));
    }

    /**
     * Returns an Observable that invokes an {@link Observer}'s {@link Observer#onError onError} method when the
     * Observer subscribes to it.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param exception
     *            the particular Throwable to pass to {@link Observer#onError onError}
     * @param <T>
     *            the type of the items (ostensibly) emitted by the Observable
     * @return an Observable that invokes the {@link Observer}'s {@link Observer#onError onError} method when
     *         the Observer subscribes to it
     * @see <a href="http://reactivex.io/documentation/operators/empty-never-throw.html">ReactiveX operators documentation: Throw</a>
     */
    public final static <T> Single<T> error(final Throwable exception) {
        return Single.create(new OnSubscribe<T>() {

            @Override
            public void call(SingleSubscriber<? super T> te) {
                te.onError(exception);
            }

        });
    }

    /**
     * Converts a {@link Future} into an Observable.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an Observable that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from} method.
     * <p>
     * <em>Important note:</em> This Observable is blocking; you cannot unsubscribe from it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts a {@link Future} into an Observable, with a timeout on the Future.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an Observable that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from} method.
     * <p>
     * <em>Important note:</em> This Observable is blocking; you cannot unsubscribe from it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param future
     *            the source {@link Future}
     * @param timeout
     *            the maximum time to wait before calling {@code get}
     * @param unit
     *            the {@link TimeUnit} of the {@code timeout} argument
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future, long timeout, TimeUnit unit) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future, timeout, unit));
    }

    /**
     * Converts a {@link Future}, operating on a specified {@link Scheduler}, into an Observable.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.s.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into an Observable that emits the
     * return value of the {@link Future#get} method of that object, by passing the object into the {@code from} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param future
     *            the source {@link Future}
     * @param scheduler
     *            the {@link Scheduler} to wait for the Future on. Use a Scheduler such as {@link Schedulers#io()} that can block and wait on the Future
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting Observable
     * @return an Observable that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future, Scheduler scheduler) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future)).subscribeOn(scheduler);
    }

    /**
     * Returns an Observable that emits a single item and then completes.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.just.png" alt="">
     * <p>
     * To convert any object into an Observable that emits that object, pass that object into the {@code just} method.
     * <p>
     * This is similar to the {@link #from(java.lang.Object[])} method, except that {@code from} will convert
     * an {@link Iterable} object into an Observable that emits each of the items in the Iterable, one at a
     * time, while the {@code just} method converts an Iterable into an Observable that emits the entire
     * Iterable as a single item.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param value
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return an Observable that emits {@code value} as a single item and then completes
     * @see <a href="http://reactivex.io/documentation/operators/just.html">ReactiveX operators documentation: Just</a>
     */
    public final static <T> Single<T> just(final T value) {
        // TODO add similar optimization as ScalarSynchronousObservable
        return Single.create(new OnSubscribe<T>() {

            @Override
            public void call(SingleSubscriber<? super T> te) {
                te.onSuccess(value);
            }

        });
    }

    /**
     * Flattens a Single that emits a Single into a single Single that emits the items emitted by
     * the nested Single, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.oo.png" alt="">
     * <p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source
     *            a Single that emits a Single
     * @return a Single that emits the item that is the result of flattening the Single emitted by the {@code source} Single
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Single<T> merge(final Single<? extends Single<? extends T>> source) {
        return Single.create(new OnSubscribe<T>() {

            @Override
            public void call(final SingleSubscriber<? super T> child) {
                source.subscribe(new SingleSubscriber<Single<? extends T>>() {

                    @Override
                    public void onSuccess(Single<? extends T> innerSingle) {
                        innerSingle.subscribe(child);
                    }

                    @Override
                    public void onError(Throwable error) {
                        child.onError(error);
                    }

                });
            }
        });
    }

    /**
     * Flattens two Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2) {
        return Observable.merge(toObservable(t1), toObservable(t2));
    }

    /**
     * Flattens three Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3));
    }

    /**
     * Flattens four Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4));
    }

    /**
     * Flattens five Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @param t5
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5));
    }

    /**
     * Flattens six Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @param t5
     *            a Single to be merged
     * @param t6
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6));
    }

    /**
     * Flattens seven Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @param t5
     *            a Single to be merged
     * @param t6
     *            a Single to be merged
     * @param t7
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7));
    }

    /**
     * Flattens eight Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @param t5
     *            a Single to be merged
     * @param t6
     *            a Single to be merged
     * @param t7
     *            a Single to be merged
     * @param t8
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7), toObservable(t8));
    }

    /**
     * Flattens nine Observables into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code merge} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @param t2
     *            a Single to be merged
     * @param t3
     *            a Single to be merged
     * @param t4
     *            a Single to be merged
     * @param t5
     *            a Single to be merged
     * @param t6
     *            a Single to be merged
     * @param t7
     *            a Single to be merged
     * @param t8
     *            a Single to be merged
     * @param t9
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8, Single<? extends T> t9) {
        return Observable.merge(toObservable(t1), toObservable(t2), toObservable(t3), toObservable(t4), toObservable(t5), toObservable(t6), toObservable(t7), toObservable(t8), toObservable(t9));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * two items emitted, in sequence, by two other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1} and the first item
     * emitted by {@code o2}; the second item emitted by the new Observable will be the result of the function
     * applied to the second item emitted by {@code o1} and the second item emitted by {@code o2}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results
     *            in an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * three items emitted, in sequence, by three other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, and the first item emitted by {@code o3}; the second item emitted by the new
     * Observable will be the result of the function applied to the second item emitted by {@code o1}, the
     * second item emitted by {@code o2}, and the second item emitted by {@code o3}; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * four items emitted, in sequence, by four other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, and the first item emitted by {@code 04};
     * the second item emitted by the new Observable will be the result of the function applied to the second
     * item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * five items emitted, in sequence, by five other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by {@code o1}, the first item
     * emitted by {@code o2}, the first item emitted by {@code o3}, the first item emitted by {@code o4}, and
     * the first item emitted by {@code o5}; the second item emitted by the new Observable will be the result of
     * the function applied to the second item emitted by each of those Observables; and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4), toObservable(o5) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * six items emitted, in sequence, by six other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6,
            Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4), toObservable(o5), toObservable(o6) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * seven items emitted, in sequence, by seven other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7,
            Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4), toObservable(o5), toObservable(o6), toObservable(o7) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * eight items emitted, in sequence, by eight other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param o8
     *            an eighth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7, Single<? extends T8> o8,
            Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4), toObservable(o5), toObservable(o6), toObservable(o7), toObservable(o8) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to combinations of
     * nine items emitted, in sequence, by nine other Observables.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <p> {@code zip} applies this function in strict sequence, so the first item emitted by the new Observable
     * will be the result of the function applied to the first item emitted by each source Observable, the
     * second item emitted by the new Observable will be the result of the function applied to the second item
     * emitted by each of those Observables, and so forth.
     * <p>
     * The resulting {@code Observable<R>} returned from {@code zip} will invoke {@link Observer#onNext onNext} as many times as the number of {@code onNext} invocations of the source Observable that
     * emits the fewest
     * items.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Observable
     * @param o2
     *            a second source Observable
     * @param o3
     *            a third source Observable
     * @param o4
     *            a fourth source Observable
     * @param o5
     *            a fifth source Observable
     * @param o6
     *            a sixth source Observable
     * @param o7
     *            a seventh source Observable
     * @param o8
     *            an eighth source Observable
     * @param o9
     *            a ninth source Observable
     * @param zipFunction
     *            a function that, when applied to an item emitted by each of the source Observables, results in
     *            an item that will be emitted by the resulting Observable
     * @return an Observable that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7, Single<? extends T8> o8,
            Single<? extends T9> o9, Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
        return just(new Observable<?>[] { toObservable(o1), toObservable(o2), toObservable(o3), toObservable(o4), toObservable(o5), toObservable(o6), toObservable(o7), toObservable(o8), toObservable(o9) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the items emitted from the current Observable, then the next, one after
     * the other, without interleaving them.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatWith.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be concatenated after the current
     * @return an Observable that emits items emitted by the two source Observables, one after the other,
     *         without interleaving them
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final Observable<T> concatWith(Single<? extends T> t1) {
        return concat(this, t1);
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source Observable, where that function returns an Observable, and then merging those resulting
     * Observables and emitting the results of this merger.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns an
     *            Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source Observable and merging the results of the Observables obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Single<R> flatMap(final Func1<? super T, ? extends Single<? extends R>> func) {
        return merge(map(func));
    }

    /**
     * Returns an Observable that emits items based on applying a function that you supply to each item emitted
     * by the source Observable, where that function returns an Observable, and then merging those resulting
     * Observables and emitting the results of this merger.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param func
     *            a function that, when applied to an item emitted by the source Observable, returns an
     *            Observable
     * @return an Observable that emits the result of applying the transformation function to each item emitted
     *         by the source Observable and merging the results of the Observables obtained from this
     *         transformation
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Observable<R> flatMapObservable(Func1<? super T, ? extends Observable<? extends R>> func) {
        return Observable.merge(toObservable(map(func)));
    }

    /**
     * Returns a Single that applies a specified function to the item emitted by the source Single and
     * emits the result of this function applications.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.map.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code map} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param func
     *            a function to apply to the item emitted by the Single
     * @return a Single that emits the item from the source Single, transformed by the specified function
     * @see <a href="http://reactivex.io/documentation/operators/map.html">ReactiveX operators documentation: Map</a>
     */
    public final <R> Single<R> map(Func1<? super T, ? extends R> func) {
        return lift(new OperatorMap<T, R>(func));
    }

    /**
     * Flattens this and another Observable into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Observables so that they appear as a single Observable, by
     * using the {@code mergeWith} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Observables
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final Observable<T> mergeWith(Single<? extends T> t1) {
        return merge(this, t1);
    }

    /**
     * Modifies an Observable to perform its emissions and notifications on a specified {@link Scheduler},
     * asynchronously with an unbounded buffer.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.observeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to notify {@link Observer}s on
     * @return the source Observable modified so that its {@link Observer}s are notified on the specified {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     */
    public final Single<T> observeOn(Scheduler scheduler) {
        return lift(new OperatorObserveOn<T>(scheduler));
    }

    /**
     * Instructs an Observable to emit an item (returned by a specified function) rather than invoking {@link Observer#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturn.png" alt="">
     * <p>
     * By default, when an Observable encounters an error that prevents it from emitting the expected item to
     * its {@link Observer}, the Observable invokes its Observer's {@code onError} method, and then quits
     * without invoking any more of its Observer's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to an Observable's {@code onErrorReturn} method, if the original Observable encounters an error, instead of invoking its Observer's
     * {@code onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param resumeFunction
     *            a function that returns an item that the new Observable will emit if the source Observable
     *            encounters an error
     * @return the original Observable with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    public final Single<T> onErrorReturn(Func1<Throwable, ? extends T> resumeFunction) {
        return lift(new OperatorOnErrorReturn<T>(resumeFunction));
    }

    /**
     * Subscribes to an Observable but ignore its emissions and notifications.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws OnErrorNotImplementedException
     *             if the Observable tries to call {@code onError}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    public final Subscription subscribe() {
        return subscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                // do nothing
            }

        });
    }

    /**
     * Subscribes to an Observable and provides a callback to handle the items it emits.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onNext
     *            the {@code Action1<T>} you have designed to accept emissions from the Observable
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     * @throws OnErrorNotImplementedException
     *             if the Observable tries to call {@code onError}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    public final Subscription subscribe(final Action1<? super T> onSuccess) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onSuccess can not be null");
        }

        return subscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                throw new OnErrorNotImplementedException(e);
            }

            @Override
            public final void onNext(T args) {
                onSuccess.call(args);
            }

        });
    }

    /**
     * Subscribes to an Observable and provides callbacks to handle the items it emits and any error
     * notification it issues.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onNext
     *            the {@code Action1<T>} you have designed to accept emissions from the Observable
     * @param onError
     *            the {@code Action1<Throwable>} you have designed to accept any error notification from the
     *            Observable
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     * @throws IllegalArgumentException
     *             if {@code onNext} is null, or
     *             if {@code onError} is null
     */
    public final Subscription subscribe(final Action1<? super T> onSuccess, final Action1<Throwable> onError) {
        if (onSuccess == null) {
            throw new IllegalArgumentException("onSuccess can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        return subscribe(new Subscriber<T>() {

            @Override
            public final void onCompleted() {
                // do nothing
            }

            @Override
            public final void onError(Throwable e) {
                onError.call(e);
            }

            @Override
            public final void onNext(T args) {
                onSuccess.call(args);
            }

        });
    }

    /**
     * Subscribes to an Observable and invokes {@link OnSubscribe} function without any contract protection,
     * error handling, unsubscribe, or execution hooks.
     * <p>
     * Use this only for implementing an {@link Operator} that requires nested subscriptions. For other
     * purposes, use {@link #subscribe(Subscriber)} which ensures the Rx contract and other functionality.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code unsafeSubscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param subscriber
     *            the Subscriber that will handle emissions and notifications from the Observable
     */
    public final void unsafeSubscribe(Subscriber<? super T> subscriber) {
        try {
            // new Subscriber so onStart it
            subscriber.onStart();
            // TODO add back the hook
            //            hook.onSubscribeStart(this, onSubscribe).call(subscriber);
            onSubscribe.call(subscriber);
            hook.onSubscribeReturn(subscriber);
        } catch (Throwable e) {
            // special handling for certain Throwable/Error/Exception types
            Exceptions.throwIfFatal(e);
            // if an unhandled error occurs executing the onSubscribe we will propagate it
            try {
                subscriber.onError(hook.onSubscribeError(e));
            } catch (OnErrorNotImplementedException e2) {
                // special handling when onError is not implemented ... we just rethrow
                throw e2;
            } catch (Throwable e2) {
                // if this happens it means the onError itself failed (perhaps an invalid function implementation)
                // so we are unable to propagate the error correctly and will just throw
                RuntimeException r = new RuntimeException("Error occurred attempting to subscribe [" + e.getMessage() + "] and then again while trying to pass to onError.", e2);
                // TODO could the hook be the cause of the error in the on error handling.
                hook.onSubscribeError(r);
                // TODO why aren't we throwing the hook's return value.
                throw r;
            }
        }
    }

    /**
     * Subscribes to an Single and provides a Subscriber that implements functions to handle the item the
     * Single emits and any error notification it issues.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Subscribers to
     * unsubscribe, that is, to stop receiving items and notifications before the Observable completes, which
     * also invokes the Subscriber's {@link Subscriber#onCompleted onCompleted} method.</li>
     * </ol><p>
     * An {@code Single<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Single<T>} implementation indicates
     * otherwise, Subscriber should make no assumptions about the order in which multiple Subscribers will
     * receive their notifications.
     * <p>
     * For more information see the
     * <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation</a>.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param subscriber
     *            the {@link Subscriber} that will handle emissions and notifications from the Observable
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws IllegalStateException
     *             if {@code subscribe} is unable to obtain an {@code OnSubscribe<>} function
     * @throws IllegalArgumentException
     *             if the {@link Subscriber} provided as the argument to {@code subscribe} is {@code null}
     * @throws OnErrorNotImplementedException
     *             if the {@link Subscriber}'s {@code onError} method is null
     * @throws RuntimeException
     *             if the {@link Subscriber}'s {@code onError} method itself threw a {@code Throwable}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    public final Subscription subscribe(Subscriber<? super T> subscriber) {
        // validate and proceed
        if (subscriber == null) {
            throw new IllegalArgumentException("observer can not be null");
        }
        if (onSubscribe == null) {
            throw new IllegalStateException("onSubscribe function can not be null.");
            /*
             * the subscribe function can also be overridden but generally that's not the appropriate approach
             * so I won't mention that in the exception
             */
        }

        // new Subscriber so onStart it
        subscriber.onStart();

        /*
         * See https://github.com/ReactiveX/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls
         * to user code from within an Observer"
         */
        // if not already wrapped
        if (!(subscriber instanceof SafeSubscriber)) {
            // assign to `observer` so we return the protected version
            subscriber = new SafeSubscriber<T>(subscriber);
        }

        // The code below is exactly the same an unsafeSubscribe but not used because it would add a sigificent depth to alreay huge call stacks.
        try {
            // allow the hook to intercept and/or decorate
            // TODO add back the hook
            //            hook.onSubscribeStart(this, onSubscribe).call(subscriber);
            onSubscribe.call(subscriber);
            return hook.onSubscribeReturn(subscriber);
        } catch (Throwable e) {
            // special handling for certain Throwable/Error/Exception types
            Exceptions.throwIfFatal(e);
            // if an unhandled error occurs executing the onSubscribe we will propagate it
            try {
                subscriber.onError(hook.onSubscribeError(e));
            } catch (OnErrorNotImplementedException e2) {
                // special handling when onError is not implemented ... we just rethrow
                throw e2;
            } catch (Throwable e2) {
                // if this happens it means the onError itself failed (perhaps an invalid function implementation)
                // so we are unable to propagate the error correctly and will just throw
                RuntimeException r = new RuntimeException("Error occurred attempting to subscribe [" + e.getMessage() + "] and then again while trying to pass to onError.", e2);
                // TODO could the hook be the cause of the error in the on error handling.
                hook.onSubscribeError(r);
                // TODO why aren't we throwing the hook's return value.
                throw r;
            }
            return Subscriptions.empty();
        }
    }

    /**
     * Subscribes to an Single and provides a SingleSubscriber that implements functions to handle the item the
     * Single emits and any error notification it issues.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Subscribers to
     * unsubscribe, that is, to stop receiving items and notifications before the Observable completes, which
     * also invokes the Subscriber's {@link Subscriber#onCompleted onCompleted} method.</li>
     * </ol><p>
     * An {@code Single<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Single<T>} implementation indicates
     * otherwise, Subscriber should make no assumptions about the order in which multiple Subscribers will
     * receive their notifications.
     * <p>
     * For more information see the
     * <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation</a>.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param subscriber
     *            the {@link Subscriber} that will handle emissions and notifications from the Observable
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws IllegalStateException
     *             if {@code subscribe} is unable to obtain an {@code OnSubscribe<>} function
     * @throws IllegalArgumentException
     *             if the {@link Subscriber} provided as the argument to {@code subscribe} is {@code null}
     * @throws OnErrorNotImplementedException
     *             if the {@link Subscriber}'s {@code onError} method is null
     * @throws RuntimeException
     *             if the {@link Subscriber}'s {@code onError} method itself threw a {@code Throwable}
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    public final Subscription subscribe(final SingleSubscriber<? super T> te) {
        Subscriber<T> s = new Subscriber<T>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                te.onError(e);
            }

            @Override
            public void onNext(T t) {
                te.onSuccess(t);
            }

        };
        te.add(s);
        subscribe(s);
        return s;
    }

    /**
     * Asynchronously subscribes Observers to this Observable on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the source Observable modified so that its subscriptions happen on the
     *         specified {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    public final Single<T> subscribeOn(Scheduler scheduler) {
        return nest().lift(new OperatorSubscribeOn<T>(scheduler));
    }

    /**
     * Returns an Observable that mirrors the source Observable but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting Observable terminates and notifies observers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.1.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration between emitted items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the source Observable modified to notify observers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit) {
        return timeout(timeout, timeUnit, null, Schedulers.computation());
    }

    /**
     * Returns an Observable that mirrors the source Observable but applies a timeout policy for each emitted
     * item, where this policy is governed on a specified Scheduler. If the next item isn't emitted within the
     * specified timeout duration starting from its predecessor, the resulting Observable terminates and
     * notifies observers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.1s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the Scheduler to run the timeout timers on
     * @return the source Observable modified to notify observers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return timeout(timeout, timeUnit, null, scheduler);
    }

    /**
     * Returns an Observable that mirrors the source Observable but applies a timeout policy for each emitted
     * item. If the next item isn't emitted within the specified timeout duration starting from its predecessor,
     * the resulting Observable begins instead to mirror a fallback Observable.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param other
     *            the fallback Observable to use in case of a timeout
     * @return the source Observable modified to switch to the fallback Observable in case of a timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Single<? extends T> other) {
        return timeout(timeout, timeUnit, other, Schedulers.computation());
    }

    /**
     * Returns an Observable that mirrors the source Observable but applies a timeout policy for each emitted
     * item using a specified Scheduler. If the next item isn't emitted within the specified timeout duration
     * starting from its predecessor, the resulting Observable begins instead to mirror a fallback Observable.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.2s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration between items before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param other
     *            the Observable to use as the fallback in case of a timeout
     * @param scheduler
     *            the {@link Scheduler} to run the timeout timers on
     * @return the source Observable modified so that it will switch to the fallback Observable in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Single<? extends T> other, Scheduler scheduler) {
        if (other == null) {
            other = Single.<T> error(new TimeoutException());
        }
        return lift(new OperatorTimeout<T>(timeout, timeUnit, toObservable(other), scheduler));
    }

    /**
     * Returns an Observable that emits items that are the result of applying a specified function to pairs of
     * values, one each from the source Observable and another specified Observable.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T2>
     *            the type of items emitted by the {@code other} Observable
     * @param <R>
     *            the type of items emitted by the resulting Observable
     * @param other
     *            the other Observable
     * @param zipFunction
     *            a function that combines the pairs of items from the two Observables to generate the items to
     *            be emitted by the resulting Observable
     * @return an Observable that pairs up values from the source Observable and the {@code other} Observable
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final <T2, R> Single<R> zipWith(Single<? extends T2> other, Func2<? super T, ? super T2, ? extends R> zipFunction) {
        return zip(this, other, zipFunction);
    }

}
