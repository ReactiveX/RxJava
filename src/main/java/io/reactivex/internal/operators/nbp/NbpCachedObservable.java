/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.NbpObservable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SerialResource;
import io.reactivex.internal.util.*;

/**
 * An observable which auto-connects to another observable, caches the elements
 * from that observable but allows terminating the connection and completing the cache.
 *
 * @param <T> the source element type
 */
public final class NbpCachedObservable<T> extends NbpObservable<T> {
    /** The cache and replay state. */
    private CacheState<T> state;

    /**
     * Creates a cached Observable with a default capacity hint of 16.
     * @param source the source Observable to cache
     * @return the CachedObservable instance
     */
    public static <T> NbpCachedObservable<T> from(NbpObservable<? extends T> source) {
        return from(source, 16);
    }
    
    /**
     * Creates a cached Observable with the given capacity hint.
     * @param source the source Observable to cache
     * @param capacityHint the hint for the internal buffer size
     * @return the CachedObservable instance
     */
    public static <T> NbpCachedObservable<T> from(NbpObservable<? extends T> source, int capacityHint) {
        if (capacityHint < 1) {
            throw new IllegalArgumentException("capacityHint > 0 required");
        }
        CacheState<T> state = new CacheState<>(source, capacityHint);
        CachedSubscribe<T> onSubscribe = new CachedSubscribe<>(state);
        return new NbpCachedObservable<>(onSubscribe, state);
    }
    
    /**
     * Private constructor because state needs to be shared between the Observable body and
     * the onSubscribe function.
     * @param onSubscribe
     * @param state
     */
    private NbpCachedObservable(NbpOnSubscribe<T> onSubscribe, CacheState<T> state) {
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
     * Returns the number of events currently cached.
     * @return
     */
    /* public */ int cachedEventCount() {
        return state.size();
    }
    
    /**
     * Contains the active child producers and the values to replay.
     *
     * @param <T>
     */
    static final class CacheState<T> extends LinkedArrayList implements NbpSubscriber<T> {
        /** The source observable to connect to. */
        final NbpObservable<? extends T> source;
        /** Holds onto the subscriber connected to source. */
        final SerialResource<Disposable> connection;
        /** Guarded by connection (not this). */
        volatile ReplaySubscription<?>[] producers;
        /** The default empty array of producers. */
        static final ReplaySubscription<?>[] EMPTY = new ReplaySubscription<?>[0];
        
        /** Set to true after connection. */
        volatile boolean isConnected;
        /** 
         * Indicates that the source has completed emitting values or the
         * Observable was forcefully terminated.
         */
        boolean sourceDone;
        
        public CacheState(NbpObservable<? extends T> source, int capacityHint) {
            super(capacityHint);
            this.source = source;
            this.producers = EMPTY;
            this.connection = new SerialResource<>(Disposable::dispose);
        }
        /**
         * Adds a ReplayProducer to the producers array atomically.
         * @param p
         */
        public void addProducer(ReplaySubscription<T> p) {
            // guarding by connection to save on allocating another object
            // thus there are two distinct locks guarding the value-addition and child come-and-go
            synchronized (connection) {
                ReplaySubscription<?>[] a = producers;
                int n = a.length;
                ReplaySubscription<?>[] b = new ReplaySubscription<?>[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = p;
                producers = b;
            }
        }
        /**
         * Removes the ReplayProducer (if present) from the producers array atomically.
         * @param p
         */
        public void removeProducer(ReplaySubscription<T> p) {
            synchronized (connection) {
                ReplaySubscription<?>[] a = producers;
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
                ReplaySubscription<?>[] b = new ReplaySubscription<?>[n - 1];
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
                producers = b;
            }
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            connection.setResource(s);
        }
        
        /**
         * Connects the cache to the source.
         * Make sure this is called only once.
         */
        public void connect() {
            source.unsafeSubscribe(this);
            isConnected = true;
        }
        @Override
        public void onNext(T t) {
            if (!sourceDone) {
                Object o = NotificationLite.next(t);
                add(o);
                dispatch();
            }
        }
        @Override
        public void onError(Throwable e) {
            if (!sourceDone) {
                sourceDone = true;
                Object o = NotificationLite.error(e);
                add(o);
                connection.dispose();
                dispatch();
            }
        }
        @Override
        public void onComplete() {
            if (!sourceDone) {
                sourceDone = true;
                Object o = NotificationLite.complete();
                add(o);
                connection.dispose();
                dispatch();
            }
        }
        /**
         * Signals all known children there is work to do.
         */
        void dispatch() {
            ReplaySubscription<?>[] a = producers;
            for (ReplaySubscription<?> rp : a) {
                rp.replay();
            }
        }
    }
    
    /**
     * Manages the subscription of child subscribers by setting up a replay producer and
     * performs auto-connection of the very first subscription.
     * @param <T> the value type emitted
     */
    static final class CachedSubscribe<T> extends AtomicBoolean implements NbpOnSubscribe<T> {
        /** */
        private static final long serialVersionUID = -2817751667698696782L;
        final CacheState<T> state;
        public CachedSubscribe(CacheState<T> state) {
            this.state = state;
        }
        @Override
        public void accept(NbpSubscriber<? super T> t) {
            // we can connect first because we replay everything anyway
            ReplaySubscription<T> rp = new ReplaySubscription<>(t, state);
            state.addProducer(rp);
            
            t.onSubscribe(rp);

            // we ensure a single connection here to save an instance field of AtomicBoolean in state.
            if (!get() && compareAndSet(false, true)) {
                state.connect();
            }
            
            rp.replay();
        }
    }
    
    /**
     * Keeps track of the current request amount and the replay position for a child Subscriber.
     *
     * @param <T>
     */
    static final class ReplaySubscription<T> implements Disposable {
        /** The actual child subscriber. */
        final NbpSubscriber<? super T> child;
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
        
        /** Set if the Subscription has been cancelled/disposed. */
        volatile boolean cancelled;
        
        public ReplaySubscription(NbpSubscriber<? super T> child, CacheState<T> state) {
            this.child = child;
            this.state = state;
        }

        public boolean isDisposed() {
            return cancelled;
        }
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                state.removeProducer(this);
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
                final NbpSubscriber<? super T> child = this.child;
                
                for (;;) {
                    
                    if (cancelled) {
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

                        while (j < s) {
                            if (cancelled) {
                                skipFinal = true;
                                return;
                            }
                            if (k == n) {
                                b = (Object[])b[n];
                                k = 0;
                            }
                            Object o = b[k];
                            
                            try {
                                if (NotificationLite.accept(o, child)) {
                                    skipFinal = true;
                                    dispose();
                                    return;
                                }
                            } catch (Throwable err) {
                                skipFinal = true;
                                dispose();
                                if (!NotificationLite.isError(o) && !NotificationLite.isComplete(o)) {
                                    child.onError(err);
                                }
                                return;
                            }
                            
                            k++;
                            j++;
                        }
                        
                        if (cancelled) {
                            skipFinal = true;
                            return;
                        }
                        
                        index = j;
                        currentIndexInBuffer = k;
                        currentBuffer = b;
                    
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