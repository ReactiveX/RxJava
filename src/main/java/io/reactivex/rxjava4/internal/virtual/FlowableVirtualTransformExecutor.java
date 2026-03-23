/*
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

package io.reactivex.rxjava4.internal.virtual;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.util.BackpressureHelper;
import io.reactivex.rxjava4.operators.SpscArrayQueue;

public final class FlowableVirtualTransformExecutor<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final VirtualTransformer<T, R> transformer;

    final ExecutorService executor;

    final Scheduler scheduler;

    final int prefetch;

    public FlowableVirtualTransformExecutor(Flowable<T> source,
            VirtualTransformer<T, R> transformer,
            ExecutorService executor,
            Scheduler scheduler,
            int prefetch) {
        this.source = source;
        this.transformer = transformer;
        this.executor = executor;
        this.scheduler = scheduler;
        this.prefetch = prefetch;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        var parent = new ExecutorVirtualTransformSubscriber<>(s, transformer, prefetch);
        source.subscribe(parent);
        if (executor != null) {
            executor.submit((Callable<Void>)parent);
        } else {
            var worker = scheduler.createWorker();
            parent.worker = worker;
            worker.schedule(parent);
        }
    }

    static final class ExecutorVirtualTransformSubscriber<T, R> extends AtomicLong
    implements FlowableSubscriber<T>, Subscription, VirtualEmitter<R>, Callable<Void>, Runnable {

        private static final long serialVersionUID = -4702456711290258571L;

        Subscriber<? super R> downstream;

        final VirtualTransformer<T, R> transformer;

        final int prefetch;

        final AtomicLong requested;

        final VirtualResumable producerReady;

        final VirtualResumable consumerReady;

        final SpscArrayQueue<T> queue;

        Subscription upstream;

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        static final Throwable STOP = new Throwable("Downstream cancelled");

        long produced;

        Worker worker;

        ExecutorVirtualTransformSubscriber(Subscriber<? super R> downstream,
                VirtualTransformer<T, R> transformer,
                int prefetch) {
            this.downstream = downstream;
            this.transformer = transformer;
            this.prefetch = prefetch;
            this.requested = new AtomicLong();
            this.producerReady = new VirtualResumable();
            this.consumerReady = new VirtualResumable();
            this.queue = new SpscArrayQueue<>(prefetch);
        }

        @Override
        public void onSubscribe(Subscription s) {
            upstream = s;
            downstream.onSubscribe(this);
            s.request(prefetch);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            if (getAndIncrement() == 0) {
                producerReady.resume();
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            onComplete();
        }

        @Override
        public void onComplete() {
            done = true;
            if (getAndIncrement() == 0) {
                producerReady.resume();
            }
        }

        @Override
        public void emit(R item) throws Throwable {
            Objects.requireNonNull(item, "item is null");

            var p = produced;
            while (requested.get() == p && !cancelled) {
                consumerReady.await();
            }

            if (cancelled) {
                throw STOP;
            }

            downstream.onNext(item);

            produced = p + 1;
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            consumerReady.resume();
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            // cleanup(); don't kill the worker

            var w = worker;
            worker = null;
            if (w != null) {
                w.close();
            }

            producerReady.resume();
            consumerReady.resume();
        }

        @Override
        public void run() {
            call();
        }

        @Override
        public Void call() {
            try {
                try {
                    var consumed = 0L;
                    var limit = prefetch - (prefetch >> 2);
                    var wip = 0L;

                    while (!cancelled) {
                        var d = done;
                        var v = queue.poll();
                        var empty = v == null;

                        if (d && empty) {
                            var ex = error;
                            if (ex != null) {
                                downstream.onError(ex);
                            } else {
                                downstream.onComplete();
                            }
                            break;
                        }

                        if (!empty) {

                            if (++consumed == limit) {
                                consumed = 0;
                                upstream.request(limit);
                            }

                            transformer.transform(v, this);

                            continue;
                        }

                        wip = addAndGet(-wip);
                        if (wip == 0L) {
                            producerReady.await();
                        }
                    }
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    if (ex != STOP && !cancelled) {
                        upstream.cancel();
                        downstream.onError(ex);
                    }
                    return null;
                }
            } finally {
                queue.clear();
                downstream = null;
            }
            return null;
        }
    }
}