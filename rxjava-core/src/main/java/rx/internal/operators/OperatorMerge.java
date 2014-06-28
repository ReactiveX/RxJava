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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Action1;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.ScalarSynchronousObservable;
import rx.internal.util.SubscriptionIndexedRingBuffer;
import rx.internal.util.SynchronizedQueue;

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
        return new MergeSubscriber<T>(child);

    }

    private static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<? super T> actual;
        private MergeProducer<T> mergeProducer;
        private int wip;
        private boolean completed;

        private SubscriptionIndexedRingBuffer<InnerSubscriber<T>> childrenSubscribers;

        private Queue<T> scalarValueQueue;
        /* protected by lock on MergeSubscriber instance */
        private int missedEmitting = 0;
        private boolean emitLock = false;

        /**
         * Using synchronized instead of ReentrantLock or AtomicInteger is faster when there is no contention.
         * 
         * <pre> {@code
         * Using ReentrantLock:
         * r.o.OperatorMergePerf.merge1SyncStreamOfN           1000  thrpt         5    44185.294     1295.565    ops/s
         * 
         * Using synchronized(this):
         * r.o.OperatorMergePerf.merge1SyncStreamOfN           1000  thrpt         5    79715.981     3704.486    ops/s
         * 
         * Still slower though than allowing concurrency:
         * r.o.OperatorMergePerf.merge1SyncStreamOfN           1000  thrpt         5   149331.046     4851.290    ops/s
         * } </pre>
         */

        public MergeSubscriber(Subscriber<? super T> actual) {
            super(actual);
            this.actual = actual;
        }

        /*
         * This is expected to be executed sequentially as per the Rx contract or it will not work.
         */
        @Override
        public void onNext(Observable<? extends T> t) {
            if (t instanceof ScalarSynchronousObservable) {
                handleScalarSynchronousObservable(t);
            } else {
                if (t == null || isUnsubscribed()) {
                    return;
                }
                synchronized (this) {
                    // synchronized here because mergeProducer can be created concurrently by `request`
                    // and because `wip` can be concurrently changed by children Observables
                    if (mergeProducer == null) {
                        // this means it's an Observable without backpressure support
                        mergeProducer = new MergeProducer<T>(null);
                    }
                    wip++;
                }
                handleNewSource(t);
            }
        }

        private void handleNewSource(Observable<? extends T> t) {
            if (childrenSubscribers == null) {
                // lazily create this only if we receive Observables we need to subscribe to
                childrenSubscribers = new SubscriptionIndexedRingBuffer<InnerSubscriber<T>>();
                add(childrenSubscribers);
            }
            InnerSubscriber<T> i = new InnerSubscriber<T>(this);
            i.sindex = childrenSubscribers.add(i);
            t.unsafeSubscribe(i);
        }

        private void handleScalarSynchronousObservable(Observable<? extends T> t) {
            // fast-path for scalar, synchronous values such as Observable.from(int)
            /**
             * Without this optimization:
             * 
             * <pre> {@code
             * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  8761528.407   122140.413    ops/s
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5    83421.928     1679.594    ops/s
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5       89.598        5.418    ops/s
             * 
             * With this optimization:
             * 
             * Benchmark                                          (size)   Mode   Samples        Score  Score error    Units
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1         1  thrpt         5  9576378.710   217797.685    ops/s
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1      1000  thrpt         5   113544.228     8389.422    ops/s
             * r.o.OperatorMergePerf.oneStreamOfNthatMergesIn1   1000000  thrpt         5      108.526        3.661    ops/s
             * } </pre>
             * 
             * 8.7m vs 9.6m onNext/second
             * 83m vs 113m onNext/second
             * 89m vs 108m onNext/second
             * 
             */
            if (getEmitLock()) {
                try {
                    actual.onNext(((ScalarSynchronousObservable<T>) t).get());
                    // we handle this Observable without ever incrementing the wip or touching other machinery so just return here
                    return;
                } finally {
                    if (releaseEmitLock()) {
                        drainQueuesIfNeeded();
                    }
                }
            } else {
                // enqueue the values for later delivery
                if (scalarValueQueue == null) {
                    scalarValueQueue = new SynchronizedQueue<T>(); // look at alternative here
                }
                scalarValueQueue.offer(((ScalarSynchronousObservable<T>) t).get());
                return;
            }
        }

        private synchronized boolean releaseEmitLock() {
            emitLock = false;
            if (missedEmitting == 0) {
                return false;
            } else {
                return true;
            }
        }

        private synchronized boolean getEmitLock() {
            if (emitLock) {
                missedEmitting++;
                return false;
            } else {
                emitLock = true;
                missedEmitting = 0;
                return true;
            }
        }

        private void drainQueuesIfNeeded() {
            while (true) {
                if (getEmitLock()) {
                    try {
                        if (scalarValueQueue != null && scalarValueQueue.size() > 0) {
                            for (T t : scalarValueQueue) {
                                actual.onNext(t);
                            }
                        }
                        if (childrenSubscribers != null) {
                            childrenSubscribers.forEach(DRAIN_ACTION);
                        }
                    } finally {
                        if (!releaseEmitLock()) {
                            return;
                        }
                        // otherwise we'll loop and get whatever was added 
                    }
                } else {
                    return;
                }
            }
        }

        final Action1 DRAIN_ACTION = new Action1<InnerSubscriber<T>>() {

            @Override
            public void call(InnerSubscriber<T> s) {
                if (s.q != null) {
                    Object o;
                    while ((o = s.q.poll()) != null) {
                        if (s.q.isCompleted(o)) {
                            completeInner(s);
                        } else {
                            s.q.accept(o, actual);
                        }
                    }
                    s.q.requestIfNeeded(s);
                }
            }

        };

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
                childrenSubscribers.remove(s.sindex);
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
                mergeProducer = new MergeProducer<T>(p);
            }
            return mergeProducer;
        }
    }

    private static final class MergeProducer<T> implements Producer {

        private final Producer parentProducer;

        public MergeProducer(Producer parentProducer) {
            this.parentProducer = parentProducer;
        }

        @Override
        public void request(int n) {
            // we always just want to receive all of them so we can subscribe to all and merge them
            parentProducer.request(-1);
        }

    }

    private static final class InnerSubscriber<T> extends Subscriber<T> {
        public int sindex;
        final MergeSubscriber<T> parentSubscriber;
        /** Make sure the inner termination events are delivered only once. */
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<InnerSubscriber> ONCE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(InnerSubscriber.class, "once");

        private final RxRingBuffer q = RxRingBuffer.getSpscInstance();

        public InnerSubscriber(MergeSubscriber<T> parent) {
            this.parentSubscriber = parent;
            add(q);
            q.requestIfNeeded(this);
        }

        @Override
        public void onNext(T t) {
            boolean drain = false;
            if (parentSubscriber.getEmitLock()) {
                try {
                    if (q.emitWithoutQueue(t, parentSubscriber.actual)) {
                        q.requestIfNeeded(this);
                    }
                } finally {
                    drain = parentSubscriber.releaseEmitLock();
                }
            } else {
                // can't emit so must enqueue
                try {
                    enqueueOnNext(t);
                    drain = true;
                } catch (MissingBackpressureException e) {
                    onError(e);
                }
            }
            if (drain) {
                /**
                 * This extra check for whether to call drain is ugly, but it helps:
                 * <pre> {@code
                 * Without:
                 * r.o.OperatorMergePerf.mergeNSyncStreamsOfN     1000  thrpt         5       61.812        1.455    ops/s
                 * 
                 * With:
                 * r.o.OperatorMergePerf.mergeNSyncStreamsOfN     1000  thrpt         5       78.795        1.766    ops/s
                 * } </pre>
                 */
                parentSubscriber.drainQueuesIfNeeded();
            }
        }

        @Override
        public void onError(Throwable e) {
            // it doesn't go through queues, it immediately onErrors and tears everything down
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                parentSubscriber.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                boolean drain = false;
                if (parentSubscriber.getEmitLock()) {
                    try {
                        // drain this queue first (before sending complete)
                        if (q != null) {
                            Object o;
                            while ((o = q.poll()) != null) {
                                q.accept(o, parentSubscriber.actual);
                            }
                        }
                        parentSubscriber.completeInner(this);
                    } finally {
                        drain = parentSubscriber.releaseEmitLock();
                    }
                } else {
                    // can't emit so must enqueue
                    enqueueComplete();
                    drain = true;
                }
                if (drain) {
                    parentSubscriber.drainQueuesIfNeeded();
                }
            }
        }

        private void enqueueOnNext(T t) throws MissingBackpressureException {
            q.onNext(t);
        }

        private void enqueueComplete() {
            q.onCompleted();
        }

    }
}
