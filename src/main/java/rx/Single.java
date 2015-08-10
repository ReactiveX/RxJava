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
 * The Single class implements the Reactive Pattern for a single value response. See {@link Observable} for the
 * implementation of the Reactive Pattern for a stream or vector of values.
 * <p>
 * {@code Single} behaves the same as {@link Observable} except that it can only emit either a single successful
 * value, or an error (there is no "onComplete" notification as there is for {@link Observable})
 * <p>
 * Like an {@link Observable}, a {@code Single} is lazy, can be either "hot" or "cold", synchronous or
 * asynchronous.
 * <p>
 * The documentation for this class makes use of marble diagrams. The following legend explains these diagrams:
 * <p>
 * <img width="605" height="285" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.legend.png" alt="">
 * <p>
 * For more information see the <a href="http://reactivex.io/documentation/observable.html">ReactiveX
 * documentation</a>.
 * 
 * @param <T>
 *            the type of the item emitted by the Single
 * @since (If this class graduates from "Experimental" replace this parenthetical with the release number)
 */
@Experimental
public class Single<T> {

    final Observable.OnSubscribe<T> onSubscribe;

    /**
     * Creates a Single with a Function to execute when it is subscribed to (executed).
     * <p>
     * <em>Note:</em> Use {@link #create(OnSubscribe)} to create a Single, instead of this constructor,
     * unless you specifically have a need for inheritance.
     * 
     * @param f
     *            {@code OnExecute} to be executed when {@code execute(SingleSubscriber)} or
     *            {@code subscribe(Subscriber)} is called
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
     * Returns a Single that will execute the specified function when a {@link SingleSubscriber} executes it or
     * a {@link Subscriber} subscribes to it.
     * <p>
     * <img width="640" height="200" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.create.png" alt="">
     * <p>
     * Write the function you pass to {@code create} so that it behaves as a Single: It should invoke the
     * SingleSubscriber {@link SingleSubscriber#onSuccess onSuccess} and/or
     * {@link SingleSubscriber#onError onError} methods appropriately.
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
     *            a function that accepts an {@code SingleSubscriber<T>}, and invokes its {@code onSuccess} or
     *            {@code onError} methods as appropriate
     * @return a Single that, when a {@link Subscriber} subscribes to it, will execute the specified function
     * @see <a href="http://reactivex.io/documentation/operators/create.html">ReactiveX operators documentation: Create</a>
     */
    public final static <T> Single<T> create(OnSubscribe<T> f) {
        return new Single<T>(f); // TODO need hook 
    }

    /**
     * Invoked when Single.execute is called.
     */
    public interface OnSubscribe<T> extends Action1<SingleSubscriber<? super T>> {
        // cover for generics insanity
    }

    /**
     * Lifts a function to the current Single and returns a new Single that when subscribed to will pass the
     * values of the current Single through the Operator function.
     * <p>
     * In other words, this allows chaining TaskExecutors together on a Single for acting on the values within
     * the Single.
     * <p>
     * {@code task.map(...).filter(...).lift(new OperatorA()).lift(new OperatorB(...)).subscribe() }
     * <p>
     * If the operator you are creating is designed to act on the item emitted by a source Single, use
     * {@code lift}. If your operator is designed to transform the source Single as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@link #compose}.
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
     * Transform a Single by applying a particular Transformer function to it.
     * <p>
     * This method operates on the Single itself whereas {@link #lift} operates on the Single's Subscribers or
     * Observers.
     * <p>
     * If the operator you are creating is designed to act on the individual item emitted by a Single, use
     * {@link #lift}. If your operator is designed to transform the source Single as a whole (for instance, by
     * applying a particular set of existing RxJava operators to it) use {@code compose}.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code compose} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param transformer
     *            implements the function that transforms the source Single
     * @return the source Single, transformed by the transformer function
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
    public interface Transformer<T, R> extends Func1<Single<T>, Single<R>> {
        // cover for generics insanity
    }

    /**
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toObservable.png" alt="">
     *
     * @warn more complete description needed
     */
    private static <T> Observable<T> asObservable(Single<T> t) {
        // is this sufficient, or do I need to keep the outer Single and subscribe to it?
        return Observable.create(t.onSubscribe);
    }

    /**
     * INTERNAL: Used with lift and operators.
     * 
     * Converts the source {@code Single<T>} into an {@code Single<Observable<T>>} that emits an Observable
     * that emits the same emission as the source Single.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.nest.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code nest} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return a Single that emits an Observable that emits the same item as the source Single
     * @see <a href="http://reactivex.io/documentation/operators/to.html">ReactiveX operators documentation: To</a>
     */
    private final Single<Observable<T>> nest() {
        return Single.just(asObservable(this));
    }

    /* *********************************************************************************************************
     * Operators Below Here
     * *********************************************************************************************************
     */

    /**
     * Returns an Observable that emits the items emitted by two Singles, one after the other.
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
     * @return an Observable that emits items emitted by the two source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2) {
        return Observable.concat(asObservable(t1), asObservable(t2));
    }

    /**
     * Returns an Observable that emits the items emitted by three Singles, one after the other.
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
     * @return an Observable that emits items emitted by the three source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3));
    }

    /**
     * Returns an Observable that emits the items emitted by four Singles, one after the other.
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
     * @return an Observable that emits items emitted by the four source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4));
    }

    /**
     * Returns an Observable that emits the items emitted by five Singles, one after the other.
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
     * @return an Observable that emits items emitted by the five source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5));
    }

    /**
     * Returns an Observable that emits the items emitted by six Singles, one after the other.
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
     * @return an Observable that emits items emitted by the six source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6));
    }

    /**
     * Returns an Observable that emits the items emitted by seven Singles, one after the other.
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
     * @return an Observable that emits items emitted by the seven source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7));
    }

    /**
     * Returns an Observable that emits the items emitted by eight Singles, one after the other.
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
     * @return an Observable that emits items emitted by the eight source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7), asObservable(t8));
    }

    /**
     * Returns an Observable that emits the items emitted by nine Singles, one after the other.
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
     * @return an Observable that emits items emitted by the nine source Singles, one after the other.
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final static <T> Observable<T> concat(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8, Single<? extends T> t9) {
        return Observable.concat(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7), asObservable(t8), asObservable(t9));
    }

    /**
     * Returns a Single that invokes a subscriber's {@link SingleSubscriber#onError onError} method when the
     * subscriber subscribes to it.
     * <p>
     * <img width="640" height="190" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.error.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code error} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param exception
     *            the particular Throwable to pass to {@link SingleSubscriber#onError onError}
     * @param <T>
     *            the type of the item (ostensibly) emitted by the Single
     * @return a Single that invokes the subscriber's {@link SingleSubscriber#onError onError} method when
     *         the subscriber subscribes to it
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
     * Converts a {@link Future} into a {@code Single}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a Single that emits the return
     * value of the {@link Future#get} method of that object, by passing the object into the {@code from}
     * method.
     * <p>
     * <em>Important note:</em> This Single is blocking; you cannot unsubscribe from it.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code from} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param future
     *            the source {@link Future}
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future));
    }

    /**
     * Converts a {@link Future} into a {@code Single}, with a timeout on the Future.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a {@code Single} that emits
     * the return value of the {@link Future#get} method of that object, by passing the object into the
     * {@code from} method.
     * <p>
     * <em>Important note:</em> This {@code Single} is blocking; you cannot unsubscribe from it.
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
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future, long timeout, TimeUnit unit) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future, timeout, unit));
    }

    /**
     * Converts a {@link Future}, operating on a specified {@link Scheduler}, into a {@code Single}.
     * <p>
     * <img width="640" height="315" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.from.Future.s.png" alt="">
     * <p>
     * You can convert any object that supports the {@link Future} interface into a {@code Single} that emits
     * the return value of the {@link Future#get} method of that object, by passing the object into the
     * {@code from} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param future
     *            the source {@link Future}
     * @param scheduler
     *            the {@link Scheduler} to wait for the Future on. Use a Scheduler such as
     *            {@link Schedulers#io()} that can block and wait on the Future
     * @param <T>
     *            the type of object that the {@link Future} returns, and also the type of item to be emitted by
     *            the resulting {@code Single}
     * @return a {@code Single} that emits the item from the source {@link Future}
     * @see <a href="http://reactivex.io/documentation/operators/from.html">ReactiveX operators documentation: From</a>
     */
    public final static <T> Single<T> from(Future<? extends T> future, Scheduler scheduler) {
        return new Single<T>(OnSubscribeToObservableFuture.toObservableFuture(future)).subscribeOn(scheduler);
    }

    /**
     * Returns a {@code Single} that emits a specified item.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.just.png" alt="">
     * <p>
     * To convert any object into a {@code Single} that emits that object, pass that object into the
     * {@code just} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code just} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param value
     *            the item to emit
     * @param <T>
     *            the type of that item
     * @return a {@code Single} that emits {@code value}
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
     * Flattens a {@code Single} that emits a {@code Single} into a single {@code Single} that emits the item
     * emitted by the nested {@code Single}, without any transformation.
     * <p>
     * <img width="640" height="370" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.oo.png" alt="">
     * <p>
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code merge} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param source
     *            a {@code Single} that emits a {@code Single}
     * @return a {@code Single} that emits the item that is the result of flattening the {@code Single} emitted
     *         by {@code source}
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
     * Flattens two Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2) {
        return Observable.merge(asObservable(t1), asObservable(t2));
    }

    /**
     * Flattens three Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3));
    }

    /**
     * Flattens four Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4));
    }

    /**
     * Flattens five Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5));
    }

    /**
     * Flattens six Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6));
    }

    /**
     * Flattens seven Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7));
    }

    /**
     * Flattens eight Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7), asObservable(t8));
    }

    /**
     * Flattens nine Singles into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code merge} method.
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
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final static <T> Observable<T> merge(Single<? extends T> t1, Single<? extends T> t2, Single<? extends T> t3, Single<? extends T> t4, Single<? extends T> t5, Single<? extends T> t6, Single<? extends T> t7, Single<? extends T> t8, Single<? extends T> t9) {
        return Observable.merge(asObservable(t1), asObservable(t2), asObservable(t3), asObservable(t4), asObservable(t5), asObservable(t6), asObservable(t7), asObservable(t8), asObservable(t9));
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
     * two other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns a Single that emits the results of a specified combiner function applied to three items emitted
     * by three other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to four items
     * emitted by four other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to five items
     * emitted by five other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param o5
     *            a fifth source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4), asObservable(o5) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to six items
     * emitted by six other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param o5
     *            a fifth source Single
     * @param o6
     *            a sixth source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6,
            Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4), asObservable(o5), asObservable(o6) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to seven items
     * emitted by seven other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param o5
     *            a fifth source Single
     * @param o6
     *            a sixth source Single
     * @param o7
     *            a seventh source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7,
            Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4), asObservable(o5), asObservable(o6), asObservable(o7) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to eight items
     * emitted by eight other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param o5
     *            a fifth source Single
     * @param o6
     *            a sixth source Single
     * @param o7
     *            a seventh source Single
     * @param o8
     *            an eighth source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7, Single<? extends T8> o8,
            Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4), asObservable(o5), asObservable(o6), asObservable(o7), asObservable(o8) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the results of a specified combiner function applied to nine items
     * emitted by nine other Singles.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param o1
     *            the first source Single
     * @param o2
     *            a second source Single
     * @param o3
     *            a third source Single
     * @param o4
     *            a fourth source Single
     * @param o5
     *            a fifth source Single
     * @param o6
     *            a sixth source Single
     * @param o7
     *            a seventh source Single
     * @param o8
     *            an eighth source Single
     * @param o9
     *            a ninth source Single
     * @param zipFunction
     *            a function that, when applied to the item emitted by each of the source Singles, results in an
     *            item that will be emitted by the resulting Single
     * @return a Single that emits the zipped results
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Single<R> zip(Single<? extends T1> o1, Single<? extends T2> o2, Single<? extends T3> o3, Single<? extends T4> o4, Single<? extends T5> o5, Single<? extends T6> o6, Single<? extends T7> o7, Single<? extends T8> o8,
            Single<? extends T9> o9, Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
        return just(new Observable<?>[] { asObservable(o1), asObservable(o2), asObservable(o3), asObservable(o4), asObservable(o5), asObservable(o6), asObservable(o7), asObservable(o8), asObservable(o9) }).lift(new OperatorZip<R>(zipFunction));
    }

    /**
     * Returns an Observable that emits the item emitted by the source Single, then the item emitted by the
     * specified Single.
     * <p>
     * <img width="640" height="335" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.concatWith.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code concat} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be concatenated after the current
     * @return an Observable that emits the item emitted by the source Single, followed by the item emitted by
     *         {@code t1}
     * @see <a href="http://reactivex.io/documentation/operators/concat.html">ReactiveX operators documentation: Concat</a>
     */
    public final Observable<T> concatWith(Single<? extends T> t1) {
        return concat(this, t1);
    }

    /**
     * Returns a Single that is based on applying a specified function to the item emitted by the source Single,
     * where that function returns a Single.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMap.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMap} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param func
     *            a function that, when applied to the item emitted by the source Single, returns a Single
     * @return the Single returned from {@code func} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Single<R> flatMap(final Func1<? super T, ? extends Single<? extends R>> func) {
        return merge(map(func));
    }

    /**
     * Returns an Observable that emits items based on applying a specified function to the item emitted by the
     * source Observable, where that function returns an Observable.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.flatMapObservable.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code flatMapObservable} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param func
     *            a function that, when applied to the item emitted by the source Single, returns an
     *            Observable
     * @return the Observable returned from {@code func} when applied to the item emitted by the source Single
     * @see <a href="http://reactivex.io/documentation/operators/flatmap.html">ReactiveX operators documentation: FlatMap</a>
     */
    public final <R> Observable<R> flatMapObservable(Func1<? super T, ? extends Observable<? extends R>> func) {
        return Observable.merge(asObservable(map(func)));
    }

    /**
     * Returns a Single that applies a specified function to the item emitted by the source Single and
     * emits the result of this function application.
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
     * Flattens this and another Single into a single Observable, without any transformation.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.merge.png" alt="">
     * <p>
     * You can combine items emitted by multiple Singles so that they appear as a single Observable, by using
     * the {@code mergeWith} method.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code mergeWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param t1
     *            a Single to be merged
     * @return an Observable that emits all of the items emitted by the source Singles
     * @see <a href="http://reactivex.io/documentation/operators/merge.html">ReactiveX operators documentation: Merge</a>
     */
    public final Observable<T> mergeWith(Single<? extends T> t1) {
        return merge(this, t1);
    }

    /**
     * Modifies a Single to emit its item (or notify of its error) on a specified {@link Scheduler},
     * asynchronously.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.observeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to notify subscribers on
     * @return the source Single modified so that its subscribers are notified on the specified
     *         {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/observeon.html">ReactiveX operators documentation: ObserveOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #subscribeOn
     */
    public final Single<T> observeOn(Scheduler scheduler) {
        return lift(new OperatorObserveOn<T>(scheduler));
    }

    /**
     * Instructs a Single to emit an item (returned by a specified function) rather than invoking
     * {@link SingleSubscriber#onError onError} if it encounters an error.
     * <p>
     * <img width="640" height="310" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.onErrorReturn.png" alt="">
     * <p>
     * By default, when a Single encounters an error that prevents it from emitting the expected item to its
     * subscriber, the Single invokes its subscriber's {@link Subscriber#onError} method, and then quits
     * without invoking any more of its subscriber's methods. The {@code onErrorReturn} method changes this
     * behavior. If you pass a function ({@code resumeFunction}) to a Single's {@code onErrorReturn} method, if
     * the original Single encounters an error, instead of invoking its subscriber's
     * {@link Subscriber#onError} method, it will instead emit the return value of {@code resumeFunction}.
     * <p>
     * You can use this to prevent errors from propagating or to supply fallback data should errors be
     * encountered.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code onErrorReturn} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param resumeFunction
     *            a function that returns an item that the new Single will emit if the source Single encounters
     *            an error
     * @return the original Single with appropriately modified behavior
     * @see <a href="http://reactivex.io/documentation/operators/catch.html">ReactiveX operators documentation: Catch</a>
     */
    public final Single<T> onErrorReturn(Func1<Throwable, ? extends T> resumeFunction) {
        return lift(new OperatorOnErrorReturn<T>(resumeFunction));
    }

    /**
     * Subscribes to a Single but ignore its emission or notification.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws OnErrorNotImplementedException
     *             if the Single tries to call {@link Subscriber#onError}
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
     * Subscribes to a Single and provides a callback to handle the item it emits.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onSuccess
     *            the {@code Action1<T>} you have designed to accept the emission from the Single
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws IllegalArgumentException
     *             if {@code onNext} is null
     * @throws OnErrorNotImplementedException
     *             if the Single tries to call {@link Subscriber#onError}
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
     * Subscribes to a Single and provides callbacks to handle the item it emits or any error notification it
     * issues.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param onSuccess
     *            the {@code Action1<T>} you have designed to accept the emission from the Single
     * @param onError
     *            the {@code Action1<Throwable>} you have designed to accept any error notification from the
     *            Single
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
     * Subscribes to a Single and invokes the {@link OnSubscribe} function without any contract protection,
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
     *            the Subscriber that will handle the emission or notification from the Single
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
     * Subscribes to a Single and provides a Subscriber that implements functions to handle the item the Single
     * emits or any error notification it issues.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Subscribers to
     * unsubscribe, that is, to stop receiving the item or notification before the Single completes.</li>
     * </ol><p>
     * A {@code Single<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Single<T>} implementation indicates
     * otherwise, Subscribers should make no assumptions about the order in which multiple Subscribers will
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
     *            the {@link Subscriber} that will handle the emission or notification from the Single
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
     * Subscribes to a Single and provides a {@link SingleSubscriber} that implements functions to handle the
     * item the Single emits or any error notification it issues.
     * <p>
     * A typical implementation of {@code subscribe} does the following:
     * <ol>
     * <li>It stores a reference to the Subscriber in a collection object, such as a {@code List<T>} object.</li>
     * <li>It returns a reference to the {@link Subscription} interface. This enables Subscribers to
     * unsubscribe, that is, to stop receiving the item or notification before the Single completes.</li>
     * </ol><p>
     * A {@code Single<T>} instance is responsible for accepting all subscriptions and notifying all
     * Subscribers. Unless the documentation for a particular {@code Single<T>} implementation indicates
     * otherwise, Subscribers should make no assumptions about the order in which multiple Subscribers will
     * receive their notifications.
     * <p>
     * For more information see the
     * <a href="http://reactivex.io/documentation/observable.html">ReactiveX documentation</a>.
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param te
     *            the {@link SingleSubscriber} that will handle the emission or notification from the Single
     * @return a {@link Subscription} reference can request the {@link Single} stop work.
     * @throws IllegalStateException
     *             if {@code subscribe} is unable to obtain an {@code OnSubscribe<>} function
     * @throws IllegalArgumentException
     *             if the {@link SingleSubscriber} provided as the argument to {@code subscribe} is {@code null}
     * @throws OnErrorNotImplementedException
     *             if the {@link SingleSubscriber}'s {@code onError} method is null
     * @throws RuntimeException
     *             if the {@link SingleSubscriber}'s {@code onError} method itself threw a {@code Throwable}
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
     * Asynchronously subscribes subscribers to this Single on the specified {@link Scheduler}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.subscribeOn.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param scheduler
     *            the {@link Scheduler} to perform subscription actions on
     * @return the source Single modified so that its subscriptions happen on the specified {@link Scheduler}
     * @see <a href="http://reactivex.io/documentation/operators/subscribeon.html">ReactiveX operators documentation: SubscribeOn</a>
     * @see <a href="http://www.grahamlea.com/2014/07/rxjava-threading-examples/">RxJava Threading Examples</a>
     * @see #observeOn
     */
    public final Single<T> subscribeOn(Scheduler scheduler) {
        return nest().lift(new OperatorSubscribeOn<T>(scheduler));
    }
    
    /**
     * Converts this Single into an {@link Observable}.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.toObservable.png" alt="">
     * 
     * @return an {@link Observable} that emits a single item T.
     */
    public final Observable<T> toObservable() {
    	return asObservable(this);
    }

    /**
     * Returns a Single that mirrors the source Single but applies a timeout policy for its emitted item. If it
     * is not emitted within the specified timeout duration, the resulting Single terminates and notifies
     * subscribers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.1.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration before the Single times out
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument.
     * @return the source Single modified to notify subscribers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit) {
        return timeout(timeout, timeUnit, null, Schedulers.computation());
    }

    /**
     * Returns a Single that mirrors the source Single but applies a timeout policy for its emitted item, where
     * this policy is governed on a specified Scheduler. If the item is not emitted within the specified timeout
     * duration, the resulting Single terminates and notifies subscribers of a {@code TimeoutException}.
     * <p>
     * <img width="640" height="300" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.1s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration before the Single times out
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param scheduler
     *            the Scheduler to run the timeout timers on
     * @return the source Single modified to notify subscribers of a {@code TimeoutException} in case of a
     *         timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return timeout(timeout, timeUnit, null, scheduler);
    }

    /**
     * Returns a Single that mirrors the source Single but applies a timeout policy for its emitted item. If it
     * is not emitted within the specified timeout duration, the resulting Single instead mirrors a fallback
     * Single.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.2.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>This version of {@code timeout} operates by default on the {@code computation} {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum time before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param other
     *            the fallback Single to use in case of a timeout
     * @return the source Single modified to switch to the fallback Single in case of a timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Single<? extends T> other) {
        return timeout(timeout, timeUnit, other, Schedulers.computation());
    }

    /**
     * Returns a Single that mirrors the source Single but applies a timeout policy for its emitted item, using
     * a specified Scheduler. If the item isn't emitted within the specified timeout duration, the resulting
     * Single instead mirrors a fallback Single.
     * <p>
     * <img width="640" height="305" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.timeout.2s.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>you specify which {@link Scheduler} this operator will use</dd>
     * </dl>
     * 
     * @param timeout
     *            maximum duration before a timeout occurs
     * @param timeUnit
     *            the unit of time that applies to the {@code timeout} argument
     * @param other
     *            the Single to use as the fallback in case of a timeout
     * @param scheduler
     *            the {@link Scheduler} to run the timeout timers on
     * @return the source Single modified so that it will switch to the fallback Singlein case of a timeout
     * @see <a href="http://reactivex.io/documentation/operators/timeout.html">ReactiveX operators documentation: Timeout</a>
     */
    public final Single<T> timeout(long timeout, TimeUnit timeUnit, Single<? extends T> other, Scheduler scheduler) {
        if (other == null) {
            other = Single.<T> error(new TimeoutException());
        }
        return lift(new OperatorTimeout<T>(timeout, timeUnit, asObservable(other), scheduler));
    }

    /**
     * Returns a Single that emits the result of applying a specified function to the pair of items emitted by
     * the source Single and another specified Single.
     * <p>
     * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
     * <dl>
     * <dt><b>Scheduler:</b></dt>
     * <dd>{@code zipWith} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <T2>
     *            the type of items emitted by the {@code other} Single
     * @param <R>
     *            the type of items emitted by the resulting Single
     * @param other
     *            the other Observable
     * @param zipFunction
     *            a function that combines the pairs of items from the two Observables to generate the items to
     *            be emitted by the resulting Single
     * @return an Observable that pairs up values from the source Observable and the {@code other} Observable
     *         and emits the results of {@code zipFunction} applied to these pairs
     * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
     */
    public final <T2, R> Single<R> zipWith(Single<? extends T2> other, Func2<? super T, ? super T2, ? extends R> zipFunction) {
        return zip(this, other, zipFunction);
    }

}
