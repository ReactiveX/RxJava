/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.MpscLinkedQueue;
import io.reactivex.internal.subscribers.QueueDrainSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.NotificationLite;
import io.reactivex.processors.UnicastProcessor;
import io.reactivex.subscribers.SerializedSubscriber;

public final class FlowableWindowTimed<T> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final long maxSize;
    final int bufferSize;
    final boolean restartTimerOnMaxSize;

    public FlowableWindowTimed(Flowable<T> source,
            long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, long maxSize,
            int bufferSize, boolean restartTimerOnMaxSize) {
        super(source);
        this.timespan = timespan;
        this.timeskip = timeskip;
        this.unit = unit;
        this.scheduler = scheduler;
        this.maxSize = maxSize;
        this.bufferSize = bufferSize;
        this.restartTimerOnMaxSize = restartTimerOnMaxSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> s) {
        SerializedSubscriber<Flowable<T>> actual = new SerializedSubscriber<Flowable<T>>(s);

        if (timespan == timeskip) {
            if (maxSize == Long.MAX_VALUE) {
                source.subscribe(new WindowExactUnboundedSubscriber<T>(
                        actual,
                        timespan, unit, scheduler, bufferSize));
                return;
            }
            source.subscribe(new WindowExactBoundedSubscriber<T>(
                        actual,
                        timespan, unit, scheduler,
                        bufferSize, maxSize, restartTimerOnMaxSize));
            return;
        }
        source.subscribe(new WindowSkipSubscriber<T>(actual,
                timespan, timeskip, unit, scheduler.createWorker(), bufferSize));
    }

    static final class WindowExactUnboundedSubscriber<T>
            extends QueueDrainSubscriber<T, Object, Flowable<T>>
            implements FlowableSubscriber<T>, Subscription, Runnable {
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        final int bufferSize;

        Subscription s;

        UnicastProcessor<T> window;

        final SequentialDisposable timer = new SequentialDisposable();

        static final Object NEXT = new Object();

        volatile boolean terminated;

        WindowExactUnboundedSubscriber(Subscriber<? super Flowable<T>> actual, long timespan, TimeUnit unit,
                Scheduler scheduler, int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.timespan = timespan;
            this.unit = unit;
            this.scheduler = scheduler;
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
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
                    a.onError(new MissingBackpressureException("Could not deliver first window due to lack of requests."));
                    return;
                }

                if (!cancelled) {
                    if (timer.replace(scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit))) {
                        s.request(Long.MAX_VALUE);
                    }
                }
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

            actual.onError(t);
            dispose();
        }

        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }

            actual.onComplete();
            dispose();
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        public void dispose() {
            DisposableHelper.dispose(timer);
        }

        @Override
        public void run() {

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

            final SimplePlainQueue<Object> q = queue;
            final Subscriber<? super Flowable<T>> a = actual;
            UnicastProcessor<T> w = window;

            int missed = 1;
            for (;;) {

                for (;;) {
                    boolean term = terminated; // NOPMD

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
                            w = UnicastProcessor.<T>create(bufferSize);
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
                                a.onError(new MissingBackpressureException("Could not deliver first window due to lack of requests."));
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
    }

    static final class WindowExactBoundedSubscriber<T>
    extends QueueDrainSubscriber<T, Object, Flowable<T>>
    implements Subscription {
        final long timespan;
        final TimeUnit unit;
        final Scheduler scheduler;
        final int bufferSize;
        final boolean restartTimerOnMaxSize;
        final long maxSize;
        final Scheduler.Worker worker;

        long count;

        long producerIndex;

        Subscription s;

        UnicastProcessor<T> window;

        volatile boolean terminated;

        final SequentialDisposable timer = new SequentialDisposable();

        WindowExactBoundedSubscriber(
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
            if (restartTimerOnMaxSize) {
                worker = scheduler.createWorker();
            } else {
                worker = null;
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {

                this.s = s;

                Subscriber<? super Flowable<T>> a = actual;

                a.onSubscribe(this);

                if (cancelled) {
                    return;
                }

                UnicastProcessor<T> w = UnicastProcessor.<T>create(bufferSize);
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
                    a.onError(new MissingBackpressureException("Could not deliver initial window due to lack of requests."));
                    return;
                }

                Disposable d;
                ConsumerIndexHolder consumerIndexHolder = new ConsumerIndexHolder(producerIndex, this);
                if (restartTimerOnMaxSize) {
                    d = worker.schedulePeriodically(consumerIndexHolder, timespan, timespan, unit);
                } else {
                    d = scheduler.schedulePeriodicallyDirect(consumerIndexHolder, timespan, timespan, unit);
                }

                if (timer.replace(d)) {
                    s.request(Long.MAX_VALUE);
                }
            }
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
                        w = UnicastProcessor.<T>create(bufferSize);
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
                            timer.replace(task);
                        }
                    } else {
                        window = null;
                        s.cancel();
                        actual.onError(new MissingBackpressureException("Could not deliver window due to lack of requests"));
                        dispose();
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

            actual.onError(t);
            dispose();
        }

        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }

            actual.onComplete();
            dispose();
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        public void dispose() {
            DisposableHelper.dispose(timer);
            Worker w = worker;
            if (w != null) {
                w.dispose();
            }
        }

        void drainLoop() {
            final SimplePlainQueue<Object> q = queue;
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
                        Throwable err = error;
                        if (err != null) {
                            w.onError(err);
                        } else {
                            w.onComplete();
                        }
                        dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (isHolder) {
                        ConsumerIndexHolder consumerIndexHolder = (ConsumerIndexHolder) o;
                        if (restartTimerOnMaxSize || producerIndex == consumerIndexHolder.index) {
                            w.onComplete();
                            count = 0;
                            w = UnicastProcessor.<T>create(bufferSize);
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
                                a.onError(new MissingBackpressureException("Could not deliver first window due to lack of requests."));
                                dispose();
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
                            w = UnicastProcessor.<T>create(bufferSize);
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
                                timer.replace(task);
                            }

                        } else {
                            window = null;
                            s.cancel();
                            actual.onError(new MissingBackpressureException("Could not deliver window due to lack of requests"));
                            dispose();
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
            ConsumerIndexHolder(long index, WindowExactBoundedSubscriber<?> parent) {
                this.index = index;
                this.parent = parent;
            }

            @Override
            public void run() {
                WindowExactBoundedSubscriber<?> p = parent;

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
    implements Subscription, Runnable {
        final long timespan;
        final long timeskip;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        final int bufferSize;

        final List<UnicastProcessor<T>> windows;

        Subscription s;

        volatile boolean terminated;

        WindowSkipSubscriber(Subscriber<? super Flowable<T>> actual,
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
            if (SubscriptionHelper.validate(this.s, s)) {

                this.s = s;

                actual.onSubscribe(this);

                if (cancelled) {
                    return;
                }

                long r = requested();
                if (r != 0L) {
                    final UnicastProcessor<T> w = UnicastProcessor.<T>create(bufferSize);
                    windows.add(w);

                    actual.onNext(w);
                    if (r != Long.MAX_VALUE) {
                        produced(1);
                    }
                    worker.schedule(new Completion(w), timespan, unit);

                    worker.schedulePeriodically(this, timeskip, timeskip, unit);

                    s.request(Long.MAX_VALUE);

                } else {
                    s.cancel();
                    actual.onError(new MissingBackpressureException("Could not emit the first window due to lack of requests"));
                }
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
                queue.offer(t);
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

            actual.onError(t);
            dispose();
        }

        @Override
        public void onComplete() {
            done = true;
            if (enter()) {
                drainLoop();
            }

            actual.onComplete();
            dispose();
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        public void dispose() {
            worker.dispose();
        }

        void complete(UnicastProcessor<T> w) {
            queue.offer(new SubjectWork<T>(w, false));
            if (enter()) {
                drainLoop();
            }
        }

        @SuppressWarnings("unchecked")
        void drainLoop() {
            final SimplePlainQueue<Object> q = queue;
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

                    if (d && (empty || sw)) {
                        q.clear();
                        Throwable e = error;
                        if (e != null) {
                            for (UnicastProcessor<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (UnicastProcessor<T> w : ws) {
                                w.onComplete();
                            }
                        }
                        ws.clear();
                        dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (sw) {
                        SubjectWork<T> work = (SubjectWork<T>)v;

                        if (work.open) {
                            if (cancelled) {
                                continue;
                            }

                            long r = requested();
                            if (r != 0L) {
                                final UnicastProcessor<T> w = UnicastProcessor.<T>create(bufferSize);
                                ws.add(w);
                                a.onNext(w);
                                if (r != Long.MAX_VALUE) {
                                    produced(1);
                                }

                                worker.schedule(new Completion(w), timespan, unit);
                            } else {
                                a.onError(new MissingBackpressureException("Can't emit window due to lack of requests"));
                            }
                        } else {
                            ws.remove(work.w);
                            work.w.onComplete();
                            if (ws.isEmpty() && cancelled) {
                                terminated = true;
                            }
                        }
                    } else {
                        for (UnicastProcessor<T> w : ws) {
                            w.onNext((T)v);
                        }
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

            UnicastProcessor<T> w = UnicastProcessor.<T>create(bufferSize);

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
            SubjectWork(UnicastProcessor<T> w, boolean open) {
                this.w = w;
                this.open = open;
            }
        }

        final class Completion implements Runnable {
            private final UnicastProcessor<T> processor;

            Completion(UnicastProcessor<T> processor) {
                this.processor = processor;
            }

            @Override
            public void run() {
                complete(processor);
            }
        }
    }

}
