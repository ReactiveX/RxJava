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

package io.reactivex.internal.operators.parallel;

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.queue.SpscArrayQueue;
import io.reactivex.internal.schedulers.SchedulerMultiWorkerSupport;
import io.reactivex.internal.schedulers.SchedulerMultiWorkerSupport.WorkerCallback;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Ensures each 'rail' from upstream runs on a Worker from a Scheduler.
 *
 * @param <T> the value type
 */
public final class ParallelRunOn<T> extends ParallelFlowable<T> {
    final ParallelFlowable<? extends T> source;

    final Scheduler scheduler;

    final int prefetch;

    public ParallelRunOn(ParallelFlowable<? extends T> parent,
            Scheduler scheduler, int prefetch) {
        this.source = parent;
        this.scheduler = scheduler;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribe(final Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;

        @SuppressWarnings("unchecked")
        final Subscriber<T>[] parents = new Subscriber[n];

        if (scheduler instanceof SchedulerMultiWorkerSupport) {
            SchedulerMultiWorkerSupport multiworker = (SchedulerMultiWorkerSupport) scheduler;
            multiworker.createWorkers(n, new MultiWorkerCallback(subscribers, parents));
        } else {
            for (int i = 0; i < n; i++) {
                createSubscriber(i, subscribers, parents, scheduler.createWorker());
            }
        }
        source.subscribe(parents);
    }

    void createSubscriber(int i, Subscriber<? super T>[] subscribers,
            Subscriber<T>[] parents, Scheduler.Worker worker) {

        Subscriber<? super T> a = subscribers[i];

        SpscArrayQueue<T> q = new SpscArrayQueue<T>(prefetch);

        if (a instanceof ConditionalSubscriber) {
            parents[i] = new RunOnConditionalSubscriber<T>((ConditionalSubscriber<? super T>)a, prefetch, q, worker);
        } else {
            parents[i] = new RunOnSubscriber<T>(a, prefetch, q, worker);
        }
    }

    final class MultiWorkerCallback implements WorkerCallback {

        final Subscriber<? super T>[] subscribers;

        final Subscriber<T>[] parents;

        MultiWorkerCallback(Subscriber<? super T>[] subscribers,
                Subscriber<T>[] parents) {
            this.subscribers = subscribers;
            this.parents = parents;
        }

        @Override
        public void onWorker(int i, Worker w) {
            createSubscriber(i, subscribers, parents, w);
        }
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    abstract static class BaseRunOnSubscriber<T> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 9222303586456402150L;

        final int prefetch;

        final int limit;

        final SpscArrayQueue<T> queue;

        final Worker worker;

        Subscription s;

        volatile boolean done;

        Throwable error;

        final AtomicLong requested = new AtomicLong();

        volatile boolean cancelled;

        int consumed;

        BaseRunOnSubscriber(int prefetch, SpscArrayQueue<T> queue, Worker worker) {
            this.prefetch = prefetch;
            this.queue = queue;
            this.limit = prefetch - (prefetch >> 2);
            this.worker = worker;
        }

        @Override
        public final void onNext(T t) {
            if (done) {
                return;
            }
            if (!queue.offer(t)) {
                s.cancel();
                onError(new MissingBackpressureException("Queue is full?!"));
                return;
            }
            schedule();
        }

        @Override
        public final void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            schedule();
        }

        @Override
        public final void onComplete() {
            if (done) {
                return;
            }
            done = true;
            schedule();
        }

        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                schedule();
            }
        }

        @Override
        public final void cancel() {
            if (!cancelled) {
                cancelled = true;
                s.cancel();
                worker.dispose();

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        final void schedule() {
            if (getAndIncrement() == 0) {
                worker.schedule(this);
            }
        }
    }

    static final class RunOnSubscriber<T> extends BaseRunOnSubscriber<T> {

        private static final long serialVersionUID = 1075119423897941642L;

        final Subscriber<? super T> actual;

        RunOnSubscriber(Subscriber<? super T> actual, int prefetch, SpscArrayQueue<T> queue, Worker worker) {
            super(prefetch, queue, worker);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(prefetch);
            }
        }

        @Override
        public void run() {
            int missed = 1;
            int c = consumed;
            SpscArrayQueue<T> q = queue;
            Subscriber<? super T> a = actual;
            int lim = limit;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();

                            a.onError(ex);

                            worker.dispose();
                            return;
                        }
                    }

                    T v = q.poll();

                    boolean empty = v == null;

                    if (d && empty) {
                        a.onComplete();

                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(v);

                    e++;

                    int p = ++c;
                    if (p == lim) {
                        c = 0;
                        s.request(p);
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    if (done) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();

                            a.onError(ex);

                            worker.dispose();
                            return;
                        }
                        if (q.isEmpty()) {
                            a.onComplete();

                            worker.dispose();
                            return;
                        }
                    }
                }

                if (e != 0L && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                int w = get();
                if (w == missed) {
                    consumed = c;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }

    static final class RunOnConditionalSubscriber<T> extends BaseRunOnSubscriber<T> {

        private static final long serialVersionUID = 1075119423897941642L;

        final ConditionalSubscriber<? super T> actual;

        RunOnConditionalSubscriber(ConditionalSubscriber<? super T> actual, int prefetch, SpscArrayQueue<T> queue, Worker worker) {
            super(prefetch, queue, worker);
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);

                s.request(prefetch);
            }
        }

        @Override
        public void run() {
            int missed = 1;
            int c = consumed;
            SpscArrayQueue<T> q = queue;
            ConditionalSubscriber<? super T> a = actual;
            int lim = limit;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();

                            a.onError(ex);

                            worker.dispose();
                            return;
                        }
                    }

                    T v = q.poll();

                    boolean empty = v == null;

                    if (d && empty) {
                        a.onComplete();

                        worker.dispose();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (a.tryOnNext(v)) {
                        e++;
                    }

                    int p = ++c;
                    if (p == lim) {
                        c = 0;
                        s.request(p);
                    }
                }

                if (e == r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    if (done) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();

                            a.onError(ex);

                            worker.dispose();
                            return;
                        }
                        if (q.isEmpty()) {
                            a.onComplete();

                            worker.dispose();
                            return;
                        }
                    }
                }

                if (e != 0L && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                int w = get();
                if (w == missed) {
                    consumed = c;
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }
}
