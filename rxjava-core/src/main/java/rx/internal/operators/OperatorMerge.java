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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action1;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.SubscriptionRandomList;

/**
 * Flattens a list of {@link Observable}s into one {@code Observable}, without any transformation.
 * <p>
 * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/Netflix/RxJava/images/rx-operators/merge.png">
 * <p>
 * You can combine the items emitted by multiple {@code Observable}s so that they act like a single {@code Observable}, by using the merge operation.
 * 
 * @param <T>
 *            the type of the items emitted by both the source and merged {@code Observable}s
 */
public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {

    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SubscriptionRandomList<InnerSubscriber<T>> childrenSubscriptions = new SubscriptionRandomList<InnerSubscriber<T>>();
        child.add(childrenSubscriptions);
        return new MergeSubscriber<T>(child, childrenSubscriptions);

    }

    @SuppressWarnings("rawtypes")
    private final static NotificationLite NOTIFICATION = NotificationLite.instance();

    private static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<? super T> actual;
        final SubscriptionRandomList<InnerSubscriber<T>> childrenSubscribers;
        private MergeProducer<T> mergeProducer;
        private int wip;
        private boolean completed;

        public MergeSubscriber(Subscriber<? super T> actual, SubscriptionRandomList<InnerSubscriber<T>> childrenSubscriptions) {
            super(actual);
            this.actual = actual;
            this.childrenSubscribers = childrenSubscriptions;
        }

        @Override
        public void onNext(Observable<? extends T> t) {
            if (t == null) {
                // bad Observable
                return;
            }
            synchronized (this) {
                if (mergeProducer == null) {
                    // this means it's an Observable without backpressure support
                    mergeProducer = new MergeProducer<T>(this, null, actual, childrenSubscribers);
                }
                wip++;
            }
            if (!isUnsubscribed()) {
                mergeProducer.handleNewSource(t);
            }
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            boolean c = false;
            synchronized (this) {
                completed = true;
                if (wip == 0) {
                    c = true;
                }
            }
            if (c) {
                // complete outside of lock
                actual.onCompleted();
            }
        }

        void completeInner(InnerSubscriber<T> s) {
            try {
                boolean sendOnComplete = false;
                synchronized (this) {
                    wip--;
                    if (wip == 0 && completed) {
                        sendOnComplete = true;
                    }
                }
                if (sendOnComplete) {
                    actual.onCompleted();
                }
            } finally {
                childrenSubscribers.remove(s);
            }
        }

        @Override
        protected Producer onSetProducer(final Producer p) {
            /*
             * Decouple the Producer chain.
             * 
             * Upwards we will just request -1 to get all Observables (on first request to MergeProducer)
             * 
             * For requests coming from the child we want to route them instead
             * to the MergeProducer which will manage draining queues to deliver events.
             */
            synchronized (this) {
                if (mergeProducer != null) {
                    throw new IllegalStateException("Received Producer in `onSetProducer` after `onNext`");
                }
                mergeProducer = new MergeProducer<T>(this, p, actual, childrenSubscribers);
            }
            return mergeProducer;
        }
    }

    private static final class MergeProducer<T> implements Producer {

        private final MergeSubscriber<T> parentSubscriber;
        private final SubscriptionRandomList<InnerSubscriber<T>> childrenSubscribers;
        private final Producer parentProducer;

        private AtomicInteger requested = new AtomicInteger(-1); // default to infinite
        private AtomicInteger infiniteRequestSent = new AtomicInteger(0);
        private AtomicInteger emitLock = new AtomicInteger(0);

        /* protected by `emitLock` */
        private final Subscriber<? super T> child;
        @SuppressWarnings("unused")
        final Action1<InnerSubscriber<T>> DRAIN_ACTION = new Action1<InnerSubscriber<T>>() {

            @Override
            public void call(InnerSubscriber<T> is) {
                _unsafeDrainQueue(is, is.getQ());
            }

        };

        /* used to ensure serialized emission to the child Subscriber */

        public MergeProducer(MergeSubscriber<T> parentSubscriber, Producer parentProducer, Subscriber<? super T> child, SubscriptionRandomList<InnerSubscriber<T>> childrenSubscribers) {
            this.parentSubscriber = parentSubscriber;
            this.parentProducer = parentProducer;
            this.child = child;
            this.childrenSubscribers = childrenSubscribers;
        }

        @Override
        public void request(int n) {
            int r = requested.getAndAdd(n);
            if (r < n) {
                // this means it was negative so let's add the diff
                requested.getAndAdd((n - r));
            }

            // do outside of lock
            if (infiniteRequestSent.compareAndSet(0, 1)) {
                // parentProducer can be null if we're merging an Observable<Observable> without backpressure support
                if (parentProducer != null) {
                    // request up to our parent to start sending us the Observables for merging
                    parentProducer.request(-1);
                }
            }

            claimAndDrainQueues();
        }

        /**
         * Loop over all queues and emit up to the requested amount
         */
        private void claimAndDrainQueues() {
            // try draining queues
            if (emitLock.getAndIncrement() == 0) {
                do {
                    // TODO change this to use iteratorStartingAt or forEach(Node<E> startingAt, Action1<Node<E>> action)
                    // so it resumes from the last node ... rather than always starting at the beginning
                    childrenSubscribers.forEach(DRAIN_ACTION);
                } while (emitLock.decrementAndGet() > 0);
            }
        }

        /*
         * ONLY call this if you hold the EMIT_LOCK
         */
        private void _unsafeDrainQueue(InnerSubscriber<T> is, RxRingBuffer q) {
            if (q == null || is == null) {
                return;
            }
            Object o = null;
            while (requested.decrementAndGet() != 0 && (o = q.poll()) != null) { // TODO this seems wrong
                // we don't receive errors via the queue
                if (q.isCompleted(o)) {
                    is.complete(); // nothing can be done with 'q' after this
                } else {
                    q.accept(o, child);
                }
            }
            if (!is.isUnsubscribed()) {
                q.requestIfNeeded(is);
            }
        }

        private void emitOrEnqueue(InnerSubscriber<T> is, Object o) throws MissingBackpressureException {
            RxRingBuffer q = is.getQ();
            if (q == null) {
                // if we are unsubscribed
                return;
            }
            boolean enqueue = true;
            int el = 0;
            if (emitLock.getAndIncrement() == 0) {
                try {
                    // we can write and skip queueing
                    // first drain anything in our own queue
                    _unsafeDrainQueue(is, q);
                    // emit
                    if (requested.decrementAndGet() != 0) { // TODO this seems wrong
                        if (q.isCompleted(o)) {
                            is.complete(); // nothing can be done with 'q' after this
                        } else {
                            q.emitWithoutQueue(o, child);
                        }
                        enqueue = false;
                    }
                } finally {
                    // we always set to 0 here so anyone can now claim the work, including this thread again below
                    el = emitLock.getAndSet(0);
                }
            }
            if (enqueue) {
                if (q.isCompleted(o)) {
                    q.onCompleted();
                } else {
                    try {
                        q.onNext((T) o);
                    } catch (MissingBackpressureException e) {
                        throw e;
                    }
                }
                claimAndDrainQueues();
            } else {
                if (el > 1) {
                    // only do this after emitting if there is other work queued
                    claimAndDrainQueues();
                }
            }
        }

        private void onNext(InnerSubscriber<T> is, T t) throws MissingBackpressureException {
            emitOrEnqueue(is, t);
        }

        private void onCompleted(InnerSubscriber<T> is) throws MissingBackpressureException {
            emitOrEnqueue(is, NOTIFICATION.completed());
        }

        private void handleNewSource(Observable<? extends T> t) {
            InnerSubscriber<T> i = new InnerSubscriber<T>(this, parentSubscriber);
            childrenSubscribers.add(i);
            RxRingBuffer q = i.getQ();
            if (q != null) {
                q.requestIfNeeded(i);
            }
            t.unsafeSubscribe(i);
        }
    }

    private static final class InnerSubscriber<T> extends Subscriber<T> {
        final MergeProducer<T> mergeProducer;
        final MergeSubscriber<T> parent;
        /** Make sure the inner termination events are delivered only once. */
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<InnerSubscriber> ONCE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(InnerSubscriber.class, "once");
        final RxRingBuffer _q = RxRingBuffer.getSpmcInstance();

        public InnerSubscriber(MergeProducer<T> mergeProducer, MergeSubscriber<T> parent) {
            this.mergeProducer = mergeProducer;
            this.parent = parent;
        }

        private RxRingBuffer getQ() {
            if (!isUnsubscribed()) {
                return _q;
            } else {
                return null;
            }
        }

        @Override
        public void onNext(T t) {
            try {
                mergeProducer.onNext(this, t);
            } catch (MissingBackpressureException e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable e) {
            // it doesn't go through queues, it immediately onErrors and tears everything down
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            try {
                mergeProducer.onCompleted(this);
            } catch (MissingBackpressureException e) {
                onError(e);
            }
        }

        void complete() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parent.completeInner(this);
            }
        }

    }
}
