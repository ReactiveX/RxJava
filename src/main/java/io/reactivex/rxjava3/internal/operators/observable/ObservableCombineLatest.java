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

import java.util.Objects;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.*;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.util.AtomicThrowable;

public final class ObservableCombineLatest<T, R> extends Observable<R> {
    final ObservableSource<? extends T>[] sources;
    final Iterable<? extends ObservableSource<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> combiner;
    final int bufferSize;
    final boolean delayError;

    public ObservableCombineLatest(ObservableSource<? extends T>[] sources,
            Iterable<? extends ObservableSource<? extends T>> sourcesIterable,
            Function<? super Object[], ? extends R> combiner, int bufferSize,
            boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Observer<? super R> observer) {
        ObservableSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new ObservableSource[8];
            for (ObservableSource<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    ObservableSource<? extends T>[] b = new ObservableSource[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptyDisposable.complete(observer);
            return;
        }

        LatestCoordinator<T, R> lc = new LatestCoordinator<T, R>(observer, combiner, count, bufferSize, delayError);
        lc.subscribe(sources);
    }

    static final class LatestCoordinator<T, R> extends AtomicInteger implements Disposable {

        private static final long serialVersionUID = 8567835998786448817L;
        final Observer<? super R> downstream;
        final Function<? super Object[], ? extends R> combiner;
        final CombinerObserver<T, R>[] observers;
        Object[] latest;
        final SpscLinkedArrayQueue<Object[]> queue;
        final boolean delayError;

        volatile boolean cancelled;

        volatile boolean done;

        final AtomicThrowable errors = new AtomicThrowable();

        int active;
        int complete;

        @SuppressWarnings("unchecked")
        LatestCoordinator(Observer<? super R> actual,
                Function<? super Object[], ? extends R> combiner,
                int count, int bufferSize, boolean delayError) {
            this.downstream = actual;
            this.combiner = combiner;
            this.delayError = delayError;
            this.latest = new Object[count];
            CombinerObserver<T, R>[] as = new CombinerObserver[count];
            for (int i = 0; i < count; i++) {
                as[i] = new CombinerObserver<T, R>(this, i);
            }
            this.observers = as;
            this.queue = new SpscLinkedArrayQueue<Object[]>(bufferSize);
        }

        public void subscribe(ObservableSource<? extends T>[] sources) {
            Observer<T>[] as = observers;
            int len = as.length;
            downstream.onSubscribe(this);
            for (int i = 0; i < len; i++) {
                if (done || cancelled) {
                    return;
                }
                sources[i].subscribe(as[i]);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                cancelSources();
                if (getAndIncrement() == 0) {
                    clear(queue);
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void cancelSources() {
            for (CombinerObserver<T, R> observer : observers) {
                observer.dispose();
            }
        }

        void clear(SpscLinkedArrayQueue<?> q) {
            synchronized (this) {
                latest = null;
            }
            q.clear();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final SpscLinkedArrayQueue<Object[]> q = queue;
            final Observer<? super R> a = downstream;
            final boolean delayError = this.delayError;

            int missed = 1;
            for (;;) {

                for (;;) {
                    if (cancelled) {
                        clear(q);
                        return;
                    }

                    if (!delayError && errors.get() != null) {
                        cancelSources();
                        clear(q);
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    boolean d = done;
                    Object[] s = q.poll();
                    boolean empty = s == null;

                    if (d && empty) {
                        clear(q);
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    R v;

                    try {
                        v = Objects.requireNonNull(combiner.apply(s), "The combiner returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        errors.tryAddThrowableOrReport(ex);
                        cancelSources();
                        clear(q);
                        errors.tryTerminateConsumer(a);
                        return;
                    }

                    a.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        void innerNext(int index, T item) {
            boolean shouldDrain = false;
            synchronized (this) {
                Object[] latest = this.latest;
                if (latest == null) {
                    return;
                }
                Object o = latest[index];
                int a = active;
                if (o == null) {
                    active = ++a;
                }
                latest[index] = item;
                if (a == latest.length) {
                    queue.offer(latest.clone());
                    shouldDrain = true;
                }
            }
            if (shouldDrain) {
                drain();
            }
        }

        void innerError(int index, Throwable ex) {
            if (errors.tryAddThrowableOrReport(ex)) {
                boolean cancelOthers = true;
                if (delayError) {
                    synchronized (this) {
                        Object[] latest = this.latest;
                        if (latest == null) {
                            return;
                        }

                        cancelOthers = latest[index] == null;
                        if (cancelOthers || ++complete == latest.length) {
                            done = true;
                        }
                    }
                }
                if (cancelOthers) {
                    cancelSources();
                }
                drain();
            }
        }

        void innerComplete(int index) {
            boolean cancelOthers = false;
            synchronized (this) {
                Object[] latest = this.latest;
                if (latest == null) {
                    return;
                }

                cancelOthers = latest[index] == null;
                if (cancelOthers || ++complete == latest.length) {
                    done = true;
                }
            }
            if (cancelOthers) {
                cancelSources();
            }
            drain();
        }

    }

    static final class CombinerObserver<T, R> extends AtomicReference<Disposable> implements Observer<T> {
        private static final long serialVersionUID = -4823716997131257941L;

        final LatestCoordinator<T, R> parent;

        final int index;

        CombinerObserver(LatestCoordinator<T, R> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onNext(T t) {
            parent.innerNext(index, t);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(index, t);
        }

        @Override
        public void onComplete() {
            parent.innerComplete(index);
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
