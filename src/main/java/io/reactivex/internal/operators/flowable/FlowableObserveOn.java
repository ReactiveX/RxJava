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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.annotations.Nullable;
import io.reactivex.exceptions.*;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableObserveOn<T> extends AbstractFlowableWithUpstream<T, T> {
final Scheduler scheduler;

    final boolean delayError;

    final int prefetch;

    public FlowableObserveOn(
            Flowable<T> source,
            Scheduler scheduler,
            boolean delayError,
            int prefetch) {
        super(source);
        this.scheduler = scheduler;
        this.delayError = delayError;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        Worker worker = scheduler.createWorker();

        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new ObserveOnConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>) s, worker, delayError, prefetch));
        } else {
            source.subscribe(new ObserveOnSubscriber<T>(s, worker, delayError, prefetch));
        }
    }

    abstract static class BaseObserveOnSubscriber<T>
    extends BasicIntQueueSubscription<T>
    implements FlowableSubscriber<T>, Runnable {
        private static final long serialVersionUID = -8241002408341274697L;

        final Worker worker;

        final boolean delayError;

        final int prefetch;

        final int limit;

        final AtomicLong requested;

        Subscription s;

        SimpleQueue<T> queue;

        volatile boolean cancelled;

        volatile boolean done;

        Throwable error;

        int sourceMode;

        long produced;

        boolean outputFused;

        BaseObserveOnSubscriber(
                Worker worker,
                boolean delayError,
                int prefetch) {
            this.worker = worker;
            this.delayError = delayError;
            this.prefetch = prefetch;
            this.requested = new AtomicLong();
            this.limit = prefetch - (prefetch >> 2);
        }

        @Override
        public final void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode == ASYNC) {
                trySchedule();
                return;
            }
            if (!queue.offer(t)) {
                s.cancel();

                error = new MissingBackpressureException("Queue is full?!");
                done = true;
            }
            trySchedule();
        }

        @Override
        public final void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            trySchedule();
        }

        @Override
        public final void onComplete() {
            if (!done) {
                done = true;
                trySchedule();
            }
        }

        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                trySchedule();
            }
        }

        @Override
        public final void cancel() {
            if (cancelled) {
                return;
            }

            cancelled = true;
            s.cancel();
            worker.dispose();

            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        final void trySchedule() {
            if (getAndIncrement() != 0) {
                return;
            }
            worker.schedule(this);
        }

        @Override
        public final void run() {
            if (outputFused) {
                runBackfused();
            } else if (sourceMode == SYNC) {
                runSync();
            } else {
                runAsync();
            }
        }

        abstract void runBackfused();

        abstract void runSync();

        abstract void runAsync();

        final boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a) {
            if (cancelled) {
                clear();
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        worker.dispose();
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        clear();
                        a.onError(e);
                        worker.dispose();
                        return true;
                    } else
                    if (empty) {
                        a.onComplete();
                        worker.dispose();
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public final int requestFusion(int requestedMode) {
            if ((requestedMode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public final void clear() {
            queue.clear();
        }

        @Override
        public final boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    static final class ObserveOnSubscriber<T> extends BaseObserveOnSubscriber<T>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -4547113800637756442L;

        final Subscriber<? super T> actual;

        ObserveOnSubscriber(
                Subscriber<? super T> actual,
                Worker worker,
                boolean delayError,
                int prefetch) {
            super(worker, delayError, prefetch);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> f = (QueueSubscription<T>) s;

                    int m = f.requestFusion(ANY | BOUNDARY);

                    if (m == SYNC) {
                        sourceMode = SYNC;
                        queue = f;
                        done = true;

                        actual.onSubscribe(this);
                        return;
                    } else
                    if (m == ASYNC) {
                        sourceMode = ASYNC;
                        queue = f;

                        actual.onSubscribe(this);

                        s.request(prefetch);

                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);

                actual.onSubscribe(this);

                s.request(prefetch);
            }
        }

        @Override
        void runSync() {
            int missed = 1;

            final Subscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    T v;

                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        s.cancel();
                        a.onError(ex);
                        worker.dispose();
                        return;
                    }

                    if (cancelled) {
                        return;
                    }
                    if (v == null) {
                        a.onComplete();
                        worker.dispose();
                        return;
                    }

                    a.onNext(v);

                    e++;
                }

                if (cancelled) {
                    return;
                }

                if (q.isEmpty()) {
                    a.onComplete();
                    worker.dispose();
                    return;
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        @Override
        void runAsync() {
            int missed = 1;

            final Subscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    boolean d = done;
                    T v;

                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();

                        a.onError(ex);
                        worker.dispose();
                        return;
                    }

                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;
                    if (e == limit) {
                        if (r != Long.MAX_VALUE) {
                            r = requested.addAndGet(-e);
                        }
                        s.request(e);
                        e = 0L;
                    }
                }

                if (e == r && checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        @Override
        void runBackfused() {
            int missed = 1;

            for (;;) {

                if (cancelled) {
                    return;
                }

                boolean d = done;

                actual.onNext(null);

                if (d) {
                    Throwable e = error;
                    if (e != null) {
                        actual.onError(e);
                    } else {
                        actual.onComplete();
                    }
                    worker.dispose();
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v = queue.poll();
            if (v != null && sourceMode != SYNC) {
                long p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    s.request(p);
                } else {
                    produced = p;
                }
            }
            return v;
        }

    }

    static final class ObserveOnConditionalSubscriber<T>
    extends BaseObserveOnSubscriber<T> {

        private static final long serialVersionUID = 644624475404284533L;

        final ConditionalSubscriber<? super T> actual;

        long consumed;

        ObserveOnConditionalSubscriber(
                ConditionalSubscriber<? super T> actual,
                Worker worker,
                boolean delayError,
                int prefetch) {
            super(worker, delayError, prefetch);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<T> f = (QueueSubscription<T>) s;

                    int m = f.requestFusion(ANY | BOUNDARY);

                    if (m == SYNC) {
                        sourceMode = SYNC;
                        queue = f;
                        done = true;

                        actual.onSubscribe(this);
                        return;
                    } else
                    if (m == ASYNC) {
                        sourceMode = ASYNC;
                        queue = f;

                        actual.onSubscribe(this);

                        s.request(prefetch);

                        return;
                    }
                }

                queue = new SpscArrayQueue<T>(prefetch);

                actual.onSubscribe(this);

                s.request(prefetch);
            }
        }

        @Override
        void runSync() {
            int missed = 1;

            final ConditionalSubscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long e = produced;

            for (;;) {

                long r = requested.get();

                while (e != r) {
                    T v;
                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        s.cancel();
                        a.onError(ex);
                        worker.dispose();
                        return;
                    }

                    if (cancelled) {
                        return;
                    }
                    if (v == null) {
                        a.onComplete();
                        worker.dispose();
                        return;
                    }

                    if (a.tryOnNext(v)) {
                        e++;
                    }
                }

                if (cancelled) {
                    return;
                }

                if (q.isEmpty()) {
                    a.onComplete();
                    worker.dispose();
                    return;
                }

                int w = get();
                if (missed == w) {
                    produced = e;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }

        @Override
        void runAsync() {
            int missed = 1;

            final ConditionalSubscriber<? super T> a = actual;
            final SimpleQueue<T> q = queue;

            long emitted = produced;
            long polled = consumed;

            for (;;) {

                long r = requested.get();

                while (emitted != r) {
                    boolean d = done;
                    T v;
                    try {
                        v = q.poll();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);

                        s.cancel();
                        q.clear();

                        a.onError(ex);
                        worker.dispose();
                        return;
                    }
                    boolean empty = v == null;

                    if (checkTerminated(d, empty, a)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (a.tryOnNext(v)) {
                        emitted++;
                    }

                    polled++;

                    if (polled == limit) {
                        s.request(polled);
                        polled = 0L;
                    }
                }

                if (emitted == r && checkTerminated(done, q.isEmpty(), a)) {
                    return;
                }

                int w = get();
                if (missed == w) {
                    produced = emitted;
                    consumed = polled;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }

        }

        @Override
        void runBackfused() {
            int missed = 1;

            for (;;) {

                if (cancelled) {
                    return;
                }

                boolean d = done;

                actual.onNext(null);

                if (d) {
                    Throwable e = error;
                    if (e != null) {
                        actual.onError(e);
                    } else {
                        actual.onComplete();
                    }
                    worker.dispose();
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v = queue.poll();
            if (v != null && sourceMode != SYNC) {
                long p = consumed + 1;
                if (p == limit) {
                    consumed = 0;
                    s.request(p);
                } else {
                    consumed = p;
                }
            }
            return v;
        }
    }
}
