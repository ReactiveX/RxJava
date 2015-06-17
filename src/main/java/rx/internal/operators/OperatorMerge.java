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
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.Operator;
import rx.Observable;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.internal.util.*;
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
public final class OperatorMerge<T, R> implements Operator<R, T> {
    /**
     * Creates a new instance of the operator with the given delayError and maxConcurrency settings.
     * @param delayErrors
     * @param maxConcurrent the maximum number of concurrent subscriptions or Integer.MAX_VALUE for unlimited
     * @param func the mapper function that maps a value into an observable
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> OperatorMerge<T, R> instance(boolean delayErrors, int maxConcurrent, 
            Func1<? super T, ? extends Observable<? extends R>> func) {
        if (maxConcurrent == Integer.MAX_VALUE && (Func1)func == UtilityFunctions.identity()) {
            if (delayErrors) {
                return (OperatorMerge<T, R>)HolderDelayErrors.INSTANCE;
            }
            return (OperatorMerge<T, R>)HolderPlain.INSTANCE;
        }
        return new OperatorMerge<T, R>(delayErrors, maxConcurrent, func);
    }
    
    static final class HolderPlain {
        @SuppressWarnings({"rawtypes", "unchecked"})
        static final OperatorMerge<Object, Object> INSTANCE = new OperatorMerge<Object, Object>(false, Integer.MAX_VALUE, (Func1)UtilityFunctions.identity());
    }
    static final class HolderDelayErrors {
        @SuppressWarnings({"rawtypes", "unchecked"})
        static final OperatorMerge<Object, Object> INSTANCE = new OperatorMerge<Object, Object>(true, Integer.MAX_VALUE, (Func1)UtilityFunctions.identity());
    }

    final boolean delayErrors;
    final int maxConcurrent;
    final Func1<? super T, ? extends Observable<? extends R>> func;

    private OperatorMerge(boolean delayErrors, int maxConcurrent, 
            Func1<? super T, ? extends Observable<? extends R>> func) {
        this.delayErrors = delayErrors;
        this.maxConcurrent = maxConcurrent;
        this.func = func;
    }

    @Override
    public Subscriber<T> call(final Subscriber<? super R> child) {
        MergeSubscriber<T, R> subscriber = new MergeSubscriber<T, R>(child, delayErrors, 
                maxConcurrent, func);
        MergeProducer<T, R> producer = new MergeProducer<T, R>(subscriber);
        subscriber.producer = producer;
        producer.lazySet(0); // make sure subscriber.producer is visible
        
        child.add(subscriber);
        child.setProducer(producer);
        
        return subscriber;
    }

    static final class MergeProducer<T, R> extends AtomicLong implements Producer {
        /** */
        private static final long serialVersionUID = -1214379189873595503L;

        final MergeSubscriber<T, R> subscriber;
        /** Indicates the producer mode: -1 no request, 0 unbounded fast, 1 bounded. */
        volatile int mode;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MergeProducer> MODE
        = AtomicIntegerFieldUpdater.newUpdater(MergeProducer.class, "mode");
        
        public MergeProducer(MergeSubscriber<T, R> subscriber) {
            this.subscriber = subscriber;
            this.mode = -1;
        }
        
        @Override
        public void request(long n) {
            if (n >= 0) {
                int m = mode;
                // if in unbounded mode, quit
                if (m == 0) {
                    return;
                }
                // if bounded mode, update request count and emit
                if (m == 1) {
                    BackpressureUtils.getAndAddRequest(this, n);
                    subscriber.emit();
                    return;
                }
                // mode has not been set
                if (n == Long.MAX_VALUE) {
                    // try setting mode to unbounded
                    if (MODE.compareAndSet(this, -1, 0)) {
                        subscriber.requestAll();
                        return;
                    }
                } else {
                    if (MODE.compareAndSet(this, -1, 1)) {
                        BackpressureUtils.getAndAddRequest(this, n);
                        subscriber.requestDefault();
                        return;
                    }
                }
                // mode has changed concurrently
                // recheck state
                m = mode;
                // if in unbounded mode, quit
                if (m == 0) {
                    return;
                } else
                // if bounded mode, update request count and emit
                if (m == 1) {
                    BackpressureUtils.getAndAddRequest(this, n);
                    subscriber.emit();
                    return;
                }
                throw new IllegalStateException("Shouldn't be in mode: " + m);
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
    static final class MergeSubscriber<T, R> extends Subscriber<T> {
        final Subscriber<? super R> child;
        final boolean delayErrors;
        final int maxConcurrent;
        final Func1<? super T, ? extends Observable<? extends R>> func;
        
        MergeProducer<T, R> producer;
        
        /** Upstream might not produce scalar values at all so avoid creating a queue for it upfront. */
        volatile RxRingBuffer scalarQueue;
        /** Tracks the active subscriptions to sources. */
        volatile CompositeSubscription subscriptions;
        /** Due to the emission loop, we need to store errors somewhere if !delayErrors. */
        volatile ConcurrentLinkedQueue<Throwable> errors;
        
        final NotificationLite<R> nl;
        
        volatile boolean done;
        
        /** Guarded by this. */
        boolean emitting;
        /** Guarded by this. */
        boolean missed;
        
        final Object innerGuard;
        /** Copy-on-write array, guarded by innerGuard. */
        volatile InnerSubscriber<?, ?>[] innerSubscribers;
        
        /** Used to generate unique InnerSubscriber IDs. Modified from onNext only. */
        long uniqueId;
        
        /** Which was the last InnerSubscriber that emitted? Accessed if emitting == true. */
        long lastId;
        /** What was its index in the innerSubscribers array? Accessed if emitting == true. */
        int lastIndex;
        
        /** An empty array to avoid creating new empty arrays in removeInner. */ 
        static final InnerSubscriber<?, ?>[] EMPTY = new InnerSubscriber<?, ?>[0];

        /** In fast mode, this counts the number of active subscriptions, including the one to upstream. */
        volatile int fastWip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<MergeSubscriber> FAST_WIP
        = AtomicIntegerFieldUpdater.newUpdater(MergeSubscriber.class, "fastWip");
        
        volatile boolean fastMode;
        
        /** Guarded by this. */
        Object[] fastArray;
        /** Guarded by this. */
        int fastSize;
        
        /** Fast, unbounded emission report contents of 'errors'. */
        static final Object REPORT_ERROR = new Object();
        /** Fast, unbounded emission complete. */
        static final Object REPORT_COMPLETED = new Object();
        
        public MergeSubscriber(Subscriber<? super R> child, boolean delayErrors, 
                int maxConcurrent, Func1<? super T, ? extends Observable<? extends R>> func) {
            this.child = child;
            this.delayErrors = delayErrors;
            this.maxConcurrent = maxConcurrent;
            this.nl = NotificationLite.instance();
            this.innerGuard = new Object();
            this.innerSubscribers = EMPTY;
            this.func = func;
            this.request(0);
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
        
        /** 
         * In case the downstream requests less than Long.MAX_VALUE initially, do the standard
         * queued processing.
         */
        void requestDefault() {
            request(Math.min(maxConcurrent, RxRingBuffer.SIZE));
        }
        /**
         * In case the downstream requests Long.MAX_VALUE, perform a simplified merging without
         * using queues at all.
         */
        void requestAll() {
            FAST_WIP.lazySet(this, 1);
            fastMode = true;
            request(maxConcurrent == Integer.MAX_VALUE ? Long.MAX_VALUE : maxConcurrent);
        }
        
        @Override
        public void onNext(T value) {
            
            Observable<? extends R> t;
            try {
                t = func.call(value);
            } catch (Throwable e) {
                onError(e);
                return;
            }
            if (t == null) {
                return;
            }
            if (fastMode) {
                if (t.getClass() == ScalarSynchronousObservable.class) {
                    R v = ((ScalarSynchronousObservable<? extends R>)t).get();
                    fastEmitNext(v);
                    if (maxConcurrent != Integer.MAX_VALUE) {
                        request(1);
                    }
                } else {
                    FastInnerSubscriber<T, R> fastInner = new FastInnerSubscriber<T, R>(this);
                    getOrCreateComposite().add(fastInner);
                    FAST_WIP.getAndIncrement(this);
                    t.unsafeSubscribe(fastInner);
                }
            } else {
                if (t.getClass() == ScalarSynchronousObservable.class) {
                    tryEmitScalar(((ScalarSynchronousObservable<? extends R>)t).get());
                } else {
                    InnerSubscriber<T, R> inner = new InnerSubscriber<T, R>(this, uniqueId++);
                    addInner(inner);
                    t.unsafeSubscribe(inner);
                    emit();
                }
            }
        }
        
        void innerError(Throwable e, FastInnerSubscriber<T, R> fastInner) {
            getOrCreateErrorQueue().offer(e);
            subscriptions.remove(fastInner);
            int n = FAST_WIP.decrementAndGet(this);
            // if we don't delay errors or this was the very last event
            if (!delayErrors || n == 0) {
                try {
                    fastEmitError();
                } finally {
                    unsubscribe();
                }
            } else {
                request(1);
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
        
        
        void innerComplete(FastInnerSubscriber<T, R> fastInner) {
            subscriptions.remove(fastInner);
            if (FAST_WIP.decrementAndGet(this) == 0) {
                Queue<Throwable> e = errors;
                if (e == null || e.isEmpty()) {
                    fastEmitCompleted();
                } else {
                    fastEmitError();
                }
            } else {
                request(1);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            getOrCreateErrorQueue().offer(e);
            if (fastMode) {
                int n = FAST_WIP.decrementAndGet(this);
                // if we don't delay errors or this was the very last event
                if (!delayErrors || n == 0) {
                    fastEmitError();
                }
            } else {
                done = true;
                emit();
            }
        }
        @Override
        public void onCompleted() {
            if (fastMode) {
                if (FAST_WIP.decrementAndGet(this) == 0) {
                    Queue<Throwable> e = errors;
                    if (e == null || e.isEmpty()) {
                        fastEmitCompleted();
                    } else {
                        reportError();
                    }
                }
            } else {
                done = true;
                emit();
            }
        }
        
        void addInner(InnerSubscriber<T, R> inner) {
            getOrCreateComposite().add(inner);
            synchronized (innerGuard) {
                InnerSubscriber<?, ?>[] a = innerSubscribers;
                int n = a.length;
                InnerSubscriber<?, ?>[] b = new InnerSubscriber<?, ?>[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                innerSubscribers = b;
            }
        }
        void removeInner(InnerSubscriber<T, R> inner) {
            inner.queue.release();
            // subscription is non-null here because the very first addInner will create it before
            // this can be called
            subscriptions.remove(inner);
            synchronized (innerGuard) {
                InnerSubscriber<?, ?>[] a = innerSubscribers;
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
                InnerSubscriber<?, ?>[] b = new InnerSubscriber<?, ?>[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                innerSubscribers = b;
            }
        }
        
        /**
         * Tries a fast-path emission of a scalar value or posts it to the scalar queue
         * for processing later.
         * @param value
         */
        void tryEmitScalar(R value) {
            boolean success = false;
            synchronized (this) {
                // if nobody is emitting and child has available requests
                if (!emitting && producer.get() > 0) {
                    emitting = true;
                    success = true;
                }
            }
            if (success) {
                boolean skipFinal = false;
                try {
                    try {
                        child.onNext(value);
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
                    producer.produced(1);
                    request(1);
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
                // in case there was some activity, continue with the regular emission loop
                emitLoop();
            } else {
                // either someone else is emitting or there was no available request
                // queue up the scalar
                RxRingBuffer svq = scalarQueue;
                if (svq == null) {
                    svq = RxRingBuffer.getSpscInstance();
                    add(svq);
                    scalarQueue = svq;
                }
                try {
                    svq.onNext(nl.next(value));
                } catch (MissingBackpressureException ex) {
                    onError(ex);
                    return;
                } catch (IllegalStateException ex) {
                    // if there was an IAE and we are unsubscribed, just quit
                    if (!isUnsubscribed()) {
                        onError(ex);
                    }
                    return;
                }
                // then try to emit it (or notify the running emission loop).
                emit();
            }
        }
        
        void fastEmitNext(R value) {
            synchronized (this) {
                if (emitting) {
                    int s = fastSize;
                    Object[] o = fastArray;
                    if (o == null) {
                        o = new Object[4];
                        fastArray = o;
                    } else
                    if (s == o.length) {
                        Object[] o2 = new Object[s + (s >> 2)];
                        System.arraycopy(o, 0, o2, 0, s);
                        fastArray = o2;
                        o = o2;
                    }
                    o[s] = value;
                    fastSize = s + 1;
                    missed = true;
                    return;
                }
                emitting = true;
            }
            boolean skipFinal = false;
            try {
                child.onNext(value);
                for (int k = Integer.MAX_VALUE; k >= 0; k--) {
                    Object[] a;
                    int s;
                    synchronized (this) {
                        if (!missed) {
                            emitting = false;
                            skipFinal = true;
                            return;
                        }
                        a = fastArray;
                        s = fastSize;
                        fastArray = null;
                        fastSize = 0;
                        missed = false;
                    }
                    for (int i = 0; i < s; i++) {
                        Object v = a[i];
                        if (v == REPORT_ERROR) {
                            reportError();
                            skipFinal = true;
                            return;
                        } else
                        if (v == REPORT_COMPLETED) {
                            child.onCompleted();
                            skipFinal = true;
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        R v2 = (R)v;
                        child.onNext(v2);
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
        void fastEmitError() {
            synchronized (this) {
                if (emitting) {
                    Object[] o = new Object[1];
                    o[0] = REPORT_ERROR;
                    fastArray = o;
                    fastSize = 1;
                    missed = true;
                    return;
                }
                emitting = true;
            }
            reportError();
            // terminal event, no need to loop or stop emitting
        }
        void fastEmitCompleted() {
            synchronized (this) {
                if (emitting) {
                    int s = fastSize;
                    Object[] o = fastArray;
                    if (o == null) {
                        o = new Object[4];
                        fastArray = o;
                    } else
                    if (s == o.length) {
                        Object[] o2 = new Object[s + (s >> 2)];
                        System.arraycopy(o, 0, o2, 0, s);
                        fastArray = o;
                    }
                    o[s] = REPORT_COMPLETED;
                    fastSize = s+ 1;
                    missed = true;
                    return;
                }
                emitting = true;
            }
            // if we get here, fastArray is always null because fastEmitNext always empties the arrays
            child.onCompleted();
         // terminal event, no need to loop or stop emitting
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
                final Subscriber<? super R> child = this.child;
                for (;;) {
                    // eagerly check if child unsubscribed or we reached a terminal state.
                    if (checkTerminate()) {
                        skipFinal = true;
                        return;
                    }
                    RxRingBuffer svq = scalarQueue;
                    
                    long r = producer.get();
                    
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
                                R v = nl.getValue(o);
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
                                replenishMain++;
                                scalarEmission++;
                                r--;
                            }
                            if (scalarEmission > 0) {
                                r = producer.produced(scalarEmission);
                            }
                            if (r == 0L || o == null) {
                                break;
                            }
                        }
                    }

                    /*
                     * We need to read done before innerSubscribers because innerSubcribers are added
                     * before done is set to true. If it were the other way around, we could read an empty
                     * innerSubscribers, get paused and then read a done flag but an async producer
                     * might have added more subscribers between the two.
                     */
                    boolean d = done;
                    InnerSubscriber<?, ?>[] inner = innerSubscribers;
                    int n = inner.length;
                    
                    // check if upstream is done, there are no scalar values and no active inner subscriptions
                    if (d && (svq == null || svq.isEmpty()) && n == 0) {
                        Queue<Throwable> e = errors;
                        if (e == null || e.isEmpty()) {
                            child.onCompleted();
                        } else {
                            reportError();
                        }
                        if (svq != null) {
                            svq.release();
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
                            InnerSubscriber<T, R> is = (InnerSubscriber<T, R>)inner[j];
                            
                            Object o = null;
                            for (;;) {
                                int produced = 0;
                                while (r > 0) {
                                    // eagerly check if child unsubscribed or we reached a terminal state.
                                    if (checkTerminate()) {
                                        skipFinal = true;
                                        return;
                                    }
                                    o = is.queue.poll();
                                    if (o == null) {
                                        break;
                                    }
                                    R v = nl.getValue(o);
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
                                    r = producer.produced(produced);
                                    is.requestMore(produced);
                                }
                                // if we run out of requests or queued values, break
                                if (r == 0 || o == null) {
                                    break;
                                }
                            }
                            if (is.done && is.queue.isEmpty()) {
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
         * an error was reported and we don't delay errors or simply the main
         * source finished and no scalar values are queued.
         * @param scalarEmpty true if the scalar value queue is empty (using parameter to avoid re-reading scalarQueue).
         * @return true if a terminal state is reached and the emission loop should quit.
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
        
        static final class InnerSubscriber<T, R> extends Subscriber<R> {
            final RxRingBuffer queue;
            final MergeSubscriber<T, R> parent;
            final long id;
            volatile boolean done;
            public InnerSubscriber(MergeSubscriber<T, R> parent, long id) {
                this.queue = RxRingBuffer.getSpscInstance();
                this.parent = parent;
                this.id = id;
                this.add(queue);
            }
            @Override
            public void onStart() {
                request(RxRingBuffer.SIZE);
            }
            @Override
            public void onNext(R t) {
                try {
                    queue.onNext(parent.nl.next(t));
                } catch (MissingBackpressureException ex) {
                    unsubscribe();
                    onError(ex);
                    return;
                } catch (IllegalStateException ex) {
                    if (!isUnsubscribed()) {
                        unsubscribe();
                        onError(ex);
                    }
                    return;
                }
                parent.emit();
            }
            @Override
            public void onError(Throwable e) {
                done = true;
                parent.onError(e);
            }
            @Override
            public void onCompleted() {
                done = true;
                parent.emit();
            }
            
            void requestMore(long n) {
                request(n);
            }
        }
    }
    /**
     * Subscriber that requests Long.MAX_VALUE from upstream and doesn't queue. 
     * @param <T>
     */
    static final class FastInnerSubscriber<T, R> extends Subscriber<R> {
        final MergeSubscriber<T, R> parent;
        boolean done;
        public FastInnerSubscriber(MergeSubscriber<T, R> parent) {
            this.parent = parent;
        }
        @Override
        public void onNext(R t) {
            try {
                parent.fastEmitNext(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
            }
        }
        @Override
        public void onError(Throwable e) {
            if (!done) {
                done = true; // avoid calling parent twice because of fastWip
                parent.innerError(e, this);
            }
        }
        @Override
        public void onCompleted() {
            if (!done) {
                done = true;
                parent.innerComplete(this);
            }
        }
    }
}