/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Multicasts a Flowable over a selector function.
 * 
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class FlowablePublishMulticast<T, R> extends AbstractFlowableWithUpstream<T, R> {

    final Function<? super Flowable<T>, ? extends Publisher<? extends R>> selector;
    
    final int prefetch;
    
    final boolean delayError;

    public FlowablePublishMulticast(Publisher<T> source,
            Function<? super Flowable<T>, ? extends Publisher<? extends R>> selector, int prefetch,
            boolean delayError) {
        super(source);
        this.selector = selector;
        this.prefetch = prefetch;
        this.delayError = delayError;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        MulticastProcessor<T> mp = new MulticastProcessor<T>(prefetch, delayError);
        
        Publisher<? extends R> other;
        
        try {
            other = ObjectHelper.requireNonNull(selector.apply(mp), "selector returned a null Publisher");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }
        
        OutputCanceller<R> out = new OutputCanceller<R>(s, mp);
        
        other.subscribe(out);
        
        source.subscribe(mp);
    }
    
    static final class OutputCanceller<R> implements Subscriber<R>, Subscription {
        final Subscriber<? super R> actual;
        
        final MulticastProcessor<?> processor;

        Subscription s;
        
        public OutputCanceller(Subscriber<? super R> actual, MulticastProcessor<?> processor) {
            this.actual = actual;
            this.processor = processor;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(R t) {
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            try {
                actual.onError(t);
            } finally {
                processor.dispose();
            }
        }
        
        @Override
        public void onComplete() {
            try {
                actual.onComplete();
            } finally {
                processor.dispose();
            }
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
            processor.dispose();
        }
    }
    
    static final class MulticastProcessor<T> extends Flowable<T> implements Subscriber<T>, Disposable {
        
        @SuppressWarnings("rawtypes")
        static final MulticastSubscription[] EMPTY = new MulticastSubscription[0];
        
        @SuppressWarnings("rawtypes")
        static final MulticastSubscription[] TERMINATED = new MulticastSubscription[0];
        
        final AtomicInteger wip;
        
        final AtomicReference<MulticastSubscription<T>[]> subscribers;
        
        final int prefetch;
        
        final int limit;
        
        final boolean delayError;
        
        final AtomicReference<Subscription> s;
        
        SimpleQueue<T> queue;
        
        int sourceMode;
        
        volatile boolean done;
        Throwable error;
        
        @SuppressWarnings("unchecked")
        public MulticastProcessor(int prefetch, boolean delayError) {
            this.prefetch = prefetch;
            this.delayError = delayError;
            this.limit = prefetch - (prefetch >> 2);
            this.wip = new AtomicInteger();
            this.s = new AtomicReference<Subscription>();
            this.subscribers = new AtomicReference<MulticastSubscription<T>[]>(EMPTY);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> qs = (QueueSubscription<T>) s;
                    
                    int m = qs.requestFusion(QueueSubscription.ANY);
                    if (m == QueueSubscription.SYNC) {
                        sourceMode = m;
                        queue = qs;
                        done = true;
                        drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        sourceMode = m;
                        queue = qs;
                        QueueDrainHelper.request(s, prefetch);
                        return;
                    }
                }
                
                queue = QueueDrainHelper.createQueue(prefetch);
                
                QueueDrainHelper.request(s, prefetch);
            }
        }
        
        @Override
        public void dispose() {
            SubscriptionHelper.cancel(s);
            if (wip.getAndIncrement() == 0) {
                queue.clear();
            }
        }
        
        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(s.get());
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode == QueueSubscription.NONE) {
                if (!queue.offer(t)) {
                    SubscriptionHelper.cancel(s);
                    onError(new MissingBackpressureException());
                    return;
                }
            }
            drain();
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            drain();
        }
        
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                drain();
            }
        }

        boolean add(MulticastSubscription<T> s) {
            for (;;) {
                MulticastSubscription<T>[] current = subscribers.get();
                if (current == TERMINATED) {
                    return false;
                }
                int n = current.length;
                @SuppressWarnings("unchecked")
                MulticastSubscription<T>[] next = new MulticastSubscription[n + 1];
                System.arraycopy(current, 0, next, 0, n);
                next[n] = s;
                if (subscribers.compareAndSet(current, next)) {
                    return true;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void remove(MulticastSubscription<T> s) {
            for (;;) {
                MulticastSubscription<T>[] current = subscribers.get();
                if (current == TERMINATED || current == EMPTY) {
                    return;
                }
                int n = current.length;
                int j = -1;
                
                for (int i = 0; i < n; i++) {
                    if (current[i] == s) {
                        j = i;
                        break;
                    }
                }
                
                if (j < 0) {
                    return;
                }
                MulticastSubscription<T>[] next;
                if (n == 1) {
                    next = EMPTY;
                } else {
                    next = new MulticastSubscription[n - 1];
                    System.arraycopy(current, 0, next, 0, j);
                    System.arraycopy(current, j + 1, next, j, n - j - 1);
                }
                if (subscribers.compareAndSet(current, next)) {
                    return;
                }
            }
        }
        
        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            MulticastSubscription<T> ms = new MulticastSubscription<T>(s, this);
            s.onSubscribe(ms);
            if (add(ms)) {
                if (ms.isCancelled()) {
                    remove(ms);
                    return;
                }
                drain();
            } else {
                Throwable ex = error;
                if (ex != null) {
                    s.onError(ex);
                } else {
                    s.onComplete();
                }
            }
        }
        
        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            
            SimpleQueue<T> q = queue;
            
            for (;;) {
                MulticastSubscription<T>[] array = subscribers.get();

                int n = array.length;
                
                if (q != null && n != 0) {
                    long r = Long.MAX_VALUE;
                    
                    for (MulticastSubscription<T> ms : array) {
                        long u = ms.get();
                        if (u != Long.MIN_VALUE) {
                            if (r > u) {
                                r = u;
                            }
                        }
                    }
                    
                    long e = 0L;
                    while (e != r) {
                        if (isDisposed()) {
                            q.clear();
                            return;
                        }
                        
                        boolean d = done;
                        
                        if (d && !delayError) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                                return;
                            }
                        }
                        
                        T v;
                        
                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            SubscriptionHelper.cancel(s);
                            errorAll(ex);
                            return;
                        }
                        
                        boolean empty = v == null;
                        
                        if (d && empty) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                            } else {
                                completeAll();
                            }
                            return;
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        for (MulticastSubscription<T> ms : array) {
                            if (ms.get() != Long.MIN_VALUE) {
                                ms.actual.onNext(v);
                            }
                        }
                        
                        e++;
                    }
                    
                    if (e == r) {
                        if (isDisposed()) {
                            q.clear();
                            return;
                        }
                        
                        boolean d = done;
                        
                        if (d && !delayError) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                                return;
                            }
                        }
                        
                        if (d && q.isEmpty()) {
                            Throwable ex = error;
                            if (ex != null) {
                                errorAll(ex);
                            } else {
                                completeAll();
                            }
                            return;
                        }
                    }
                    
                    for (MulticastSubscription<T> ms : array) {
                        BackpressureHelper.produced(ms, e);
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (q == null) {
                    q = queue;
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void errorAll(Throwable ex) {
            for (MulticastSubscription<T> ms : subscribers.getAndSet(TERMINATED)) {
                if (ms.get() != Long.MIN_VALUE) {
                    ms.actual.onError(ex);
                }
            }
        }
        
        @SuppressWarnings("unchecked")
        void completeAll() {
            for (MulticastSubscription<T> ms : subscribers.getAndSet(TERMINATED)) {
                if (ms.get() != Long.MIN_VALUE) {
                    ms.actual.onComplete();
                }
            }
        }
    }
    
    static final class MulticastSubscription<T> 
    extends AtomicLong
    implements Subscription {
        
        /** */
        private static final long serialVersionUID = 8664815189257569791L;
        
        final Subscriber<? super T> actual;
        
        final MulticastProcessor<T> parent;
        
        public MulticastSubscription(Subscriber<? super T> actual, MulticastProcessor<T> parent) {
            this.actual = actual;
            this.parent = parent;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = get();
                    if (r == Long.MIN_VALUE) {
                        return;
                    }
                    if (r != Long.MAX_VALUE) {
                        long u = BackpressureHelper.addCap(r, n);
                        if (compareAndSet(r, u)) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                parent.drain();
            }
        }
        
        @Override
        public void cancel() {
            getAndSet(Long.MIN_VALUE);
            parent.remove(this);
        }
        
        public boolean isCancelled() {
            return get() == Long.MIN_VALUE;
        }
    }
}
