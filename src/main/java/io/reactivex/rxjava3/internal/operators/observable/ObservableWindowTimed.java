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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.fuseable.SimplePlainQueue;
import io.reactivex.rxjava3.internal.queue.MpscLinkedQueue;
import io.reactivex.rxjava3.subjects.UnicastSubject;

public final class ObservableWindowTimed<T> extends AbstractObservableWithUpstream<T, Observable<T>> {
    final long timespan;
    final long timeskip;
    final TimeUnit unit;
    final Scheduler scheduler;
    final long maxSize;
    final int bufferSize;
    final boolean restartTimerOnMaxSize;

    public ObservableWindowTimed(Observable<T> source,
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
    protected void subscribeActual(Observer<? super Observable<T>> downstream) {
        if (timespan == timeskip) {
            if (maxSize == Long.MAX_VALUE) {
                source.subscribe(new WindowExactUnboundedObserver<T>(
                        downstream,
                        timespan, unit, scheduler, bufferSize));
                return;
            }
            source.subscribe(new WindowExactBoundedObserver<T>(
                        downstream,
                        timespan, unit, scheduler,
                        bufferSize, maxSize, restartTimerOnMaxSize));
            return;
        }
        source.subscribe(new WindowSkipObserver<T>(downstream,
                timespan, timeskip, unit, scheduler.createWorker(), bufferSize));
    }

    abstract static class AbstractWindowObserver<T>
    extends AtomicInteger
    implements Observer<T>, Disposable {
        private static final long serialVersionUID = 5724293814035355511L;

        final Observer<? super Observable<T>> downstream;

        final SimplePlainQueue<Object> queue;

        final long timespan;
        final TimeUnit unit;
        final int bufferSize;

        long emitted;

        volatile boolean done;
        Throwable error;

        Disposable upstream;

        final AtomicBoolean downstreamCancelled;

        volatile boolean upstreamCancelled;

        final AtomicInteger windowCount;

        AbstractWindowObserver(Observer<? super Observable<T>> downstream, long timespan, TimeUnit unit, int bufferSize) {
            this.downstream = downstream;
            this.queue = new MpscLinkedQueue<Object>();
            this.timespan = timespan;
            this.unit = unit;
            this.bufferSize = bufferSize;
            this.downstreamCancelled = new AtomicBoolean();
            this.windowCount = new AtomicInteger(1);
        }

        @Override
        public final void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);

                createFirstWindow();
            }
        }

        abstract void createFirstWindow();

        @Override
        public final void onNext(T t) {
            queue.offer(t);
            drain();
        }

        @Override
        public final void onError(Throwable t) {
            error = t;
            done = true;
            drain();
        }

        @Override
        public final void onComplete() {
            done = true;
            drain();
        }

        @Override
        public final void dispose() {
            if (downstreamCancelled.compareAndSet(false, true)) {
                windowDone();
            }
        }

        @Override
        public final boolean isDisposed() {
            return downstreamCancelled.get();
        }

        final void windowDone() {
            if (windowCount.decrementAndGet() == 0) {
                cleanupResources();
                upstream.dispose();
                upstreamCancelled = true;
                drain();
            }
        }

        abstract void cleanupResources();

        abstract void drain();
    }

    static final class WindowExactUnboundedObserver<T>
            extends AbstractWindowObserver<T>
            implements Runnable {

        private static final long serialVersionUID = 1155822639622580836L;

        final Scheduler scheduler;

        UnicastSubject<T> window;

        final SequentialDisposable timer;

        static final Object NEXT_WINDOW = new Object();

        final Runnable windowRunnable;

        WindowExactUnboundedObserver(Observer<? super Observable<T>> actual, long timespan, TimeUnit unit,
                Scheduler scheduler, int bufferSize) {
            super(actual, timespan, unit, bufferSize);
            this.scheduler = scheduler;
            this.timer = new SequentialDisposable();
            this.windowRunnable = new WindowRunnable();
        }

        @Override
        void createFirstWindow() {
            if (!downstreamCancelled.get()) {
                windowCount.getAndIncrement();
                window = UnicastSubject.create(bufferSize, windowRunnable);

                emitted = 1;

                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                downstream.onNext(intercept);

                timer.replace(scheduler.schedulePeriodicallyDirect(this, timespan, timespan, unit));

                if (intercept.tryAbandon()) {
                    window.onComplete();
                }
            }
        }

        @Override
        public void run() {
            queue.offer(NEXT_WINDOW);
            drain();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final SimplePlainQueue<Object> queue = this.queue;
            final Observer<? super Observable<T>> downstream = this.downstream;
            UnicastSubject<T> window = this.window;

            int missed = 1;
            for (;;) {

                if (upstreamCancelled) {
                    queue.clear();
                    window = null;
                    this.window = null;
                } else {
                    boolean isDone = done;
                    Object o = queue.poll();
                    boolean isEmpty = o == null;

                    if (isDone && isEmpty) {
                        Throwable ex = error;
                        if (ex != null) {
                            if (window != null) {
                                window.onError(ex);
                            }
                            downstream.onError(ex);
                        } else {
                            if (window != null) {
                                window.onComplete();
                            }
                            downstream.onComplete();
                        }
                        cleanupResources();
                        upstreamCancelled = true;
                        continue;
                    }
                    else if (!isEmpty) {

                        if (o == NEXT_WINDOW) {
                            if (window != null) {
                                window.onComplete();
                                window = null;
                                this.window = null;
                            }
                            if (downstreamCancelled.get()) {
                                timer.dispose();
                            } else {
                                emitted++;

                                windowCount.getAndIncrement();
                                window = UnicastSubject.create(bufferSize, windowRunnable);
                                this.window = window;

                                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                                downstream.onNext(intercept);

                                if (intercept.tryAbandon()) {
                                    window.onComplete();
                                }
                            }
                        } else if (window != null) {
                            @SuppressWarnings("unchecked")
                            T item = (T)o;
                            window.onNext(item);
                        }

                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        void cleanupResources() {
            timer.dispose();
        }

        final class WindowRunnable implements Runnable {
            @Override
            public void run() {
                windowDone();
            }
        }
    }

    static final class WindowExactBoundedObserver<T>
    extends AbstractWindowObserver<T>
    implements Runnable {
        private static final long serialVersionUID = -6130475889925953722L;

        final Scheduler scheduler;
        final boolean restartTimerOnMaxSize;
        final long maxSize;
        final Scheduler.Worker worker;

        long count;

        UnicastSubject<T> window;

        final SequentialDisposable timer;

        WindowExactBoundedObserver(
                Observer<? super Observable<T>> actual,
                long timespan, TimeUnit unit, Scheduler scheduler,
                int bufferSize, long maxSize, boolean restartTimerOnMaxSize) {
            super(actual, timespan, unit, bufferSize);
            this.scheduler = scheduler;
            this.maxSize = maxSize;
            this.restartTimerOnMaxSize = restartTimerOnMaxSize;
            if (restartTimerOnMaxSize) {
                worker = scheduler.createWorker();
            } else {
                worker = null;
            }
            this.timer = new SequentialDisposable();
        }

        @Override
        void createFirstWindow() {
            if (!downstreamCancelled.get()) {
                emitted = 1;

                windowCount.getAndIncrement();
                window = UnicastSubject.create(bufferSize, this);

                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                downstream.onNext(intercept);

                Runnable boundaryTask = new WindowBoundaryRunnable(this, 1L);
                if (restartTimerOnMaxSize) {
                    timer.replace(worker.schedulePeriodically(boundaryTask, timespan, timespan, unit));
                } else {
                    timer.replace(scheduler.schedulePeriodicallyDirect(boundaryTask, timespan, timespan, unit));
                }

                if (intercept.tryAbandon()) {
                    window.onComplete();
                }
            }
        }

        @Override
        public void run() {
            windowDone();
        }

        @Override
        void cleanupResources() {
            timer.dispose();
            Worker w = worker;
            if (w != null) {
                w.dispose();
            }
        }

        void boundary(WindowBoundaryRunnable sender) {
            queue.offer(sender);
            drain();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final SimplePlainQueue<Object> queue = this.queue;
            final Observer<? super Observable<T>> downstream = this.downstream;
            UnicastSubject<T> window = this.window;

            for (;;) {

                if (upstreamCancelled) {
                    queue.clear();
                    window = null;
                    this.window = null;
                } else {

                    boolean isDone = done;
                    Object o = queue.poll();
                    boolean isEmpty = o == null;

                    if (isDone && isEmpty) {
                        Throwable ex = error;
                        if (ex != null) {
                            if (window != null) {
                                window.onError(ex);
                            }
                            downstream.onError(ex);
                        } else {
                            if (window != null) {
                                window.onComplete();
                            }
                            downstream.onComplete();
                        }
                        cleanupResources();
                        upstreamCancelled = true;
                        continue;
                    } else if (!isEmpty) {
                        if (o instanceof WindowBoundaryRunnable) {
                            WindowBoundaryRunnable boundary = (WindowBoundaryRunnable) o;
                            if (boundary.index == emitted || !restartTimerOnMaxSize) {
                                this.count = 0;
                                window = createNewWindow(window);
                            }
                        } else if (window != null) {
                            @SuppressWarnings("unchecked")
                            T item = (T)o;
                            window.onNext(item);

                            long count = this.count + 1;
                            if (count == maxSize) {
                                this.count = 0;
                                window = createNewWindow(window);
                            } else {
                                this.count = count;
                            }
                        }

                        continue;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        UnicastSubject<T> createNewWindow(UnicastSubject<T> window) {
            if (window != null) {
                window.onComplete();
                window = null;
            }

            if (downstreamCancelled.get()) {
                cleanupResources();
            } else {
                long emitted = this.emitted;
                this.emitted = ++emitted;

                windowCount.getAndIncrement();
                window = UnicastSubject.create(bufferSize, this);
                this.window = window;

                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                downstream.onNext(intercept);

                if (restartTimerOnMaxSize) {
                    timer.update(worker.schedulePeriodically(new WindowBoundaryRunnable(this, emitted), timespan, timespan, unit));
                }

                if (intercept.tryAbandon()) {
                    window.onComplete();
                }
            }

            return window;
        }

        static final class WindowBoundaryRunnable implements Runnable {

            final WindowExactBoundedObserver<?> parent;

            final long index;

            WindowBoundaryRunnable(WindowExactBoundedObserver<?> parent, long index) {
                this.parent = parent;
                this.index = index;
            }

            @Override
            public void run() {
                parent.boundary(this);
            }
        }
    }

    static final class WindowSkipObserver<T>
    extends AbstractWindowObserver<T>
    implements Runnable {
        private static final long serialVersionUID = -7852870764194095894L;

        final long timeskip;
        final Scheduler.Worker worker;

        final List<UnicastSubject<T>> windows;

        WindowSkipObserver(Observer<? super Observable<T>> actual,
                long timespan, long timeskip, TimeUnit unit,
                Worker worker, int bufferSize) {
            super(actual, timespan, unit, bufferSize);
            this.timeskip = timeskip;
            this.worker = worker;
            this.windows = new LinkedList<UnicastSubject<T>>();
        }

        @Override
        void createFirstWindow() {
            if (!downstreamCancelled.get()) {
                emitted = 1;

                windowCount.getAndIncrement();
                UnicastSubject<T> window = UnicastSubject.create(bufferSize, this);
                windows.add(window);

                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                downstream.onNext(intercept);

                worker.schedule(new WindowBoundaryRunnable(this, false), timespan, unit);
                worker.schedulePeriodically(new WindowBoundaryRunnable(this, true), timeskip, timeskip, unit);

                if (intercept.tryAbandon()) {
                    window.onComplete();
                    windows.remove(window);
                }
            }
        }

        @Override
        void cleanupResources() {
            worker.dispose();
        }

        @Override
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final SimplePlainQueue<Object> queue = this.queue;
            final Observer<? super Observable<T>> downstream = this.downstream;
            final List<UnicastSubject<T>> windows = this.windows;

            for (;;) {
                if (upstreamCancelled) {
                    queue.clear();
                    windows.clear();
                } else {
                    boolean isDone = done;
                    Object o = queue.poll();
                    boolean isEmpty = o == null;

                    if (isDone && isEmpty) {
                        Throwable ex = error;
                        if (ex != null) {
                            for (UnicastSubject<T> window : windows) {
                                window.onError(ex);
                            }
                            downstream.onError(ex);
                        } else {
                            for (UnicastSubject<T> window : windows) {
                                window.onComplete();
                            }
                            downstream.onComplete();
                        }
                        cleanupResources();
                        upstreamCancelled = true;
                        continue;
                    } else if (!isEmpty) {
                        if (o == WINDOW_OPEN) {
                            if (!downstreamCancelled.get()) {
                                long emitted = this.emitted;
                                this.emitted = ++emitted;

                                windowCount.getAndIncrement();
                                UnicastSubject<T> window = UnicastSubject.create(bufferSize, this);
                                windows.add(window);

                                ObservableWindowSubscribeIntercept<T> intercept = new ObservableWindowSubscribeIntercept<T>(window);
                                downstream.onNext(intercept);

                                worker.schedule(new WindowBoundaryRunnable(this, false), timespan, unit);

                                if (intercept.tryAbandon()) {
                                    window.onComplete();
                                }
                            }
                        } else if (o == WINDOW_CLOSE) {
                            if (!windows.isEmpty()) {
                                windows.remove(0).onComplete();
                            }
                        } else {
                            @SuppressWarnings("unchecked")
                            T item = (T)o;
                            for (UnicastSubject<T> window : windows) {
                                window.onNext(item);
                            }
                        }
                        continue;
                    }
                }
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void run() {
            windowDone();
        }

        void boundary(boolean isOpen) {
            queue.offer(isOpen ? WINDOW_OPEN : WINDOW_CLOSE);
            drain();
        }

        static final Object WINDOW_OPEN = new Object();
        static final Object WINDOW_CLOSE = new Object();

        static final class WindowBoundaryRunnable implements Runnable {

            final WindowSkipObserver<?> parent;

            final boolean isOpen;

            WindowBoundaryRunnable(WindowSkipObserver<?> parent, boolean isOpen) {
                this.parent = parent;
                this.isOpen = isOpen;
            }

            @Override
            public void run() {
                parent.boundary(isOpen);
            }
        }
    }

}
