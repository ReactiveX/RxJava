/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.Operator;
import rx.Observable;
import rx.exceptions.MissingBackpressureException;
import rx.internal.util.RxRingBuffer;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Flattens a list of Observables into one Observable sequence, without any transformation.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/merge.png" alt="">
 * <p>
 * You can combine the items emitted by multiple Observables so that they act like a single
 * Observable, by using the merge operation.
 *
 * @param <T> the emitted value type
 */
public final class OperatorMergeMaxConcurrent<T> implements Operator<T, Observable<? extends T>> {
    final int maxConcurrency;
    
    public OperatorMergeMaxConcurrent(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
    
    @Override
    public Subscriber<? super Observable<? extends T>> call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        
        SourceSubscriber<T> ssub = new SourceSubscriber<T>(maxConcurrency, s, csub);
        child.setProducer(new MergeMaxConcurrentProducer<T>(ssub));
        
        return ssub;
    }
    /** Routes the requests from downstream to the sourcesubscriber. */
    static final class MergeMaxConcurrentProducer<T> implements Producer {
        final SourceSubscriber<T> ssub;
        public MergeMaxConcurrentProducer(SourceSubscriber<T> ssub) {
            this.ssub = ssub;
        }
        @Override
        public void request(long n) {
            ssub.downstreamRequest(n);
        }
    }
    static final class SourceSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final NotificationLite<T> nl = NotificationLite.instance();
        final int maxConcurrency;
        final Subscriber<T> s;
        final CompositeSubscription csub;
        final Object guard;

        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<SourceSubscriber> WIP
                = AtomicIntegerFieldUpdater.newUpdater(SourceSubscriber.class, "wip");
        volatile int sourceIndex;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<SourceSubscriber> SOURCE_INDEX
                = AtomicIntegerFieldUpdater.newUpdater(SourceSubscriber.class, "sourceIndex");
        
        /** Guarded by guard. */
        int active;
        /** Guarded by guard. */
        final Queue<Observable<? extends T>> queue;
        
        /** Indicates the emitting phase. Guarded by this. */
        boolean emitting;
        /** Counts the missed emitting calls. Guarded by this. */
        int missedEmitting;
        /** The last buffer index in the round-robin drain scheme. Accessed while emitting == true. */
        int lastIndex;
        
        /** Guarded by itself. */
        final List<MergeItemSubscriber> subscribers;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<SourceSubscriber> REQUESTED
                = AtomicLongFieldUpdater.newUpdater(SourceSubscriber.class, "requested");
        
        
        public SourceSubscriber(int maxConcurrency, Subscriber<T> s, CompositeSubscription csub) {
            super(s);
            this.maxConcurrency = maxConcurrency;
            this.s = s;
            this.csub = csub;
            this.guard = new Object();
            this.queue = new ArrayDeque<Observable<? extends T>>(maxConcurrency);
            this.subscribers = Collections.synchronizedList(new ArrayList<MergeItemSubscriber>());
            this.wip = 1;
        }
        
        @Override
        public void onStart() {
            request(maxConcurrency);
        }
        
        @Override
        public void onNext(Observable<? extends T> t) {
            synchronized (guard) {
                queue.add(t);
            }
            subscribeNext();
        }
        
        void subscribeNext() {
            Observable<? extends T> t;
            synchronized (guard) {
                t = queue.peek();
                if (t == null || active >= maxConcurrency) {
                    return;
                }
                active++;
                queue.poll();
            }
            
            MergeItemSubscriber itemSub = new MergeItemSubscriber(SOURCE_INDEX.getAndIncrement(this));
            subscribers.add(itemSub);

            csub.add(itemSub);
            
            WIP.incrementAndGet(this);
            
            t.unsafeSubscribe(itemSub);
            
            request(1);
        }
        
        @Override
        public void onError(Throwable e) {
            Object[] active;
            synchronized (subscribers) {
                active = subscribers.toArray();
                subscribers.clear();
            }
            
            try {
                s.onError(e);
                
                unsubscribe();
            } finally {
                for (Object o : active) {
                    @SuppressWarnings("unchecked")
                    MergeItemSubscriber a = (MergeItemSubscriber)o;
                    a.release();
                }
            }
            
        }
        
        @Override
        public void onCompleted() {
            WIP.decrementAndGet(this);
            drain();
        }
        
        protected void downstreamRequest(long n) {
            for (;;) {
                long r = requested;
                long u;
                if (r != Long.MAX_VALUE && n == Long.MAX_VALUE) {
                    u = Long.MAX_VALUE;
                } else
                if (r + n < 0) {
                    u = Long.MAX_VALUE;
                } else {
                    u = r + n;
                }
                if (REQUESTED.compareAndSet(this, r, u)) {
                    break;
                }
            }
            drain();
        }
        
        protected void drain() {
            synchronized (this) {
                if (emitting) {
                    missedEmitting++;
                    return;
                }
                emitting = true;
                missedEmitting = 0;
            }
            final List<SourceSubscriber<T>.MergeItemSubscriber> subs = subscribers;
            final Subscriber<T> child = s;
            Object[] active = new Object[subs.size()];
            do {
                long r;
                
                outer:
                while ((r = requested) > 0) {
                    int idx = lastIndex;
                    synchronized (subs) {
                        if (subs.size() == active.length) {
                            active = subs.toArray(active);
                        } else {
                            active = subs.toArray();
                        }
                    }
                    
                    int resumeIndex = 0;
                    int j = 0;
                    for (Object o : active) {
                        @SuppressWarnings("unchecked")
                        MergeItemSubscriber e = (MergeItemSubscriber)o;
                        if (e.index == idx) {
                            resumeIndex = j;
                            break;
                        }
                        j++;
                    }
                    int sumConsumed = 0;
                    for (int i = 0; i < active.length; i++) {
                        j = (i + resumeIndex) % active.length;

                        @SuppressWarnings("unchecked")
                        final MergeItemSubscriber e = (MergeItemSubscriber)active[j];
                        final RxRingBuffer b = e.buffer;
                        lastIndex = e.index;
                        
                        if (!e.once && b.peek() == null) {
                            subs.remove(e);
                            
                            synchronized (guard) {
                                this.active--;
                            }
                            csub.remove(e);

                            e.release();
                            
                            subscribeNext();
                            
                            WIP.decrementAndGet(this);
                            
                            continue outer;
                        }
                        
                        int consumed = 0;
                        Object v;
                        while (r > 0 && (v = b.poll()) != null) {
                            nl.accept(child, v);
                            if (child.isUnsubscribed()) {
                                return;
                            }
                            r--;
                            consumed++;
                        }
                        if (consumed > 0) {
                            sumConsumed += consumed;
                            REQUESTED.addAndGet(this, -consumed);
                            e.requestMore(consumed);
                        }
                        if (r == 0) {
                            break outer;
                        }
                    }
                    if (sumConsumed == 0) {
                        break;
                    }
                }
                
                if (active.length == 0) {
                    if (wip == 0) {
                        child.onCompleted();
                        return;
                    }
                }
                synchronized (this) {
                    if (missedEmitting == 0) {
                        emitting = false;
                        break;
                    }
                    missedEmitting = 0;
                }
            } while (true);
        }
        final class MergeItemSubscriber extends Subscriber<T> {
            volatile boolean once = true;
            final int index;
            final RxRingBuffer buffer;
            
            public MergeItemSubscriber(int index) {
                buffer = RxRingBuffer.getSpmcInstance();
                this.index = index;
            }
            
            @Override
            public void onStart() {
                request(RxRingBuffer.SIZE);
            }
            
            @Override
            public void onNext(T t) {
                try {
                    buffer.onNext(t);
                } catch (MissingBackpressureException ex) {
                    onError(ex);
                    return;
                }

                drain();
            }

            @Override
            public void onError(Throwable e) {
                SourceSubscriber.this.onError(e);
            }

            @Override
            public void onCompleted() {
                if (once) {
                    once = false;
                    drain();
                }
            }
            /** Request more from upstream. */
            void requestMore(long n) {
                request(n);
            }
            void release() {
                // NO-OP for now
                buffer.release();
            }
        }
    }
}
