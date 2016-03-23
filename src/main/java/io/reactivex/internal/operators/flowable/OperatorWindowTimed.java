/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import java.nio.channels.CancelledKeyException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Flowable.Operator;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.flowable.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorWindowTimed<T> implements Operator<Flowable<T>, T> {
    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final long maxSize;
    final int bufferSize;
    final boolean restartTimerOnMaxSize;

    public OperatorWindowTimed(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, long maxSize,
            int bufferSize, boolean restartTimerOnMaxSize) {
        this.timespan = timespan;
        this.timeskip = timeskip;
        this.unit = unit;
        this.scheduler = scheduler;
        this.maxSize = maxSize;
        this.bufferSize = bufferSize;
        this.restartTimerOnMaxSize = restartTimerOnMaxSize;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super Flowable<T>> t) {
        SerializedSubscriber<Flowable<T>> actual = new SerializedSubscriber<Flowable<T>>(t);
        
        if (timespan == timeskip) {
            if (maxSize == Long.MAX_VALUE) {
                return new WindowExactUnboundedSubscriber<T>(
                        actual, 
                        timespan, unit, scheduler, bufferSize);
            }
            return new WindowExactBoundedSubscriber<T>(
                        actual,
                        timespan, unit, scheduler, 
                        bufferSize, maxSize, restartTimerOnMaxSize);
        }
        return new WindowSkipSubscriber<T>(actual,
                timespan, timeskip, unit, scheduler.createWorker(), bufferSize);
    }
    
    static final class WindowExactUnboundedSubscriber<T> 
            extends QueueDrainSubscriber<T, Object, Flowable<T>> 
            implements Subscriber<T>, Subscription, Disposable, Runnable {
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        final int bufferSize;
        
        Subscription s;
        
        boolean selfCancel;
        
        UnicastProcessor<T> window;

        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        static final Object NEXT = new Object();
        
        volatile boolean terminated;
        
        public WindowExactUnboundedSubscriber(Subscriber<? super Flowable<T>> actual, long timespan, TimeUnit unit,
                Scheduler scheduler, int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
            this.bufferSize = bufferSize;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            this.s = s;
            
            window = UnicastProcessor.<T>create(bufferSize);
            
            Subscriber<? super Flowable<T>> a = actual;
            a.onSubscribe(this);
            
            long r = requested();
            if (r != 0L) {
                a.onNext(window);
                if (r != Long.MAX_VALUE) {
                    produced(1);
                }
            } else {
                cancelled = true;
                s.cancel();
                a.onError(new IllegalStateException("Could not deliver first window due to lack of requests."));
                return;
            }
            
            if (!cancelled) {
                Disposable d = scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit);
                if (!timer.compareAndSet(null, d)) {
                    d.dispose();
                    return;
                }
                
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (terminated) {
                return;
            }
            if (fastEnter()) {
                window.onNext(t);
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
        
        @Override
        public void dispose() {
            selfCancel = true;
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public void run() {

            if (selfCancel) {
                throw new CancelledKeyException();
            }

            if (cancelled) {
                terminated = true;
                dispose();
            }
            queue.offer(NEXT);
            if (enter()) {
                drainLoop();
            }

        }
        
        void drainLoop() {
            
            final Queue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            UnicastProcessor<T> w = window;
            
            int missed = 1;
            for (;;) {
                
                for (;;) {
                    boolean term = terminated;
                    
                    boolean d = done;
                    
                    Object o = q.poll();
                    
                    if (d && (o == null || o == NEXT)) {
                        window = null;
                        q.clear();
                        dispose();
                        Throwable err = error;
                        if (err != null) {
                            w.onError(err);
                        } else {
                            w.onComplete();
                        }
                        return;
                    }
                    
                    if (o == null) {
                        break;
                    }
                    
                    if (o == NEXT) {
                        w.onComplete();
                        if (!term) {
                            w = UnicastProcessor.create(bufferSize);
                            window = w;
                            
                            long r = requested();
                            if (r != 0L) {
                                a.onNext(w);
                                if (r != Long.MAX_VALUE) {
                                    produced(1);
                                }
                            } else {
                                window = null;
                                queue.clear();
                                s.cancel();
                                dispose();
                                a.onError(new IllegalStateException("Could not deliver first window due to lack of requests."));
                                return;
                            }
                        } else {
                            s.cancel();
                        }
                        continue;
                    }
                    
                    w.onNext(NotificationLite.<T>getValue(o));
                }
                
                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public boolean accept(Subscriber<? super Flowable<T>> a, Object v) {
            // not used in this operator
            return true;
        }
    }
    
    static final class WindowExactBoundedSubscriber<T>
    extends QueueDrainSubscriber<T, Object, Flowable<T>>
    implements Subscription, Disposable {
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        final int bufferSize;
        final boolean restartTimerOnMaxSize;
        final long maxSize;
        
        boolean selfCancel;
        
        long count;
        
        long producerIndex;
        
        Subscription s;
        
        UnicastProcessor<T> window;
        
        Scheduler.Worker worker;
        
        volatile boolean terminated;
        
        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();
        
        static final Disposable CANCELLED = new Disposable() {
            @Override
            public void dispose() { }
        };
        
        public WindowExactBoundedSubscriber(
                Subscriber<? super Flowable<T>> actual, 
                long timespan, TimeUnit unit, Scheduler scheduler, 
                int bufferSize, long maxSize, boolean restartTimerOnMaxSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
            this.bufferSize = bufferSize;
            this.maxSize = maxSize;
            this.restartTimerOnMaxSize = restartTimerOnMaxSize;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            Subscriber<? super Flowable<T>> a = actual;
            
            a.onSubscribe(this);
            
            if (cancelled) {
                return;
            }
            
            UnicastProcessor<T> w = UnicastProcessor.create(bufferSize);
            window = w;
            
            long r = requested();
            if (r != 0L) {
                a.onNext(w);
                if (r != Long.MAX_VALUE) {
                    produced(1);
                }
            } else {
                cancelled = true;
                s.cancel();
                a.onError(new IllegalStateException("Could not deliver initial window due to lack of requests."));
                return;
            }
            
            Disposable d;
            ConsumerIndexHolder consumerIndexHolder = new ConsumerIndexHolder(producerIndex, this);
            if (restartTimerOnMaxSize) {
                Scheduler.Worker sw = scheduler.createWorker();
                sw.schedulePeriodically(consumerIndexHolder, timespan, timespan, unit);
                d = sw;
            } else {
                d = scheduler.schedulePeriodicallyDirect(consumerIndexHolder, timespan, timespan, unit);
            }
            
            if (!timer.compareAndSet(null, d)) {
                d.dispose();
                return;
            }
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (terminated) {
                return;
            }

            if (fastEnter()) {
                UnicastProcessor<T> w = window;
                w.onNext(t);
                
                long c = count + 1;
                
                if (c >= maxSize) {
                    producerIndex++;
                    count = 0;
                    
                    w.onComplete();
                    
                    long r = requested();
                    
                    if (r != 0L) {
                        w = UnicastProcessor.create(bufferSize);
                        window = w;
                        actual.onNext(w);
                        if (r != Long.MAX_VALUE) {
                            produced(1);
                        }
                        if (restartTimerOnMaxSize) {
                            Disposable tm = timer.get();
                            
                            tm.dispose();
                            Disposable task = worker.schedulePeriodically(
                                    new ConsumerIndexHolder(producerIndex, this), timespan, timespan, unit);
                            if (!timer.compareAndSet(tm, task)) {
                                task.dispose();
                            }
                        }
                    } else {
                        window = null;
                        s.cancel();
                        dispose();
                        actual.onError(new IllegalStateException("Could not deliver window due to lack of requests"));
                        return;
                    }
                } else {
                    count = c;
                }
                
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        @Override
        public void dispose() {
            selfCancel = true;
            Disposable d = timer.get();
            if (d != CANCELLED) {
                d = timer.getAndSet(CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
        }
        
        @Override
        public boolean accept(Subscriber<? super Flowable<T>> a, Object v) {
            // not needed in this operator
            return false;
        }
        
        void drainLoop() {
            final Queue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            UnicastProcessor<T> w = window;
            
            int missed = 1;
            for (;;) {
                
                for (;;) {
                    if (terminated) {
                        s.cancel();
                        q.clear();
                        dispose();
                        return;
                    }
                    
                    boolean d = done;
                    
                    Object o = q.poll();
                    
                    boolean empty = o == null;
                    boolean isHolder = o instanceof ConsumerIndexHolder;
                    
                    if (d && (empty || isHolder)) {
                        window = null;
                        q.clear();
                        dispose();
                        Throwable err = error;
                        if (err != null) {
                            w.onError(err);
                        } else {
                            w.onComplete();
                        }
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (isHolder) {
                        ConsumerIndexHolder consumerIndexHolder = (ConsumerIndexHolder) o;
                        if (producerIndex == consumerIndexHolder.index) {
                            w = UnicastProcessor.create(bufferSize);
                            window = w;
                            
                            long r = requested();
                            if (r != 0L) {
                                a.onNext(w);
                                if (r != Long.MAX_VALUE) {
                                    produced(1);
                                }
                            } else {
                                window = null;
                                queue.clear();
                                s.cancel();
                                dispose();
                                a.onError(new IllegalStateException("Could not deliver first window due to lack of requests."));
                                return;
                            }
                        }
                        continue;
                    }
                    
                    w.onNext(NotificationLite.<T>getValue(o));
                    long c = count + 1;
                    
                    if (c >= maxSize) {
                        producerIndex++;
                        count = 0;
                        
                        w.onComplete();
                        
                        long r = requested();
                        
                        if (r != 0L) {
                            w = UnicastProcessor.create(bufferSize);
                            window = w;
                            actual.onNext(w);
                            if (r != Long.MAX_VALUE) {
                                produced(1);
                            }
                            
                            if (restartTimerOnMaxSize) {
                                Disposable tm = timer.get();
                                tm.dispose();
                                
                                Disposable task = worker.schedulePeriodically(
                                        new ConsumerIndexHolder(producerIndex, this), timespan, timespan, unit);
                                if (!timer.compareAndSet(tm, task)) {
                                    task.dispose();
                                }
                            }
                            
                        } else {
                            window = null;
                            s.cancel();
                            dispose();
                            actual.onError(new IllegalStateException("Could not deliver window due to lack of requests"));
                            return;
                        }
                    } else {
                        count = c;
                    }
                }
                
                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        static final class ConsumerIndexHolder implements Runnable {
            final long index;
            final WindowExactBoundedSubscriber<?> parent;
            public ConsumerIndexHolder(long index, WindowExactBoundedSubscriber<?> parent) {
                this.index = index;
                this.parent = parent;
            }
            
            @Override
            public void run() {
                WindowExactBoundedSubscriber<?> p = parent;
                if (p.selfCancel) {
                    throw new CancelledKeyException();
                }

                if (!p.cancelled) {
                    p.queue.offer(this);
                } else {
                    p.terminated = true;
                    p.dispose();
                }
                if (p.enter()) {
                    p.drainLoop();
                }
            }
        }
    }
    
    static final class WindowSkipSubscriber<T>
    extends QueueDrainSubscriber<T, Object, Flowable<T>>
    implements Subscription, Disposable, Runnable {
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final int bufferSize;
        
        final List<UnicastProcessor<T>> windows;
        
        Subscription s;
        
        volatile boolean terminated;
        
        public WindowSkipSubscriber(Subscriber<? super Flowable<T>> actual,
                long timespan, long timeskip, TimeUnit unit, 
                Worker worker, int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.timespan = timespan;
            this.timeskip = timeskip;
            this.unit = unit;
            this.worker = worker;
            this.bufferSize = bufferSize;
            this.windows = new LinkedList<UnicastProcessor<T>>();
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
            
            if (cancelled) {
                return;
            }
            
            long r = requested();
            if (r != 0L) {
                final UnicastProcessor<T> w = UnicastProcessor.create(bufferSize);
                windows.add(w);
                
                actual.onNext(w);
                if (r != Long.MAX_VALUE) {
                    produced(1);
                }
                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        complete(w);
                    }
                }, timespan, unit);
                
                worker.schedulePeriodically(this, timeskip, timeskip, unit);
                
                s.request(Long.MAX_VALUE);
                
            } else {
                s.cancel();
                actual.onError(new IllegalStateException("Could not emit the first window due to lack of requests"));
                return;
            }
        }
        
        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                for (UnicastProcessor<T> w : windows) {
                    w.onNext(t);
                }
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }
        
        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }
            
            dispose();
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            requested(n);
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        @Override
        public void dispose() {
            worker.dispose();
        }
        
        @Override
        public boolean accept(Subscriber<? super Flowable<T>> a, Object v) {
            // not used by this operator
            return false;
        }
        
        void complete(UnicastProcessor<T> w) {
            queue.offer(new SubjectWork<T>(w, false));
            if (enter()) {
                drainLoop();
            }
        }
        
        void drainLoop() {
            final Queue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            final List<UnicastProcessor<T>> ws = windows;
            
            int missed = 1;
            
            for (;;) {
                
                for (;;) {
                    if (terminated) {
                        s.cancel();
                        dispose();
                        q.clear();
                        ws.clear();
                        return;
                    }
                    
                    boolean d = done;
                    
                    Object v = q.poll();
                    
                    boolean empty = v == null;
                    boolean sw = v instanceof SubjectWork;
                    
                    if (d && (v == null || v instanceof SubjectWork)) {
                        q.clear();
                        dispose();
                        Throwable e = error;
                        if (e != null) {
                            for (UnicastProcessor<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (UnicastProcessor<T> w : ws) {
                                w.onError(e);
                            }
                        }
                        ws.clear();
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }
                    
                    if (sw) {
                        @SuppressWarnings("unchecked")
                        SubjectWork<T> work = (SubjectWork<T>)v;
                        
                        if (work.open) {
                            if (cancelled) {
                                continue;
                            }
                            
                            long r = requested();
                            if (r != 0L) {
                                final UnicastProcessor<T> w = UnicastProcessor.create(bufferSize);
                                ws.add(w);
                                a.onNext(w);
                                if (r != Long.MAX_VALUE) {
                                    produced(1);
                                }
                                
                                worker.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        complete(w);
                                    }
                                }, timespan, unit);
                            } else {
                                a.onError(new IllegalStateException("Can't emit window due to lack of requests"));
                                continue;
                            }
                        } else {
                            ws.remove(work.w);
                            work.w.onComplete();
                            if (ws.isEmpty() && cancelled) {
                                terminated = true;
                            }
                            continue;
                        }
                    }
                    
                    for (UnicastProcessor<T> w : ws) {
                        w.onNext(NotificationLite.<T>getValue(v));
                    }
                }
                
                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        @Override
        public void run() {

            UnicastProcessor<T> w = UnicastProcessor.create(bufferSize);
            
            SubjectWork<T> sw = new SubjectWork<T>(w, true);
            if (!cancelled) {
                queue.offer(sw);
            }
            if (enter()) {
                drainLoop();
            }
        }
        
        static final class SubjectWork<T> {
            final UnicastProcessor<T> w;
            final boolean open;
            public SubjectWork(UnicastProcessor<T> w, boolean open) {
                this.w = w;
                this.open = open;
            }
        }
    }
    
}
