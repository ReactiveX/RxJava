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
package rx.observables;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.operators.OperationForEachFuture;
import rx.operators.OperationMostRecent;
import rx.operators.OperationNext;
import rx.operators.OperationToFuture;
import rx.operators.OperationToIterator;
import rx.operators.SafeObservableSubscription;
import rx.operators.SafeObserver;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Actions;
import rx.util.functions.Func1;

/**
 * An extension of {@link Observable} that provides blocking operators.
 * <p>
 * You construct a BlockingObservable from an Observable with {@link #from(Observable)} or {@link Observable#toBlockingObservable()} <p>
 * The documentation for this interface makes use of a form of marble diagram that has been
 * modified to illustrate blocking operators. The following legend explains these marble diagrams:
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.legend.png">
 * <p>
 * For more information see the
 * <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators">Blocking
 * Observable Operators</a> page at the RxJava Wiki.
 * 
 * @param <T>
 */
public class BlockingObservable<T> {

    private final Observable<? extends T> o;

    private BlockingObservable(Observable<? extends T> o) {
        this.o = o;
    }

    /**
     * Convert an Observable into a BlockingObservable.
     */
    public static <T> BlockingObservable<T> from(final Observable<? extends T> o) {
        return new BlockingObservable<T>(o);
    }

    /**
     * Used for protecting against errors being thrown from {@link Observer} implementations and
     * ensuring onNext/onError/onCompleted contract compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect
     * calls to user code from within an operator"
     */
    private Subscription protectivelyWrapAndSubscribe(Observer<? super T> observer) {
        SafeObservableSubscription subscription = new SafeObservableSubscription();
        return subscription.wrap(o.subscribe(new SafeObserver<T>(subscription, observer)));
    }

    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless
     * the run() method is called or the task is scheduled on an Executor.
     * @param onNext the action to call with each emitted element
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.util.functions.Action1, rx.Scheduler) 
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext) {
        return OperationForEachFuture.forEachFuture(o, onNext);
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless
     * the run() method is called or the task is scheduled on an Executor.
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.util.functions.Action1, rx.util.functions.Action1, rx.Scheduler) 
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext,
            Action1<? super Throwable> onError) {
        return OperationForEachFuture.forEachFuture(o, onNext, onError);
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future.
     * <p>
     * <em>Important note:</em> The returned task blocks indefinitely unless
     * the run() method is called or the task is scheduled on an Executor.
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onCompleted the action to call when the source completes
     * @return the Future representing the entire for-each operation
     * @see #forEachFuture(rx.util.functions.Action1, rx.util.functions.Action1, rx.util.functions.Action0, rx.Scheduler) 
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext,
            Action1<? super Throwable> onError,
            Action0 onCompleted) {
        return OperationForEachFuture.forEachFuture(o, onNext, onError, onCompleted);
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future, scheduled on the given scheduler.
     * @param onNext the action to call with each emitted element
     * @param scheduler the scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext, 
            Scheduler scheduler) {
        FutureTask<Void> task = OperationForEachFuture.forEachFuture(o, onNext);
        scheduler.schedule(Actions.fromRunnable(task));
        return task;
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future, scheduled on the given scheduler.
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param scheduler the scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext,
            Action1<? super Throwable> onError, 
            Scheduler scheduler) {
        FutureTask<Void> task = OperationForEachFuture.forEachFuture(o, onNext, onError);
        scheduler.schedule(Actions.fromRunnable(task));
        return task;
    }
    
    /**
     * Subscribes to the given source and calls the callback for each emitted item,
     * and surfaces the completion or error through a Future, scheduled on the given scheduler.
     * @param onNext the action to call with each emitted element
     * @param onError the action to call when an exception is emitted
     * @param onCompleted the action to call when the source completes
     * @param scheduler the scheduler where the task will await the termination of the for-each
     * @return the Future representing the entire for-each operation
     */
    public FutureTask<Void> forEachFuture(
            Action1<? super T> onNext,
            Action1<? super Throwable> onError,
            Action0 onCompleted,
            Scheduler scheduler) {
        FutureTask<Void> task = OperationForEachFuture.forEachFuture(o, onNext, onError, onCompleted);
        scheduler.schedule(Actions.fromRunnable(task));
        return task;
    }
    
    /**
     * Invoke a method on each item emitted by the {@link Observable}; block until the Observable
     * completes.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link Observable#subscribe(Observer)}, but it blocks. Because it blocks it does
     * not need the {@link Observer#onCompleted()} or {@link Observer#onError(Throwable)} methods.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
     * 
     * @param onNext
     *            the {@link Action1} to invoke for every item emitted by the {@link Observable}
     * @throws RuntimeException
     *             if an error occurs
     */
    public void forEach(final Action1<? super T> onNext) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exceptionFromOnError = new AtomicReference<Throwable>();

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
            public void onError(Throwable e) {
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
     * Returns an {@link Iterator} that iterates over all items emitted by a specified {@link Observable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.getIterator.png">
     * 
     * @return an {@link Iterator} that can iterate over the items emitted by the {@link Observable}
     */
    public Iterator<T> getIterator() {
        return OperationToIterator.toIterator(o);
    }

    /**
     * Returns the first item emitted by a specified {@link Observable},
     * or IllegalArgumentException if source contains no elements
     * 
     * @return the first item emitted by the source {@link Observable}
     * @throws IllegalArgumentException
     *            if source contains no elements
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229177(v=vs.103).aspx">MSDN: Observable.First</a>
     */
    public T first() {
        return from(o.first()).single();
    }

    /**
     * Returns the first item emitted by a specified {@link Observable} that matches a predicate,
     * or IllegalArgumentException if no such items are emitted.
     * 
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the first item emitted by the {@link Observable} that matches the predicate
     * @throws IllegalArgumentException
     *            if no such items are emitted.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229739(v=vs.103).aspx">MSDN: Observable.First</a>
     */
    public T first(Func1<? super T, Boolean> predicate) {
        return from(o.first(predicate)).single();
    }

    /**
     * Returns the first item emitted by a specified {@link Observable}, or a default value if no
     * items are emitted.
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no items
     * @return the first item emitted by the {@link Observable}, or the default value if no items
     *         are emitted
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229320(v=vs.103).aspx">MSDN: Observable.FirstOrDefault</a>
     */
    public T firstOrDefault(T defaultValue) {
        return from(o.take(1)).singleOrDefault(defaultValue);
    }

    /**
     * Returns the first item emitted by a specified {@link Observable} that matches a predicate, or
     * a default value if no such items are emitted.
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the first item emitted by the {@link Observable} that matches the predicate, or the
     *         default value if no matching items are emitted
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229759(v=vs.103).aspx">MSDN: Observable.FirstOrDefault</a>
     */
    public T firstOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).firstOrDefault(defaultValue);
    }

    /**
     * Returns the last item emitted by a specified {@link Observable}, or throws IllegalArgumentException
     * if source contains no elements.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.png">
     * 
     * @return the last item emitted by the source {@link Observable}
     * @throws IllegalArgumentException if source contains no elements
     */
    public T last() {
        return from(o.last()).single();
    }

    /**
     * Returns the last item emitted by a specified {@link Observable} that matches a predicate,
     * or throws IllegalArgumentException if no such items are emitted.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     * 
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the last item emitted by the {@link Observable} that matches the predicate
     * @throws IllegalArgumentException
     *            if no such items are emitted.
     */
    public T last(final Func1<? super T, Boolean> predicate) {
        return from(o.last(predicate)).single();
    }

    /**
     * Returns the last item emitted by a specified {@link Observable}, or a default value if no
     * items are emitted.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.png">
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no items
     * @return the last item emitted by the {@link Observable}, or the default value if no items
     *         are emitted
     */
    public T lastOrDefault(T defaultValue) {
        return from(o.takeLast(1)).singleOrDefault(defaultValue);
    }

    /**
     * Returns the last item emitted by a specified {@link Observable} that matches a predicate, or
     * a default value if no such items are emitted.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the last item emitted by the {@link Observable} that matches the predicate, or the
     *         default value if no matching items are emitted
     */
    public T lastOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).lastOrDefault(defaultValue);
    }

    /**
     * Returns an {@link Iterable} that always returns the item most recently emitted by an {@link Observable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
     * 
     * @param initialValue
     *            the initial value that will be yielded by the {@link Iterable} sequence if the {@link Observable} has not yet emitted an item
     * @return an {@link Iterable} that on each iteration returns the item that the {@link Observable} has most recently emitted
     */
    public Iterable<T> mostRecent(T initialValue) {
        return OperationMostRecent.mostRecent(o, initialValue);
    }

    /**
     * Returns an {@link Iterable} that blocks until the {@link Observable} emits another item,
     * then returns that item.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
     * 
     * @return an {@link Iterable} that blocks upon each iteration until the {@link Observable} emits a new item, whereupon the Iterable returns that item
     */
    public Iterable<T> next() {
        return OperationNext.next(o);
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return that item,
     * otherwise throw an IllegalArgumentException.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
     * 
     * @return the single item emitted by the {@link Observable}
     */
    public T single() {
        return from(o.single()).toIterable().iterator().next();
    }

    /**
     * If the {@link Observable} completes after emitting a single item that matches a given
     * predicate, return that item, otherwise throw an IllegalArgumentException.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     * 
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the single item emitted by the source {@link Observable} that matches the predicate
     */
    public T single(Func1<? super T, Boolean> predicate) {
        return from(o.single(predicate)).toIterable().iterator().next();
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return that item; if it
     * emits more than one item, throw an IllegalArgumentException; if it emits no items, return a default value.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.png">
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no items
     * @return the single item emitted by the {@link Observable}, or the default value if no items
     *         are emitted
     */
    public T singleOrDefault(T defaultValue) {
        Iterator<? extends T> it = this.toIterable().iterator();

        if (!it.hasNext()) {
            return defaultValue;
        }

        T result = it.next();
        if (it.hasNext()) {
            throw new IllegalArgumentException("Sequence contains too many elements");
        }
        return result;
    }

    /**
     * If the {@link Observable} completes after emitting a single item that matches a predicate,
     * return that item; if it emits more than one such item, throw an IllegalArgumentException; if it emits no
     * items, return a default value.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     * 
     * @param defaultValue
     *            a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@link Observable}
     * @return the single item emitted by the {@link Observable} that matches the predicate, or the
     *         default value if no such items are emitted
     */
    public T singleOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).singleOrDefault(defaultValue);
    }

    /**
     * Returns a {@link Future} representing the single value emitted by an {@link Observable}.
     * <p>
     * <code>toFuture()</code> throws an exception if the Observable emits more than one item. If
     * the Observable may emit more than item, use {@link Observable#toList toList()}.toFuture()</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toFuture.png">
     * 
     * @return a {@link Future} that expects a single item to be emitted by the source {@link Observable}
     */
    public Future<T> toFuture() {
        return OperationToFuture.toFuture(o);
    }

    /**
     * Converts an {@link Observable} into an {@link Iterable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterable.png">
     * 
     * @return an {@link Iterable} version of the underlying {@link Observable}
     */
    public Iterable<T> toIterable() {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return getIterator();
            }
        };
    }
}
