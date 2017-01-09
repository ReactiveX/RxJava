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

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.disposables.ArrayCompositeDisposable;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;

public final class ObservableSequenceEqual<T> extends Observable<Boolean> {
    final ObservableSource<? extends T> first;
    final ObservableSource<? extends T> second;
    final BiPredicate<? super T, ? super T> comparer;
    final int bufferSize;

    public ObservableSequenceEqual(ObservableSource<? extends T> first, ObservableSource<? extends T> second,
                                   BiPredicate<? super T, ? super T> comparer, int bufferSize) {
        this.first = first;
        this.second = second;
        this.comparer = comparer;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Observer<? super Boolean> s) {
        EqualCoordinator<T> ec = new EqualCoordinator<T>(s, bufferSize, first, second, comparer);
        s.onSubscribe(ec);
        ec.subscribe();
    }

    static final class EqualCoordinator<T> extends AtomicInteger implements Disposable {

        private static final long serialVersionUID = -6178010334400373240L;
        final Observer<? super Boolean> actual;
        final BiPredicate<? super T, ? super T> comparer;
        final ArrayCompositeDisposable resources;
        final ObservableSource<? extends T> first;
        final ObservableSource<? extends T> second;
        final EqualObserver<T>[] observers;

        volatile boolean cancelled;

        T v1;

        T v2;

        EqualCoordinator(Observer<? super Boolean> actual, int bufferSize,
                                ObservableSource<? extends T> first, ObservableSource<? extends T> second,
                                BiPredicate<? super T, ? super T> comparer) {
            this.actual = actual;
            this.first = first;
            this.second = second;
            this.comparer = comparer;
            @SuppressWarnings("unchecked")
            EqualObserver<T>[] as = new EqualObserver[2];
            this.observers = as;
            as[0] = new EqualObserver<T>(this, 0, bufferSize);
            as[1] = new EqualObserver<T>(this, 1, bufferSize);
            this.resources = new ArrayCompositeDisposable(2);
        }

        boolean setDisposable(Disposable s, int index) {
            return resources.setResource(index, s);
        }

        void subscribe() {
            EqualObserver<T>[] as = observers;
            first.subscribe(as[0]);
            second.subscribe(as[1]);
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                resources.dispose();

                if (getAndIncrement() == 0) {
                    EqualObserver<T>[] as = observers;
                    as[0].queue.clear();
                    as[1].queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void cancel(SpscLinkedArrayQueue<T> q1, SpscLinkedArrayQueue<T> q2) {
            cancelled = true;
            q1.clear();
            q2.clear();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            EqualObserver<T>[] as = observers;

            final EqualObserver<T> s1 = as[0];
            final SpscLinkedArrayQueue<T> q1 = s1.queue;
            final EqualObserver<T> s2 = as[1];
            final SpscLinkedArrayQueue<T> q2 = s2.queue;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        q1.clear();
                        q2.clear();
                        return;
                    }

                    boolean d1 = s1.done;

                    if (d1) {
                        Throwable e = s1.error;
                        if (e != null) {
                            cancel(q1, q2);

                            actual.onError(e);
                            return;
                        }
                    }

                    boolean d2 = s2.done;
                    if (d2) {
                        Throwable e = s2.error;
                        if (e != null) {
                            cancel(q1, q2);

                            actual.onError(e);
                            return;
                        }
                    }

                    if (v1 == null) {
                        v1 = q1.poll();
                    }
                    boolean e1 = v1 == null;

                    if (v2 == null) {
                        v2 = q2.poll();
                    }
                    boolean e2 = v2 == null;

                    if (d1 && d2 && e1 && e2) {
                        actual.onNext(true);
                        actual.onComplete();
                        return;
                    }
                    if ((d1 && d2) && (e1 != e2)) {
                        cancel(q1, q2);

                        actual.onNext(false);
                        actual.onComplete();
                        return;
                    }

                    if (!e1 && !e2) {
                        boolean c;

                        try {
                            c = comparer.test(v1, v2);
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            cancel(q1, q2);

                            actual.onError(ex);
                            return;
                        }

                        if (!c) {
                            cancel(q1, q2);

                            actual.onNext(false);
                            actual.onComplete();
                            return;
                        }

                        v1 = null;
                        v2 = null;
                    }

                    if (e1 || e2) {
                        break;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class EqualObserver<T> implements Observer<T> {
        final EqualCoordinator<T> parent;
        final SpscLinkedArrayQueue<T> queue;
        final int index;

        volatile boolean done;
        Throwable error;

        EqualObserver(EqualCoordinator<T> parent, int index, int bufferSize) {
            this.parent = parent;
            this.index = index;
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }

        @Override
        public void onSubscribe(Disposable s) {
            parent.setDisposable(s, index);
        }

        @Override
        public void onNext(T t) {
            queue.offer(t);
            parent.drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            parent.drain();
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }
    }
}
