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
package rx.quasar;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observers.SafeSubscriber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.AbstractFuture;
import co.paralleluniverse.strands.ConditionSynchronizer;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.DelegatingReceivePort;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;

/**
 * An extension of {@link Observable} that provides blocking operators, compatible with both threads and fibers.
 * <p>
 * You construct a <code>BlockingObservable</code> from an
 * <code>Observable</code> with {@link #from(Observable)}.
 * <p>
 * The documentation for this interface makes use of a form of marble diagram
 * that has been modified to illustrate blocking operators. The following legend
 * explains these marble diagrams:
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
    private static final int BUFFER_SIZE = 10;
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
     * Used for protecting against errors being thrown from {@link Subscriber} implementations and ensuring onNext/onError/onCompleted contract
     * compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for discussion on
     * "Guideline 6.4: Protect calls to user code from within an operator"
     */
    private Subscription protectivelyWrapAndSubscribe(Subscriber<? super T> observer) {
        return o.subscribe(new SafeSubscriber<T>(observer));
    }

    /**
     * Invoke a method on each item emitted by the {@link Observable}; block
     * until the Observable completes.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link Observable#subscribe(Subscriber)}, but it blocks.
     * Because it blocks it does not need the {@link Subscriber#onCompleted()} or {@link Subscriber#onError(Throwable)} methods.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
     *
     * @param onNext
     *               the {@link Action1} to invoke for every item emitted by the {@link Observable}
     * @throws RuntimeException
     *                          if an error occurs
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#foreach">RxJava Wiki: forEach()</a>
     */
    @Suspendable
    public void forEach(final Action1<? super T> onNext) {
        try {
            final AtomicBoolean done = new AtomicBoolean(false);
            final ConditionSynchronizer sync = new SimpleConditionSynchronizer(this);
            final AtomicReference<Throwable> exceptionFromOnError = new AtomicReference<Throwable>();

            /**
             * Wrapping since raw functions provided by the user are being invoked.
             *
             * See https://github.com/Netflix/RxJava/issues/216 for discussion on
             * "Guideline 6.4: Protect calls to user code from within an operator"
             */
            protectivelyWrapAndSubscribe(new Subscriber<T>() {
                @Override
                public void onCompleted() {
                    done.set(true);
                    sync.signalAll();
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
                    done.set(true);
                    sync.signalAll();
                }

                @Override
                public void onNext(T args) {
                    onNext.call(args);
                }
            });
            // block until the subscription completes and then return
            try {
                final Object token = sync.register();
                try {
                    for (int i = 0; !done.get(); i++)
                        sync.await(i);
                } finally {
                    sync.unregister(token);
                }
            } catch (InterruptedException e) {
            // set the interrupted flag again so callers can still get it
                // for more information see https://github.com/Netflix/RxJava/pull/147#issuecomment-13624780
                Strand.currentStrand().interrupt();
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
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns an {@link ReceivePort} that receives all items emitted by a
     * specified {@link Observable}.
     *
     * @return an {@link ReceivePort} that receives all items emitted by a
     *         specified {@link Observable}.
     */
    public ReceivePort<T> toChannel() {
        return ChannelObservable.subscribe(BUFFER_SIZE, Channels.OverflowPolicy.BLOCK, o);
    }

    /**
     * Returns the first item emitted by a specified {@link Observable}, or
     * <code>IllegalArgumentException</code> if source contains no elements.
     *
     * @return the first item emitted by the source {@link Observable}
     * @throws IllegalArgumentException
     *                                  if source contains no elements
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: first()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229177.aspx">MSDN: Observable.First</a>
     */
    @Suspendable
    public T first() {
        return from(o.first()).single();
    }

    /**
     * Returns the first item emitted by a specified {@link Observable} that
     * matches a predicate, or <code>IllegalArgumentException</code> if no such
     * item is emitted.
     *
     * @param predicate
     *                  a predicate function to evaluate items emitted by the {@link Observable}
     * @return the first item emitted by the {@link Observable} that matches the
     *         predicate
     * @throws IllegalArgumentException
     *                                  if no such items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: first()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229739.aspx">MSDN: Observable.First</a>
     */
    @Suspendable
    public T first(Func1<? super T, Boolean> predicate) {
        return from(o.first(predicate)).single();
    }

    /**
     * Returns the first item emitted by a specified {@link Observable}, or a
     * default value if no items are emitted.
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no items
     * @return the first item emitted by the {@link Observable}, or the default
     *         value if no items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: firstOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229320.aspx">MSDN: Observable.FirstOrDefault</a>
     */
    @Suspendable
    public T firstOrDefault(T defaultValue) {
        return from(o.take(1)).singleOrDefault(defaultValue);
    }

    /**
     * Returns the first item emitted by a specified {@link Observable} that
     * matches a predicate, or a default value if no such items are emitted.
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *                     a predicate function to evaluate items emitted by the {@link Observable}
     * @return the first item emitted by the {@link Observable} that matches the
     *         predicate, or the default value if no matching items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#first-and-firstordefault">RxJava Wiki: firstOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229759.aspx">MSDN: Observable.FirstOrDefault</a>
     */
    @Suspendable
    public T firstOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).firstOrDefault(defaultValue);
    }

    /**
     * Returns the last item emitted by a specified {@link Observable}, or
     * throws <code>IllegalArgumentException</code> if it emits no items.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.png">
     *
     * @return the last item emitted by the source {@link Observable}
     * @throws IllegalArgumentException
     *                                  if source contains no elements
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava Wiki: last()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.last.aspx">MSDN: Observable.Last</a>
     */
    @Suspendable
    public T last() {
        return from(o.last()).single();
    }

    /**
     * Returns the last item emitted by a specified {@link Observable} that
     * matches a predicate, or throws <code>IllegalArgumentException</code> if
     * it emits no such items.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     *
     * @param predicate
     *                  a predicate function to evaluate items emitted by the {@link Observable}
     * @return the last item emitted by the {@link Observable} that matches the
     *         predicate
     * @throws IllegalArgumentException
     *                                  if no such items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava Wiki: last()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.last.aspx">MSDN: Observable.Last</a>
     */
    @Suspendable
    public T last(final Func1<? super T, Boolean> predicate) {
        return from(o.last(predicate)).single();
    }

    /**
     * Returns the last item emitted by a specified {@link Observable}, or a
     * default value if no items are emitted.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.png">
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no items
     * @return the last item emitted by the {@link Observable}, or the default
     *         value if no items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava Wiki: lastOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.lastordefault.aspx">MSDN: Observable.LastOrDefault</a>
     */
    @Suspendable
    public T lastOrDefault(T defaultValue) {
        return from(o.takeLast(1)).singleOrDefault(defaultValue);
    }

    /**
     * Returns the last item emitted by a specified {@link Observable} that
     * matches a predicate, or a default value if no such items are emitted.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *                     a predicate function to evaluate items emitted by the {@link Observable}
     * @return the last item emitted by the {@link Observable} that matches the
     *         predicate, or the default value if no matching items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#last-and-lastordefault">RxJava Wiki: lastOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.lastordefault.aspx">MSDN: Observable.LastOrDefault</a>
     */
    @Suspendable
    public T lastOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).lastOrDefault(defaultValue);
    }

    /**
     * Returns an {@link Iterable} that always returns the item most recently
     * emitted by an {@link Observable}.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
     *
     * @param initialValue
     *                     the initial value that will be yielded by the {@link Iterable} sequence if the {@link Observable} has not yet emitted an item
     * @return an {@link Iterable} that on each iteration returns the item that
     *         the {@link Observable} has most recently emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#mostrecent">RxJava wiki: mostRecent()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229751.aspx">MSDN: Observable.MostRecent</a>
     */
    public ReceivePort<T> mostRecent(T initialValue) {
        return new RecentReceivePort<T>(ChannelObservable.subscribe(1, Channels.OverflowPolicy.DISPLACE, o), initialValue);
    }

    /**
     * Returns an {@link Iterable} that blocks until the {@link Observable} emits another item, then returns that item.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
     *
     * @return an {@link Iterable} that blocks upon each iteration until the {@link Observable} emits a new item, whereupon the Iterable returns that item
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#next">RxJava Wiki: next()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh211897.aspx">MSDN: Observable.Next</a>
     */
    public ReceivePort<T> next() {
        return ChannelObservable.subscribe(1, Channels.OverflowPolicy.DISPLACE, o);
    }

    /**
     * Returns the latest item emitted by the underlying Observable, waiting if
     * necessary for one to become available.
     * <p>
     * If the underlying Observable produces items faster than the
     * <code>Iterator.next()</code> takes them, <code>onNext</code> events might
     * be skipped, but <code>onError</code> or <code>onCompleted</code> events
     * are not.
     * <p>
     * Note also that an <code>onNext()</code> directly followed by
     * <code>onCompleted()</code> might hide the <code>onNext()</code> event.
     *
     * @return the receive port
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#latest">RxJava wiki: latest()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh212115.aspx">MSDN: Observable.Latest</a>
     */
    public ReceivePort<T> latest() {
        return new LatestReceivePort<T>(ChannelObservable.subscribe(1, Channels.OverflowPolicy.DISPLACE, o));
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return
     * that item, otherwise throw an <code>IllegalArgumentException</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
     *
     * @return the single item emitted by the {@link Observable}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: single()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.single.aspx">MSDN: Observable.Single</a>
     */
    @Suspendable
    public T single() {
        try {
            return from(o.single()).toChannel().receive();
        } catch (ProducerException e) {
            throw Exceptions.propagate(e.getCause());
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
            throw Exceptions.propagate(e);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * If the {@link Observable} completes after emitting a single item that
     * matches a given predicate, return that item, otherwise throw an
     * <code>IllegalArgumentException</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     *
     * @param predicate
     *                  a predicate function to evaluate items emitted by the {@link Observable}
     * @return the single item emitted by the source {@link Observable} that
     *         matches the predicate
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: single()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.single.aspx">MSDN: Observable.Single</a>
     */
    @Suspendable
    public T single(Func1<? super T, Boolean> predicate) {
        try {
            return from(o.single(predicate)).toChannel().receive();
        } catch (ProducerException e) {
            throw Exceptions.propagate(e.getCause());
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
            throw Exceptions.propagate(e);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * If the {@link Observable} completes after emitting a single item, return
     * that item; if it emits more than one item, throw an
     * <code>IllegalArgumentException</code>; if it emits no items, return a
     * default value.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.png">
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no items
     * @return the single item emitted by the {@link Observable}, or the default
     *         value if no items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: singleOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.singleordefault.aspx">MSDN: Observable.SingleOrDefault</a>
     */
    @Suspendable
    public T singleOrDefault(T defaultValue) {
        try {
            ReceivePort<? extends T> c = this.toChannel();

            T result = c.receive();
            if (result == null)
                return defaultValue;
            if (c.receive() != null) {
                throw new IllegalArgumentException("Sequence contains too many elements");
            }
            return result;
        } catch (ProducerException e) {
            throw Exceptions.propagate(e.getCause());
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
            throw Exceptions.propagate(e);
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    /**
     * If the {@link Observable} completes after emitting a single item that
     * matches a predicate, return that item; if it emits more than one such
     * item, throw an <code>IllegalArgumentException</code>; if it emits no
     * items, return a default value.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     *
     * @param defaultValue
     *                     a default value to return if the {@link Observable} emits no matching items
     * @param predicate
     *                     a predicate function to evaluate items emitted by the {@link Observable}
     * @return the single item emitted by the {@link Observable} that matches
     *         the predicate, or the default value if no such items are emitted
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#single-and-singleordefault">RxJava Wiki: singleOrDefault()</a>
     * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.singleordefault.aspx">MSDN: Observable.SingleOrDefault</a>
     */
    @Suspendable
    public T singleOrDefault(T defaultValue, Func1<? super T, Boolean> predicate) {
        return from(o.filter(predicate)).singleOrDefault(defaultValue);
    }

    /**
     * Returns a {@link Future} representing the single value emitted by an {@link Observable}.
     * <p>
     * <code>toFuture()</code> throws an exception if the Observable emits more
     * than one item. If the Observable may emit more than item, use {@link Observable#toList toList()}.toFuture()</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toFuture.png">
     *
     * @return a {@link Future} that expects a single item to be emitted by the source {@link Observable}
     * @see <a href="https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators#transformations-tofuture-toiterable-and-toiteratorgetiterator">RxJava Wiki: toFuture()</a>
     */
    public Future<T> toFuture() {
        return new AbstractFuture<T>() {
            final AtomicReference<T> val = new AtomicReference<T>();

            {
                o.subscribe(new Observer<T>() {
                    @Override
                    public void onCompleted() {
                        set(val.get());
                    }

                    @Override
                    public void onError(Throwable e) {
                        setException(e);
                    }

                    @Override
                    public void onNext(T t) {
                        if (!val.compareAndSet(null, t))
                            setException(new IllegalStateException("Observable.toFuture() only supports sequences with a single value."));
                    }
                });
            }
        };
    }

    private static class RecentReceivePort<V> extends DelegatingReceivePort<V> {
        private V value;

        public RecentReceivePort(ReceivePort<V> target, V initialValue) {
            super(target);
            this.value = initialValue;
        }

        @Override
        public V receive(long timeout, TimeUnit unit) {
            return getValue();
        }

        @Override
        public V receive() {
            return getValue();
        }

        @Override
        public V tryReceive() {
            return getValue();
        }

        private V getValue() {
            V v = super.tryReceive();
            if (v == null) {
                if (isClosed())
                    return null;
                return value;
            }
            this.value = v;
            return v;
        }
    }

    private static class LatestReceivePort<V> extends DelegatingReceivePort<V> {
        private V value;

        public LatestReceivePort(ReceivePort<V> target) {
            super(target);
            this.value = null;
        }

        @Override
        public V receive() throws SuspendExecution, InterruptedException {
            V v = getValue();
            if (v == null && !isClosed()) {
                this.value = super.receive();
                return value;
            }
            return v;
        }

        @Override
        public V receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
            V v = getValue();
            if (v == null && !isClosed()) {
                this.value = super.receive(timeout, unit);
                return value;
            }
            return v;
        }

        @Override
        public V tryReceive() {
            return getValue();
        }

        @Override
        public boolean isClosed() {
            return super.isClosed() && value == null;
        }

        private V getValue() {
            V v = tryReceive();
            if (v == null) {
                v = value;
                if (isClosed()) {
                    this.value = null;
                    return null;
                }
                return v;
            }
            this.value = v;
            return v;
        }
    }
}
