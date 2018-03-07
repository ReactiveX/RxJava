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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.HasUpstreamObservableSource;
import io.reactivex.internal.util.*;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Timed;

public final class ObservableReplay<T> extends ConnectableObservable<T> implements HasUpstreamObservableSource<T>, Disposable {
    /** The source observable. */
    final ObservableSource<T> source;
    /** Holds the current subscriber that is, will be or just was subscribed to the source observable. */
    final AtomicReference<ReplayObserver<T>> current;
    /** A factory that creates the appropriate buffer for the ReplayObserver. */
    final BufferSupplier<T> bufferFactory;

    final ObservableSource<T> onSubscribe;

    interface BufferSupplier<T> {
        ReplayBuffer<T> call();
    }

    @SuppressWarnings("rawtypes")
    static final BufferSupplier DEFAULT_UNBOUNDED_FACTORY = new UnBoundedFactory();

    /**
     * Given a connectable observable factory, it multicasts over the generated
     * ConnectableObservable via a selector function.
     * @param <U> the value type of the ConnectableObservable
     * @param <R> the result value type
     * @param connectableFactory the factory that returns a ConnectableObservable for each individual subscriber
     * @param selector the function that receives an Observable and should return another Observable that will be subscribed to
     * @return the new Observable instance
     */
    public static <U, R> Observable<R> multicastSelector(
            final Callable<? extends ConnectableObservable<U>> connectableFactory,
            final Function<? super Observable<U>, ? extends ObservableSource<R>> selector) {
        return RxJavaPlugins.onAssembly(new MulticastReplay<R, U>(connectableFactory, selector));
    }

    /**
     * Child Observers will observe the events of the ConnectableObservable on the
     * specified scheduler.
     * @param <T> the value type
     * @param co the connectable observable instance
     * @param scheduler the target scheduler
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableObservable<T> observeOn(final ConnectableObservable<T> co, final Scheduler scheduler) {
        final Observable<T> observable = co.observeOn(scheduler);
        return RxJavaPlugins.onAssembly(new Replay<T>(co, observable));
    }

    /**
     * Creates a replaying ConnectableObservable with an unbounded buffer.
     * @param <T> the value type
     * @param source the source observable
     * @return the new ConnectableObservable instance
     */
    @SuppressWarnings("unchecked")
    public static <T> ConnectableObservable<T> createFrom(ObservableSource<? extends T> source) {
        return create(source, DEFAULT_UNBOUNDED_FACTORY);
    }

    /**
     * Creates a replaying ConnectableObservable with a size bound buffer.
     * @param <T> the value type
     * @param source the source ObservableSource to use
     * @param bufferSize the maximum number of elements to hold
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableObservable<T> create(ObservableSource<T> source,
            final int bufferSize) {
        if (bufferSize == Integer.MAX_VALUE) {
            return createFrom(source);
        }
        return create(source, new ReplayBufferSupplier<T>(bufferSize));
    }

    /**
     * Creates a replaying ConnectableObservable with a time bound buffer.
     * @param <T> the value type
     * @param source the source ObservableSource to use
     * @param maxAge the maximum age of entries
     * @param unit the unit of measure of the age amount
     * @param scheduler the target scheduler providing the current time
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableObservable<T> create(ObservableSource<T> source,
            long maxAge, TimeUnit unit, Scheduler scheduler) {
        return create(source, maxAge, unit, scheduler, Integer.MAX_VALUE);
    }

    /**
     * Creates a replaying ConnectableObservable with a size and time bound buffer.
     * @param <T> the value type
     * @param source the source ObservableSource to use
     * @param maxAge the maximum age of entries
     * @param unit the unit of measure of the age amount
     * @param scheduler the target scheduler providing the current time
     * @param bufferSize the maximum number of elements to hold
     * @return the new ConnectableObservable instance
     */
    public static <T> ConnectableObservable<T> create(ObservableSource<T> source,
            final long maxAge, final TimeUnit unit, final Scheduler scheduler, final int bufferSize) {
        return create(source, new ScheduledReplaySupplier<T>(bufferSize, maxAge, unit, scheduler));
    }

    /**
     * Creates a OperatorReplay instance to replay values of the given source observable.
     * @param source the source observable
     * @param bufferFactory the factory to instantiate the appropriate buffer when the observable becomes active
     * @return the connectable observable
     */
    static <T> ConnectableObservable<T> create(ObservableSource<T> source,
            final BufferSupplier<T> bufferFactory) {
        // the current connection to source needs to be shared between the operator and its onSubscribe call
        final AtomicReference<ReplayObserver<T>> curr = new AtomicReference<ReplayObserver<T>>();
        ObservableSource<T> onSubscribe = new ReplaySource<T>(curr, bufferFactory);
        return RxJavaPlugins.onAssembly(new ObservableReplay<T>(onSubscribe, source, curr, bufferFactory));
    }

    private ObservableReplay(ObservableSource<T> onSubscribe, ObservableSource<T> source,
                             final AtomicReference<ReplayObserver<T>> current,
                             final BufferSupplier<T> bufferFactory) {
        this.onSubscribe = onSubscribe;
        this.source = source;
        this.current = current;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public ObservableSource<T> source() {
        return source;
    }

    @Override
    public void dispose() {
        current.lazySet(null);
    }

    @Override
    public boolean isDisposed() {
        Disposable d = current.get();
        return d == null || d.isDisposed();
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        onSubscribe.subscribe(observer);
    }

    @Override
    public void connect(Consumer<? super Disposable> connection) {
        boolean doConnect;
        ReplayObserver<T> ps;
        // we loop because concurrent connect/disconnect and termination may change the state
        for (;;) {
            // retrieve the current subscriber-to-source instance
            ps = current.get();
            // if there is none yet or the current has been disposed
            if (ps == null || ps.isDisposed()) {
                // create a new subscriber-to-source
                ReplayBuffer<T> buf = bufferFactory.call();

                ReplayObserver<T> u = new ReplayObserver<T>(buf);
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
         * Synchronous source consumers have the opportunity to disconnect via dispose() on the
         * Disposable as subscribe() may never return in its own.
         *
         * Note however, that asynchronously disconnecting a running source might leave
         * child observers without any terminal event; ReplaySubject does not have this
         * issue because the dispose() call was always triggered by the child observers
         * themselves.
         */

        try {
            connection.accept(ps);
        } catch (Throwable ex) {
            if (doConnect) {
                ps.shouldConnect.compareAndSet(true, false);
            }
            Exceptions.throwIfFatal(ex);
            throw ExceptionHelper.wrapOrThrow(ex);
        }
        if (doConnect) {
            source.subscribe(ps);
        }
    }

    @SuppressWarnings("rawtypes")
    static final class ReplayObserver<T>
    extends AtomicReference<Disposable>
    implements Observer<T>, Disposable {
        private static final long serialVersionUID = -533785617179540163L;
        /** Holds notifications from upstream. */
        final ReplayBuffer<T> buffer;
        /** Indicates this Observer received a terminal event. */
        boolean done;

        /** Indicates an empty array of inner observers. */
        static final InnerDisposable[] EMPTY = new InnerDisposable[0];
        /** Indicates a terminated ReplayObserver. */
        static final InnerDisposable[] TERMINATED = new InnerDisposable[0];

        /** Tracks the subscribed observers. */
        final AtomicReference<InnerDisposable[]> observers;
        /**
         * Atomically changed from false to true by connect to make sure the
         * connection is only performed by one thread.
         */
        final AtomicBoolean shouldConnect;

        ReplayObserver(ReplayBuffer<T> buffer) {
            this.buffer = buffer;

            this.observers = new AtomicReference<InnerDisposable[]>(EMPTY);
            this.shouldConnect = new AtomicBoolean();
        }

        @Override
        public boolean isDisposed() {
            return observers.get() == TERMINATED;
        }

        @Override
        public void dispose() {
            observers.set(TERMINATED);
            // unlike OperatorPublish, we can't null out the terminated so
            // late observers can still get replay
            // current.compareAndSet(ReplayObserver.this, null);
            // we don't care if it fails because it means the current has
            // been replaced in the meantime
            DisposableHelper.dispose(this);
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
                InnerDisposable[] c = observers.get();
                // if this subscriber-to-source reached a terminal state by receiving
                // an onError or onComplete, just refuse to add the new producer
                if (c == TERMINATED) {
                    return false;
                }
                // we perform a copy-on-write logic
                int len = c.length;
                InnerDisposable[] u = new InnerDisposable[len + 1];
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
         * Atomically removes the given InnerDisposable from the observers array.
         * @param producer the producer to remove
         */
        void remove(InnerDisposable<T> producer) {
            // the state can change so we do a CAS loop to achieve atomicity
            for (;;) {
                // let's read the current observers array
                InnerDisposable[] c = observers.get();

                int len = c.length;
                // if it is either empty or terminated, there is nothing to remove so we quit
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
                InnerDisposable[] u;
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

        @Override
        public void onSubscribe(Disposable p) {
            if (DisposableHelper.setOnce(this, p)) {
                replay();
            }
        }

        @Override
        public void onNext(T t) {
            if (!done) {
                buffer.next(t);
                replay();
            }
        }
        @Override
        public void onError(Throwable e) {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                buffer.error(e);
                replayFinal();
            } else {
                RxJavaPlugins.onError(e);
            }
        }
        @Override
        public void onComplete() {
            // The observer front is accessed serially as required by spec so
            // no need to CAS in the terminal value
            if (!done) {
                done = true;
                buffer.complete();
                replayFinal();
            }
        }

        /**
         * Tries to replay the buffer contents to all known observers.
         */
        void replay() {
            @SuppressWarnings("unchecked")
            InnerDisposable<T>[] a = observers.get();
            for (InnerDisposable<T> rp : a) {
                buffer.replay(rp);
            }
        }

        /**
         * Tries to replay the buffer contents to all known observers.
         */
        void replayFinal() {
            @SuppressWarnings("unchecked")
            InnerDisposable<T>[] a = observers.getAndSet(TERMINATED);
            for (InnerDisposable<T> rp : a) {
                buffer.replay(rp);
            }
        }
    }
    /**
     * A Disposable that manages the disposed state of a
     * child Observer in thread-safe manner.
     * @param <T> the value type
     */
    static final class InnerDisposable<T>
    extends AtomicInteger
    implements Disposable {
        private static final long serialVersionUID = 2728361546769921047L;
        /**
         * The parent subscriber-to-source used to allow removing the child in case of
         * child dispose() call.
         */
        final ReplayObserver<T> parent;
        /** The actual child subscriber. */
        final Observer<? super T> child;
        /**
         * Holds an object that represents the current location in the buffer.
         * Guarded by the emitter loop.
         */
        Object index;

        volatile boolean cancelled;

        InnerDisposable(ReplayObserver<T> parent, Observer<? super T> child) {
            this.parent = parent;
            this.child = child;
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                // remove this from the parent
                parent.remove(this);
            }
        }
        /**
         * Convenience method to auto-cast the index object.
         * @return the index Object or null
         */
        @SuppressWarnings("unchecked")
        <U> U index() {
            return (U)index;
        }
    }
    /**
     * The interface for interacting with various buffering logic.
     *
     * @param <T> the value type
     */
    interface ReplayBuffer<T> {
        /**
         * Adds a regular value to the buffer.
         * @param value the value to be stored in the buffer
         */
        void next(T value);
        /**
         * Adds a terminal exception to the buffer.
         * @param e the error to be stored in the buffer
         */
        void error(Throwable e);
        /**
         * Adds a completion event to the buffer.
         */
        void complete();
        /**
         * Tries to replay the buffered values to the
         * subscriber inside the output if there
         * is new value and requests available at the
         * same time.
         * @param output the receiver of the buffered events
         */
        void replay(InnerDisposable<T> output);
    }

    /**
     * Holds an unbounded list of events.
     *
     * @param <T> the value type
     */
    static final class UnboundedReplayBuffer<T> extends ArrayList<Object> implements ReplayBuffer<T> {

        private static final long serialVersionUID = 7063189396499112664L;
        /** The total number of events in the buffer. */
        volatile int size;

        UnboundedReplayBuffer(int capacityHint) {
            super(capacityHint);
        }
        @Override
        public void next(T value) {
            add(NotificationLite.next(value));
            size++;
        }

        @Override
        public void error(Throwable e) {
            add(NotificationLite.error(e));
            size++;
        }

        @Override
        public void complete() {
            add(NotificationLite.complete());
            size++;
        }

        @Override
        public void replay(InnerDisposable<T> output) {
            if (output.getAndIncrement() != 0) {
                return;
            }

            final Observer<? super T> child = output.child;

            int missed = 1;

            for (;;) {
                if (output.isDisposed()) {
                    return;
                }
                int sourceIndex = size;

                Integer destinationIndexObject = output.index();
                int destinationIndex = destinationIndexObject != null ? destinationIndexObject : 0;

                while (destinationIndex < sourceIndex) {
                    Object o = get(destinationIndex);
                    if (NotificationLite.accept(o, child)) {
                        return;
                    }
                    if (output.isDisposed()) {
                        return;
                    }
                    destinationIndex++;
                }

                output.index = destinationIndex;
                missed = output.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    /**
     * Represents a node in a bounded replay buffer's linked list.
     */
    static final class Node extends AtomicReference<Node> {

        private static final long serialVersionUID = 245354315435971818L;
        final Object value;
        Node(Object value) {
            this.value = value;
        }
    }

    /**
     * Base class for bounded buffering with options to specify an
     * enter and leave transforms and custom truncation behavior.
     *
     * @param <T> the value type
     */
    abstract static class BoundedReplayBuffer<T> extends AtomicReference<Node> implements ReplayBuffer<T> {

        private static final long serialVersionUID = 2346567790059478686L;

        Node tail;
        int size;

        BoundedReplayBuffer() {
            Node n = new Node(null);
            tail = n;
            set(n);
        }

        /**
         * Add a new node to the linked list.
         * @param n the Node instance to add as last
         */
        final void addLast(Node n) {
            tail.set(n);
            tail = n;
            size++;
        }
        /**
         * Remove the first node from the linked list.
         */
        final void removeFirst() {
            Node head = get();
            Node next = head.get();
            size--;
            // can't just move the head because it would retain the very first value
            // can't null out the head's value because of late replayers would see null
            setFirst(next);
        }

        final void trimHead() {
            Node head = get();
            if (head.value != null) {
                Node n = new Node(null);
                n.lazySet(head.get());
                set(n);
            }
        }

        /* test */ final void removeSome(int n) {
            Node head = get();
            while (n > 0) {
                head = head.get();
                n--;
                size--;
            }

            setFirst(head);
        }
        /**
         * Arranges the given node is the new head from now on.
         * @param n the Node instance to set as first
         */
        final void setFirst(Node n) {
            set(n);
        }

        @Override
        public final void next(T value) {
            Object o = enterTransform(NotificationLite.next(value));
            Node n = new Node(o);
            addLast(n);
            truncate();
        }

        @Override
        public final void error(Throwable e) {
            Object o = enterTransform(NotificationLite.error(e));
            Node n = new Node(o);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void complete() {
            Object o = enterTransform(NotificationLite.complete());
            Node n = new Node(o);
            addLast(n);
            truncateFinal();
        }

        @Override
        public final void replay(InnerDisposable<T> output) {
            if (output.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {
                Node node = output.index();
                if (node == null) {
                    node = getHead();
                    output.index = node;
                }

                for (;;) {
                    if (output.isDisposed()) {
                        return;
                    }

                    Node v = node.get();
                    if (v != null) {
                        Object o = leaveTransform(v.value);
                        if (NotificationLite.accept(o, output.child)) {
                            output.index = null;
                            return;
                        }
                        node = v;
                    } else {
                        break;
                    }
                }

                output.index = node;

                missed = output.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }

        }

        /**
         * Override this to wrap the NotificationLite object into a
         * container to be used later by truncate.
         * @param value the value to transform into the internal representation
         * @return the transformed value
         */
        Object enterTransform(Object value) {
            return value;
        }
        /**
         * Override this to unwrap the transformed value into a
         * NotificationLite object.
         * @param value the value in the internal representation to transform
         * @return the transformed value
         */
        Object leaveTransform(Object value) {
            return value;
        }
        /**
         * Override this method to truncate a non-terminated buffer
         * based on its current properties.
         */
        abstract void truncate();

        /**
         * Override this method to truncate a terminated buffer
         * based on its properties (i.e., truncate but the very last node).
         */
        void truncateFinal() {
            trimHead();
        }
        /* test */ final  void collect(Collection<? super T> output) {
            Node n = getHead();
            for (;;) {
                Node next = n.get();
                if (next != null) {
                    Object o = next.value;
                    Object v = leaveTransform(o);
                    if (NotificationLite.isComplete(v) || NotificationLite.isError(v)) {
                        break;
                    }
                    output.add(NotificationLite.<T>getValue(v));
                    n = next;
                } else {
                    break;
                }
            }
        }
        /* test */ boolean hasError() {
            return tail.value != null && NotificationLite.isError(leaveTransform(tail.value));
        }
        /* test */ boolean hasCompleted() {
            return tail.value != null && NotificationLite.isComplete(leaveTransform(tail.value));
        }

        Node getHead() {
            return get();
        }
    }

    /**
     * A bounded replay buffer implementation with size limit only.
     *
     * @param <T> the value type
     */
    static final class SizeBoundReplayBuffer<T> extends BoundedReplayBuffer<T> {

        private static final long serialVersionUID = -5898283885385201806L;

        final int limit;
        SizeBoundReplayBuffer(int limit) {
            this.limit = limit;
        }

        @Override
        void truncate() {
            // overflow can be at most one element
            if (size > limit) {
                removeFirst();
            }
        }

        // no need for final truncation because values are truncated one by one
    }

    /**
     * Size and time bound replay buffer.
     *
     * @param <T> the buffered value type
     */
    static final class SizeAndTimeBoundReplayBuffer<T> extends BoundedReplayBuffer<T> {

        private static final long serialVersionUID = 3457957419649567404L;
        final Scheduler scheduler;
        final long maxAge;
        final TimeUnit unit;
        final int limit;
        SizeAndTimeBoundReplayBuffer(int limit, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.limit = limit;
            this.maxAge = maxAge;
            this.unit = unit;
        }

        @Override
        Object enterTransform(Object value) {
            return new Timed<Object>(value, scheduler.now(unit), unit);
        }

        @Override
        Object leaveTransform(Object value) {
            return ((Timed<?>)value).value();
        }

        @Override
        void truncate() {
            long timeLimit = scheduler.now(unit) - maxAge;

            Node prev = get();
            Node next = prev.get();

            int e = 0;
            for (;;) {
                if (next != null) {
                    if (size > limit) {
                        e++;
                        size--;
                        prev = next;
                        next = next.get();
                    } else {
                        Timed<?> v = (Timed<?>)next.value;
                        if (v.time() <= timeLimit) {
                            e++;
                            size--;
                            prev = next;
                            next = next.get();
                        } else {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }
            if (e != 0) {
                setFirst(prev);
            }
        }
        @Override
        void truncateFinal() {
            long timeLimit = scheduler.now(unit) - maxAge;

            Node prev = get();
            Node next = prev.get();

            int e = 0;
            for (;;) {
                if (next != null && size > 1) {
                    Timed<?> v = (Timed<?>)next.value;
                    if (v.time() <= timeLimit) {
                        e++;
                        size--;
                        prev = next;
                        next = next.get();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (e != 0) {
                setFirst(prev);
            }
        }

        @Override
        Node getHead() {
            long timeLimit = scheduler.now(unit) - maxAge;
            Node prev = get();
            Node next = prev.get();
            for (;;) {
                if (next == null) {
                    break;
                }
                Timed<?> v = (Timed<?>)next.value;
                if (NotificationLite.isComplete(v.value()) || NotificationLite.isError(v.value())) {
                    break;
                }
                if (v.time() <= timeLimit) {
                    prev = next;
                    next = next.get();
                } else {
                    break;
                }
            }
            return prev;
        }
    }

    static final class UnBoundedFactory implements BufferSupplier<Object> {
        @Override
        public ReplayBuffer<Object> call() {
            return new UnboundedReplayBuffer<Object>(16);
        }
    }

    static final class DisposeConsumer<R> implements Consumer<Disposable> {
        private final ObserverResourceWrapper<R> srw;

        DisposeConsumer(ObserverResourceWrapper<R> srw) {
            this.srw = srw;
        }

        @Override
        public void accept(Disposable r) {
            srw.setResource(r);
        }
    }

    static final class ReplayBufferSupplier<T> implements BufferSupplier<T> {
        private final int bufferSize;

        ReplayBufferSupplier(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public ReplayBuffer<T> call() {
            return new SizeBoundReplayBuffer<T>(bufferSize);
        }
    }

    static final class ScheduledReplaySupplier<T> implements BufferSupplier<T> {
        private final int bufferSize;
        private final long maxAge;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        ScheduledReplaySupplier(int bufferSize, long maxAge, TimeUnit unit, Scheduler scheduler) {
            this.bufferSize = bufferSize;
            this.maxAge = maxAge;
            this.unit = unit;
            this.scheduler = scheduler;
        }

        @Override
        public ReplayBuffer<T> call() {
            return new SizeAndTimeBoundReplayBuffer<T>(bufferSize, maxAge, unit, scheduler);
        }
    }

    static final class ReplaySource<T> implements ObservableSource<T> {
        private final AtomicReference<ReplayObserver<T>> curr;
        private final BufferSupplier<T> bufferFactory;

        ReplaySource(AtomicReference<ReplayObserver<T>> curr, BufferSupplier<T> bufferFactory) {
            this.curr = curr;
            this.bufferFactory = bufferFactory;
        }

        @Override
        public void subscribe(Observer<? super T> child) {
            // concurrent connection/disconnection may change the state,
            // we loop to be atomic while the child subscribes
            for (;;) {
                // get the current subscriber-to-source
                ReplayObserver<T> r = curr.get();
                // if there isn't one
                if (r == null) {
                    // create a new subscriber to source
                    ReplayBuffer<T> buf = bufferFactory.call();

                    ReplayObserver<T> u = new ReplayObserver<T>(buf);
                    // let's try setting it as the current subscriber-to-source
                    if (!curr.compareAndSet(null, u)) {
                        // didn't work, maybe someone else did it or the current subscriber
                        // to source has just finished
                        continue;
                    }
                    // we won, let's use it going onwards
                    r = u;
                }

                // create the backpressure-managing producer for this child
                InnerDisposable<T> inner = new InnerDisposable<T>(r, child);
                // the producer has been registered with the current subscriber-to-source so
                // at least it will receive the next terminal event
                // setting the producer will trigger the first request to be considered by
                // the subscriber-to-source.
                child.onSubscribe(inner);
                // we try to add it to the array of observers
                // if it fails, no worries because we will still have its buffer
                // so it is going to replay it for us
                r.add(inner);

                if (inner.isDisposed()) {
                    r.remove(inner);
                    return;
                }

                // replay the contents of the buffer
                r.buffer.replay(inner);

                break; // NOPMD
            }
        }
    }

    static final class MulticastReplay<R, U> extends Observable<R> {
        private final Callable<? extends ConnectableObservable<U>> connectableFactory;
        private final Function<? super Observable<U>, ? extends ObservableSource<R>> selector;

        MulticastReplay(Callable<? extends ConnectableObservable<U>> connectableFactory, Function<? super Observable<U>, ? extends ObservableSource<R>> selector) {
            this.connectableFactory = connectableFactory;
            this.selector = selector;
        }

        @Override
        protected void subscribeActual(Observer<? super R> child) {
            ConnectableObservable<U> co;
            ObservableSource<R> observable;
            try {
                co = ObjectHelper.requireNonNull(connectableFactory.call(), "The connectableFactory returned a null ConnectableObservable");
                observable = ObjectHelper.requireNonNull(selector.apply(co), "The selector returned a null ObservableSource");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, child);
                return;
            }

            final ObserverResourceWrapper<R> srw = new ObserverResourceWrapper<R>(child);

            observable.subscribe(srw);

            co.connect(new DisposeConsumer<R>(srw));
        }
    }

    static final class Replay<T> extends ConnectableObservable<T> {
        private final ConnectableObservable<T> co;
        private final Observable<T> observable;

        Replay(ConnectableObservable<T> co, Observable<T> observable) {
            this.co = co;
            this.observable = observable;
        }

        @Override
        public void connect(Consumer<? super Disposable> connection) {
            co.connect(connection);
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            observable.subscribe(observer);
        }
    }
}
