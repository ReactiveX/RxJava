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

import java.util.Arrays;
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
        final T[] latest;
        final SpscLinkedArrayQueue<Object> queue;
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
            this.latest = (T[])new Object[count];
            this.observers = new CombinerObserver[count];
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize);
        }

        public void subscribe(ObservableSource<? extends T>[] sources) {
            Observer<T>[] as = observers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new CombinerObserver<T, R>(this, i);
            }
            lazySet(0); // release array contents
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

        void cancel(SpscLinkedArrayQueue<?> q) {
            clear(q);
            cancelSources();
        }

        void cancelSources() {
            for (CombinerObserver<T, R> s : observers) {
                s.dispose();
            }
        }

        void clear(SpscLinkedArrayQueue<?> q) {
            synchronized (this) {
                Arrays.fill(latest, null);
            }
            q.clear();
        }

        void combine(T value, int index) {
            CombinerObserver<T, R> cs = observers[index];

            int a;
            int c;
            int len;
            boolean empty;
            boolean f;
            synchronized (this) {
                if (cancelled) {
                    return;
                }
                len = latest.length;
                T o = latest[index];
                a = active;
                if (o == null) {
                    active = ++a;
                }
                c = complete;
                if (value == null) {
                    complete = ++c;
                } else {
                    latest[index] = value;
                }
                f = a == len;
                // see if either all sources completed
                empty = c == len
                        || (value == null && o == null); // or this source completed without any value
                if (!empty) {
                    if (value != null && f) {
                        queue.offer(cs, latest.clone());
                    } else
                    if (value == null && errors.get() != null) {
                        done = true; // if this source completed without a value
                    }
                } else {
                    done = true;
                }
            }
            if (!f && value != null) {
                return;
            }
            drain();
        }
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final SpscLinkedArrayQueue<Object> q = queue;
            final Observer<? super R> a = actual;
            final boolean delayError = this.delayError;

            int missed = 1;
            for (;;) {

                if (checkTerminated(done, q.isEmpty(), a, q, delayError)) {
                    return;
                }

                for (;;) {

                    boolean d = done;
                    @SuppressWarnings("unchecked")
                    CombinerObserver<T, R> cs = (CombinerObserver<T, R>)q.poll();
                    boolean empty = cs == null;

                    if (checkTerminated(d, empty, a, q, delayError)) {
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    T[] array = (T[])q.poll();

                    R v;
                    try {
                        v = ObjectHelper.requireNonNull(combiner.apply(array), "The combiner returned a null");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        cancelled = true;
                        cancel(q);
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


        boolean checkTerminated(boolean d, boolean empty, Observer<?> a, SpscLinkedArrayQueue<?> q, boolean delayError) {
            if (cancelled) {
                cancel(q);
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        cancel(q);
                        Throwable e = errors.terminate();
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = errors.get();
                    if (e != null) {
                        cancel(q);
                        a.onError(errors.terminate());
                        return true;
                    } else
                    if (empty) {
                        clear(queue);
                        a.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }

        void onError(Throwable e) {
            if (!errors.addThrowable(e)) {
                RxJavaPlugins.onError(e);
            }
        }
    }

    static final class CombinerObserver<T, R> implements Observer<T> {
        final LatestCoordinator<T, R> parent;
        final int index;

        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

        CombinerObserver(LatestCoordinator<T, R> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(T t) {
            parent.combine(t, index);
        }

        @Override
        public void onError(Throwable t) {
            parent.onError(t);
            parent.combine(null, index);
        }

        @Override
        public void onComplete() {
            parent.combine(null, index);
        }

        public void dispose() {
            DisposableHelper.dispose(s);
        }
    }
}
