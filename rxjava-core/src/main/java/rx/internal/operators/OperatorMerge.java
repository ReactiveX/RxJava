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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.MissingBackpressureException;
import rx.functions.Func1;
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
        final NotificationLite<T> on = NotificationLite.instance();
        final Subscriber<? super T> actual;
        private final MergeProducer<T> mergeProducer;
        private int wip;
        private boolean completed;

        private SubscriptionIndexedRingBuffer<InnerSubscriber<T>> childrenSubscribers;

        private Queue<Object> scalarValueQueue;
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
            this.actual = actual;
            this.mergeProducer = new MergeProducer<T>(this);
            // we specifically want to receive all Observables without any backpressure
            request(-1);
            // decoupled the subscription chain because we need to decouple and control backpressure
            actual.add(this);
            actual.setProducer(mergeProducer);
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
                    // synchronized here because `wip` can be concurrently changed by children Observables
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
            MergeProducer<T> producerIfNeeded = null;
            // if we have received a request then we need to respect it, otherwise we fast-path
            if (mergeProducer.requested > 0) {
                producerIfNeeded = mergeProducer;
            }
            InnerSubscriber<T> i = new InnerSubscriber<T>(this, producerIfNeeded);
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
            boolean enqueue = true;
            if (getEmitLock()) {
                try {
                    long r = mergeProducer.requested;
                    if (r == 0) {
                        // no outstanding requests
                        enqueue = true;
                    } else {
                        enqueue = false;
                    }

                    // do this within the lock
                    if (!enqueue) {
                        actual.onNext(((ScalarSynchronousObservable<T>) t).get());
                        if (r > 0) {
                            mergeProducer.REQUESTED.decrementAndGet(mergeProducer);
                        }
                        // we handle this Observable without ever incrementing the wip or touching other machinery so just return here
                        return;
                    }
                } finally {
                    if (releaseEmitLock()) {
                        drainQueuesIfNeeded();
                    }
                }
            }

            if (enqueue) {
                // enqueue the values for later delivery
                if (scalarValueQueue == null) {
                    scalarValueQueue = new SynchronizedQueue<Object>(); // look at alternative here
                }
                scalarValueQueue.offer(on.next(((ScalarSynchronousObservable<T>) t).get()));
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

        private boolean drainQueuesIfNeeded() {
            while (true) {
                if (getEmitLock()) {
                    try {
                        drainScalarValueQueue();
                        drainChildrenQueues();
                    } finally {
                        if (!releaseEmitLock()) {
                            return true;
                        }
                        // otherwise we'll loop and get whatever was added 
                    }
                } else {
                    return false;
                }
            }
        }

        int lastDrainedIndex = 0;

        /**
         * ONLY call when holding the EmitLock.
         */
        private void drainChildrenQueues() {
            if (childrenSubscribers != null) {
                lastDrainedIndex = childrenSubscribers.forEach(DRAIN_ACTION, lastDrainedIndex);
            }
        }

        /**
         * ONLY call when holding the EmitLock.
         */
        private void drainScalarValueQueue() {
            if (scalarValueQueue != null && scalarValueQueue.size() > 0) {
                long r = mergeProducer.requested;
                if (r < 0) {
                    // drain it all
                    Object o = null;
                    while ((o = scalarValueQueue.poll()) != null) {
                        on.accept(actual, o);
                    }
                } else if (r > 0) {
                    // drain what was requested
                    long toEmit = r;
                    int emittedWhileDraining = 0;
                    for (int i = 0; i < toEmit; i++) {
                        Object o = scalarValueQueue.poll();
                        if (o == null) {
                            break;
                        } else {
                            on.accept(actual, o);
                            emittedWhileDraining++;
                        }
                    }
                    // decrement the number we emitted from outstanding requests
                    mergeProducer.REQUESTED.getAndAdd(mergeProducer, -emittedWhileDraining);
                }

            }
        }

        final Func1<InnerSubscriber<T>, Boolean> DRAIN_ACTION = new Func1<InnerSubscriber<T>, Boolean>() {

            @Override
            public Boolean call(InnerSubscriber<T> s) {
                if (s.q != null) {
                    long r = mergeProducer.requested;
                    int emitted = 0;
                    if (r < 0) {
                        // drain it all
                        Object o;
                        while ((o = s.q.poll()) != null) {
                            if (s.q.isCompleted(o)) {
                                completeInner(s);
                            } else {
                                if (!s.q.accept(o, actual)) {
                                    emitted++;
                                }
                            }
                        }
                    } else if (r > 0) {
                        // drain what was requested
                        long toEmit = r;
                        Object o;
                        for (int i = 0; i < toEmit; i++) {
                            o = s.q.poll();
                            if (o == null) {
                                // no more items
                                break;
                            } else if (s.q.isCompleted(o)) {
                                completeInner(s);
                            } else {
                                if (!s.q.accept(o, actual)) {
                                    emitted++;
                                }
                            }
                        }

                        // decrement the number we emitted from outstanding requests
                        mergeProducer.REQUESTED.getAndAdd(mergeProducer, -emitted);
                    }

                    if (emitted > 0) {
                        /*
                         * `s.emitted` is not volatile (because of performance impact of making it so shown by JMH tests)
                         * but `emitted` can ONLY be touched by the thread holding the `emitLock` which we're currently inside.
                         * 
                         * Entering and leaving the emitLock flushes all values so this is visible to us.
                         */
                        emitted += s.emitted;
                        // TODO we may want to store this in s.emitted and only request if above batch
                        // reset this since we have requested them all
                        s.emitted = 0;
                        s.request(emitted);
                    }
                    if (emitted == r) {
                        // we emitted as many as were requested so stop the forEach loop
                        return Boolean.FALSE;
                    }
                }
                return Boolean.TRUE;
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

    }

    private static final class MergeProducer<T> implements Producer {

        private final MergeSubscriber<T> ms;

        public MergeProducer(MergeSubscriber<T> ms) {
            this.ms = ms;
        }

        private volatile long requested = 0;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<MergeProducer> REQUESTED = AtomicLongFieldUpdater.newUpdater(MergeProducer.class, "requested");

        @Override
        public void request(int n) {
            if (n < 0) {
                requested = -1;
            } else {
                if (REQUESTED.getAndAdd(this, n) == 0) {
                    ms.drainQueuesIfNeeded();
                }
            }
        }

    }

    private static final class InnerSubscriber<T> extends Subscriber<T> {
        public int sindex;
        final MergeSubscriber<T> parentSubscriber;
        final MergeProducer<T> producer;
        /** Make sure the inner termination events are delivered only once. */
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<InnerSubscriber> ONCE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(InnerSubscriber.class, "once");
        private final RxRingBuffer q = RxRingBuffer.getSpscInstance();
        private boolean mayNeedToDrain = false;
        /* protected by emitLock */
        int emitted = 0;

        static AtomicInteger c = new AtomicInteger();
        
        public InnerSubscriber(MergeSubscriber<T> parent, MergeProducer<T> producer) {
            this.parentSubscriber = parent;
            this.producer = producer;
            add(q);
            request(q.capacity());
        }

        @Override
        public void onNext(T t) {
            emit(t, false);
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
                emit(null, true);
            }
        }

        private void emit(T t, boolean complete) {
            boolean drain = false;
            if (parentSubscriber.getEmitLock()) {
                try {
                    // when we have the lock, nothing else can cause producer.requested to decrement, but it can increment at any time
                    if (mayNeedToDrain) {
                        // drain the queue if there is anything in it before emitting the current value
                        emitted += drainQueue();
                        mayNeedToDrain = false;
                    }
                    if (producer == null) {
                        // no backpressure requested
                        if (complete) {
                            parentSubscriber.completeInner(this);
                        } else {
                            parentSubscriber.actual.onNext(t);
                            emitted++;
                        }
                    } else {
                        if (producer.requested > 0) {
                            if (complete) {
                                parentSubscriber.completeInner(this);
                            } else {
                                parentSubscriber.actual.onNext(t);
                                emitted++;
                                producer.REQUESTED.decrementAndGet(producer);
                            }
                        } else {
                            // no requests available, so enqueue it
                            enqueue(t, complete);
                            drain = true;
                        }
                    }
                } finally {
                    drain = parentSubscriber.releaseEmitLock();
                }
                if (emitted > 256) {
                    // this is for batching requests when we're in a use case that isn't queueing, always fast-pathing the onNext
                    request(emitted);
                    // we are modifying this outside of the emit lock ... but this can be considered a "lazySet"
                    // and it will be flushed before anything else touches it because the emitLock will be obtained
                    // before any other usage of it
                    emitted = 0;
                }
            } else {
                enqueue(t, complete);
                drain = true;
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
                mayNeedToDrain = !parentSubscriber.drainQueuesIfNeeded();
            }
        }

        private void enqueue(T t, boolean complete) {
            try {
                if (complete) {
                    q.onCompleted();
                } else {
                    q.onNext(t);
                }
                mayNeedToDrain = true;
            } catch (MissingBackpressureException e) {
                onError(e);
            }
        }

        private int drainQueue() {
            int emittedWhileDraining = 0;
            if (q != null) {
                if (producer == null) {
                    Object o;
                    while ((o = q.poll()) != null) {
                        if (!q.accept(o, parentSubscriber.actual)) {
                            // non-terminal event so let's increment count
                            emittedWhileDraining++;
                        }
                    }
                } else {
                    long toEmit = producer.requested;
                    for (int i = 0; i < toEmit; i++) {
                        Object o = q.poll();
                        if (o == null) {
                            break;
                        } else {
                            if (!q.accept(o, parentSubscriber.actual)) {
                                // non-terminal event so let's increment count
                                emittedWhileDraining++;
                            }
                        }
                    }
                    // decrement the number we emitted from outstanding requests
                    producer.REQUESTED.getAndAdd(producer, -emittedWhileDraining);
                }
            }
            return emittedWhileDraining;
        }
    }
}
