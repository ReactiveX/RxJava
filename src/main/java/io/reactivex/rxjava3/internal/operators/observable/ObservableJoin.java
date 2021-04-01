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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.operators.observable.ObservableGroupJoin.*;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

public final class ObservableJoin<TLeft, TRight, TLeftEnd, TRightEnd, R> extends AbstractObservableWithUpstream<TLeft, R> {

    final ObservableSource<? extends TRight> other;

    final Function<? super TLeft, ? extends ObservableSource<TLeftEnd>> leftEnd;

    final Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd;

    final BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector;

    public ObservableJoin(
            ObservableSource<TLeft> source,
            ObservableSource<? extends TRight> other,
            Function<? super TLeft, ? extends ObservableSource<TLeftEnd>> leftEnd,
            Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
            BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector) {
        super(source);
        this.other = other;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.resultSelector = resultSelector;
    }

    @Override
    protected void subscribeActual(Observer<? super R> observer) {

        JoinDisposable<TLeft, TRight, TLeftEnd, TRightEnd, R> parent =
                new JoinDisposable<>(
                        observer, leftEnd, rightEnd, resultSelector);

        observer.onSubscribe(parent);

        LeftRightObserver left = new LeftRightObserver(parent, true);
        parent.disposables.add(left);
        LeftRightObserver right = new LeftRightObserver(parent, false);
        parent.disposables.add(right);

        source.subscribe(left);
        other.subscribe(right);
    }

    static final class JoinDisposable<TLeft, TRight, TLeftEnd, TRightEnd, R>
    extends AtomicInteger implements Disposable, JoinSupport {

        private static final long serialVersionUID = -6071216598687999801L;

        final Observer<? super R> downstream;

        final SpscLinkedArrayQueue<Object> queue;

        final CompositeDisposable disposables;

        final Map<Integer, TLeft> lefts;

        final Map<Integer, TRight> rights;

        final AtomicReference<Throwable> error;

        final Function<? super TLeft, ? extends ObservableSource<TLeftEnd>> leftEnd;

        final Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd;

        final BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector;

        final AtomicInteger active;

        int leftIndex;

        int rightIndex;

        volatile boolean cancelled;

        static final Integer LEFT_VALUE = 1;

        static final Integer RIGHT_VALUE = 2;

        static final Integer LEFT_CLOSE = 3;

        static final Integer RIGHT_CLOSE = 4;

        JoinDisposable(Observer<? super R> actual,
                Function<? super TLeft, ? extends ObservableSource<TLeftEnd>> leftEnd,
                Function<? super TRight, ? extends ObservableSource<TRightEnd>> rightEnd,
                        BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector) {
            this.downstream = actual;
            this.disposables = new CompositeDisposable();
            this.queue = new SpscLinkedArrayQueue<>(bufferSize());
            this.lefts = new LinkedHashMap<>();
            this.rights = new LinkedHashMap<>();
            this.error = new AtomicReference<>();
            this.leftEnd = leftEnd;
            this.rightEnd = rightEnd;
            this.resultSelector = resultSelector;
            this.active = new AtomicInteger(2);
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                cancelAll();
                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void cancelAll() {
            disposables.dispose();
        }

        void errorAll(Observer<?> a) {
            Throwable ex = ExceptionHelper.terminate(error);

            lefts.clear();
            rights.clear();

            a.onError(ex);
        }

        void fail(Throwable exc, Observer<?> a, SpscLinkedArrayQueue<?> q) {
            Exceptions.throwIfFatal(exc);
            ExceptionHelper.addThrowable(error, exc);
            q.clear();
            cancelAll();
            errorAll(a);
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            SpscLinkedArrayQueue<Object> q = queue;
            Observer<? super R> a = downstream;

            for (;;) {
                for (;;) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        q.clear();
                        cancelAll();
                        errorAll(a);
                        return;
                    }

                    boolean d = active.get() == 0;

                    Integer mode = (Integer)q.poll();

                    boolean empty = mode == null;

                    if (d && empty) {

                        lefts.clear();
                        rights.clear();
                        disposables.dispose();

                        a.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    Object val = q.poll();

                    if (mode == LEFT_VALUE) {
                        @SuppressWarnings("unchecked")
                        TLeft left = (TLeft)val;

                        int idx = leftIndex++;
                        lefts.put(idx, left);

                        ObservableSource<TLeftEnd> p;

                        try {
                            p = Objects.requireNonNull(leftEnd.apply(left), "The leftEnd returned a null ObservableSource");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndObserver end = new LeftRightEndObserver(this, true, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        for (TRight right : rights.values()) {

                            R w;

                            try {
                                w = Objects.requireNonNull(resultSelector.apply(left, right), "The resultSelector returned a null value");
                            } catch (Throwable exc) {
                                fail(exc, a, q);
                                return;
                            }

                            a.onNext(w);
                        }
                    }
                    else if (mode == RIGHT_VALUE) {
                        @SuppressWarnings("unchecked")
                        TRight right = (TRight)val;

                        int idx = rightIndex++;

                        rights.put(idx, right);

                        ObservableSource<TRightEnd> p;

                        try {
                            p = Objects.requireNonNull(rightEnd.apply(right), "The rightEnd returned a null ObservableSource");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndObserver end = new LeftRightEndObserver(this, false, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        for (TLeft left : lefts.values()) {

                            R w;

                            try {
                                w = Objects.requireNonNull(resultSelector.apply(left, right), "The resultSelector returned a null value");
                            } catch (Throwable exc) {
                                fail(exc, a, q);
                                return;
                            }

                            a.onNext(w);
                        }
                    }
                    else if (mode == LEFT_CLOSE) {
                        LeftRightEndObserver end = (LeftRightEndObserver)val;

                        lefts.remove(end.index);
                        disposables.remove(end);
                    } else {
                        LeftRightEndObserver end = (LeftRightEndObserver)val;

                        rights.remove(end.index);
                        disposables.remove(end);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void innerError(Throwable ex) {
            if (ExceptionHelper.addThrowable(error, ex)) {
                active.decrementAndGet();
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void innerComplete(LeftRightObserver sender) {
            disposables.delete(sender);
            active.decrementAndGet();
            drain();
        }

        @Override
        public void innerValue(boolean isLeft, Object o) {
            synchronized (this) {
                queue.offer(isLeft ? LEFT_VALUE : RIGHT_VALUE, o);
            }
            drain();
        }

        @Override
        public void innerClose(boolean isLeft, LeftRightEndObserver index) {
            synchronized (this) {
                queue.offer(isLeft ? LEFT_CLOSE : RIGHT_CLOSE, index);
            }
            drain();
        }

        @Override
        public void innerCloseError(Throwable ex) {
            if (ExceptionHelper.addThrowable(error, ex)) {
                drain();
            } else {
                RxJavaPlugins.onError(ex);
            }
        }
    }
}
