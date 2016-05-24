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

package rx.internal.operators;

import java.util.*;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.exceptions.CompositeException;
import rx.functions.FuncN;
import rx.internal.util.RxRingBuffer;
import rx.internal.util.atomic.SpscLinkedArrayQueue;
import rx.plugins.RxJavaPlugins;

public final class OnSubscribeCombineLatest<T, R> implements OnSubscribe<R> {
    final Observable<? extends T>[] sources;
    final Iterable<? extends Observable<? extends T>> sourcesIterable;
    final FuncN<? extends R> combiner;
    final int bufferSize;
    final boolean delayError;
    
    public OnSubscribeCombineLatest(Iterable<? extends Observable<? extends T>> sourcesIterable,
            FuncN<? extends R> combiner) {
        this(null, sourcesIterable, combiner, RxRingBuffer.SIZE, false);
    }
    
    public OnSubscribeCombineLatest(Observable<? extends T>[] sources,
            Iterable<? extends Observable<? extends T>> sourcesIterable,
            FuncN<? extends R> combiner, int bufferSize,
            boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void call(Subscriber<? super R> s) {
        Observable<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            if (sourcesIterable instanceof List) {
                // unchecked & raw: javac type inference problem otherwise
                List list = (List)sourcesIterable;
                sources = (Observable[])list.toArray(new Observable[list.size()]);
                count = sources.length;
            } else {
                sources = new Observable[8];
                for (Observable<? extends T> p : sourcesIterable) {
                    if (count == sources.length) {
                        Observable<? extends T>[] b = new Observable[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = p;
                }
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            s.onCompleted();
            return;
        }
        
        LatestCoordinator<T, R> lc = new LatestCoordinator<T, R>(s, combiner, count, bufferSize, delayError);
        lc.subscribe(sources);
    }
    
    static final class LatestCoordinator<T, R> extends AtomicInteger implements Producer, Subscription {
        /** */
        private static final long serialVersionUID = 8567835998786448817L;
        final Subscriber<? super R> actual;
        final FuncN<? extends R> combiner;
        final int count;
        final CombinerSubscriber<T, R>[] subscribers;
        final int bufferSize;
        final Object[] latest;
        final SpscLinkedArrayQueue<Object> queue;
        final boolean delayError;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        
        final AtomicLong requested;

        final AtomicReference<Throwable> error;
        
        int active;
        int complete;
        
        /** Indicates the particular source hasn't emitted any value yet. */
        static final Object MISSING = new Object();
        
        @SuppressWarnings("unchecked")
        public LatestCoordinator(Subscriber<? super R> actual, 
                FuncN<? extends R> combiner, 
                int count, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.combiner = combiner;
            this.count = count;
            this.bufferSize = bufferSize;
            this.delayError = delayError;
            this.latest = new Object[count];
            Arrays.fill(latest, MISSING);
            this.subscribers = new CombinerSubscriber[count];
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize);
            this.requested = new AtomicLong();
            this.error = new AtomicReference<Throwable>();
        }
        
        @SuppressWarnings("unchecked")
        public void subscribe(Observable<? extends T>[] sources) {
            Subscriber<T>[] as = subscribers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new CombinerSubscriber<T, R>(this, i);
            }
            lazySet(0); // release array contents
            actual.add(this);
            actual.setProducer(this);
            for (int i = 0; i < len; i++) {
                if (cancelled) {
                    return;
                }
                ((Observable<T>)sources[i]).subscribe(as[i]);
            }
        }
        
        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("n >= required but it was " + n);
            }
            if (n != 0) {
                BackpressureUtils.getAndAddRequest(requested, n);
                drain();
            }
        }
        
        @Override
        public void unsubscribe() {
            if (!cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    cancel(queue);
                }
            }
        }
        
        @Override
        public boolean isUnsubscribed() {
            return cancelled;
        }
        
        void cancel(Queue<?> q) {
            q.clear();
            for (CombinerSubscriber<T, R> s : subscribers) {
                s.unsubscribe();
            }
        }
        
        /**
         * Combine the given notification value from the indexth source with the existing known
         * latest values.
         * @param value the notification to combine, null indicates the source terminated normally
         * @param index the index of the source subscriber
         */
        void combine(Object value, int index) {
            CombinerSubscriber<T, R> combinerSubscriber = subscribers[index];
            
            int activeCount;
            int completedCount;
            int sourceCount;
            boolean empty;
            boolean allSourcesFinished;
            synchronized (this) {
                sourceCount = latest.length;
                Object o = latest[index];
                activeCount = active;
                if (o == MISSING) {
                    active = ++activeCount;
                }
                completedCount = complete;
                if (value == null) {
                    complete = ++completedCount;
                } else {
                    latest[index] = combinerSubscriber.nl.getValue(value);
                }
                allSourcesFinished = activeCount == sourceCount;
                // see if either all sources completed
                empty = completedCount == sourceCount 
                        || (value == null && o == MISSING); // or this source completed without any value
                if (!empty) {
                    if (value != null && allSourcesFinished) {
                        queue.offer(combinerSubscriber, latest.clone());
                    } else
                    if (value == null && error.get() != null && (o == MISSING || !delayError)) {
                        done = true; // if this source completed without a value
                    }
                } else {
                    done = true;
                }
            }
            if (!allSourcesFinished && value != null) {
                combinerSubscriber.requestMore(1);
                return;
            }
            drain();
        }
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            final Queue<Object> q = queue;
            final Subscriber<? super R> a = actual;
            final boolean delayError = this.delayError;
            final AtomicLong localRequested = this.requested;
            
            int missed = 1;
            for (;;) {
                
                if (checkTerminated(done, q.isEmpty(), a, q, delayError)) {
                    return;
                }
                
                long requestAmount = localRequested.get();
                boolean unbounded = requestAmount == Long.MAX_VALUE;
                long emitted = 0L;
                
                while (requestAmount != 0L) {
                    
                    boolean d = done;
                    @SuppressWarnings("unchecked")
                    CombinerSubscriber<T, R> cs = (CombinerSubscriber<T, R>)q.peek();
                    boolean empty = cs == null;
                    
                    if (checkTerminated(d, empty, a, q, delayError)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }

                    q.poll();
                    Object[] array = (Object[])q.poll();
                    
                    if (array == null) {
                        cancelled = true;
                        cancel(q);
                        a.onError(new IllegalStateException("Broken queue?! Sender received but not the array."));
                        return;
                    }
                    
                    R v;
                    try {
                        v = combiner.call(array);
                    } catch (Throwable ex) {
                        cancelled = true;
                        cancel(q);
                        a.onError(ex);
                        return;
                    }
                    
                    a.onNext(v);
                    
                    cs.requestMore(1);
                    
                    requestAmount--;
                    emitted--;
                }
                
                if (emitted != 0L) {
                    if (!unbounded) {
                        localRequested.addAndGet(emitted);
                    }
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        
        boolean checkTerminated(boolean mainDone, boolean queueEmpty, Subscriber<?> childSubscriber, Queue<?> q, boolean delayError) {
            if (cancelled) {
                cancel(q);
                return true;
            }
            if (mainDone) {
                if (delayError) {
                    if (queueEmpty) {
                        Throwable e = error.get();
                        if (e != null) {
                            childSubscriber.onError(e);
                        } else {
                            childSubscriber.onCompleted();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error.get();
                    if (e != null) {
                        cancel(q);
                        childSubscriber.onError(e);
                        return true;
                    } else
                    if (queueEmpty) {
                        childSubscriber.onCompleted();
                        return true;
                    }
                }
            }
            return false;
        }
        
        void onError(Throwable e) {
            AtomicReference<Throwable> localError = this.error;
            for (;;) {
                Throwable curr = localError.get();
                Throwable next;
                if (curr != null) {
                    if (curr instanceof CompositeException) {
                        CompositeException ce = (CompositeException) curr;
                        List<Throwable> es = new ArrayList<Throwable>(ce.getExceptions());
                        es.add(e);
                        next = new CompositeException(es);
                    } else {
                        next = new CompositeException(Arrays.asList(curr, e));
                    }
                } else {
                    next = e;
                }
                if (localError.compareAndSet(curr, next)) {
                    return;
                }
            }
        }
    }
    
    static final class CombinerSubscriber<T, R> extends Subscriber<T> {
        final LatestCoordinator<T, R> parent;
        final int index;
        final NotificationLite<T> nl;
        
        boolean done;
        
        public CombinerSubscriber(LatestCoordinator<T, R> parent, int index) {
            this.parent = parent;
            this.index = index;
            this.nl = NotificationLite.instance();
            request(parent.bufferSize);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            parent.combine(nl.next(t), index);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                return;
            }
            parent.onError(t);
            done = true;
            parent.combine(null, index);
        }
        
        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            parent.combine(null, index);
        }
        
        public void requestMore(long n) {
            request(n);
        }
    }
}
