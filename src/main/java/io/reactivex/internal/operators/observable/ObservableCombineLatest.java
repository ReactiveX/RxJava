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

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

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
    public void subscribeActual(Observer<? super R> s) {
        ObservableSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Observable[8];
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
            EmptyDisposable.complete(s);
            return;
        }

        LatestCoordinator<T, R> lc = new LatestCoordinator<T, R>(s, combiner, count, bufferSize, delayError);
        lc.subscribe(sources);
    }

    static final class LatestCoordinator<T, R> extends AtomicInteger implements Disposable {

        private static final long serialVersionUID = 8567835998786448817L;
        final Observer<? super R> actual;
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
            this.actual = actual;
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
            actual.onSubscribe(this);
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
            for (CombinerObserver<T, R> s : observers) {
                s.dispose();
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
            final Observer<? super R> a = actual;
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
                        a.onError(errors.terminate());
                        return;
                    }

                    boolean d = done;
                    Object[] s = q.poll();
                    boolean empty = s == null;

                    if (d && empty) {
                        clear(q);
                        Throwable ex = errors.terminate();
                        if (ex == null) {
                            a.onComplete();
                        } else {
                            a.onError(ex);
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    R v;

                    try {
                        v = ObjectHelper.requireNonNull(combiner.apply(s), "The combiner returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        errors.addThrowable(ex);
                        cancelSources();
                        clear(q);
                        ex = errors.terminate();
                        a.onError(ex);
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
            if (errors.addThrowable(ex)) {
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
            } else {
                RxJavaPlugins.onError(ex);
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
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this, s);
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
