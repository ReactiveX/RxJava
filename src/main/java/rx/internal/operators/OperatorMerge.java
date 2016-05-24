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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import rx.*;
import rx.Observable;
import rx.Observable.Operator;
import rx.exceptions.*;
import rx.internal.util.*;
import rx.internal.util.atomic.*;
import rx.internal.util.unsafe.*;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of {@link Observable}s into one {@code Observable}, without any transformation.
 * <p>
 * <img width="640" height="380" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/merge.png" alt="">
 * <p>
 * You can combine the items emitted by multiple {@code Observable}s so that they act like a single {@code Observable}, by using the merge operation.
 * <p>
 * The {@code instance(true)} call behaves like {@link OperatorMerge} except that if any of the merged Observables notify of
 * an error via {@code onError}, {@code mergeDelayError} will refrain from propagating that error
 * notification until all of the merged Observables have finished emitting items.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/mergeDelayError.png" alt="">
 * <p>
 * Even if multiple merged Observables send {@code onError} notifications, {@code mergeDelayError} will
 * only invoke the {@code onError} method of its Observers once.
 * <p>
 * This operation allows an Observer to receive all successfully emitted items from all of the
 * source Observables without being interrupted by an error notification from one of them.
 * <p>
 * <em>Note:</em> If this is used on an Observable that never completes, it will never call {@code onError} and will effectively swallow errors.

 * @param <T>
 *            the type of the items emitted by both the source and merged {@code Observable}s
 */
public final class OperatorMerge<T> implements Operator<T, Observable<? extends T>> {
    /** Lazy initialization via inner-class holder. */
    private static final class HolderNoDelay {
        /** A singleton instance. */
        static final OperatorMerge<Object> INSTANCE = new OperatorMerge<Object>(false, Integer.MAX_VALUE);
    }
    /** Lazy initialization via inner-class holder. */
    private static final class HolderDelayErrors {
        /** A singleton instance. */
        static final OperatorMerge<Object> INSTANCE = new OperatorMerge<Object>(true, Integer.MAX_VALUE);
    }
    /**
     * @param <T> the common value type
     * @param delayErrors should the merge delay errors?
     * @return a singleton instance of this stateless operator.
     */
    @SuppressWarnings("unchecked")
    public static <T> OperatorMerge<T> instance(boolean delayErrors) {
        if (delayErrors) {
            return (OperatorMerge<T>)HolderDelayErrors.INSTANCE;
        }
        return (OperatorMerge<T>)HolderNoDelay.INSTANCE;
    }
    /**
     * Creates a new instance of the operator with the given delayError and maxConcurrency settings.
     * @param <T> the value type
     * @param delayErrors
     * @param maxConcurrent the maximum number of concurrent subscriptions or Integer.MAX_VALUE for unlimited
     * @return the Operator instance with the given settings
     */
    public static <T> OperatorMerge<T> instance(boolean delayErrors, int maxConcurrent) {
        if (maxConcurrent <= 0) {
            throw new IllegalArgumentException("maxConcurrent > 0 required but it was " + maxConcurrent);
        }
        if (maxConcurrent == Integer.MAX_VALUE) {
            return instance(delayErrors);
        }
        return new OperatorMerge<T>(delayErrors, maxConcurrent);
    }

    final boolean delayErrors;
    final int maxConcurrent;

    OperatorMerge(boolean delayErrors, int maxConcurrent) {
        this.delayErrors = delayErrors;
        this.maxConcurrent = maxConcurrent;
    }

    @Override
    public Subscriber<Observable<? extends T>> call(final Subscriber<? super T> child) {
        MergeSubscriber<T> subscriber = new MergeSubscriber<T>(child, delayErrors, maxConcurrent);
        MergeProducer<T> producer = new MergeProducer<T>(subscriber);
        subscriber.producer = producer;
        
        child.add(subscriber);
        child.setProducer(producer);
        
        return subscriber;
    }

    static final class MergeProducer<T> extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -1214379189873595503L;

        final MergeSubscriber<T> subscriber;
        
        public MergeProducer(MergeSubscriber<T> subscriber) {
            this.subscriber = subscriber;
        }
        
        @Override
        public void request(long n) {
            if (n > 0) {
                if (get() == Long.MAX_VALUE) {
                    return;
                }
                BackpressureUtils.getAndAddRequest(this, n);
                subscriber.emit();
            } else 
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required");
            }
        }
        public long produced(int n) {
            return addAndGet(-n);
        }
    }
    
    /**
     * The subscriber that observes Observables. 
     * @param <T> the value type
     */
    static final class MergeSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final Subscriber<? super T> child;
        final boolean delayErrors;
        final int maxConcurrent;
        
        MergeProducer<T> producer;
        
        volatile Queue<Object> queue;
        
        /** Tracks the active subscriptions to sources. */
        volatile CompositeSubscription subscriptions;
        /** Due to the emission loop, we need to store errors somewhere if !delayErrors. */
        volatile ConcurrentLinkedQueue<Throwable> errors;
        
        final NotificationLite<T> nl;
        
        volatile boolean done;
        
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        
        final Object innerGuard;
        /** Copy-on-write array, guarded by innerGuard. */
        volatile InnerSubscriber<?>[] innerSubscribers;
        
        /** Used to generate unique InnerSubscriber IDs. Modified from onNext only. */
        long uniqueId;
        
        /** Which was the last InnerSubscriber that emitted? Accessed if emitting == true. */
        long lastId;
        /** What was its index in the innerSubscribers array? Accessed if emitting == true. */
        int lastIndex;
        
        /** An empty array to avoid creating new empty arrays in removeInner. */ 
        static final InnerSubscriber<?>[] EMPTY = new InnerSubscriber<?>[0];

        final int scalarEmissionLimit;
        
        int scalarEmissionCount;
        
        public MergeSubscriber(Subscriber<? super T> child, boolean delayErrors, int maxConcurrent) {
            this.child = child;
            this.delayErrors = delayErrors;
            this.maxConcurrent = maxConcurrent;
            this.nl = NotificationLite.instance();
            this.innerGuard = new Object();
            this.innerSubscribers = EMPTY;
            if (maxConcurrent == Integer.MAX_VALUE) {
                scalarEmissionLimit = Integer.MAX_VALUE;
                request(Long.MAX_VALUE);
            } else {
                scalarEmissionLimit = Math.max(1, maxConcurrent >> 1);
                request(maxConcurrent);
            }
        }
        
        Queue<Throwable> getOrCreateErrorQueue() {
            ConcurrentLinkedQueue<Throwable> q = errors;
            if (q == null) {
                synchronized (this) {
                    q = errors;
                    if (q == null) {
                        q = new ConcurrentLinkedQueue<Throwable>();
                        errors = q;
                    }
                }
            }
            return q;
        }
        CompositeSubscription getOrCreateComposite() {
            CompositeSubscription c = subscriptions;
            if (c == null) {
                boolean shouldAdd = false;
                synchronized (this) {
                    c = subscriptions;
                    if (c == null) {
                        c = new CompositeSubscription();
                        subscriptions = c;
                        shouldAdd = true;
                    }
                }
                if (shouldAdd) {
                    add(c);
                }
            }
            return c;
        }
        
        @Override
        public void onNext(Observable<? extends T> t) {
            if (t == null) {
                return;
            }
            if (t == Observable.empty()) {
                emitEmpty();
            } else
            if (t instanceof ScalarSynchronousObservable) {
                tryEmit(((ScalarSynchronousObservable<? extends T>)t).get());
            } else {
                InnerSubscriber<T> inner = new InnerSubscriber<T>(this, uniqueId++);
                addInner(inner);
                t.unsafeSubscribe(inner);
                emit();
            }
        }
        
        void emitEmpty() {
            int produced = scalarEmissionCount + 1;
            if (produced == scalarEmissionLimit) {
                scalarEmissionCount = 0;
                this.requestMore(produced);
            } else {
                scalarEmissionCount = produced;
            }
        }
        
        private void reportError() {
            List<Throwable> list = new ArrayList<Throwable>(errors);
            if (list.size() == 1) {
                child.onError(list.get(0));
            } else {
                child.onError(new CompositeException(list));
            }
        }
        
        @Override
        public void onError(Throwable e) {
            getOrCreateErrorQueue().offer(e);
            done = true;
            emit();
        }
        @Override
        public void onCompleted() {
            done = true;
            emit();
        }
        
        void addInner(InnerSubscriber<T> inner) {
            getOrCreateComposite().add(inner);
            synchronized (innerGuard) {
                InnerSubscriber<?>[] a = innerSubscribers;
                int n = a.length;
                InnerSubscriber<?>[] b = new InnerSubscriber<?>[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                innerSubscribers = b;
            }
        }
        void removeInner(InnerSubscriber<T> inner) {
            RxRingBuffer q = inner.queue;
            if (q != null) {
                q.release();
            }
            // subscription is non-null here because the very first addInner will create it before
            // this can be called
            subscriptions.remove(inner);
            synchronized (innerGuard) {
                InnerSubscriber<?>[] a = innerSubscribers;
                int n = a.length;
                int j = -1;
                // locate the inner
                for (int i = 0; i < n; i++) {
                    if (inner.equals(a[i])) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                if (n == 1) {
                    innerSubscribers = EMPTY;
                    return;
                }
                InnerSubscriber<?>[] b = new InnerSubscriber<?>[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                innerSubscribers = b;
            }
        }
        
        /**
         * Tries to emit the value directly to the child if
         * no concurrent emission is happening at the moment.
         * <p>
         * Since the scalar-value queue optimization applies
         * to both the main source and the inner subscribers,
         * we handle things in a shared manner.
         * 
         * @param subscriber
         * @param value
         */
        void tryEmit(InnerSubscriber<T> subscriber, T value) {
            boolean success = false;
            long r = producer.get();
            if (r != 0L) {
                synchronized (this) {
                    // if nobody is emitting and child has available requests
                    r = producer.get();
                    if (!emitting && r != 0L) {
                        emitting = true;
                        success = true;
                    }
                }
            }
            if (success) {
                emitScalar(subscriber, value, r);
            } else {
                queueScalar(subscriber, value);
            }
        }

        protected void queueScalar(InnerSubscriber<T> subscriber, T value) {
            /*
             * If the attempt to make a fast-path emission failed
             * due to lack of requests or an ongoing emission,
             * enqueue the value and try the slow emission path.
             */
            RxRingBuffer q = subscriber.queue;
            if (q == null) {
                q = RxRingBuffer.getSpscInstance();
                subscriber.add(q);
                subscriber.queue = q;
            }
            try {
                q.onNext(nl.next(value));
            } catch (MissingBackpressureException ex) {
                subscriber.unsubscribe();
                subscriber.onError(ex);
                return;
            } catch (IllegalStateException ex) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.unsubscribe();
                    subscriber.onError(ex);
                }
                return;
            }
            emit();
        }

        protected void emitScalar(InnerSubscriber<T> subscriber, T value, long r) {
            boolean skipFinal = false;
            try {
                try {
                    child.onNext(value);
                } catch (Throwable t) {
                    if (!delayErrors) {
                        Exceptions.throwIfFatal(t);
                        skipFinal = true;
                        subscriber.unsubscribe();
                        subscriber.onError(t);
                        return;
                    }
                    getOrCreateErrorQueue().offer(t);
                }
                if (r != Long.MAX_VALUE) {
                    producer.produced(1);
                }
                subscriber.requestMore(1);
                // check if some state changed while emitting
                synchronized (this) {
                    skipFinal = true;
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
            /*
             * In the synchronized block below request(1) we check
             * if there was a concurrent emission attempt and if there was,
             * we stay in emission mode and enter the emission loop
             * which will take care all the queued up state and 
             * emission possibilities.
             */
            emitLoop();
        }

        public void requestMore(long n) {
            request(n);
        }
        
        /**
         * Tries to emit the value directly to the child if
         * no concurrent emission is happening at the moment.
         * <p>
         * Since the scalar-value queue optimization applies
         * to both the main source and the inner subscribers,
         * we handle things in a shared manner.
         * 
         * @param subscriber
         * @param value
         */
        void tryEmit(T value) {
            boolean success = false;
            long r = producer.get();
            if (r != 0L) {
                synchronized (this) {
                    // if nobody is emitting and child has available requests
                    r = producer.get();
                    if (!emitting && r != 0L) {
                        emitting = true;
                        success = true;
                    }
                }
            }
            if (success) {
                emitScalar(value, r);
            } else {
                queueScalar(value);
            }
        }

        protected void queueScalar(T value) {
            /*
             * If the attempt to make a fast-path emission failed
             * due to lack of requests or an ongoing emission,
             * enqueue the value and try the slow emission path.
             */
            Queue<Object> q = this.queue;
            if (q == null) {
                int mc = maxConcurrent;
                if (mc == Integer.MAX_VALUE) {
                    q = new SpscUnboundedAtomicArrayQueue<Object>(RxRingBuffer.SIZE);
                } else {
                    if (Pow2.isPowerOfTwo(mc)) {
                        if (UnsafeAccess.isUnsafeAvailable()) {
                            q = new SpscArrayQueue<Object>(mc);
                        } else {
                            q = new SpscAtomicArrayQueue<Object>(mc);
                        }
                    } else {
                        q = new SpscExactAtomicArrayQueue<Object>(mc);
                    }
                }
                this.queue = q;
            }
            if (!q.offer(nl.next(value))) {
                unsubscribe();
                onError(OnErrorThrowable.addValueAsLastCause(new MissingBackpressureException(), value));
                return;
            }
            emit();
        }

        protected void emitScalar(T value, long r) {
            boolean skipFinal = false;
            try {
                try {
                    child.onNext(value);
                } catch (Throwable t) {
                    if (!delayErrors) {
                        Exceptions.throwIfFatal(t);
                        skipFinal = true;
                        this.unsubscribe();
                        this.onError(t);
                        return;
                    }
                    getOrCreateErrorQueue().offer(t);
                }
                if (r != Long.MAX_VALUE) {
                    producer.produced(1);
                }
                
                int produced = scalarEmissionCount + 1;
                if (produced == scalarEmissionLimit) {
                    scalarEmissionCount = 0;
                    this.requestMore(produced);
                } else {
                    scalarEmissionCount = produced;
                }
                
                // check if some state changed while emitting
                synchronized (this) {
                    skipFinal = true;
                    if (!missed) {
                        emitting = false;
                        return;
                    }
                    missed = false;
                }
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
            /*
             * In the synchronized block below request(1) we check
             * if there was a concurrent emission attempt and if there was,
             * we stay in emission mode and enter the emission loop
             * which will take care all the queued up state and 
             * emission possibilities.
             */
            emitLoop();
        }
        
        void emit() {
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            emitLoop();
        }
        /**
         * The standard emission loop serializing events and requests.
         */
        void emitLoop() {
            boolean skipFinal = false;
            try {
                final Subscriber<? super T> child = this.child;
                for (;;) {
                    // eagerly check if child unsubscribed or we reached a terminal state.
                    if (checkTerminate()) {
                        skipFinal = true;
                        return;
                    }
                    Queue<Object> svq = queue;
                    
                    long r = producer.get();
                    boolean unbounded = r == Long.MAX_VALUE;
                    
                    // count the number of 'completed' sources to replenish them in batches
                    int replenishMain = 0;

                    // try emitting as many scalars as possible
                    if (svq != null) {
                        for (;;) {
                            int scalarEmission = 0;
                            Object o = null;
                            while (r > 0) {
                                o = svq.poll();
                                // eagerly check if child unsubscribed or we reached a terminal state.
                                if (checkTerminate()) {
                                    skipFinal = true;
                                    return;
                                }
                                if (o == null) {
                                    break;
                                }
                                T v = nl.getValue(o);
                                // if child throws, report bounce it back immediately
                                try {
                                    child.onNext(v);
                                } catch (Throwable t) {
                                    if (!delayErrors) {
                                        Exceptions.throwIfFatal(t);
                                        skipFinal = true;
                                        unsubscribe();
                                        child.onError(t);
                                        return;
                                    }
                                    getOrCreateErrorQueue().offer(t);
                                }
                                replenishMain++;
                                scalarEmission++;
                                r--;
                            }
                            if (scalarEmission > 0) {
                                if (unbounded) {
                                    r = Long.MAX_VALUE;
                                } else {
                                    r = producer.produced(scalarEmission);
                                }
                            }
                            if (r == 0L || o == null) {
                                break;
                            }
                        }
                    }

                    /*
                     * We need to read done before innerSubscribers because innerSubscribers are added
                     * before done is set to true. If it were the other way around, we could read an empty
                     * innerSubscribers, get paused and then read a done flag but an async producer
                     * might have added more subscribers between the two.
                     */
                    boolean d = done;
                    // re-read svq because it could have been created
                    // asynchronously just before done was set to true.
                    svq = queue;
                    // read the current set of inner subscribers
                    InnerSubscriber<?>[] inner = innerSubscribers;
                    int n = inner.length;
                    
                    // check if upstream is done, there are no scalar values 
                    // and no active inner subscriptions
                    if (d && (svq == null || svq.isEmpty()) && n == 0) {
                        Queue<Throwable> e = errors;
                        if (e == null || e.isEmpty()) {
                            child.onCompleted();
                        } else {
                            reportError();
                        }
                        skipFinal = true;
                        return;
                    }
                    
                    boolean innerCompleted = false;
                    if (n > 0) {
                        // let's continue the round-robin emission from last location
                        long startId = lastId;
                        int index = lastIndex;
                        
                        // in case there were changes in the array or the index
                        // no longer points to the inner with the cached id
                        if (n <= index || inner[index].id != startId) {
                            if (n <= index) {
                                index = 0;
                            }
                            // try locating the inner with the cached index
                            int j = index;
                            for (int i = 0; i < n; i++) {
                                if (inner[j].id == startId) {
                                    break;
                                }
                                // wrap around in round-robin fashion
                                j++;
                                if (j == n) {
                                    j = 0;
                                }
                            }
                            // if we found it again, j will point to it
                            // otherwise, we continue with the replacement at j
                            index = j;
                            lastIndex = j;
                            lastId = inner[j].id;
                        }
                        
                        int j = index;
                        // loop through all sources once to avoid delaying any new sources too much
                        for (int i = 0; i < n; i++) {
                            // eagerly check if child unsubscribed or we reached a terminal state.
                            if (checkTerminate()) {
                                skipFinal = true;
                                return;
                            }
                            @SuppressWarnings("unchecked")
                            InnerSubscriber<T> is = (InnerSubscriber<T>)inner[j];
                            
                            Object o = null;
                            for (;;) {
                                int produced = 0;
                                while (r > 0) {
                                    // eagerly check if child unsubscribed or we reached a terminal state.
                                    if (checkTerminate()) {
                                        skipFinal = true;
                                        return;
                                    }
                                    RxRingBuffer q = is.queue;
                                    if (q == null) {
                                        break;
                                    }
                                    o = q.poll();
                                    if (o == null) {
                                        break;
                                    }
                                    T v = nl.getValue(o);
                                    // if child throws, report bounce it back immediately
                                    try {
                                        child.onNext(v);
                                    } catch (Throwable t) {
                                        skipFinal = true;
                                        Exceptions.throwIfFatal(t);
                                        try {
                                            child.onError(t);
                                        } finally {
                                            unsubscribe();
                                        }
                                        return;
                                    }
                                    r--;
                                    produced++;
                                }
                                if (produced > 0) {
                                    if (!unbounded) {
                                        r = producer.produced(produced);
                                    } else {
                                        r = Long.MAX_VALUE;
                                    }
                                    is.requestMore(produced);
                                }
                                // if we run out of requests or queued values, break
                                if (r == 0 || o == null) {
                                    break;
                                }
                            }
                            boolean innerDone = is.done;
                            RxRingBuffer innerQueue = is.queue;
                            if (innerDone && (innerQueue == null || innerQueue.isEmpty())) {
                                removeInner(is);
                                if (checkTerminate()) {
                                    skipFinal = true;
                                    return;
                                }
                                replenishMain++;
                                innerCompleted = true;
                            }
                            // if we run out of requests, don't try the other sources
                            if (r == 0) {
                                break;
                            }
                            
                            // wrap around in round-robin fashion
                            j++;
                            if (j == n) {
                                j = 0;
                            }
                        }
                        // if we run out of requests or just completed a round, save the index and id
                        lastIndex = j;
                        lastId = inner[j].id;
                    }
                    
                    if (replenishMain > 0) {
                        request(replenishMain);
                    }
                    // if one or more inner completed, loop again to see if we can terminate the whole stream
                    if (innerCompleted) {
                        continue;
                    }
                    // in case there were updates to the state, we loop again
                    synchronized (this) {
                        if (!missed) {
                            skipFinal = true;
                            emitting = false;
                            break;
                        }
                        missed = false;
                    }
                }
            } finally {
                if (!skipFinal) {
                    synchronized (this) {
                        emitting = false;
                    }
                }
            }
        }
        
        /**
         * Check if the operator reached some terminal state: child unsubscribed,
         * an error was reported and we don't delay errors.
         * @return true if the child unsubscribed or there are errors available and merge doesn't delay errors.
         */
        boolean checkTerminate() {
            if (child.isUnsubscribed()) {
                return true;
            }
            Queue<Throwable> e = errors;
            if (!delayErrors && (e != null && !e.isEmpty())) {
                try {
                    reportError();
                } finally {
                    unsubscribe();
                }
                return true;
            }
            return false;
        }
    }
    static final class InnerSubscriber<T> extends Subscriber<T> {
        final MergeSubscriber<T> parent;
        final long id;
        volatile boolean done;
        volatile RxRingBuffer queue;
        int outstanding;
        static final int limit = RxRingBuffer.SIZE / 4;
        
        public InnerSubscriber(MergeSubscriber<T> parent, long id) {
            this.parent = parent;
            this.id = id;
        }
        @Override
        public void onStart() {
            outstanding = RxRingBuffer.SIZE;
            request(RxRingBuffer.SIZE);
        }
        @Override
        public void onNext(T t) {
            parent.tryEmit(this, t);
        }
        @Override
        public void onError(Throwable e) {
            done = true;
            parent.getOrCreateErrorQueue().offer(e);
            parent.emit();
        }
        @Override
        public void onCompleted() {
            done = true;
            parent.emit();
        }
        public void requestMore(long n) {
            int r = outstanding - (int)n;
            if (r > limit) {
                outstanding = r;
                return;
            }
            outstanding = RxRingBuffer.SIZE;
            int k = RxRingBuffer.SIZE - r;
            if (k > 0) {
                request(k);
            }
        }
    }}