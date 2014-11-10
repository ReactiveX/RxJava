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
package rx.observables;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.internal.operators.BlockingOperatorLatest;
import rx.internal.operators.BlockingOperatorMostRecent;
import rx.internal.operators.BlockingOperatorNext;
import rx.internal.operators.BlockingOperatorToFuture;
import rx.internal.operators.BlockingOperatorToIterator;
import rx.internal.util.UtilityFunctions;

/**
 * {@code BlockingObservable} is a variety of {@link Observable} that provides blocking operators. It can be
 * useful for testing and demo purposes, but is generally inappropriate for production applications (if you
 * think you need to use a {@code BlockingObservable} this is usually a sign that you should rethink your
 * design).
 * <p>
 * You construct a {@code BlockingObservable} from an {@code Observable} with {@link #from(Observable)} or
 * {@link Observable#toBlocking()}.
 * <p>
 * The documentation for this interface makes use of a form of marble diagram that has been modified to
 * illustrate blocking operators. The following legend explains these marble diagrams:
 * <p>
 * <img width="640" height="301" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.legend.png" alt="">
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators">RxJava wiki: Blocking
 *      Observable Operators</a>
 * @param <T>
 *           the type of item emitted by the {@code BlockingObservable}
 */
public final class BlockingObservable<T> {

    private final Observable<? extends T> o;

    private BlockingObservable(Observable<? extends T> o) {
        this.o = o;
    }

    /**
     * Converts an {@link Observable} into a {@code BlockingObservable}.
     *
     * @param o
     *          the {@link Observable} you want to convert
     * @return a {@code BlockingObservable} version of {@code o}
     */
    public static <T> BlockingObservable<T> from(final Observable<? extends T> o) {
        return new BlockingObservable<T>(o);
    }

    /**
     * Invokes a method on each item emitted by this {@code BlockingObservable} and blocks until the Observable
     * completes.
     * <p>
     * <em>Note:</em> This will block even if the underlying Observable is asynchronous.
     * <p>
     * <img width="640" height="330" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.forEach.png" alt="">
     * <p>
     * This is similar to {@link Observable#subscribe(Subscriber)}, but it blocks. Because it blocks it does not
     * need the {@link Subscriber#onCompleted()} or {@link Subscriber#onError(Throwable)} methods. If the
     * underlying Observable terminates with an error, rather than calling {@code onError}, this method will
     * throw an exception.
     *
     * @param onNext
     *            the {@link Action1} to invoke for each item emitted by the {@code BlockingObservable}
     * @throws RuntimeException
     *             if an error occurs
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#foreach">RxJava wiki: forEach()</a>
     */
    public void forEach(final Action1<? super T> onNext) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exceptionFromOnError = new AtomicReference<Throwable>();

        /*
         * Use 'subscribe' instead of 'unsafeSubscribe' for Rx contract behavior
         * as this is the final subscribe in the chain.
         */
        o.subscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                /*
                 * If we receive an onError event we set the reference on the
                 * outer thread so we can git it and throw after the
                 * latch.await().
                 * 
                 * We do this instead of throwing directly since this may be on
                 * a different thread and the latch is still waiting.
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
            // for more information see https://github.com/ReactiveX/RxJava/pull/147#issuecomment-13624780
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
     * Returns an {@link Iterator} that iterates over all items emitted by this {@code BlockingObservable}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.getIterator.png" alt="">
     *
     * @return an {@link Iterator} that can iterate over the items emitted by this {@code BlockingObservable}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava wiki: getIterator()</a>
     */
    public Iterator<T> getIterator() {
        return BlockingOperatorToIterator.toIterator(o);
    }

    /**
     * Returns the first item emitted by this {@code BlockingObservable}, or throws
     * {@code NoSuchElementException} if it emits no items.
     *
     * @return the first item emitted by this {@code BlockingObservable}
     * @throws NoSuchElementException
     *             if this {@code BlockingObservable} emits no items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava wiki: first()</a>
     */
    public T first() {
        return blockForSingle(o.first());
    }

    /**
     * Returns the first item emitted by this {@code BlockingObservable} that matches a predicate, or throws
     * {@code NoSuchElementException} if it emits no such item.
     *
     * @param predicate
     *            a predicate function to evaluate items emitted by this {@code BlockingObservable}
     * @return the first item emitted by this {@code BlockingObservable} that matches the predicate
     * @throws NoSuchElementException
     *             if this {@code BlockingObservable} emits no such items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava wiki: first()</a>
     */
    public T first(Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.first(predicate));
    }

    /**
     * Returns the first item emitted by this {@code BlockingObservable}, or a default value if it emits no
     * items.
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no items
     * @return the first item emitted by this {@code BlockingObservable}, or the default value if it emits no
     *         items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava wiki: firstOrDefault()</a>
     */
    public T firstOrDefault(T defaultValue) {
        return blockForSingle(o.map(UtilityFunctions.<T>identity()).firstOrDefault(defaultValue));
    }

    /**
     * Returns the first item emitted by this {@code BlockingObservable} that matches a predicate, or a default
     * value if it emits no such items.
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by this {@code BlockingObservable}
     * @return the first item emitted by this {@code BlockingObservable} that matches the predicate, or the
     *         default value if this {@code BlockingObservable} emits no matching items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava wiki: firstOrDefault()</a>
     */
    public T firstOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.filter(predicate).map(UtilityFunctions.<T>identity()).firstOrDefault(defaultValue));
    }

    /**
     * Returns the last item emitted by this {@code BlockingObservable}, or throws
     * {@code NoSuchElementException} if this {@code BlockingObservable} emits no items.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.last.png" alt="">
     *
     * @return the last item emitted by this {@code BlockingObservable}
     * @throws NoSuchElementException
     *             if this {@code BlockingObservable} emits no items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava wiki: last()</a>
     */
    public T last() {
        return blockForSingle(o.last());
    }

    /**
     * Returns the last item emitted by this {@code BlockingObservable} that matches a predicate, or throws
     * {@code NoSuchElementException} if it emits no such items.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.last.p.png" alt="">
     *
     * @param predicate
     *            a predicate function to evaluate items emitted by the {@code BlockingObservable}
     * @return the last item emitted by the {@code BlockingObservable} that matches the predicate
     * @throws NoSuchElementException
     *             if this {@code BlockingObservable} emits no items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava wiki: last()</a>
     */
    public T last(final Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.last(predicate));
    }

    /**
     * Returns the last item emitted by this {@code BlockingObservable}, or a default value if it emits no
     * items.
     * <p>
     * <img width="640" height="310" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.lastOrDefault.png" alt="">
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no items
     * @return the last item emitted by the {@code BlockingObservable}, or the default value if it emits no
     *         items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava wiki: lastOrDefault()</a>
     */
    public T lastOrDefault(T defaultValue) {
        return blockForSingle(o.map(UtilityFunctions.<T>identity()).lastOrDefault(defaultValue));
    }

    /**
     * Returns the last item emitted by this {@code BlockingObservable} that matches a predicate, or a default
     * value if it emits no such items.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png" alt="">
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by this {@code BlockingObservable}
     * @return the last item emitted by this {@code BlockingObservable} that matches the predicate, or the
     *         default value if it emits no matching items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava wiki: lastOrDefault()</a>
     */
    public T lastOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.filter(predicate).map(UtilityFunctions.<T>identity()).lastOrDefault(defaultValue));
    }

    /**
     * Returns an {@link Iterable} that always returns the item most recently emitted by this
     * {@code BlockingObservable}.
     * <p>
     * <img width="640" height="490" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.mostRecent.png" alt="">
     *
     * @param initialValue
     *            the initial value that the {@link Iterable} sequence will yield if this
     *            {@code BlockingObservable} has not yet emitted an item
     * @return an {@link Iterable} that on each iteration returns the item that this {@code BlockingObservable}
     *         has most recently emitted
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#mostrecent">RxJava wiki: mostRecent()</a>
     */
    public Iterable<T> mostRecent(T initialValue) {
        return BlockingOperatorMostRecent.mostRecent(o, initialValue);
    }

    /**
     * Returns an {@link Iterable} that blocks until this {@code BlockingObservable} emits another item, then
     * returns that item.
     * <p>
     * <img width="640" height="490" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.next.png" alt="">
     *
     * @return an {@link Iterable} that blocks upon each iteration until this {@code BlockingObservable} emits
     *         a new item, whereupon the Iterable returns that item
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#next">RxJava wiki: next()</a>
     */
    public Iterable<T> next() {
        return BlockingOperatorNext.next(o);
    }

    /**
     * Returns an {@link Iterable} that returns the latest item emitted by this {@code BlockingObservable},
     * waiting if necessary for one to become available.
     * <p>
     * If this {@code BlockingObservable} produces items faster than {@code Iterator.next} takes them,
     * {@code onNext} events might be skipped, but {@code onError} or {@code onCompleted} events are not.
     * <p>
     * Note also that an {@code onNext} directly followed by {@code onCompleted} might hide the {@code onNext}
     * event.
     *
     * @return an Iterable that always returns the latest item emitted by this {@code BlockingObservable}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#latest">RxJava wiki: latest()</a>
     */
    public Iterable<T> latest() {
        return BlockingOperatorLatest.latest(o);
    }

    /**
     * If this {@code BlockingObservable} completes after emitting a single item, return that item, otherwise
     * throw a {@code NoSuchElementException}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.single.png" alt="">
     *
     * @return the single item emitted by this {@code BlockingObservable}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava wiki: single()</a>
     */
    public T single() {
        return blockForSingle(o.single());
    }

    /**
     * If this {@code BlockingObservable} completes after emitting a single item that matches a given predicate,
     * return that item, otherwise throw a {@code NoSuchElementException}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.single.p.png" alt="">
     *
     * @param predicate
     *            a predicate function to evaluate items emitted by this {@link BlockingObservable}
     * @return the single item emitted by this {@code BlockingObservable} that matches the predicate
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava wiki: single()</a>
     */
    public T single(Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.single(predicate));
    }

    /**
     * If this {@code BlockingObservable} completes after emitting a single item, return that item; if it emits
     * more than one item, throw an {@code IllegalArgumentException}; if it emits no items, return a default
     * value.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.singleOrDefault.png" alt="">
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no items
     * @return the single item emitted by this {@code BlockingObservable}, or the default value if it emits no
     *         items
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava wiki: singleOrDefault()</a>
     */
    public T singleOrDefault(T defaultValue) {
        return blockForSingle(o.map(UtilityFunctions.<T>identity()).singleOrDefault(defaultValue));
    }

    /**
     * If this {@code BlockingObservable} completes after emitting a single item that matches a predicate,
     * return that item; if it emits more than one such item, throw an {@code IllegalArgumentException}; if it
     * emits no items, return a default value.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png" alt="">
     *
     * @param defaultValue
     *            a default value to return if this {@code BlockingObservable} emits no matching items
     * @param predicate
     *            a predicate function to evaluate items emitted by this {@code BlockingObservable}
     * @return the single item emitted by the {@code BlockingObservable} that matches the predicate, or the
     *         default value if no such items are emitted
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava wiki: singleOrDefault()</a>
     */
    public T singleOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return blockForSingle(o.filter(predicate).map(UtilityFunctions.<T>identity()).singleOrDefault(defaultValue));
    }

    /**
     * Returns a {@link Future} representing the single value emitted by this {@code BlockingObservable}.
     * <p>
     * If {@link BlockingObservable} emits more than one item, {@link java.util.concurrent.Future} will receive an
     * {@link java.lang.IllegalArgumentException}. If {@link BlockingObservable} is empty, {@link java.util.concurrent.Future}
     * will receive an {@link java.util.NoSuchElementException}.
     * <p>
     * If the {@code BlockingObservable} may emit more than one item, use {@code Observable.toList().toBlocking().toFuture()}.
     * <p>
     * <img width="640" height="395" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.toFuture.png" alt="">
     *
     * @return a {@link Future} that expects a single item to be emitted by this {@code BlockingObservable}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava wiki: toFuture()</a>
     */
    public Future<T> toFuture() {
        return BlockingOperatorToFuture.toFuture(o);
    }

    /**
     * Converts this {@code BlockingObservable} into an {@link Iterable}.
     * <p>
     * <img width="640" height="315" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/B.toIterable.png" alt="">
     *
     * @return an {@link Iterable} version of this {@code BlockingObservable}
     * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava wiki: toIterable()</a>
     */
    public Iterable<T> toIterable() {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return getIterator();
            }
        };
    }

    /**
     * Helper method which handles the actual blocking for a single response.
     * <p>
     * If the {@link Observable} errors, it will be thrown right away.
     *
     * @return the actual item
     */
    private T blockForSingle(final Observable<? extends T> observable) {
        final AtomicReference<T> returnItem = new AtomicReference<T>();
        final AtomicReference<Throwable> returnException = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);

        observable.subscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(final Throwable e) {
                returnException.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(final T item) {
                returnItem.set(item);
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (returnException.get() != null) {
            if (returnException.get() instanceof RuntimeException) {
                throw (RuntimeException) returnException.get();
            } else {
                throw new RuntimeException(returnException.get());
            }
        }

        return returnItem.get();
    }
}
