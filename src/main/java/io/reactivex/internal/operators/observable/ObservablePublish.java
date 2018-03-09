/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * A connectable observable which shares an underlying source and dispatches source values to observers in a backpressure-aware
 * manner.
 * @param <T> the value type
 */
public final class ObservablePublish<T> extends ConnectableObservable<T> implements HasUpstreamObservableSource<T> {
    /** The source observable. */
    final ObservableSource<T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<PublishObserver<T>> current;

    final ObservableSource<T> onSubscribe;

    /**
     * Creates a OperatorPublish instance to publish values of the given source observable.
     * @param <T> the source value type
     * @param source the source observable
     * @return the connectable observable
     */
    public static <T> ConnectableObservable<T> create(ObservableSource<T> source) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<PublishObserver<T>> curr = new AtomicReference<PublishObserver<T>>();
        ObservableSource<T> onSubscribe = new PublishSource<T>(curr);
        return RxJavaPlugins.onAssembly(new ObservablePublish<T>(onSubscribe, source, curr));
    }

    private ObservablePublish(ObservableSource<T> onSubscribe, ObservableSource<T> source,
                              final AtomicReference<PublishObserver<T>> current) {
        this.onSubscribe = onSubscribe;
        this.source = source;
        this.current = current;
    }

    @Override
    public ObservableSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        onSubscribe.subscribe(observer);
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect;
        PublishObserver<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has been disposed
            if (ps == null || ps.isDisposed()) {
                // create a new subscriber-to-source
                PublishObserver<T> u = new PublishObserver<T>(current);
                // try setting it as the current subscriber-to-source
                if (!current.compareAndSet(ps, u)) {
                    // did not work, perhaps a new subscriber arrived
                    // and created a new subscriber-to-source as well, retry
                    continue;
                }
                ps = u;
            }
            // if connect() was called concurrently, only one of them should actually
            // connect to the source
            doConnect = !ps.shouldConnect.get() && ps.shouldConnect.compareAndSet(false, true);
            break; // NOPMD
        }
        /*
         * Notify the callback that we have a (new) connection which it can dispose
         * but since ps is unique to a connection, multiple calls to connect() will return the
         * same Disposable and even if there was a connect-disconnect-connect pair, the older
         * references won't disconnect the newer connection.
         * Synchronous source consumers have the opportunity to disconnect via dispose on the
         * Disposable as subscribe() may never return in its own.
         *
         * Note however, that asynchronously disconnecting a running source might leave
         * child observers without any terminal event; PublishSubject does not have this
         * issue because the dispose() was always triggered by the child observers
         * themselves.
         */
        try {
            connection.accept(ps);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        if (doConnect) {
            source.subscribe(ps);
        }
    }

    @SuppressWarnings("rawtypes")
    static final class PublishObserver<T>
    implements Observer<T>, Disposable {
        /** Holds onto the current connected PublishObserver. */
        final AtomicReference<PublishObserver<T>> current;

        /** Indicates an empty array of inner observers. */
        static final InnerDisposable[] EMPTY = new InnerDisposable[0];
        /** Indicates a terminated PublishObserver. */
        static final InnerDisposable[] TERMINATED = new InnerDisposable[0];

        /** Tracks the subscribed observers. */
        final AtomicReference<InnerDisposable<T>[]> observers;
        /**
         * Atomically changed from false to true by connect to make sure the
         * connection is only performed by one thread.
         */
        final AtomicBoolean shouldConnect;

        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

        @SuppressWarnings("unchecked")
        PublishObserver(AtomicReference<PublishObserver<T>> current) {
            this.observers = new AtomicReference<InnerDisposable<T>[]>(EMPTY);
            this.current = current;
            this.shouldConnect = new AtomicBoolean();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispose() {
            InnerDisposable[] ps = observers.getAndSet(TERMINATED);
            if (ps != TERMINATED) {
                current.compareAndSet(PublishObserver.this, null);

                DisposableHelper.dispose(s);
            }
        }

        @Override
        public boolean isDisposed() {
            return observers.get() == TERMINATED;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(T t) {
            for (InnerDisposable<T> inner : observers.get()) {
                inner.child.onNext(t);
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            current.compareAndSet(this, null);
            InnerDisposable<T>[] a = observers.getAndSet(TERMINATED);
            if (a.length != 0) {
                for (InnerDisposable<T> inner : a) {
                    inner.child.onError(e);
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            current.compareAndSet(this, null);
            for (InnerDisposable<T> inner : observers.getAndSet(TERMINATED)) {
                inner.child.onComplete();
            }
        }

        /**
         * Atomically try adding a new InnerDisposable to this Observer or return false if this
         * Observer was terminated.
         * @param producer the producer to add
         * @return true if succeeded, false otherwise
         */
        boolean add(InnerDisposable<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // get the current producer array
                InnerDisposable<T>[] c = observers.get();
                // if this subscriber-to-source reached a terminal state by receiving
                // an onError or onComplete, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                @SuppressWarnings("unchecked")
                InnerDisposable<T>[] u = new InnerDisposable[len + 1];
                System.arraycopy(c, 0, u, 0, len);
                u[len] = producer;
                // try setting the observers array
                if (observers.compareAndSet(c, u)) {
                    return true;
                }
                // if failed, some other operation succeeded (another add, remove or termination)
                // so retry
            }
        }

        /**
         * Atomically removes the given producer from the observers array.
         * @param producer the producer to remove
         */
        @SuppressWarnings("unchecked")
        void remove(InnerDisposable<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current observers array
                InnerDisposable<T>[] c = observers.get();
                // if it is either empty or terminated, there is nothing to remove so we quit
                int len = c.length;
                if (len == 0) {
                    return;
                }
                // let's find the supplied producer in the array
                // although this is O(n), we don't expect too many child observers in general
                int j = -1;
                for (int i = 0; i < len; i++) {
                    if (c[i].equals(producer)) {
                        j = i;
                        break;
                    }
                }
                // we didn't find it so just quit
                if (j < 0) {
                    return;
                }
                // we do copy-on-write logic here
                InnerDisposable<T>[] u;
                // we don't create a new empty array if producer was the single inhabitant
                // but rather reuse an empty array
                if (len == 1) {
                    u = EMPTY;
                } else {
                    // otherwise, create a new array one less in size
                    u = new InnerDisposable[len - 1];
                    // copy elements being before the given producer
                    System.arraycopy(c, 0, u, 0, j);
                    // copy elements being after the given producer
                    System.arraycopy(c, j + 1, u, j, len - j - 1);
                }
                // try setting this new array as
                if (observers.compareAndSet(c, u)) {
                    return;
                }
                // if we failed, it means something else happened
                // (a concurrent add/remove or termination), we need to retry
            }
        }
    }
    /**
     * A Disposable that manages the request and disposed state of a
     * child Observer in thread-safe manner.
     * {@code this} holds the parent PublishObserver or itself if disposed
     * @param <T> the value type
     */
    static final class InnerDisposable<T>
    extends AtomicReference<Object>
    implements Disposable {
        private static final long serialVersionUID = -1100270633763673112L;
        /** The actual child subscriber. */
        final Observer<? super T> child;

        InnerDisposable(Observer<? super T> child) {
            this.child = child;
        }

        @Override
        public boolean isDisposed() {
            return get() == this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void dispose() {
            Object o = getAndSet(this);
            if (o != null && o != this) {
                ((PublishObserver<T>)o).remove(this);
            }
        }

        void setParent(PublishObserver<T> p) {
            if (!compareAndSet(null, p)) {
                p.remove(this);
            }
        }
    }

    static final class PublishSource<T> implements ObservableSource<T> {
        private final AtomicReference<PublishObserver<T>> curr;

        PublishSource(AtomicReference<PublishObserver<T>> curr) {
            this.curr = curr;
        }

        @Override
        public void subscribe(Observer<? super T> child) {
            // create the backpressure-managing producer for this child
            InnerDisposable<T> inner = new InnerDisposable<T>(child);
            child.onSubscribe(inner);
            // concurrent connection/disconnection may change the state,
            // we loop to be atomic while the child subscribes
            for (;;) {
                // get the current subscriber-to-source
                PublishObserver<T> r = curr.get();
                // if there isn't one or it is disposed
                if (r == null || r.isDisposed()) {
                    // create a new subscriber to source
                    PublishObserver<T> u = new PublishObserver<T>(curr);
                    // let's try setting it as the current subscriber-to-source
                    if (!curr.compareAndSet(r, u)) {
                        // didn't work, maybe someone else did it or the current subscriber
                        // to source has just finished
                        continue;
                    }
                    // we won, let's use it going onwards
                    r = u;
                }

                /*
                 * Try adding it to the current subscriber-to-source, add is atomic in respect
                 * to other adds and the termination of the subscriber-to-source.
                 */
                if (r.add(inner)) {
                    inner.setParent(r);
                    break; // NOPMD
                }
                /*
                 * The current PublishObserver has been terminated, try with a newer one.
                 */
                /*
                 * Note: although technically correct, concurrent disconnects can cause
                 * unexpected behavior such as child observers never receiving anything
                 * (unless connected again). An alternative approach, similar to
                 * PublishSubject would be to immediately terminate such child
                 * observers as well:
                 *
                 * Object term = r.terminalEvent;
                 * if (r.nl.isCompleted(term)) {
                 *     child.onComplete();
                 * } else {
                 *     child.onError(r.nl.getError(term));
                 * }
                 * return;
                 *
                 * The original concurrent behavior was non-deterministic in this regard as well.
                 * Allowing this behavior, however, may introduce another unexpected behavior:
                 * after disconnecting a previous connection, one might not be able to prepare
                 * a new connection right after a previous termination by subscribing new child
                 * observers asynchronously before a connect call.
                 */
            }
        }
    }
}
