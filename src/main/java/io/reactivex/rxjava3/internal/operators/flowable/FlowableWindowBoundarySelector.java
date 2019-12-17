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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.fuseable.SimplePlainQueue;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.UnicastProcessor;

public final class FlowableWindowBoundarySelector<T, B, V> extends AbstractFlowableWithUpstream<T, Flowable<T>> {
    final Publisher<B> open;
    final Function<? super B, ? extends Publisher<V>> closingIndicator;
    final int bufferSize;

    public FlowableWindowBoundarySelector(
            Flowable<T> source,
            Publisher<B> open, Function<? super B, ? extends Publisher<V>> closingIndicator,
            int bufferSize) {
        super(source);
        this.open = open;
        this.closingIndicator = closingIndicator;
        this.bufferSize = bufferSize;
    }

    @Override
    protected void subscribeActual(Subscriber<? super Flowable<T>> s) {
        source.subscribe(new WindowBoundaryMainSubscriber<T, B, V>(
                s, open, closingIndicator, bufferSize));
    }

    static final class WindowBoundaryMainSubscriber<T, B, V>
    extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription, Runnable {
        private static final long serialVersionUID = 8646217640096099753L;

        final Subscriber<? super Flowable<T>> downstream;
        final Publisher<B> open;
        final Function<? super B, ? extends Publisher<V>> closingIndicator;
        final int bufferSize;
        final CompositeDisposable resources;

        final WindowStartSubscriber<B> startSubscriber;

        final List<UnicastProcessor<T>> windows;

        final SimplePlainQueue<Object> queue;

        final AtomicLong windowCount;

        final AtomicBoolean downstreamCancelled;

        final AtomicLong requested;
        long emitted;

        volatile boolean upstreamCanceled;

        volatile boolean upstreamDone;
        volatile boolean openDone;
        final AtomicThrowable error;

        Subscription upstream;

        WindowBoundaryMainSubscriber(Subscriber<? super Flowable<T>> actual,
                Publisher<B> open, Function<? super B, ? extends Publisher<V>> closingIndicator, int bufferSize) {
            this.downstream = actual;
            this.queue = new MpscLinkedQueue<Object>();
            this.open = open;
            this.closingIndicator = closingIndicator;
            this.bufferSize = bufferSize;
            this.resources = new CompositeDisposable();
            this.windows = new ArrayList<UnicastProcessor<T>>();
            this.windowCount = new AtomicLong(1L);
            this.downstreamCancelled = new AtomicBoolean();
            this.error = new AtomicThrowable();
            this.startSubscriber = new WindowStartSubscriber<B>(this);
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;

                downstream.onSubscribe(this);

                open.subscribe(startSubscriber);

                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public void onError(Throwable t) {
            startSubscriber.cancel();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        @Override
        public void onComplete() {
            startSubscriber.cancel();
            resources.dispose();
            upstreamDone = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            if (downstreamCancelled.compareAndSet(false, true)) {
                if (windowCount.decrementAndGet() == 0) {
                    upstream.cancel();
                    startSubscriber.cancel();
                    resources.dispose();
                    error.tryTerminateAndReport();
                    upstreamCanceled = true;
                    drain();
                } else {
                    startSubscriber.cancel();
                }
            }
        }

        @Override
        public void run() {
            if (windowCount.decrementAndGet() == 0) {
                upstream.cancel();
                startSubscriber.cancel();
                resources.dispose();
                error.tryTerminateAndReport();
                upstreamCanceled = true;
                drain();
            }
        }

        void open(B startValue) {
            queue.offer(new WindowStartItem<B>(startValue));
            drain();
        }

        void openError(Throwable t) {
            upstream.cancel();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        void openComplete() {
            openDone = true;
            drain();
        }

        void close(WindowEndSubscriberIntercept<T, V> what) {
            queue.offer(what);
            drain();
        }

        void closeError(Throwable t) {
            upstream.cancel();
            startSubscriber.cancel();
            resources.dispose();
            if (error.tryAddThrowableOrReport(t)) {
                upstreamDone = true;
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<? super Flowable<T>> downstream = this.downstream;
            final SimplePlainQueue<Object> queue = this.queue;
            final List<UnicastProcessor<T>> windows = this.windows;

            for (;;) {
                if (upstreamCanceled) {
                    queue.clear();
                    windows.clear();
                } else {
                    boolean isDone = upstreamDone;
                    Object o = queue.poll();
                    boolean isEmpty = o == null;

                    if (isDone) {
                        if (isEmpty || error.get() != null) {
                            terminateDownstream(downstream);
                            upstreamCanceled = true;
                            continue;
                        }
                    }

                    if (!isEmpty) {
                        if (o instanceof WindowStartItem) {
                            if (!downstreamCancelled.get()) {
                                long emitted = this.emitted;
                                if (requested.get() != emitted) {
                                    this.emitted = ++emitted;

                                    @SuppressWarnings("unchecked")
                                    B startItem = ((WindowStartItem<B>)o).item;

                                    Publisher<V> endSource;
                                    try {
                                        endSource = Objects.requireNonNull(closingIndicator.apply(startItem), "The closingIndicator returned a null Publisher");
                                    } catch (Throwable ex) {
                                        upstream.cancel();
                                        startSubscriber.cancel();
                                        resources.dispose();
                                        Exceptions.throwIfFatal(ex);
                                        error.tryAddThrowableOrReport(ex);
                                        upstreamDone = true;
                                        continue;
                                    }

                                    windowCount.getAndIncrement();
                                    UnicastProcessor<T> newWindow = UnicastProcessor.create(bufferSize, this);
                                    WindowEndSubscriberIntercept<T, V> endSubscriber = new WindowEndSubscriberIntercept<T, V>(this, newWindow);

                                    downstream.onNext(endSubscriber);

                                    if (endSubscriber.tryAbandon()) {
                                        newWindow.onComplete();
                                    } else {
                                        windows.add(newWindow);
                                        resources.add(endSubscriber);
                                        endSource.subscribe(endSubscriber);
                                    }
                                } else {
                                    upstream.cancel();
                                    startSubscriber.cancel();
                                    resources.dispose();
                                    error.tryAddThrowableOrReport(new MissingBackpressureException(FlowableWindowTimed.missingBackpressureMessage(emitted)));
                                    upstreamDone = true;
                                }
                            }
                        }
                        else if (o instanceof WindowEndSubscriberIntercept) {
                            @SuppressWarnings("unchecked")
                            UnicastProcessor<T> w = ((WindowEndSubscriberIntercept<T, V>)o).window;

                            windows.remove(w);
                            resources.delete((Disposable)o);
                            w.onComplete();
                        } else {
                            @SuppressWarnings("unchecked")
                            T item = (T)o;

                            for (UnicastProcessor<T> w : windows) {
                                w.onNext(item);
                            }
                        }

                        continue;
                    }
                    else if (openDone && windows.size() == 0) {
                        upstream.cancel();
                        startSubscriber.cancel();
                        resources.dispose();
                        terminateDownstream(downstream);
                        upstreamCanceled = true;
                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void terminateDownstream(Subscriber<?> downstream) {
            Throwable ex = error.terminate();
            if (ex == null) {
                for (UnicastProcessor<T> w : windows) {
                    w.onComplete();
                }
                downstream.onComplete();
            } else if (ex != ExceptionHelper.TERMINATED) {
                for (UnicastProcessor<T> w : windows) {
                    w.onError(ex);
                }
                downstream.onError(ex);
            }
        }

        static final class WindowStartItem<B> {

            final B item;

            WindowStartItem(B item) {
                this.item = item;
            }
        }

        static final class WindowStartSubscriber<B> extends AtomicReference<Subscription>
        implements FlowableSubscriber<B> {

            private static final long serialVersionUID = -3326496781427702834L;

            final WindowBoundaryMainSubscriber<?, B, ?> parent;

            WindowStartSubscriber(WindowBoundaryMainSubscriber<?, B, ?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(this, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(B t) {
                parent.open(t);
            }

            @Override
            public void onError(Throwable t) {
                parent.openError(t);
            }

            @Override
            public void onComplete() {
                parent.openComplete();
            }

            void cancel() {
                SubscriptionHelper.cancel(this);
            }
        }

        static final class WindowEndSubscriberIntercept<T, V> extends Flowable<T>
        implements FlowableSubscriber<V>, Disposable {

            final WindowBoundaryMainSubscriber<T, ?, V> parent;

            final UnicastProcessor<T> window;

            final AtomicReference<Subscription> upstream;

            final AtomicBoolean once;

            WindowEndSubscriberIntercept(WindowBoundaryMainSubscriber<T, ?, V> parent, UnicastProcessor<T> window) {
                this.parent = parent;
                this.window = window;
                this.upstream = new AtomicReference<Subscription>();
                this.once = new AtomicBoolean();
            }

            @Override
            public void onSubscribe(Subscription s) {
                if (SubscriptionHelper.setOnce(upstream, s)) {
                    s.request(Long.MAX_VALUE);
                }
            }

            @Override
            public void onNext(V t) {
                if (SubscriptionHelper.cancel(upstream)) {
                    parent.close(this);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (isDisposed()) {
                    RxJavaPlugins.onError(t);
                } else {
                    parent.closeError(t);
                }
            }

            @Override
            public void onComplete() {
                parent.close(this);
            }

            @Override
            public void dispose() {
                SubscriptionHelper.cancel(upstream);
            }

            @Override
            public boolean isDisposed() {
                return upstream.get() == SubscriptionHelper.CANCELLED;
            }

            @Override
            protected void subscribeActual(Subscriber<? super T> s) {
                window.subscribe(s);
                once.set(true);
            }

            boolean tryAbandon() {
                return !once.get() && once.compareAndSet(false, true);
            }
        }
    }

}
