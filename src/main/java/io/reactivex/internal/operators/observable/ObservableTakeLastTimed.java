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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;

public final class ObservableTakeLastTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long count;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    final int bufferSize;
    final boolean delayError;

    public ObservableTakeLastTimed(ObservableSource<T> source,
            long count, long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
        super(source);
        this.count = count;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new TakeLastTimedObserver<T>(t, count, time, unit, scheduler, bufferSize, delayError));
    }

    static final class TakeLastTimedObserver<T>
    extends AtomicBoolean implements Observer<T>, Disposable {

        private static final long serialVersionUID = -5677354903406201275L;
        final Observer<? super T> actual;
        final long count;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;
        final SpscLinkedArrayQueue<Object> queue;
        final boolean delayError;

        Disposable d;

        volatile boolean cancelled;

        Throwable error;

        TakeLastTimedObserver(Observer<? super T> actual, long count, long time, TimeUnit unit, Scheduler scheduler, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.count = count;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize);
            this.delayError = delayError;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            final SpscLinkedArrayQueue<Object> q = queue;

            long now = scheduler.now(unit);
            long time = this.time;
            long c = count;
            boolean unbounded = c == Long.MAX_VALUE;

            q.offer(now, t);

            while (!q.isEmpty()) {
                long ts = (Long)q.peek();
                if (ts <= now - time || (!unbounded && (q.size() >> 1) > c)) {
                    q.poll();
                    q.poll();
                } else {
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            drain();
        }

        @Override
        public void onComplete() {
            drain();
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                d.dispose();

                if (compareAndSet(false, true)) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void drain() {
            if (!compareAndSet(false, true)) {
                return;
            }

            final Observer<? super T> a = actual;
            final SpscLinkedArrayQueue<Object> q = queue;
            final boolean delayError = this.delayError;

            for (;;) {
                if (cancelled) {
                    q.clear();
                    return;
                }

                if (!delayError) {
                    Throwable ex = error;
                    if (ex != null) {
                        q.clear();
                        a.onError(ex);
                        return;
                    }
                }

                Object ts = q.poll(); // the timestamp long
                boolean empty = ts == null;

                if (empty) {
                    Throwable ex = error;
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return;
                }

                @SuppressWarnings("unchecked")
                T o = (T)q.poll();

                if ((Long)ts < scheduler.now(unit) - time) {
                    continue;
                }

                a.onNext(o);
            }
        }
    }
}
