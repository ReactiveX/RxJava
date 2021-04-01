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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.processors.UnicastProcessor;

public final class FlowableWindow<T> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final long size;

    final long skip;

    final int bufferSize;

    public FlowableWindow(Flowable<T> source, long size, long skip,  int bufferSize) {
        super(source);
        this.size = size;
        this.skip = skip;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Subscriber<? super Flowable<T>> s) {
        if (skip == size) {
            source.subscribe(new WindowExactSubscriber<>(s, size, bufferSize));
        } else
        if (skip > size) {
            source.subscribe(new WindowSkipSubscriber<>(s, size, skip, bufferSize));
        } else {
            source.subscribe(new WindowOverlapSubscriber<>(s, size, skip, bufferSize));
        }
    }

    static final class WindowExactSubscriber<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = -2365647875069161133L;

        final Subscriber<? super Flowable<T>> downstream;

        final long size;

        final AtomicBoolean once;

        final int bufferSize;

        long index;

        Subscription upstream;

        UnicastProcessor<T> window;

        WindowExactSubscriber(Subscriber<? super Flowable<T>> actual, long size, int bufferSize) {
            super(1);
            this.downstream = actual;
            this.size = size;
            this.once = new AtomicBoolean();
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long i = index;

            UnicastProcessor<T> w = window;
            FlowableWindowSubscribeIntercept<T> intercept = null;
            if (i == 0) {
                getAndIncrement();

                w = UnicastProcessor.create(bufferSize, this);
                window = w;

                intercept = new FlowableWindowSubscribeIntercept<>(w);
                downstream.onNext(intercept);
            }

            i++;

            w.onNext(t);

            if (i == size) {
                index = 0;
                window = null;
                w.onComplete();
            } else {
                index = i;
            }

            if (intercept != null && intercept.tryAbandon()) {
                intercept.window.onComplete();
            }
        }

        @Override
        public void onError(Throwable t) {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onError(t);
            }

            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onComplete();
            }

            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                long u = BackpressureHelper.multiplyCap(size, n);
                upstream.request(u);
            }
        }

        @Override
        public void cancel() {
            if (once.compareAndSet(false, true)) {
                run();
            }
        }

        @Override
        public void run() {
            if (decrementAndGet() == 0) {
                upstream.cancel();
            }
        }
    }

    static final class WindowSkipSubscriber<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = -8792836352386833856L;

        final Subscriber<? super Flowable<T>> downstream;

        final long size;

        final long skip;

        final AtomicBoolean once;

        final AtomicBoolean firstRequest;

        final int bufferSize;

        long index;

        Subscription upstream;

        UnicastProcessor<T> window;

        WindowSkipSubscriber(Subscriber<? super Flowable<T>> actual, long size, long skip, int bufferSize) {
            super(1);
            this.downstream = actual;
            this.size = size;
            this.skip = skip;
            this.once = new AtomicBoolean();
            this.firstRequest = new AtomicBoolean();
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long i = index;

            FlowableWindowSubscribeIntercept<T> intercept = null;
            UnicastProcessor<T> w = window;
            if (i == 0) {
                getAndIncrement();

                w = UnicastProcessor.create(bufferSize, this);
                window = w;

                intercept = new FlowableWindowSubscribeIntercept<>(w);
                downstream.onNext(intercept);
            }

            i++;

            if (w != null) {
                w.onNext(t);
            }

            if (i == size) {
                window = null;
                w.onComplete();
            }

            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }

            if (intercept != null && intercept.tryAbandon()) {
                intercept.window.onComplete();
            }
        }

        @Override
        public void onError(Throwable t) {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onError(t);
            }

            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            Processor<T, T> w = window;
            if (w != null) {
                window = null;
                w.onComplete();
            }

            downstream.onComplete();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (!firstRequest.get() && firstRequest.compareAndSet(false, true)) {
                    long u = BackpressureHelper.multiplyCap(size, n);
                    long v = BackpressureHelper.multiplyCap(skip - size, n - 1);
                    long w = BackpressureHelper.addCap(u, v);
                    upstream.request(w);
                } else {
                    long u = BackpressureHelper.multiplyCap(skip, n);
                    upstream.request(u);
                }
            }
        }

        @Override
        public void cancel() {
            if (once.compareAndSet(false, true)) {
                run();
            }
        }

        @Override
        public void run() {
            if (decrementAndGet() == 0) {
                upstream.cancel();
            }
        }
    }

    static final class WindowOverlapSubscriber<T>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 2428527070996323976L;

        final Subscriber<? super Flowable<T>> downstream;

        final SpscLinkedArrayQueue<UnicastProcessor<T>> queue;

        final long size;

        final long skip;

        final ArrayDeque<UnicastProcessor<T>> windows;

        final AtomicBoolean once;

        final AtomicBoolean firstRequest;

        final AtomicLong requested;

        final AtomicInteger wip;

        final int bufferSize;

        long index;

        long produced;

        Subscription upstream;

        volatile boolean done;
        Throwable error;

        volatile boolean cancelled;

        WindowOverlapSubscriber(Subscriber<? super Flowable<T>> actual, long size, long skip, int bufferSize) {
            super(1);
            this.downstream = actual;
            this.size = size;
            this.skip = skip;
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
            this.windows = new ArrayDeque<>();
            this.once = new AtomicBoolean();
            this.firstRequest = new AtomicBoolean();
            this.requested = new AtomicLong();
            this.wip = new AtomicInteger();
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            long i = index;

            UnicastProcessor<T> newWindow = null;
            if (i == 0) {
                if (!cancelled) {
                    getAndIncrement();

                    newWindow = UnicastProcessor.create(bufferSize, this);

                    windows.offer(newWindow);
                }
            }

            i++;

            for (Processor<T, T> w : windows) {
                w.onNext(t);
            }

            if (newWindow != null) {
                queue.offer(newWindow);
                drain();
            }

            long p = produced + 1;
            if (p == size) {
                produced = p - skip;

                Processor<T, T> w = windows.poll();
                if (w != null) {
                    w.onComplete();
                }
            } else {
                produced = p;
            }

            if (i == skip) {
                index = 0;
            } else {
                index = i;
            }
        }

        @Override
        public void onError(Throwable t) {
            for (Processor<T, T> w : windows) {
                w.onError(t);
            }
            windows.clear();

            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            for (Processor<T, T> w : windows) {
                w.onComplete();
            }
            windows.clear();

            done = true;
            drain();
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super Flowable<T>> a = downstream;
            final SpscLinkedArrayQueue<UnicastProcessor<T>> q = queue;
            int missed = 1;

            outer:
            for (;;) {

                if (cancelled) {
                    UnicastProcessor<T> up = null;
                    while ((up = q.poll()) != null) {
                        up.onComplete();
                    }
                } else {
                    long r = requested.get();
                    long e = 0;

                    while (e != r) {
                        boolean d = done;

                        UnicastProcessor<T> t = q.poll();

                        boolean empty = t == null;

                        if (cancelled) {
                            continue outer;
                        }

                        if (checkTerminated(d, empty, a, q)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        FlowableWindowSubscribeIntercept<T> intercept = new FlowableWindowSubscribeIntercept<>(t);
                        a.onNext(intercept);

                        if (intercept.tryAbandon()) {
                            t.onComplete();
                        }
                        e++;
                    }

                    if (e == r) {
                        if (cancelled) {
                            continue;
                        }

                        if (checkTerminated(done, q.isEmpty(), a, q)) {
                            return;
                        }
                    }

                    if (e != 0L && r != Long.MAX_VALUE) {
                        requested.addAndGet(-e);
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, SpscLinkedArrayQueue<?> q) {
            if (d) {
                Throwable e = error;

                if (e != null) {
                    q.clear();
                    a.onError(e);
                    return true;
                } else
                if (empty) {
                    a.onComplete();
                    return true;
                }
            }

            return false;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);

                if (!firstRequest.get() && firstRequest.compareAndSet(false, true)) {
                    long u = BackpressureHelper.multiplyCap(skip, n - 1);
                    long v = BackpressureHelper.addCap(size, u);
                    upstream.request(v);
                } else {
                    long u = BackpressureHelper.multiplyCap(skip, n);
                    upstream.request(u);
                }

                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (once.compareAndSet(false, true)) {
                run();
            }
            drain();
        }

        @Override
        public void run() {
            if (decrementAndGet() == 0) {
                upstream.cancel();
            }
        }
    }
}
