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

import java.util.concurrent.atomic.*;

import rx.*;
import rx.exceptions.*;
import rx.internal.util.LinkedArrayList;
import rx.subscriptions.SerialSubscription;

/**
 * An observable which auto-connects to another observable, caches the elements
 * from that observable but allows terminating the connection and completing the cache.
 *
 * @param <T> the source element type
 */
public final class CachedObservable<T> extends Observable<T> {

    /** The cache and replay state. */
    private final CacheState<T> state;

    /**
     * Creates a cached Observable with a default capacity hint of 16.
     * @param <T> the value type
     * @param source the source Observable to cache
     * @return the CachedObservable instance
     */
    @SuppressWarnings("cast")
    public static <T> CachedObservable<T> from(Observable<? extends T> source) {
        return (CachedObservable<T>)from(source, 16);
    }
    
    /**
     * Creates a cached Observable with the given capacity hint.
     * @param <T> the value type
     * @param source the source Observable to cache
     * @param capacityHint the hint for the internal buffer size
     * @return the CachedObservable instance
     */
    public static <T> CachedObservable<T> from(Observable<? extends T> source, int capacityHint) {
        if (capacityHint < 1) {
            throw new IllegalArgumentException("capacityHint > 0 required");
        }
        CacheState<T> state = new CacheState<T>(source, capacityHint);
        CachedSubscribe<T> onSubscribe = new CachedSubscribe<T>(state);
        return new CachedObservable<T>(onSubscribe, state);
    }
    
    /**
     * Private constructor because state needs to be shared between the Observable body and
     * the onSubscribe function.
     * @param onSubscribe
     * @param state
     */
    private CachedObservable(OnSubscribe<T> onSubscribe, CacheState<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    /**
     * Check if this cached observable is connected to its source.
     * @return true if already connected
     */
    /* public */boolean isConnected() {
        return state.isConnected;
    }
    
    /**
     * Returns true if there are observers subscribed to this observable.
     * @return
     */
    /* public */ boolean hasObservers() {
        return state.producers.length != 0;
    }

    /**
     * Contains the active child producers and the values to replay.
     *
     * @param <T>
     */
    static final class CacheState<T> extends LinkedArrayList implements Observer<T> {
        /** The source observable to connect to. */
        final Observable<? extends T> source;
        /** Holds onto the subscriber connected to source. */
        final SerialSubscription connection;
        /** Guarded by connection (not this). */
        volatile ReplayProducer<?>[] producers;
        /** The default empty array of producers. */
        static final ReplayProducer<?>[] EMPTY = new ReplayProducer<?>[0];
        
        final NotificationLite<T> nl;
        
        /** Set to true after connection. */
        volatile boolean isConnected;
        /** 
         * Indicates that the source has completed emitting values or the
         * Observable was forcefully terminated.
         */
        boolean sourceDone;
        
        public CacheState(Observable<? extends T> source, int capacityHint) {
            super(capacityHint);
            this.source = source;
            this.producers = EMPTY;
            this.nl = NotificationLite.instance();
            this.connection = new SerialSubscription();
        }
        /**
         * Adds a ReplayProducer to the producers array atomically.
         * @param p
         */
        public void addProducer(ReplayProducer<T> p) {
            // guarding by connection to save on allocating another object
            // thus there are two distinct locks guarding the value-addition and child come-and-go
            synchronized (connection) {
                ReplayProducer<?>[] a = producers;
                int n = a.length;
                ReplayProducer<?>[] b = new ReplayProducer<?>[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = p;
                producers = b;
            }
        }
        /**
         * Removes the ReplayProducer (if present) from the producers array atomically.
         * @param p
         */
        public void removeProducer(ReplayProducer<T> p) {
            synchronized (connection) {
                ReplayProducer<?>[] a = producers;
                int n = a.length;
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i].equals(p)) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                if (n == 1) {
                    producers = EMPTY;
                    return;
                }
                ReplayProducer<?>[] b = new ReplayProducer<?>[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                producers = b;
            }
        }
        /**
         * Connects the cache to the source.
         * Make sure this is called only once.
         */
        public void connect() {
            Subscriber<T> subscriber = new Subscriber<T>() {
                @Override
                public void onNext(T t) {
                    CacheState.this.onNext(t);
                }
                @Override
                public void onError(Throwable e) {
                    CacheState.this.onError(e);
                }
                @Override
                public void onCompleted() {
                    CacheState.this.onCompleted();
                }
            };
            connection.set(subscriber);
            source.unsafeSubscribe(subscriber);
            isConnected = true;
        }
        @Override
        public void onNext(T t) {
            if (!sourceDone) {
                Object o = nl.next(t);
                add(o);
                dispatch();
            }
        }
        @Override
        public void onError(Throwable e) {
            if (!sourceDone) {
                sourceDone = true;
                Object o = nl.error(e);
                add(o);
                connection.unsubscribe();
                dispatch();
            }
        }
        @Override
        public void onCompleted() {
            if (!sourceDone) {
                sourceDone = true;
                Object o = nl.completed();
                add(o);
                connection.unsubscribe();
                dispatch();
            }
        }
        /**
         * Signals all known children there is work to do.
         */
        void dispatch() {
            ReplayProducer<?>[] a = producers;
            for (ReplayProducer<?> rp : a) {
                rp.replay();
            }
        }
    }
    
    /**
     * Manages the subscription of child subscribers by setting up a replay producer and
     * performs auto-connection of the very first subscription.
     * @param <T> the value type emitted
     */
    static final class CachedSubscribe<T> extends AtomicBoolean implements OnSubscribe<T> {
        /** */
        private static final long serialVersionUID = -2817751667698696782L;
        final CacheState<T> state;
        public CachedSubscribe(CacheState<T> state) {
            this.state = state;
        }
        @Override
        public void call(Subscriber<? super T> t) {
            // we can connect first because we replay everything anyway
            ReplayProducer<T> rp = new ReplayProducer<T>(t, state);
            state.addProducer(rp);
            
            t.add(rp);
            t.setProducer(rp);

            // we ensure a single connection here to save an instance field of AtomicBoolean in state.
            if (!get() && compareAndSet(false, true)) {
                state.connect();
            }
            
            // no need to call rp.replay() here because the very first request will trigger it anyway
        }
    }
    
    /**
     * Keeps track of the current request amount and the replay position for a child Subscriber.
     *
     * @param <T>
     */
    static final class ReplayProducer<T> extends AtomicLong implements Producer, Subscription {
        /** */
        private static final long serialVersionUID = -2557562030197141021L;
        /** The actual child subscriber. */
        final Subscriber<? super T> child;
        /** The cache state object. */
        final CacheState<T> state;
        
        /** 
         * Contains the reference to the buffer segment in replay.
         * Accessed after reading state.size() and when emitting == true.
         */
        Object[] currentBuffer;
        /** 
         * Contains the index into the currentBuffer where the next value is expected. 
         * Accessed after reading state.size() and when emitting == true.
         */
        int currentIndexInBuffer;
        /**
         * Contains the absolute index up until the values have been replayed so far.
         */
        int index;

        /** Indicates there is a replay going on; guarded by this. */
        boolean emitting;
        /** Indicates there were some state changes/replay attempts; guarded by this. */
        boolean missed;
        
        public ReplayProducer(Subscriber<? super T> child, CacheState<T> state) {
            this.child = child;
            this.state = state;
        }
        @Override
        public void request(long n) {
            for (;;) {
                long r = get();
                if (r < 0) {
                    return;
                }
                long u = r + n;
                if (u < 0) {
                    u = Long.MAX_VALUE;
                }
                if (compareAndSet(r, u)) {
                    replay();
                    return;
                }
            }
        }
        /**
         * Updates the request count to reflect values have been produced.
         * @param n the produced item count, positive, not validated
         * @return the latest request amount after subtracting n
         */
        public long produced(long n) {
            return addAndGet(-n);
        }
        
        @Override
        public boolean isUnsubscribed() {
            return get() < 0;
        }
        @Override
        public void unsubscribe() {
            long r = get();
            if (r >= 0) {
                r = getAndSet(-1L); // unsubscribed state is negative
                if (r >= 0) {
                    state.removeProducer(this);
                }
            }
        }
        
        /**
         * Continue replaying available values if there are requests for them.
         */
        public void replay() {
            // make sure there is only a single thread emitting
            synchronized (this) {
                if (emitting) {
                    missed = true;
                    return;
                }
                emitting = true;
            }
            boolean skipFinal = false;
            try {
                final NotificationLite<T> nl = state.nl;
                final Subscriber<? super T> child = this.child;
                
                for (;;) {
                    
                    long r = get();
                    
                    if (r < 0L) {
                        skipFinal = true;
                        return;
                    }
                        
                    // read the size, if it is non-zero, we can safely read the head and
                    // read values up to the given absolute index
                    int s = state.size();
                    if (s != 0) {
                        Object[] b = currentBuffer;
                        
                        // latch onto the very first buffer now that it is available.
                        if (b == null) {
                            b = state.head();
                            currentBuffer = b;
                        }
                        final int n = b.length - 1;
                        int j = index;
                        int k = currentIndexInBuffer;
                        // eagerly emit any terminal event
                        if (r == 0) {
                            Object o = b[k];
                            if (nl.isCompleted(o)) {
                                child.onCompleted();
                                skipFinal = true;
                                unsubscribe();
                                return;
                            } else
                            if (nl.isError(o)) {
                                child.onError(nl.getError(o));
                                skipFinal = true;
                                unsubscribe();
                                return;
                            }
                        } else
                        if (r > 0) {
                            int valuesProduced = 0;
                            
                            while (j < s && r > 0) {
                                if (child.isUnsubscribed()) {
                                    skipFinal = true;
                                    return;
                                }
                                if (k == n) {
                                    b = (Object[])b[n];
                                    k = 0;
                                }
                                Object o = b[k];
                                
                                try {
                                    if (nl.accept(child, o)) {
                                        skipFinal = true;
                                        unsubscribe();
                                        return;
                                    }
                                } catch (Throwable err) {
                                    Exceptions.throwIfFatal(err);
                                    skipFinal = true;
                                    unsubscribe();
                                    if (!nl.isError(o) && !nl.isCompleted(o)) {
                                        child.onError(OnErrorThrowable.addValueAsLastCause(err, nl.getValue(o)));
                                    }
                                    return;
                                }
                                
                                k++;
                                j++;
                                r--;
                                valuesProduced++;
                            }
                            
                            if (child.isUnsubscribed()) {
                                skipFinal = true;
                                return;
                            }
                            
                            index = j;
                            currentIndexInBuffer = k;
                            currentBuffer = b;
                            produced(valuesProduced);
                        }
                    }
                    
                    synchronized (this) {
                        if (!missed) {
                            emitting = false;
                            skipFinal = true;
                            return;
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
    }
}