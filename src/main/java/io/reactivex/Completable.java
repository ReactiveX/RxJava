/**
 * Copyright 2016 Netflix, Inc.
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
package io.reactivex;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.*;

import io.reactivex.Observable.*;
import io.reactivex.Single.*;
import io.reactivex.annotations.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.completable.*;
import io.reactivex.internal.subscriptions.DisposableSubscription;
import io.reactivex.internal.util.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation without any value but only indication for completion or exception.
 * 
 * The class follows a similar event pattern as Reactive-Streams: onSubscribe (onError|onComplete)?
 */
public class Completable {
    /**
     * Callback used for building deferred computations that takes a CompletableSubscriber.
     */
    public interface CompletableOnSubscribe extends Consumer<CompletableSubscriber> {
        
    }
    
    /**
     * Convenience interface and callback used by the lift operator that given a child CompletableSubscriber,
     * return a parent CompletableSubscriber that does any kind of lifecycle-related transformations.
     */
    public interface CompletableOperator extends Function<CompletableSubscriber, CompletableSubscriber> {
        
    }
    
    /**
     * Represents the subscription API callbacks when subscribing to a Completable instance.
     */
    public interface CompletableSubscriber {
        /**
         * Called once the deferred computation completes normally.
         */
        void onComplete();
        
        /**
         * Called once if the deferred computation 'throws' an exception.
         * @param e the exception, not null.
         */
        void onError(Throwable e);
        
        /**
         * Called once by the Completable to set a Disposable on this instance which
         * then can be used to cancel the subscription at any time.
         * @param d the Disposable instance to call dispose on for cancellation, not null
         */
        void onSubscribe(Disposable d);
    }
    
    /**
     * Convenience interface and callback used by the compose operator to turn a Completable into another
     * Completable fluently.
     */
    public interface CompletableTransformer extends Function<Completable, Completable> {
        
    }
    
    /** Single instance of a complete Completable. */
    static final Completable COMPLETE = create(new CompletableOnSubscribe() {
        @Override
        public void accept(CompletableSubscriber s) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onComplete();
        }
    });
    
    /** Single instance of a never Completable. */
    static final Completable NEVER = create(new CompletableOnSubscribe() {
        @Override
        public void accept(CompletableSubscriber s) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
        }
    });
    
    /**
     * Returns a Completable which terminates as soon as one of the source Completables
     * terminates (normally or with an error) and cancels all other Completables.
     * @param sources the array of source Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable amb(final Completable... sources) {
        Objects.requireNonNull(sources, "sources is null");
        if (sources.length == 0) {
            return complete();
        }
        if (sources.length == 1) {
            return sources[0];
        }
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);

                final AtomicBoolean once = new AtomicBoolean();
                
                CompletableSubscriber inner = new CompletableSubscriber() {
                    @Override
                    public void onComplete() {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onComplete();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(e);
                        } else {
                            RxJavaPlugins.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        set.add(d);
                    }
                    
                };
                
                for (Completable c : sources) {
                    if (set.isDisposed()) {
                        return;
                    }
                    if (c == null) {
                        NullPointerException npe = new NullPointerException("One of the sources is null");
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(npe);
                        } else {
                            RxJavaPlugins.onError(npe);
                        }
                        return;
                    }
                    if (once.get() || set.isDisposed()) {
                        return;
                    }
                    
                    // no need to have separate subscribers because inner is stateless
                    c.subscribe(inner);
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
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable amb(final Iterable<? extends Completable> sources) {
        Objects.requireNonNull(sources, "sources is null");
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);

                final AtomicBoolean once = new AtomicBoolean();
                
                CompletableSubscriber inner = new CompletableSubscriber() {
                    @Override
                    public void onComplete() {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onComplete();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(e);
                        } else {
                            RxJavaPlugins.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
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
                    if (once.get() || set.isDisposed()) {
                        return;
                    }
                    
                    boolean b;
                    
                    try {
                        b = it.hasNext();
                    } catch (Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(e);
                        } else {
                            RxJavaPlugins.onError(e);
                        }
                        return;
                    }
                    
                    if (!b) {
                        if (empty) {
                            s.onComplete();
                        }
                        break;
                    }
                    
                    empty = false;
                    
                    if (once.get() || set.isDisposed()) {
                        return;
                    }

                    Completable c;
                    
                    try {
                        c = it.next();
                    } catch (Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(e);
                        } else {
                            RxJavaPlugins.onError(e);
                        }
                        return;
                    }
                    
                    if (c == null) {
                        NullPointerException npe = new NullPointerException("One of the sources is null");
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(npe);
                        } else {
                            RxJavaPlugins.onError(npe);
                        }
                        return;
                    }
                    
                    if (once.get() || set.isDisposed()) {
                        return;
                    }
                    
                    // no need to have separate subscribers because inner is stateless
                    c.subscribe(inner);
                }
            }
        });
    }
    
    /**
     * Returns a Completable instance that completes immediately when subscribed to.
     * @return a Completable instance that completes immediately 
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable complete() {
        return COMPLETE;
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable concat(Completable... sources) {
        Objects.requireNonNull(sources, "sources is null");
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
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable concat(Iterable<? extends Completable> sources) {
        Objects.requireNonNull(sources, "sources is null");
        
        return create(new CompletableOnSubscribeConcatIterable(sources));
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable concat(Flowable<? extends Completable> sources) {
        return concat(sources, 2);
    }
    
    /**
     * Returns a Completable which completes only when all sources complete, one after another.
     * @param sources the sources to concatenate
     * @param prefetch the number of sources to prefetch from the sources
     * @return the Completable instance which completes only when all sources complete
     * @throws NullPointerException if sources is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable concat(Flowable<? extends Completable> sources, int prefetch) {
        Objects.requireNonNull(sources, "sources is null");
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
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable create(CompletableOnSubscribe onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        try {
            // TODO plugin wrapping onSubscribe
            
            return new Completable(onSubscribe);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        } 
    }
    
    /**
     * Defers the subscription to a Completable instance returned by a supplier.
     * @param completableSupplier the supplier that returns the Completable that will be subscribed to.
     * @return the Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable defer(final Supplier<? extends Completable> completableSupplier) {
        Objects.requireNonNull(completableSupplier, "completableSupplier");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                Completable c;
                
                try {
                    c = completableSupplier.get();
                } catch (Throwable e) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(e);
                    return;
                }
                
                if (c == null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(new NullPointerException("The completable returned is null"));
                    return;
                }
                
                c.subscribe(s);
            }
        });
    }

    /**
     * Creates a Completable which calls the given error supplier for each subscriber
     * and emits its returned Throwable.
     * <p>
     * If the errorSupplier returns null, the child CompletableSubscribers will receive a
     * NullPointerException.
     * @param errorSupplier the error supplier, not null
     * @return the new Completable instance
     * @throws NullPointerException if errorSupplier is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable error(final Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                Throwable error;
                
                try {
                    error = errorSupplier.get();
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
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable error(final Throwable error) {
        Objects.requireNonNull(error, "error is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onError(error);
            }
        });
    }
    
    /**
     * Returns a Completable which when subscribed, executes the callable function, ignores its
     * normal result and emits onError or onCompleted only.
     * @param callable the callable instance to execute for each subscriber
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable fromCallable(final Callable<?> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                BooleanDisposable bs = new BooleanDisposable();
                s.onSubscribe(bs);
                try {
                    callable.call();
                } catch (Throwable e) {
                    if (!bs.isDisposed()) {
                        s.onError(e);
                    }
                    return;
                }
                if (!bs.isDisposed()) {
                    s.onComplete();
                }
            }
        });
    }
    
    /**
     * Returns a Completable instance that subscribes to the given flowable, ignores all values and
     * emits only the terminal event.
     * 
     * @param <T> the type of the flowable
     * @param flowable the Flowable instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if flowable is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Completable fromFlowable(final Flowable<T> flowable) {
        Objects.requireNonNull(flowable, "flowable is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber cs) {
                flowable.subscribe(new Subscriber<T>() {

                    @Override
                    public void onComplete() {
                        cs.onComplete();
                    }

                    @Override
                    public void onError(Throwable t) {
                        cs.onError(t);
                    }

                    @Override
                    public void onNext(T t) {
                        // ignored
                    }

                    @Override
                    public void onSubscribe(Subscription s) {
                        cs.onSubscribe(Disposables.from(s));
                        s.request(Long.MAX_VALUE);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable instance that subscribes to the given NbpObservable, ignores all values and
     * emits only the terminal event.
     * @param <T> the type of the NbpObservable
     * @param observable the Observable instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if flowable is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Completable fromNbpObservable(final Observable<T> observable) {
        Objects.requireNonNull(observable, "observable is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                observable.subscribe(new Observer<T>() {

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onNext(T value) {
                        // ignored
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Returns a Completable instance that runs the given Runnable for each subscriber and
     * emits either an unchecked exception or simply completes.
     * @param run the runnable to run for each subscriber
     * @return the new Completable instance
     * @throws NullPointerException if run is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable fromRunnable(final Runnable run) {
        Objects.requireNonNull(run, "run is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                BooleanDisposable bs = new BooleanDisposable();
                s.onSubscribe(bs);
                try {
                    run.run();
                } catch (Throwable e) {
                    if (!bs.isDisposed()) {
                        s.onError(e);
                    }
                    return;
                }
                if (!bs.isDisposed()) {
                    s.onComplete();
                }
            }
        });
    }
    
    /**
     * Returns a Completable instance that when subscribed to, subscribes to the Single instance and
     * emits a completion event if the single emits onSuccess or forwards any onError events.
     * @param <T> the value type of the Single
     * @param single the Single instance to subscribe to, not null
     * @return the new Completable instance
     * @throws NullPointerException if single is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Completable fromSingle(final Single<T> single) {
        Objects.requireNonNull(single, "single is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                single.subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onComplete();
                    }
                    
                });
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
        Objects.requireNonNull(sources, "sources is null");
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
    @SchedulerSupport(SchedulerKind.NONE)
    public static Completable merge(Iterable<? extends Completable> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return create(new CompletableOnSubscribeMergeIterable(sources));
    }
    
    /**
     * Returns a Completable instance that subscribes to all sources at once and
     * completes only when all source Completables complete or one of them emits an error.
     * @param sources the iterable sequence of sources.
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable merge(Flowable<? extends Completable> sources) {
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
    public static Completable merge(Flowable<? extends Completable> sources, int maxConcurrency) {
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
    protected static Completable merge0(Flowable<? extends Completable> sources, int maxConcurrency, boolean delayErrors) {
        Objects.requireNonNull(sources, "sources is null");
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
        Objects.requireNonNull(sources, "sources is null");
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
        Objects.requireNonNull(sources, "sources is null");
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
    public static Completable mergeDelayError(Flowable<? extends Completable> sources) {
        return merge0(sources, Integer.MAX_VALUE, true);
    }
    
    /**
     * Returns a Completable that subscribes to a limited number of inner Completables at once in 
     * the source sequence and delays any error emitted by either the sources 
     * observable or any of the inner Completables until all of
     * them terminate in a way or another.
     * @param sources the sequence of Completables
     * @param maxConcurrency the maximum number of concurrent subscriptions to Completables
     * @return the new Completable instance
     * @throws NullPointerException if sources is null
     */
    public static Completable mergeDelayError(Flowable<? extends Completable> sources, int maxConcurrency) {
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
     * Returns a Completable instance that fires its onComplete event after the given delay ellapsed.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public static Completable timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    /**
     * Returns a Completable instance that fires its onComplete event after the given delay ellapsed
     * by using the supplied scheduler.
     * @param delay the delay time
     * @param unit the delay unit
     * @param scheduler the scheduler where to emit the complete event
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static Completable timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
                s.onSubscribe(mad);
                if (!mad.isDisposed()) {
                    mad.set(scheduler.scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            s.onComplete();
                        }
                    }, delay, unit));
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
     * @param <R> the resource type
     * @param resourceSupplier the supplier that returns a resource to be managed. 
     * @param completableFunction the function that given a resource returns a Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @return the new Completable instance
     */
    public static <R> Completable using(Supplier<R> resourceSupplier, 
            Function<? super R, ? extends Completable> completableFunction, 
            Consumer<? super R> disposer) {
        return using(resourceSupplier, completableFunction, disposer, true);
    }
    
    /**
     * Returns a Completable instance which manages a resource along 
     * with a custom Completable instance while the subscription is active and performs eager or lazy
     * resource disposition.
     * <p>
     * If this overload performs a lazy unsubscription after the terminal event is emitted.
     * Exceptions thrown at this time will be delivered to RxJavaPlugins only.
     * 
     * @param <R> the resource type
     * @param resourceSupplier the supplier that returns a resource to be managed
     * @param completableFunction the function that given a resource returns a non-null
     * Completable instance that will be subscribed to
     * @param disposer the consumer that disposes the resource created by the resource supplier
     * @param eager if true, the resource is disposed before the terminal event is emitted, if false, the
     * resource is disposed after the terminal event has been emitted
     * @return the new Completable instance
     */
    public static <R> Completable using(
            final Supplier<R> resourceSupplier, 
            final Function<? super R, ? extends Completable> completableFunction, 
            final Consumer<? super R> disposer, 
            final boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(completableFunction, "completableFunction is null");
        Objects.requireNonNull(disposer, "disposer is null");
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                final R resource;
                
                try {
                    resource = resourceSupplier.get();
                } catch (Throwable e) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(e);
                    return;
                }
                
                Completable cs;
                
                try {
                    cs = completableFunction.apply(resource);
                } catch (Throwable e) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(e);
                    return;
                }
                
                if (cs == null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(new NullPointerException("The completable supplied is null"));
                    return;
                }
                
                final AtomicBoolean once = new AtomicBoolean();
                
                cs.subscribe(new CompletableSubscriber() {
                    Disposable d;
                    void disposeThis() {
                        d.dispose();
                        if (once.compareAndSet(false, true)) {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable ex) {
                                RxJavaPlugins.onError(ex);
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (eager) {
                            if (once.compareAndSet(false, true)) {
                                try {
                                    disposer.accept(resource);
                                } catch (Throwable ex) {
                                    s.onError(ex);
                                    return;
                                }
                            }
                        }
                        
                        s.onComplete();
                        
                        if (!eager) {
                            disposeThis();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (eager) {
                            if (once.compareAndSet(false, true)) {
                                try {
                                    disposer.accept(resource);
                                } catch (Throwable ex) {
                                    e = new CompositeException(ex, e);
                                }
                            }
                        }
                        
                        s.onError(e);
                        
                        if (!eager) {
                            disposeThis();
                        }
                    }
                    
                    @Override
                    public void onSubscribe(Disposable d) {
                        this.d = d;
                        s.onSubscribe(new Disposable() {
                            @Override
                            public void dispose() {
                                disposeThis();
                            }
                        });
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
        this.onSubscribe = onSubscribe;
    }
    
    /**
     * Returns a Completable that emits the a terminated event of either this Completable
     * or the other Completable whichever fires first.
     * @param other the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable ambWith(Completable other) {
        Objects.requireNonNull(other, "other is null");
        return amb(this, other);
    }
    
    /**
     * Subscribes to and awaits the termination of this Completable instance in a blocking manner and
     * rethrows any exception emitted.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final void await() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
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
     * false if the timeout ellapsed before this Completable terminated.
     * @throws RuntimeException wrapping an InterruptedException if the current thread is interrupted
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final boolean await(long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable compose(CompletableTransformer transformer) {
        return to(transformer);
    }
    
    /**
     * Concatenates this Completable with another Completable.
     * @param other the other Completable, not null
     * @return the new Completable which subscribes to this and then the other Completable
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable concatWith(Completable other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    /**
     * Returns a Completable which delays the emission of the completion event by the given time.
     * @param delay the delay time
     * @param unit the delay unit
     * @return the new Completable instance
     * @throws NullPointerException if unit is null
     */
    @SchedulerSupport(SchedulerKind.COMPUTATION)
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable delay(final long delay, final TimeUnit unit, final Scheduler scheduler, final boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                final CompositeDisposable set = new CompositeDisposable();
                
                subscribe(new CompletableSubscriber() {

                    
                    @Override
                    public void onComplete() {
                        set.add(scheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                s.onComplete();
                            }
                        }, delay, unit));
                    }

                    @Override
                    public void onError(final Throwable e) {
                        if (delayError) {
                            set.add(scheduler.scheduleDirect(new Runnable() {
                                @Override
                                public void run() {
                                    s.onError(e);
                                }
                            }, delay, unit));
                        } else {
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
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
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable doOnComplete(Runnable onComplete) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete, Functions.emptyRunnable(), Functions.emptyRunnable());
    }
    
    /**
     * Returns a Completable which calls the giveon onDispose callback if the child subscriber cancels
     * the subscription.
     * @param onDispose the callback to call when the child subscriber cancels the subscription
     * @return the new Completable instance
     * @throws NullPointerException if onDispose is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable doOnDispose(Runnable onDispose) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable(), onDispose);
    }
    
    /**
     * Returns a Completable which calls the given onError callback if this Completable emits an error.
     * @param onError the error callback
     * @return the new Completable instance
     * @throws NullPointerException if onError is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable doOnError(Consumer<? super Throwable> onError) {
        return doOnLifecycle(Functions.emptyConsumer(), onError, Functions.emptyRunnable(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    /**
     * Returns a Completable instance that calls the various callbacks on the specific
     * lifecycle events.
     * @param onSubscribe the consumer called when a CompletableSubscriber subscribes.
     * @param onError the consumer called when this emits an onError event
     * @param onComplete the runnable called just before when this Completable completes normally
     * @param onAfterComplete the runnable called after this Completable completes normally
     * @param onDisposed the runnable called when the child cancels the subscription
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    protected final Completable doOnLifecycle(
            final Consumer<? super Disposable> onSubscribe, 
            final Consumer<? super Throwable> onError, 
            final Runnable onComplete, 
            final Runnable onAfterComplete,
            final Runnable onDisposed) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onAfterComplete, "onAfterComplete is null");
        Objects.requireNonNull(onDisposed, "onDisposed is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        try {
                            onComplete.run();
                        } catch (Throwable e) {
                            s.onError(e);
                            return;
                        }
                        
                        s.onComplete();
                        
                        try {
                            onAfterComplete.run();
                        } catch (Throwable e) {
                            RxJavaPlugins.onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            onError.accept(e);
                        } catch (Throwable ex) {
                            e = new CompositeException(ex, e);
                        }
                        
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(final Disposable d) {
                        
                        try {
                            onSubscribe.accept(d);
                        } catch (Throwable ex) {
                            d.dispose();
                            s.onSubscribe(EmptyDisposable.INSTANCE);
                            s.onError(ex);
                            return;
                        }
                        
                        s.onSubscribe(new Disposable() {
                            @Override
                            public void dispose() {
                                try {
                                    onDisposed.run();
                                } catch (Throwable e) {
                                    RxJavaPlugins.onError(e);
                                }
                                d.dispose();
                            }
                        });
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable doOnSubscribe(Consumer<? super Disposable> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.emptyConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }
    
    /**
     * Returns a Completable instance that calls the given onTerminate callback just before this Completable
     * completes normally or with an exception
     * @param onTerminate the callback to call just before this Completable terminates
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable doOnTerminate(final Runnable onTerminate) {
        return doOnLifecycle(Functions.emptyConsumer(), new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                onTerminate.run();
            }
        }, onTerminate, Functions.emptyRunnable(), Functions.emptyRunnable());
    }
    
    /**
     * Returns a completable that first runs this Completable
     * and then the other completable.
     * <p>
     * This is an alias for {@link #concatWith(Completable)}.
     * @param other the other Completable, not null
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable endWith(Completable other) {
        return concatWith(other);
    }
    /**
     * Returns an NbpObservable that first runs this Completable instance and
     * resumes with the given next Observable.
     * @param <T> the type of the NbpObservable
     * @param next the next Observable to continue
     * @return the new Observable instance
     * @throws NullPointerException if next is null
     */
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <T> Observable<T> endWith(Observable<T> next) {
        return next.startWith(this.<T>toNbpObservable());
    }
    
    /**
     * Returns an Observable that first runs this Completable instance and
     * resumes with the given next Observable.
     * @param <T> the value type of the observable
     * @param next the next Observable to continue
     * @return the new Observable instance
     * @throws NullPointerException if next is null
     */
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <T> Flowable<T> endWith(Flowable<T> next) {
        return next.startWith(this.<T>toFlowable());
    }

    /**
     * Returns a Completable instace that calls the given onAfterComplete callback after this
     * Completable completes normally.
     * @param onAfterComplete the callback to call after this Completable emits an onComplete event.
     * @return the new Completable instance
     * @throws NullPointerException if onAfterComplete is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable finallyDo(Runnable onAfterComplete) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.emptyRunnable(), onAfterComplete, Functions.emptyRunnable());
    }
    
    /**
     * Subscribes to this Completable instance and blocks until it terminates, then returns null or
     * the emitted exception if any.
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Throwable get() {
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
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
     * ellapses, then returns null for normal termination or the emitted exception if any.
     * @param timeout the timeout value
     * @param unit the time unit
     * @return the throwable if this terminated with an error, null otherwise
     * @throws RuntimeException that wraps an InterruptedException if the wait is interrupted or
     * TimeoutException if the specified timeout ellapsed before it
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Throwable get(long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit is null");
        
        final CountDownLatch cdl = new CountDownLatch(1);
        final Throwable[] err = new Throwable[1];
        
        subscribe(new CompletableSubscriber() {

            @Override
            public void onComplete() {
                cdl.countDown();
            }

            @Override
            public void onError(Throwable e) {
                err[0] = e;
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable lift(final CompletableOperator onLift) {
        Objects.requireNonNull(onLift, "onLift is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(CompletableSubscriber s) {
                try {
                    // TODO plugin wrapping

                    CompletableSubscriber sw = onLift.apply(s);
                    
                    subscribe(sw);
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable mergeWith(Completable other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }
    
    /**
     * Returns a Completable which emits the terminal events from the thread of the specified scheduler.
     * @param scheduler the scheduler to emit terminal events on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable observeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                
                final ArrayCompositeResource<Disposable> ad = new ArrayCompositeResource<Disposable>(2, Disposables.consumeAndDispose());
                final Scheduler.Worker w = scheduler.createWorker();
                ad.set(0, w);
                
                s.onSubscribe(ad);
                
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        w.schedule(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    s.onComplete();
                                } finally {
                                    ad.dispose();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final Throwable e) {
                        w.schedule(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    s.onError(e);
                                } finally {
                                    ad.dispose();
                                }
                            }
                        });
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        ad.set(1, d);
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable onErrorComplete() {
        return onErrorComplete(Functions.alwaysTrue());
    }

    /**
     * Returns a Completable instance that if this Completable emits an error and the predicate returns
     * true, it will emit an onComplete and swallow the throwable.
     * @param predicate the predicate to call when an Throwable is emitted which should return true
     * if the Throwable should be swallowed and replaced with an onComplete.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable onErrorComplete(final Predicate<? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        boolean b;
                        
                        try {
                            b = predicate.test(e);
                        } catch (Throwable ex) {
                            s.onError(new CompositeException(ex, e));
                            return;
                        }
                        
                        if (b) {
                            s.onComplete();
                        } else {
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable onErrorResumeNext(final Function<? super Throwable, ? extends Completable> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                final SerialDisposable sd = new SerialDisposable();
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Completable c;
                        
                        try {
                            c = errorMapper.apply(e);
                        } catch (Throwable ex) {
                            s.onError(new CompositeException(ex, e));
                            return;
                        }
                        
                        if (c == null) {
                            NullPointerException npe = new NullPointerException("The completable returned is null");
                            npe.initCause(e);
                            s.onError(npe);
                            return;
                        }
                        
                        c.subscribe(new CompletableSubscriber() {

                            @Override
                            public void onComplete() {
                                s.onComplete();
                            }

                            @Override
                            public void onError(Throwable e) {
                                s.onError(e);
                            }

                            @Override
                            public void onSubscribe(Disposable d) {
                                sd.set(d);
                            }
                            
                        });
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable repeat() {
        return fromFlowable(toFlowable().repeat());
    }
    
    /**
     * Returns a Completable that subscribes repeatedly at most the given times to this Completable.
     * @param times the number of times the resubscription should happen
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is less than zero
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable repeat(long times) {
        return fromFlowable(toFlowable().repeat(times));
    }
    
    /**
     * Returns a Completable that repeatedly subscribes to this Completable so long as the given
     * stop supplier returns false.
     * @param stop the supplier that should return true to stop resubscribing.
     * @return the new Completable instance
     * @throws NullPointerException if stop is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable repeatUntil(BooleanSupplier stop) {
        return fromFlowable(toFlowable().repeatUntil(stop));
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
    @SchedulerSupport(SchedulerKind.NONE)
    /*
     * FIXME the Observable<Void> type doesn't make sense here because nulls are not allowed
     * FIXME add unit test once the type has been fixed
     */ 
    public final Completable repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<Object>> handler) {
        return fromFlowable(toFlowable().repeatWhen(handler));
    }
    
    /**
     * Returns a Completable that retries this Completable as long as it emits an onError event.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable retry() {
        return fromFlowable(toFlowable().retry());
    }
    
    /**
     * Returns a Completable that retries this Completable in case of an error as long as the predicate
     * returns true.
     * @param predicate the predicate called when this emits an error with the repeat count and the latest exception
     * and should return true to retry.
     * @return the new Completable instance
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return fromFlowable(toFlowable().retry(predicate));
    }

    /**
     * Returns a Completable that when this Completable emits an error, retries at most the given
     * number of times before giving up and emitting the last error.
     * @param times the number of times the returned Completable should retry this Completable
     * @return the new Completable instance
     * @throws IllegalArgumentException if times is negative
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable retry(long times) {
        return fromFlowable(toFlowable().retry(times));
    }

    /**
     * Returns a Completable that when this Completable emits an error, calls the given predicate with
     * the latest exception to decide whether to resubscribe to this or not.
     * @param predicate the predicate that is called with the latest throwable and should return
     * true to indicate the returned Completable should resubscribe to this Completable.
     * @return the new Completable instance
     * @throws NullPointerException if predicate is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable retry(Predicate<? super Throwable> predicate) {
        return fromFlowable(toFlowable().retry(predicate));
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
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable retryWhen(Function<? super Flowable<? extends Throwable>, ? extends Publisher<Object>> handler) {
        return fromFlowable(toFlowable().retryWhen(handler));
    }

    /**
     * Returns a Completable which first runs the other Completable
     * then this completable if the other completed normally.
     * @param other the other completable to run first
     * @return the new Completable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Completable startWith(Completable other) {
        Objects.requireNonNull(other, "other is null");
        return concat(other, this);
    }

    /**
     * Returns an NbpObservable which first delivers the events
     * of the other NbpObservable then runs this Completable.
     * @param <T> the value type
     * @param other the other NbpObservable to run first
     * @return the new NbpObservable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Observable<T> startWith(Observable<T> other) {
        Objects.requireNonNull(other, "other is null");
        return other.endWith(this.<T>toNbpObservable());
    }
    /**
     * Returns an Observable which first delivers the events
     * of the other Observable then runs this Completable.
     * @param <T> the value type
     * @param other the other Observable to run first
     * @return the new Observable instance
     * @throws NullPointerException if other is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Flowable<T> startWith(Flowable<T> other) {
        Objects.requireNonNull(other, "other is null");
        return other.endWith(this.<T>toFlowable());
    }
    
    /**
     * Subscribes to this Completable and returns a Disposable which can be used to cancel
     * the subscription.
     * @return the Disposable that allows cancelling the subscription
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe() {
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        subscribe(new CompletableSubscriber() {
            @Override
            public void onComplete() {
                // nothing to do
            }
            
            @Override
            public void onError(Throwable e) {
                RxJavaPlugins.onError(e);
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }
        });
        
        return mad;
    }
    /**
     * Subscribes the given CompletableSubscriber to this Completable instance.
     * @param s the CompletableSubscriber, not null
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final void subscribe(CompletableSubscriber s) {
        Objects.requireNonNull(s, "s is null");
        try {
            // TODO plugin wrapping the subscriber
            
            onSubscribe.accept(s);
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }

    /**
     * Subscribes to this Completable and calls back either the onError or onComplete functions.
     * 
     * @param onError the consumer that is called if this Completable emits an error
     * @param onComplete the runnable that is called if the Completable completes normally
     * @return the Disposable that can be used for cancelling the subscription asynchronously
     * @throws NullPointerException if either callback is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(final Consumer<? super Throwable> onError, final Runnable onComplete) {
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        subscribe(new CompletableSubscriber() {
            @Override
            public void onComplete() {
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    onError(e);
                }
            }
            
            @Override
            public void onError(Throwable e) {
                try {
                    onError.accept(e);
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    RxJavaPlugins.onError(e);
                }
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }
        });
        
        return mad;
    }
    
    /**
     * Subscribes a non-backpressure NbpSubscriberto this Completable instance which
     * will receive only an onError or onComplete event.
     * @param s the NbpSubscriber instance, not null
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final void subscribe(final Observer<?> s) {
        Objects.requireNonNull(s, "s is null");
        try {
            // TODO plugin wrapping the subscriber
            
            subscribe(new CompletableSubscriber() {
                @Override
                public void onComplete() {
                    s.onComplete();
                }
                
                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }
                
                @Override
                public void onSubscribe(Disposable d) {
                    s.onSubscribe(d);
                }
            });
            
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }
    
    /**
     * Subscribes to this Completable and calls the given Runnable when this Completable
     * completes normally.
     * <p>
     * If this Completable emits an error, it is sent to RxJavaPlugins.onError and gets swallowed.
     * @param onComplete the runnable called when this Completable completes normally
     * @return the Disposable that allows cancelling the subscription
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(final Runnable onComplete) {
        Objects.requireNonNull(onComplete, "onComplete is null");
        
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        subscribe(new CompletableSubscriber() {
            @Override
            public void onComplete() {
                try {
                    onComplete.run();
                } catch (Throwable e) {
                    RxJavaPlugins.onError(e);
                }
            }
            
            @Override
            public void onError(Throwable e) {
                RxJavaPlugins.onError(e);
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }
        });
        
        return mad;
    }

    /**
     * Subscribes a reactive-streams Subscriber to this Completable instance which
     * will receive only an onError or onComplete event.
     * @param <T> the value type of the subscriber
     * @param s the reactive-streams Subscriber, not null
     * @throws NullPointerException if s is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> void subscribe(Subscriber<T> s) {
        Objects.requireNonNull(s, "s is null");
        try {
            final Subscriber<?> sw = RxJavaPlugins.onSubscribe(s);
            
            if (sw == null) {
                throw new NullPointerException("The RxJavaPlugins.onSubscribe returned a null Subscriber");
            }
            
            subscribe(new CompletableSubscriber() {
                @Override
                public void onComplete() {
                    sw.onComplete();
                }
                
                @Override
                public void onError(Throwable e) {
                    sw.onError(e);
                }
                
                @Override
                public void onSubscribe(Disposable d) {
                    DisposableSubscription ds = new DisposableSubscription(d);
                    sw.onSubscribe(ds);
                }
            });
            
        } catch (NullPointerException ex) {
            throw ex;
        } catch (Throwable ex) {
            RxJavaPlugins.onError(ex);
            throw toNpe(ex);
        }
    }

    /**
     * Returns a Completable which subscribes the child subscriber on the specified scheduler, making
     * sure the subscription side-effects happen on that specific thread of the scheduler.
     * @param scheduler the Scheduler to subscribe on
     * @return the new Completable instance
     * @throws NullPointerException if scheduler is null
     */
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable subscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                // FIXME cancellation of this schedule
                scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        subscribe(s);
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
    @SchedulerSupport(SchedulerKind.COMPUTATION)
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
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Completable timeout(long timeout, TimeUnit unit, Completable other) {
        Objects.requireNonNull(other, "other is null");
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable timeout(long timeout, TimeUnit unit, Scheduler scheduler, Completable other) {
        Objects.requireNonNull(other, "other is null");
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable timeout0(long timeout, TimeUnit unit, Scheduler scheduler, Completable other) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new CompletableOnSubscribeTimeout(this, timeout, unit, scheduler, other));
    }
    
    /**
     * Allows fluent conversion to another type via a function callback.
     * @param <U> the output type
     * @param converter the function called with this which should return some other value.
     * @return the converted value
     * @throws NullPointerException if converter is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> U to(Function<? super Completable, U> converter) {
        return converter.apply(this);
    }

    /**
     * Returns an Observable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * @param <T> the value type
     * @return the new Observable created
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Flowable<T> toFlowable() {
        return Flowable.create(new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                Completable.this.subscribe(s);
            }
        });
    }
    
    /**
     * Returns an NbpObservable which when subscribed to subscribes to this Completable and
     * relays the terminal events to the subscriber.
     * @param <T> the value type
     * @return the new NbpObservable created
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Observable<T> toNbpObservable() {
        return Observable.create(new NbpOnSubscribe<T>() {
            @Override
            public void accept(Observer<? super T> s) {
                subscribe(s);
            }
        });
    }
    
    /**
     * Convers this Completable into a Single which when this Completable completes normally,
     * calls the given supplier and emits its returned value through onSuccess.
     * @param <T> the value type
     * @param completionValueSupplier the value supplier called when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValueSupplier is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Single<T> toSingle(final Supplier<? extends T> completionValueSupplier) {
        Objects.requireNonNull(completionValueSupplier, "completionValueSupplier is null");
        return Single.create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        T v;

                        try {
                            v = completionValueSupplier.get();
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
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }
                    
                });
            }
        });
    }
    
    /**
     * Convers this Completable into a Single which when this Completable completes normally,
     * emits the given value through onSuccess.
     * @param <T> the value type
     * @param completionValue the value to emit when this Completable completes normally
     * @return the new Single instance
     * @throws NullPointerException if completionValue is null
     */
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T> Single<T> toSingleDefault(final T completionValue) {
        Objects.requireNonNull(completionValue, "completionValue is null");
        return toSingle(new Supplier<T>() {
            @Override
            public T get() {
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
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Completable unsubscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new CompletableOnSubscribe() {
            @Override
            public void accept(final CompletableSubscriber s) {
                subscribe(new CompletableSubscriber() {

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(final Disposable d) {
                        s.onSubscribe(new Disposable() {
                            @Override
                            public void dispose() {
                                scheduler.scheduleDirect(new Runnable() {
                                    @Override
                                    public void run() {
                                        d.dispose();
                                    }
                                });
                            }
                        });
                    }
                    
                });
            }
        });
    }
}