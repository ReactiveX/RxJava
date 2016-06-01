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

package rx;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.OnSubscribe;
import rx.annotations.Experimental;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.operators.*;
import rx.internal.util.*;
import rx.observers.*;
import rx.plugins.*;
import rx.schedulers.Schedulers;
import rx.subscriptions.*;

/**
 * Represents a deferred computation without any value but only indication for completion or exception.
 * 
 * The class follows a similar event pattern as Reactive-Streams: onSubscribe (onError|onComplete)?
 */
@Experimental
public class Completable {
    /** The error handler instance. */
    static final RxJavaErrorHandler ERROR_HANDLER = RxJavaPlugins.getInstance().getErrorHandler();
    
    /** The completable hook. */
    static RxJavaCompletableExecutionHook HOOK = RxJavaPlugins.getInstance().getCompletableExecutionHook();

    /**
     * Callback used for building deferred computations that takes a CompletableSubscriber.
     */
    public interface CompletableOnSubscribe extends Action1<CompletableSubscriber> {
        
    }
    
    /**
     * Convenience interface and callback used by the lift operator that given a child CompletableSubscriber,
     * return a parent CompletableSubscriber that does any kind of lifecycle-related transformations.
     */
    public interface CompletableOperator extends Func1<CompletableSubscriber, CompletableSubscriber> {
        
    }
    
    /**
     * Represents the subscription API callbacks when subscribing to a Completable instance.
     */
    public interface CompletableSubscriber {
        /**
         * Called once the deferred computation completes normally.
         */
        void onCompleted();
        
        /**
         * Called once if the deferred computation 'throws' an exception.
         * @param e the exception, not null.
         */
        void onError(Throwable e);
        
        /**
         * Called once by the Completable to set a Subscription on this instance which
         * then can be used to cancel the subscription at any time.
         * @param d the Subscription instance to call dispose on for cancellation, not null
         */
        void onSubscribe(Subscription d);
    }
    
    /**
     * Convenience interface and callback used by the compose operator to turn a Completable into another
     * Completable fluently.
     */
    public interface CompletableTransformer extends Func1<Completable, Completable> {
        
    }
    
    /** Single instance of a complete Completable. */
    static final Completable COMPLETE = create(new CompletableOnSubscribe() {
        @Override
        public void call(CompletableSubscriber s) {
            s.onSubscribe(Subscriptions.unsubscribed());
            s.onCompleted();
        }
    });
    
    /** Single instance of a never Completable. */
    static final Completable NEVER = create(new CompletableOnSubscribe() {
        @Override
        public void call(CompletableSubscriber s) {
            s.onSubscribe(Subscriptions.unsubscribed());
        }
    });
    
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * @param sources the array of source Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable amb(final Completable... sources) {
        requireNonNull(sources);
        if (sources.length == 0) {
            return complete();
        }
        if (sources.length == 1) {
            return sources[0];
        }
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                final CompositeSubscription set = new CompositeSubscription();
                s.onSubscribe(set);

                final AtomicBoolean once = new AtomicBoolean();
                
                CompletableSubscriber inner = new CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(e);
                        } else {
                            ERROR_HANDLER.handleError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        set.add(d);
                    }
                    
                };
                
                for (Completable c : sources) {
                    if (set.isUnsubscribed()) {
                        return;
                    }
                    if (c == null) {
                        NullPointerException npe = new NullPointerException("One of the sources is null");
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(npe);
                        } else {
                            ERROR_HANDLER.handleError(npe);
                        }
                        return;
                    }
                    if (once.get() || set.isUnsubscribed()) {
                        return;
                    }
                    
                    // no need to have separate subscribers because inner is stateless
                    c.unsafeSubscribe(inner);
                }
            }
        });
    }
    
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * @param sources the array of source Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable amb(final Iterable<? extends Completable> sources) {
        requireNonNull(sources);
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                final CompositeSubscription set = new CompositeSubscription();
                s.onSubscribe(set);

                final AtomicBoolean once = new AtomicBoolean();
                
                CompletableSubscriber inner = new CompletableSubscriber() {
                    @Override
                    public void onCompleted() {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onCompleted();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(e);
                        } else {
                            ERROR_HANDLER.handleError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        set.add(d);
                    }
                    
                };
                
                Iterator<? extends Completable> it;
                
                try {
                    it = sources.iterator();
                } catch (Throwable e) {
                    s.onError(e);
                    return;
                }
                
                if (it == null) {
                    s.onError(new NullPointerException("The iterator returned is null"));
                    return;
                }
                
                boolean empty = true;
                
                for (;;) {
                    if (once.get() || set.isUnsubscribed()) {
                        return;
                    }
                    
                    boolean b;
                    
                    try {
                        b = it.hasNext();
                    } catch (Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(e);
                        } else {
                            ERROR_HANDLER.handleError(e);
                        }
                        return;
                    }
                    
                    if (!b) {
                        if (empty) {
                            s.onCompleted();
                        }
                        break;
                    }
                    
                    empty = false;
                    
                    if (once.get() || set.isUnsubscribed()) {
                        return;
                    }

                    Completable c;
                    
                    try {
                        c = it.next();
                    } catch (Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(e);
                        } else {
                            ERROR_HANDLER.handleError(e);
                        }
                        return;
                    }
                    
                    if (c == null) {
                        NullPointerException npe = new NullPointerException("One of the sources is null");
                        if (once.compareAndSet(false, true)) {
                            set.unsubscribe();
                            s.onError(npe);
                        } else {
                            ERROR_HANDLER.handleError(npe);
                        }
                        return;
                    }
                    
                    if (once.get() || set.isUnsubscribed()) {
                        return;
                    }
                    
                    // no need to have separate subscribers because inner is stateless
                    c.unsafeSubscribe(inner);
                }
            }
        });
    }
    
    /**
     * Returns a Completable instance that completes immediately when subscribed to.
     * @return a Completable instance that completes immediately 
     */
    public static Completable complete() {
        return COMPLETE;
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    public static Completable concat(Completable... sources) {
        requireNonNull(sources);
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return sources[0];
        }
        return create(new CompletableOnSubscribeConcatArray(sources));
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    public static Completable concat(Iterable<? extends Completable> sources) {
        requireNonNull(sources);
        
        return create(new CompletableOnSubscribeConcatIterable(sources));
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    public static Completable concat(Observable<? extends Completable> sources) {
        return concat(sources, 2);
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    public static Completable concat(Observable<? extends Completable> sources, int prefetch) {
        requireNonNull(sources);
        if (prefetch < 1) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        return create(new CompletableOnSubscribeConcat(sources, prefetch));
    }
    
    /**
     * Constructs a Completable instance by wrapping the given onSubscribe callback.
     * @param onSubscribe the callback which will receive the CompletableSubscriber instances
     * when the Completable is subscribed to.
     * @return the created Completable instance
     * @throws NullPointerException if onSubscribe is null
     */
    public static Completable create(CompletableOnSubscribe onSubscribe) {
        requireNonNull(onSubscribe);
        try {
            return new Completable(onSubscribe);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            ERROR_HANDLER.handleError(ex);
            throw toNpe(ex);
        } 
    }
    
    /**
     * Defers the subscription to a Completable instance returned by a supplier.
     * @param completableFunc0 the supplier that returns the Completable that will be subscribed to.
     * @return the Completable instance
     */
    public static Completable defer(final Func0<? extends Completable> completableFunc0) {
        requireNonNull(completableFunc0);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                Completable c;
                
                try {
                    c = completableFunc0.call();
                } catch (Throwable e) {
                    s.onSubscribe(Subscriptions.unsubscribed());
                    s.onError(e);
                    return;
                }
                
                if (c == null) {
                    s.onSubscribe(Subscriptions.unsubscribed());
                    s.onError(new NullPointerException("The completable returned is null"));
                    return;
                }
                
                c.unsafeSubscribe(s);
            }
        });
    }
    
    /**
     * Creates a Completable which calls the given error supplier for each subscriber
     * and emits its returned Throwable.
     * <p>
     * If the errorFunc0 returns null, the child CompletableSubscribers will receive a
     * NullPointerException.
     * @param errorFunc0 the error supplier, not null
     * @return the new Completable instance
     * @throws NullPointerException if errorFunc0 is null
     */
    public static Completable error(final Func0<? extends Throwable> errorFunc0) {
        requireNonNull(errorFunc0);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                s.onSubscribe(Subscriptions.unsubscribed());
                Throwable error;
                
                try {
                    error = errorFunc0.call();
                } catch (Throwable e) {
                    error = e;
                }
                
                if (error == null) {
                    error = new NullPointerException("The error supplied is null");
                }
                s.onError(error);
            }
        });
    }

    /**
     * Creates a Completable instance that emits the given Throwable exception to subscribers.
     * @param error the Throwable instance to emit, not null
     * @return the new Completable instance
     * @throws NullPointerException if error is null
     */
    public static Completable error(final Throwable error) {
        requireNonNull(error);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                s.onSubscribe(Subscriptions.unsubscribed());
                s.onError(error);
            }
        });
    }
    
    /**
     * Returns a Completable instance that runs the given Action0 for each subscriber and
     * emits either an unchecked exception or simply completes.
     * @param run the runnable to run for each subscriber
     * @return the new Completable instance
     * @throws NullPointerException if run is null
     */
    public static Completable fromAction(final Action0 action) {
        requireNonNull(action);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                try {
                    action.call();
                } catch (Throwable e) {
                    if (!bs.isUnsubscribed()) {
                        s.onError(e);
                    }
                    return;
                }
                if (!bs.isUnsubscribed()) {
                    s.onCompleted();
                }
            }
        });
    }
    
    /**
     * Returns a Completable which when subscribed, executes the callable function, ignores its
     * normal result and emits onError or onCompleted only.
     * @param callable the callable instance to execute for each subscriber
     * @return the new Completable instance
     */
    public static Completable fromCallable(final Callable<?> callable) {
        requireNonNull(callable);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                BooleanSubscription bs = new BooleanSubscription();
                s.onSubscribe(bs);
                try {
                    callable.call();
                } catch (Throwable e) {
                    if (!bs.isUnsubscribed()) {
                        s.onError(e);
                    }
                    return;
                }
                if (!bs.isUnsubscribed()) {
                    s.onCompleted();
                }
            }
        });
    }
    
    /**
     * Returns a Completable instance that reacts to the termination of the given Future in a blocking fashion.
     * <p>
     * Note that cancellation from any of the subscribers to this Completable will cancel the future.
     * @param future the future to react to
     * @return the new Completable instance
     */
    public static Completable fromFuture(Future<?> future) {
        requireNonNull(future);
        return fromObservable(Observable.from(future));
    }
    
    /**
     * Returns a Completable instance that subscribes to the given flowable, ignores all values and
     * emits only the terminal event.
     * @param flowable the Flowable instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if flowable is null
     */
    public static Completable fromObservable(final Observable<?> flowable) {
        requireNonNull(flowable);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber cs) {
                Subscriber<Object> subscriber = new Subscriber<Object>() {

                    @Override
                    public void onCompleted() {
                        cs.onCompleted();
                    }

                    @Override
                    public void onError(Throwable t) {
                        cs.onError(t);
                    }

                    @Override
                    public void onNext(Object t) {
                        // ignored
                    }
                };
                cs.onSubscribe(subscriber);
                flowable.unsafeSubscribe(subscriber);
            }
        });
    }

    /**
     * Returns a Completable instance that when subscribed to, subscribes to the Single instance and
     * emits a completion event if the single emits onSuccess or forwards any onError events.
     * @param single the Single instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if single is null
     */
    public static Completable fromSingle(final Single<?> single) {
        requireNonNull(single);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                SingleSubscriber<Object> te = new SingleSubscriber<Object>() {

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSuccess(Object value) {
                        s.onCompleted();
                    }
                    
                };
                s.onSubscribe(te);
                single.subscribe(te);
            }
        });
    }
    
    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable merge(Completable... sources) {
        requireNonNull(sources);
        if (sources.length == 0) {
            return complete();
        } else
        if (sources.length == 1) {
            return sources[0];
        }
        return create(new CompletableOnSubscribeMergeArray(sources));
    }
    
    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable merge(Iterable<? extends Completable> sources) {
        requireNonNull(sources);
        return create(new CompletableOnSubscribeMergeIterable(sources));
    }

    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable merge(Observable<? extends Completable> sources) {
        return merge0(sources, Integer.MAX_VALUE, false);
    }
    
    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * @param sources the iterable sequence of sources.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @throws IllegalArgumentException if maxConcurrency is less than 1
     */
    public static Completable merge(Observable<? extends Completable> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, false);
        
    }
    
    /**
     * Returns a Completable instance that keeps subscriptions to a limited number of sources at once and
     * completes only when all source Completables terminate in one way or another, combining any exceptions
     * thrown by either the sources Observable or the inner Completable instances.
     * @param sources the iterable sequence of sources.
     * @param maxConcurrency the maximum number of concurrent subscriptions
     * @param delayErrors delay all errors from the main source and from the inner Completables?
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     * @throws IllegalArgumentException if maxConcurrency is less than 1
     */
    protected static Completable merge0(Observable<? extends Completable> sources, int maxConcurrency, boolean delayErrors) {
        requireNonNull(sources);
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        return create(new CompletableOnSubscribeMerge(sources, maxConcurrency, delayErrors));
    }
    
    /**
     * Returns a Completable that subscribes to all Completables in the source array and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * @param sources the array of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable mergeDelayError(Completable... sources) {
        requireNonNull(sources);
        return create(new CompletableOnSubscribeMergeDelayErrorArray(sources));
    }

    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable mergeDelayError(Iterable<? extends Completable> sources) {
        requireNonNull(sources);
        return create(new CompletableOnSubscribeMergeDelayErrorIterable(sources));
    }

    /**
     * Returns a Completable that subscribes to all Completables in the source sequence and delays
     * any error emitted by either the sources observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable mergeDelayError(Observable<? extends Completable> sources) {
        return merge0(sources, Integer.MAX_VALUE, true);
    }

    
    /**
     * Returns a Completable that subscribes to a limited number of inner Completables at once in 
     * the source sequence and delays any error emitted by either the sources 
     * observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * @param sources the sequence of Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable mergeDelayError(Observable<? extends Completable> sources, int maxConcurrency) {
        return merge0(sources, maxConcurrency, true);
    }
    
    /**
     * Returns a Completable that never calls onError or onComplete.
     * @return the singleton instance that never calls onError or onComplete
     */
    public static Completable never() {
        return NEVER;
    }
    
    /**
     * Java 7 backport: throws a NullPointerException if o is null.
     * @param o the object to check
     * @return the o value
     * @throws NullPointerException if o is null
     */
    static <T> T requireNonNull(T o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return o;
    }
    
    /**
     * Returns a Completable instance that fires its onComplete event after the given delay elapsed.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     */
    public static Completable timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    /**
     * Returns a Completable instance that fires its onComplete event after the given delay elapsed
     * by using the supplied scheduler.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     */
    public static Completable timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        requireNonNull(unit);
        requireNonNull(scheduler);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                MultipleAssignmentSubscription mad = new MultipleAssignmentSubscription();
                s.onSubscribe(mad);
                if (!mad.isUnsubscribed()) {
                    final Scheduler.Worker w = scheduler.createWorker();
                    mad.set(w);
                    w.schedule(new Action0() {
                        @Override
                        public void call() {
                            try {
                                s.onCompleted();
                            } finally {
                                w.unsubscribe();
                            }
                        }
                    }, delay, unit);
                }
            }
        });
    }
    
    /**
     * Creates a NullPointerException instance and sets the given Throwable as its initial cause.
     * @param ex the Throwable instance to use as cause, not null (not verified)
     * @return the created NullPointerException
     */
    static NullPointerException toNpe(Throwable ex) {
        NullPointerException npe = new NullPointerException("Actually not, but can't pass out an exception otherwise...");
        npe.initCause(ex);
        return npe;
    }
    
    /**
     * Returns a Completable instance which manages a resource along 
     * with a custom Completable instance while the subscription is active.
     * <p>
     * This overload performs an eager unsubscription before the terminal event is emitted.
     * 
     * @param resourceFunc0 the supplier that returns a resource to be managed. 
     * @param completableFunc1 the function that given a resource returns a Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @return the new Completable instance
     */
    public static <R> Completable using(Func0<R> resourceFunc0, 
            Func1<? super R, ? extends Completable> completableFunc1, 
            Action1<? super R> disposer) {
        return using(resourceFunc0, completableFunc1, disposer, true);
    }
    
    /**
     * Returns a Completable instance which manages a resource along 
     * with a custom Completable instance while the subscription is active and performs eager or lazy
     * resource disposition.
     * <p>
     * If this overload performs a lazy unsubscription after the terminal event is emitted.
     * Exceptions thrown at this time will be delivered to RxJavaPlugins only.
     * 
     * @param resourceFunc0 the supplier that returns a resource to be managed
     * @param completableFunc1 the function that given a resource returns a non-null
     * Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @param eager if true, the resource is disposed before the terminal event is emitted, if false, the
     * resource is disposed after the terminal event has been emitted
     * @return the new Completable instance
     */
    public static <R> Completable using(final Func0<R> resourceFunc0, 
            final Func1<? super R, ? extends Completable> completableFunc1, 
            final Action1<? super R> disposer, 
            final boolean eager) {
        requireNonNull(resourceFunc0);
        requireNonNull(completableFunc1);
        requireNonNull(disposer);
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                final R resource;
                
                try {
                    resource = resourceFunc0.call();
                } catch (Throwable e) {
                    s.onSubscribe(Subscriptions.unsubscribed());
                    s.onError(e);
                    return;
                }
                
                Completable cs;
                
                try {
                    cs = completableFunc1.call(resource);
                } catch (Throwable e) {
                    try {
                        disposer.call(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(e);
                        Exceptions.throwIfFatal(ex);

                        s.onSubscribe(Subscriptions.unsubscribed());
                        s.onError(new CompositeException(Arrays.asList(e, ex)));
                        return;
                    }
                    Exceptions.throwIfFatal(e);
                    
                    s.onSubscribe(Subscriptions.unsubscribed());
                    s.onError(e);
                    return;
                }
                
                if (cs == null) {
                    try {
                        disposer.call(resource);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.onSubscribe(Subscriptions.unsubscribed());
                        s.onError(new CompositeException(Arrays.asList(new NullPointerException("The completable supplied is null"), ex)));
                        return;
                    }
                    s.onSubscribe(Subscriptions.unsubscribed());
                    s.onError(new NullPointerException("The completable supplied is null"));
                    return;
                }
                
                final AtomicBoolean once = new AtomicBoolean();
                
                cs.unsafeSubscribe(new CompletableSubscriber() {
                    Subscription d;
                    void dispose() {
                        d.unsubscribe();
                        if (once.compareAndSet(false, true)) {
                            try {
                                disposer.call(resource);
                            } catch (Throwable ex) {
                                ERROR_HANDLER.handleError(ex);
                            }
                        }
                    }

                    @Override
                    public void onCompleted() {
                        if (eager) {
                            if (once.compareAndSet(false, true)) {
                                try {
                                    disposer.call(resource);
                                } catch (Throwable ex) {
                                    s.onError(ex);
                                    return;
                                }
                            }
                        }
                        
                        s.onCompleted();
                        
                        if (!eager) {
                            dispose();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (eager) {
                            if (once.compareAndSet(false, true)) {
                                try {
                                    disposer.call(resource);
                                } catch (Throwable ex) {
                                    e = new CompositeException(Arrays.asList(e, ex));
                                }
                            }
                        }
                        
                        s.onError(e);
                        
                        if (!eager) {
                            dispose();
                        }
                    }
                    
                    @Override
                    public void onSubscribe(Subscription d) {
                        this.d = d;
                        s.onSubscribe(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                dispose();
                            }
                        }));
                    }
                });
            }
        });
    }
    
    /** The actual subscription action. */
    private final CompletableOnSubscribe onSubscribe;
    
    /**
     * Constructs a Completable instance with the given onSubscribe callback.
     * @param onSubscribe the callback that will receive CompletableSubscribers when they subscribe,
     * not null (not verified)
     */
    protected Completable(CompletableOnSubscribe onSubscribe) {
        this.onSubscribe = HOOK.onCreate(onSubscribe);
    }
    
    /**
     * Returns a Completable that emits the a terminated event of either this Completable
     * or the other Completable whichever fires first.
     * @param other the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    public final Completable ambWith(Completable other) {
        requireNonNull(other);
        return amb(this, other);
    }
    
    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner and
     * rethrows any exception emitted.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    public final void await() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Subscription d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
            return;
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (err[0] != null) {
            Exceptions.propagate(err[0]);
        }
    }
    
    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner
     * with a specific timeout and rethrows any exception emitted within the timeout window.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return true if the this Completable instance completed normally within the time limit,
     * false if the timeout elapsed before this Completable terminated.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    public final boolean await(long timeout, TimeUnit unit) {
        requireNonNull(unit);
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Subscription d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
            return true;
        }
        boolean b;
        try {
             b = cdl.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (b) {
            if (err[0] != null) {
                Exceptions.propagate(err[0]);
            }
        }
        return b;
    }
    
    /**
     * Calls the given transformer function with this instance and returns the function's resulting
     * Completable.
     * @param transformer the transformer function, not null
     * @return the Completable returned by the function
     * @throws NullPointerException if transformer is null
     */
    public final Completable compose(CompletableTransformer transformer) {
        return to(transformer);
    }
    
    /**
     * Returns an Observable which will subscribe to this Completable and once that is completed then 
     * will subscribe to the {@code next} Observable. An error event from this Completable will be 
     * propagated to the downstream subscriber and will result in skipping the subscription of the 
     * Observable.  
     * 
     * @param next the Observable to subscribe after this Completable is completed, not null
     * @return Observable that composes this Completable and next
     * @throws NullPointerException if next is null
     */
    public final <T> Observable<T> andThen(Observable<T> next) {
        requireNonNull(next);
        return next.delaySubscription(toObservable());
    }

    /**
     * Returns a Single which will subscribe to this Completable and once that is completed then
     * will subscribe to the {@code next} Single. An error event from this Completable will be
     * propagated to the downstream subscriber and will result in skipping the subscription of the
     * Single.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code andThen} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param next the Single to subscribe after this Completable is completed, not null
     * @return Single that composes this Completable and next
     */
    public final <T> Single<T> andThen(Single<T> next) {
        requireNonNull(next);
        return next.delaySubscription(toObservable());
    }

    /**
     * Returns a completable that first runs this Completable
     * and then the other completable.
     * <p>
     * This is an alias for {@link #concatWith(Completable)}.
     * @param next the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    public final Completable andThen(Completable next) {
        return concatWith(next);
    }
    
    /**
     * Concatenates this Completable with another Completable.
     * @param other the other Completable, not null
     * @return the new Completable which subscribes to this and then the other Completable
     * @throws NullPointerException if other is null
     */
    public final Completable concatWith(Completable other) {
        requireNonNull(other);
        return concat(this, other);
    }

    /**
     * Returns a Completable which delays the emission of the completion event by the given time.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    public final Completable delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation(), false);
    }
    
    /**
     * Returns a Completable which delays the emission of the completion event by the given time while
     * running on the specified scheduler.
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler to run the delayed completion on
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    public final Completable delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(delay, unit, scheduler, false);
    }
    
    /**
     * Returns a Completable which delays the emission of the completion event, and optionally the error as well, by the given time while
     * running on the specified scheduler.
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler to run the delayed completion on
     * @param delayError delay the error emission as well?
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    public final Completable delay(final long delay, final TimeUnit unit, final Scheduler scheduler, final boolean delayError) {
        requireNonNull(unit);
        requireNonNull(scheduler);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                final CompositeSubscription set = new CompositeSubscription();
                
                final Scheduler.Worker w = scheduler.createWorker();
                set.add(w);
                
                unsafeSubscribe(new CompletableSubscriber() {

                    
                    @Override
                    public void onCompleted() {
                        set.add(w.schedule(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    s.onCompleted();
                                } finally {
                                    w.unsubscribe();
                                }
                            }
                        }, delay, unit));
                    }

                    @Override
                    public void onError(final Throwable e) {
                        if (delayError) {
                            set.add(w.schedule(new Action0() {
                                @Override
                                public void call() {
                                    try {
                                        s.onError(e);
                                    } finally {
                                        w.unsubscribe();
                                    }
                                }
                            }, delay, unit));
                        } else {
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        set.add(d);
                        s.onSubscribe(set);
                    }
                    
                });
            }
        });
    }

    /**
     * Returns a Completable which calls the given onComplete callback if this Completable completes.
     * @param onComplete the callback to call when this emits an onComplete event
     * @return the new Completable instance
     * @throws NullPointerException if onComplete is null
     * @deprecated Use {@link #doOnCompleted(Action0)} instead.
     */
    @Deprecated public final Completable doOnComplete(Action0 onComplete) {
        return doOnCompleted(onComplete);
    }

    /**
     * Returns a Completable which calls the given onCompleted callback if this Completable completes.
     * @param onCompleted the callback to call when this emits an onComplete event
     * @return the new Completable instance
     * @throws NullPointerException if onComplete is null
     */
    public final Completable doOnCompleted(Action0 onCompleted) {
        return doOnLifecycle(Actions.empty(), Actions.empty(), onCompleted, Actions.empty(), Actions.empty());
    }
    
    /**
     * Returns a Completable which calls the given onUnsubscribe callback if the child subscriber cancels
     * the subscription.
     * @param onUnsubscribe the callback to call when the child subscriber cancels the subscription
     * @return the new Completable instance
     * @throws NullPointerException if onDispose is null
     */
    public final Completable doOnUnsubscribe(Action0 onUnsubscribe) {
        return doOnLifecycle(Actions.empty(), Actions.empty(), Actions.empty(), Actions.empty(), onUnsubscribe);
    }
    
    /**
     * Returns a Completable which calls the given onError callback if this Completable emits an error.
     * @param onError the error callback
     * @return the new Completable instance
     * @throws NullPointerException if onError is null
     */
    public final Completable doOnError(Action1<? super Throwable> onError) {
        return doOnLifecycle(Actions.empty(), onError, Actions.empty(), Actions.empty(), Actions.empty());
    }

    /**
     * Returns a Completable instance that calls the various callbacks on the specific
     * lifecycle events.
     * @param onSubscribe the consumer called when a CompletableSubscriber subscribes.
     * @param onError the consumer called when this emits an onError event
     * @param onComplete the runnable called just before when this Completable completes normally
     * @param onAfterComplete the runnable called after this Completable completes normally
     * @param onUnsubscribe the runnable called when the child cancels the subscription
     * @return the new Completable instance
     */
    protected final Completable doOnLifecycle(
            final Action1<? super Subscription> onSubscribe, 
            final Action1<? super Throwable> onError, 
            final Action0 onComplete, 
            final Action0 onAfterComplete,
            final Action0 onUnsubscribe) {
        requireNonNull(onSubscribe);
        requireNonNull(onError);
        requireNonNull(onComplete);
        requireNonNull(onAfterComplete);
        requireNonNull(onUnsubscribe);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        try {
                            onComplete.call();
                        } catch (Throwable e) {
                            s.onError(e);
                            return;
                        }
                        
                        s.onCompleted();
                        
                        try {
                            onAfterComplete.call();
                        } catch (Throwable e) {
                            ERROR_HANDLER.handleError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            onError.call(e);
                        } catch (Throwable ex) {
                            e = new CompositeException(Arrays.asList(e, ex));
                        }
                        
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(final Subscription d) {
                        
                        try {
                            onSubscribe.call(d);
                        } catch (Throwable ex) {
                            d.unsubscribe();
                            s.onSubscribe(Subscriptions.unsubscribed());
                            s.onError(ex);
                            return;
                        }
                        
                        s.onSubscribe(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    onUnsubscribe.call();
                                } catch (Throwable e) {
                                    ERROR_HANDLER.handleError(e);
                                }
                                d.unsubscribe();
                            }
                        }));
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable instance that calls the given onSubscribe callback with the disposable
     * that child subscribers receive on subscription.
     * @param onSubscribe the callback called when a child subscriber subscribes
     * @return the new Completable instance
     * @throws NullPointerException if onSubscribe is null
     */
    public final Completable doOnSubscribe(Action1<? super Subscription> onSubscribe) {
        return doOnLifecycle(onSubscribe, Actions.empty(), Actions.empty(), Actions.empty(), Actions.empty());
    }
    
    /**
     * Returns a Completable instance that calls the given onTerminate callback just before this Completable
     * completes normally or with an exception
     * @param onTerminate the callback to call just before this Completable terminates
     * @return the new Completable instance
     */
    public final Completable doOnTerminate(final Action0 onTerminate) {
        return doOnLifecycle(Actions.empty(), new Action1<Throwable>() {
            @Override
            public void call(Throwable e) {
                onTerminate.call();
            }
        }, onTerminate, Actions.empty(), Actions.empty());
    }
    
    /**
     * Returns a completable that first runs this Completable
     * and then the other completable.
     * <p>
     * This is an alias for {@link #concatWith(Completable)}.
     * @param other the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     * @deprecated Use {@link #andThen(rx.Completable)} instead.
     */
    @Deprecated
    public final Completable endWith(Completable other) {
        return andThen(other);
    }
    
    /**
     * Returns an Observable that first runs this Completable instance and
     * resumes with the given next Observable.
     * @param next the next Observable to continue
     * @return the new Observable instance
     * @deprecated Use {@link #andThen(rx.Observable)} instead.
     */
    @Deprecated
    public final <T> Observable<T> endWith(Observable<T> next) {
        return andThen(next);
    }

    /**
     * Returns a Completable instance that calls the given onAfterComplete callback after this
     * Completable completes normally.
     * @param onAfterComplete the callback to call after this Completable emits an onComplete event.
     * @return the new Completable instance
     * @throws NullPointerException if onAfterComplete is null
     */
    public final Completable doAfterTerminate(Action0 onAfterComplete) {
        return doOnLifecycle(Actions.empty(), Actions.empty(), Actions.empty(), onAfterComplete, Actions.empty());
    }
    
    /**
     * Subscribes to this Completable instance and blocks until it terminates, then returns null or
     * the emitted exception if any.
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted
     */
    public final Throwable get() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Subscription d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            return err[0];
        }
        try {
            cdl.await();
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        return err[0];
    }
    
    /**
     * Subscribes to this Completable instance and blocks until it terminates or the specified timeout 
     * elapses, then returns null for normal termination or the emitted exception if any.
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted or
     * TimeoutException if the specified timeout elapsed before it
     */
    public final Throwable get(long timeout, TimeUnit unit) {
        requireNonNull(unit);
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onCompleted() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Subscription d) {
                // ignored
            }
            
        });
        
        if (cdl.getCount() == 0) {
            return err[0];
        }
        boolean b;
        try {
            b = cdl.await(timeout, unit);
        } catch (InterruptedException ex) {
            throw Exceptions.propagate(ex);
        }
        if (b) {
            return err[0];
        }
        Exceptions.propagate(new TimeoutException());
        return null;
    }
    
    /**
     * Lifts a CompletableSubscriber transformation into the chain of Completables.
     * @param onLift the lifting function that transforms the child subscriber with a parent subscriber.
     * @return the new Completable instance
     * @throws NullPointerException if onLift is null
     */
    public final Completable lift(final CompletableOperator onLift) {
        requireNonNull(onLift);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(CompletableSubscriber s) {
                try {
                    CompletableOperator onLiftDecorated = HOOK.onLift(onLift);
                    CompletableSubscriber sw = onLiftDecorated.call(s);
                    
                    unsafeSubscribe(sw);
                } catch (NullPointerException ex) {
                    throw ex;
                } catch (Throwable ex) {
                    throw toNpe(ex);
                }
            }
        });
    }

    /**
     * Returns a Completable which subscribes to this and the other Completable and completes
     * when both of them complete or one emits an error.
     * @param other the other Completable instance
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    public final Completable mergeWith(Completable other) {
        requireNonNull(other);
        return merge(this, other);
    }
    
    /**
     * Returns a Completable which emits the terminal events from the thread of the specified scheduler.
     * @param scheduler the scheduler to emit terminal events on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    public final Completable observeOn(final Scheduler scheduler) {
        requireNonNull(scheduler);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                
                final SubscriptionList ad = new SubscriptionList();
                
                final Scheduler.Worker w = scheduler.createWorker();
                ad.add(w);
                
                s.onSubscribe(ad);
                
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        w.schedule(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    s.onCompleted();
                                } finally {
                                    ad.unsubscribe();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Throwable e) {
                        w.schedule(new Action0() {
                            @Override
                            public void call() {
                                try {
                                    s.onError(e);
                                } finally {
                                    ad.unsubscribe();
                                }
                            }
                        });
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        ad.add(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable instance that if this Completable emits an error, it will emit an onComplete
     * and swallow the throwable.
     * @return the new Completable instance
     */
    public final Completable onErrorComplete() {
        return onErrorComplete(UtilityFunctions.alwaysTrue());
    }

    /**
     * Returns a Completable instance that if this Completable emits an error and the predicate returns
     * true, it will emit an onComplete and swallow the throwable.
     * @param predicate the predicate to call when an Throwable is emitted which should return true
     * if the Throwable should be swallowed and replaced with an onComplete.
     * @return the new Completable instance
     */
    public final Completable onErrorComplete(final Func1<? super Throwable, Boolean> predicate) {
        requireNonNull(predicate);
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        s.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        boolean b;
                        
                        try {
                            b = predicate.call(e);
                        } catch (Throwable ex) {
                            e = new CompositeException(Arrays.asList(e, ex));
                            return;
                        }
                        
                        if (b) {
                            s.onCompleted();
                        } else {
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        s.onSubscribe(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable instance that when encounters an error from this Completable, calls the
     * specified mapper function that returns another Completable instance for it and resumes the
     * execution with it.
     * @param errorMapper the mapper function that takes the error and should return a Completable as
     * continuation.
     * @return the new Completable instance
     */
    public final Completable onErrorResumeNext(final Func1<? super Throwable, ? extends Completable> errorMapper) {
        requireNonNull(errorMapper);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                final SerialSubscription sd = new SerialSubscription();
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        s.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Completable c;
                        
                        try {
                            c = errorMapper.call(e);
                        } catch (Throwable ex) {
                            e = new CompositeException(Arrays.asList(e, ex));
                            s.onError(e);
                            return;
                        }
                        
                        if (c == null) {
                            NullPointerException npe = new NullPointerException("The completable returned is null");
                            e = new CompositeException(Arrays.asList(e, npe));
                            s.onError(e);
                            return;
                        }
                        
                        c.unsafeSubscribe(new CompletableSubscriber() {

                            @Override
                            public void onCompleted() {
                                s.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                s.onError(e);
                            }

                            @Override
                            public void onSubscribe(Subscription d) {
                                sd.set(d);
                            }
                            
                        });
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        sd.set(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable that repeatedly subscribes to this Completable until cancelled.
     * @return the new Completable instance
     */
    public final Completable repeat() {
        return fromObservable(toObservable().repeat());
    }
    
    /**
     * Returns a Completable that subscribes repeatedly at most the given times to this Completable.
     * @param times the number of times the resubscription should happen
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is less than zero
     */
    public final Completable repeat(long times) {
        return fromObservable(toObservable().repeat(times));
    }
    
    /**
     * Returns a Completable instance that repeats when the Publisher returned by the handler
     * emits an item or completes when this Publisher emits a completed event.
     * @param handler the function that transforms the stream of values indicating the completion of
     * this Completable and returns a Publisher that emits items for repeating or completes to indicate the
     * repetition should stop
     * @return the new Completable instance
     * @throws NullPointerException if stop is null
     */
    public final Completable repeatWhen(Func1<? super Observable<? extends Void>, ? extends Observable<?>> handler) {
        requireNonNull(handler); // FIXME do a null check in Observable
        return fromObservable(toObservable().repeatWhen(handler));
    }
    
    /**
     * Returns a Completable that retries this Completable as long as it emits an onError event.
     * @return the new Completable instance
     */
    public final Completable retry() {
        return fromObservable(toObservable().retry());
    }
    
    /**
     * Returns a Completable that retries this Completable in case of an error as long as the predicate
     * returns true.
     * @param predicate the predicate called when this emits an error with the repeat count and the latest exception
     * and should return true to retry.
     * @return the new Completable instance
     */
    public final Completable retry(Func2<Integer, Throwable, Boolean> predicate) {
        return fromObservable(toObservable().retry(predicate));
    }

    /**
     * Returns a Completable that when this Completable emits an error, retries at most the given
     * number of times before giving up and emitting the last error.
     * @param times the number of times the returned Completable should retry this Completable
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is negative
     */
    public final Completable retry(long times) {
        return fromObservable(toObservable().retry(times));
    }

    /**
     * Returns a Completable which given a Publisher and when this Completable emits an error, delivers
     * that error through an Observable and the Publisher should return a value indicating a retry in response
     * or a terminal event indicating a termination.
     * @param handler the handler that receives an Observable delivering Throwables and should return a Publisher that
     * emits items to indicate retries or emits terminal events to indicate termination.
     * @return the new Completable instance
     * @throws NullPointerException if handler is null
     */
    public final Completable retryWhen(Func1<? super Observable<? extends Throwable>, ? extends Observable<?>> handler) {
        return fromObservable(toObservable().retryWhen(handler));
    }

    /**
     * Returns a Completable which first runs the other Completable
     * then this completable if the other completed normally.
     * @param other the other completable to run first
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    public final Completable startWith(Completable other) {
        requireNonNull(other);
        return concat(other, this);
    }

    /**
     * Returns an Observable which first delivers the events
     * of the other Observable then runs this Completable.
     * @param other the other Observable to run first
     * @return the new Observable instance
     * @throws NullPointerException if other is null
     */
    public final <T> Observable<T> startWith(Observable<T> other) {
        requireNonNull(other);
        return this.<T>toObservable().startWith(other);
    }
    
    /**
     * Subscribes to this Completable and returns a Subscription which can be used to cancel
     * the subscription.
     * @return the Subscription that allows cancelling the subscription
     */
    public final Subscription subscribe() {
        final MultipleAssignmentSubscription mad = new MultipleAssignmentSubscription();
        unsafeSubscribe(new CompletableSubscriber() {
            @Override
            public void onCompleted() {
                mad.unsubscribe();
            }
            
            @Override
            public void onError(Throwable e) {
                ERROR_HANDLER.handleError(e);
                mad.unsubscribe();
                deliverUncaughtException(e);
            }
            
            @Override
            public void onSubscribe(Subscription d) {
                mad.set(d);
            }
        });
        
        return mad;
    }
    /**
     * Subscribes to this Completable and calls the given Action0 when this Completable
     * completes normally.
     * <p>
     * If this Completable emits an error, it is sent to ERROR_HANDLER.handleError and gets swallowed.
     * @param onComplete the runnable called when this Completable completes normally
     * @return the Subscription that allows cancelling the subscription
     */
    public final Subscription subscribe(final Action0 onComplete) {
        requireNonNull(onComplete);
        
        final MultipleAssignmentSubscription mad = new MultipleAssignmentSubscription();
        unsafeSubscribe(new CompletableSubscriber() {
            boolean done;
            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    try {
                        onComplete.call();
                    } catch (Throwable e) {
                        ERROR_HANDLER.handleError(e);
                        deliverUncaughtException(e);
                    } finally {
                        mad.unsubscribe();
                    }
                }
            }
            
            @Override
            public void onError(Throwable e) {
                ERROR_HANDLER.handleError(e);
                mad.unsubscribe();
                deliverUncaughtException(e);
            }
            
            @Override
            public void onSubscribe(Subscription d) {
                mad.set(d);
            }
        });
        
        return mad;
    }

    /**
     * Subscribes to this Completable and calls back either the onError or onComplete functions.
     * 
     * @param onError the consumer that is called if this Completable emits an error
     * @param onComplete the runnable that is called if the Completable completes normally
     * @return the Subscription that can be used for cancelling the subscription asynchronously
     * @throws NullPointerException if either callback is null
     */
    public final Subscription subscribe(final Action1<? super Throwable> onError, final Action0 onComplete) {
        requireNonNull(onError);
        requireNonNull(onComplete);
        
        final MultipleAssignmentSubscription mad = new MultipleAssignmentSubscription();
        unsafeSubscribe(new CompletableSubscriber() {
            boolean done;
            @Override
            public void onCompleted() {
                if (!done) {
                    done = true;
                    try {
                        onComplete.call();
                    } catch (Throwable e) {
                        callOnError(e);
                        return;
                    }
                    mad.unsubscribe();
                }
            }
            
            @Override
            public void onError(Throwable e) {
                if (!done) {
                    done = true;
                    callOnError(e);
                } else {
                    ERROR_HANDLER.handleError(e);
                    deliverUncaughtException(e);
                }
            }
            
            void callOnError(Throwable e) {
                try {
                    onError.call(e);
                } catch (Throwable ex) {
                    e = new CompositeException(Arrays.asList(e, ex));
                    ERROR_HANDLER.handleError(e);
                    deliverUncaughtException(e);
                } finally {
                    mad.unsubscribe();
                }
            }
            
            @Override
            public void onSubscribe(Subscription d) {
                mad.set(d);
            }
        });
        
        return mad;
    }

    private static void deliverUncaughtException(Throwable e) {
        Thread thread = Thread.currentThread();
        thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
    }

    /**
     * Subscribes the given CompletableSubscriber to this Completable instance.
     * @param s the CompletableSubscriber, not null
     * @throws NullPointerException if s is null
     */
    public final void unsafeSubscribe(CompletableSubscriber s) {
        requireNonNull(s);
        try {
            CompletableOnSubscribe onSubscribeDecorated = HOOK.onSubscribeStart(this, this.onSubscribe);
            
            onSubscribeDecorated.call(s);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            ex = HOOK.onSubscribeError(ex);
            ERROR_HANDLER.handleError(ex);
            throw toNpe(ex);
        }
    }

    /**
     * Subscribes the given CompletableSubscriber to this Completable instance
     * and handles exceptions thrown by its onXXX methods.
     * @param s the CompletableSubscriber, not null
     * @throws NullPointerException if s is null
     */
    public final void subscribe(CompletableSubscriber s) {
        if (!(s instanceof SafeCompletableSubscriber)) {
            s = new SafeCompletableSubscriber(s);
        }
        unsafeSubscribe(s);
    }

    /**
     * Subscribes a regular Subscriber to this Completable instance which
     * will receive only an onError or onComplete event.
     * @param <T> the value type of the subscriber
     * @param s the reactive-streams Subscriber, not null
     * @throws NullPointerException if s is null
     */
    public final <T> void unsafeSubscribe(final Subscriber<T> s) {
        unsafeSubscribe(s, true);
    }

    /**
     * Performs the actual unsafe subscription and calls the onStart if required.
     * @param <T> the value type of the subscriber
     * @param s the subscriber instance, not null
     * @param callOnStart if true, the Subscriber.onStart will be called
     * @throws NullPointerException if s is null
     */
    private final <T> void unsafeSubscribe(final Subscriber<T> s, boolean callOnStart) {
        requireNonNull(s);
        try {
            if (callOnStart) {
                s.onStart();
            }
            unsafeSubscribe(new CompletableSubscriber() {
                @Override
                public void onCompleted() {
                    s.onCompleted();
                }
                
                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }
                
                @Override
                public void onSubscribe(Subscription d) {
                    s.add(d);
                }
            });
            RxJavaPlugins.getInstance().getObservableExecutionHook().onSubscribeReturn(s);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            ex = HOOK.onSubscribeError(ex);
            ERROR_HANDLER.handleError(ex);
            throw toNpe(ex);
        }
    }

    /**
     * Subscribes a regular Subscriber to this Completable instance which
     * will receive only an onError or onComplete event
     * and handles exceptions thrown by its onXXX methods.
     * @param <T> the value type of the subscriber
     * @param s the reactive-streams Subscriber, not null
     * @throws NullPointerException if s is null
     */
    public final <T> void subscribe(Subscriber<T> s) {
        s.onStart();
        if (!(s instanceof SafeSubscriber)) {
            s = new SafeSubscriber<T>(s);
        }
        unsafeSubscribe(s, false);
    }

    /**
     * Returns a Completable which subscribes the child subscriber on the specified scheduler, making
     * sure the subscription side-effects happen on that specific thread of the scheduler.
     * @param scheduler the Scheduler to subscribe on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    public final Completable subscribeOn(final Scheduler scheduler) {
        requireNonNull(scheduler);
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                // FIXME cancellation of this schedule
                
                final Scheduler.Worker w = scheduler.createWorker();
                
                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            unsafeSubscribe(s);
                        } finally {
                            w.unsubscribe();
                        }
                    }
                });
            }
        });
    }

    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    public final Completable timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param other the other Completable instance to switch to in case of a timeout
     * @return the new Completable instance
     * @throws NullPointerException if unit or other is null
     */
    public final Completable timeout(long timeout, TimeUnit unit, Completable other) {
        requireNonNull(other);
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }
    
    /**
     * Returns a Completable that runs this Completable and emits a TimeoutException in case
     * this Completable doesn't complete within the given time while "waiting" on the specified
     * Scheduler.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler is null
     */
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }
    
    /**
     * Returns a Completable that runs this Completable and switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @param other the other Completable instance to switch to in case of a timeout
     * @return the new Completable instance
     * @throws NullPointerException if unit, scheduler or other is null
     */
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler, Completable other) {
        requireNonNull(other);
        return timeout0(timeout, unit, scheduler, other);
    }
    
    /**
     * Returns a Completable that runs this Completable and optionally switches to the other Completable
     * in case this Completable doesn't complete within the given time while "waiting" on
     * the specified scheduler.
     * @param timeout the timeout value
     * @param unit the timeout unit
     * @param scheduler the scheduler to use to wait for completion
     * @param other the other Completable instance to switch to in case of a timeout, 
     * if null a TimeoutException is emitted instead
     * @return the new Completable instance
     * @throws NullPointerException if unit or scheduler
     */
    public final Completable timeout0(long timeout, TimeUnit unit, Scheduler scheduler, Completable other) {
        requireNonNull(unit);
        requireNonNull(scheduler);
        return create(new CompletableOnSubscribeTimeout(this, timeout, unit, scheduler, other));
    }
    
    /**
     * Allows fluent conversion to another type via a function callback.
     * @param converter the function called with this which should return some other value.
     * @return the converted value
     * @throws NullPointerException if converter is null
     */
    public final <U> U to(Func1<? super Completable, U> converter) {
        return converter.call(this);
    }

    /**
     * Returns an Observable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * @return the new Observable created
     */
    public final <T> Observable<T> toObservable() {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> s) {
                unsafeSubscribe(s);
            }
        });
    }
    
    /**
     * Converts this Completable into a Single which when this Completable completes normally,
     * calls the given supplier and emits its returned value through onSuccess.
     * @param completionValueFunc0 the value supplier called when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValueFunc0 is null
     */
    public final <T> Single<T> toSingle(final Func0<? extends T> completionValueFunc0) {
        requireNonNull(completionValueFunc0);
        return Single.create(new rx.Single.OnSubscribe<T>() {
            @Override
            public void call(final SingleSubscriber<? super T> s) {
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        T v;

                        try {
                            v = completionValueFunc0.call();
                        } catch (Throwable e) {
                            s.onError(e);
                            return;
                        }
                        
                        if (v == null) {
                            s.onError(new NullPointerException("The value supplied is null"));
                        } else {
                            s.onSuccess(v);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(Subscription d) {
                        s.add(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Converts this Completable into a Single which when this Completable completes normally,
     * emits the given value through onSuccess.
     * @param completionValue the value to emit when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValue is null
     */
    public final <T> Single<T> toSingleDefault(final T completionValue) {
        requireNonNull(completionValue);
        return toSingle(new Func0<T>() {
            @Override
            public T call() {
                return completionValue;
            }
        });
    }
    
    /**
     * Returns a Completable which makes sure when a subscriber cancels the subscription, the 
     * dispose is called on the specified scheduler
     * @param scheduler the target scheduler where to execute the cancellation
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    public final Completable unsubscribeOn(final Scheduler scheduler) {
        requireNonNull(scheduler);
        return create(new CompletableOnSubscribe() {
            @Override
            public void call(final CompletableSubscriber s) {
                unsafeSubscribe(new CompletableSubscriber() {

                    @Override
                    public void onCompleted() {
                        s.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(final Subscription d) {
                        s.onSubscribe(Subscriptions.create(new Action0() {
                            @Override
                            public void call() {
                                final Scheduler.Worker w = scheduler.createWorker();
                                w.schedule(new Action0() {
                                    @Override
                                    public void call() {
                                        try {
                                            d.unsubscribe();
                                        } finally {
                                            w.unsubscribe();
                                        }
                                    }
                                });
                            }
                        }));
                    }
                    
                });
            }
        });
    }
}