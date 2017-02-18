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

package io.reactivex.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.operators.flowable.FlowableGroupJoin.*;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableJoin<TLeft, TRight, TLeftEnd, TRightEnd, R> extends AbstractFlowableWithUpstream<TLeft, R> {

    final Publisher<? extends TRight> other;

    final Function<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd;

    final Function<? super TRight, ? extends Publisher<TRightEnd>> rightEnd;

    final BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector;

    public FlowableJoin(
            Flowable<TLeft> source,
            Publisher<? extends TRight> other,
            Function<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd,
            Function<? super TRight, ? extends Publisher<TRightEnd>> rightEnd,
            BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector) {
        super(source);
        this.other = other;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
        this.resultSelector = resultSelector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {

        JoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R> parent =
                new JoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R>(s, leftEnd, rightEnd, resultSelector);

        s.onSubscribe(parent);

        LeftRightSubscriber left = new LeftRightSubscriber(parent, true);
        parent.disposables.add(left);
        LeftRightSubscriber right = new LeftRightSubscriber(parent, false);
        parent.disposables.add(right);

        source.subscribe(left);
        other.subscribe(right);
    }

    static final class JoinSubscription<TLeft, TRight, TLeftEnd, TRightEnd, R>
    extends AtomicInteger implements Subscription, JoinSupport {


        private static final long serialVersionUID = -6071216598687999801L;

        final Subscriber<? super R> actual;

        final AtomicLong requested;

        final SpscLinkedArrayQueue<Object> queue;

        final CompositeDisposable disposables;

        final Map<Integer, TLeft> lefts;

        final Map<Integer, TRight> rights;

        final AtomicReference<Throwable> error;

        final Function<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd;

        final Function<? super TRight, ? extends Publisher<TRightEnd>> rightEnd;

        final BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector;

        final AtomicInteger active;

        int leftIndex;

        int rightIndex;

        volatile boolean cancelled;

        static final Integer LEFT_VALUE = 1;

        static final Integer RIGHT_VALUE = 2;

        static final Integer LEFT_CLOSE = 3;

        static final Integer RIGHT_CLOSE = 4;

        JoinSubscription(Subscriber<? super R> actual, Function<? super TLeft, ? extends Publisher<TLeftEnd>> leftEnd,
                Function<? super TRight, ? extends Publisher<TRightEnd>> rightEnd,
                        BiFunction<? super TLeft, ? super TRight, ? extends R> resultSelector) {
            this.actual = actual;
            this.requested = new AtomicLong();
            this.disposables = new CompositeDisposable();
            this.queue = new SpscLinkedArrayQueue<Object>(bufferSize());
            this.lefts = new LinkedHashMap<Integer, TLeft>();
            this.rights = new LinkedHashMap<Integer, TRight>();
            this.error = new AtomicReference<Throwable>();
            this.leftEnd = leftEnd;
            this.rightEnd = rightEnd;
            this.resultSelector = resultSelector;
            this.active = new AtomicInteger(2);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
            }
        }

        @Override
        public void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            cancelAll();
            if (getAndIncrement() == 0) {
                queue.clear();
            }
        }

        void cancelAll() {
            disposables.dispose();
        }

        void errorAll(Subscriber<?> a) {
            Throwable ex = ExceptionHelper.terminate(error);

            lefts.clear();
            rights.clear();

            a.onError(ex);
        }

        void fail(Throwable exc, Subscriber<?> a, SimpleQueue<?> q) {
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
            Subscriber<? super R> a = actual;

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

                        Publisher<TLeftEnd> p;

                        try {
                            p = ObjectHelper.requireNonNull(leftEnd.apply(left), "The leftEnd returned a null Publisher");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndSubscriber end = new LeftRightEndSubscriber(this, true, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        long r = requested.get();
                        long e = 0L;

                        for (TRight right : rights.values()) {

                            R w;

                            try {
                                w = ObjectHelper.requireNonNull(resultSelector.apply(left, right), "The resultSelector returned a null value");
                            } catch (Throwable exc) {
                                fail(exc, a, q);
                                return;
                            }

                            if (e != r) {
                                a.onNext(w);

                                e++;
                            } else {
                                ExceptionHelper.addThrowable(error, new MissingBackpressureException("Could not emit value due to lack of requests"));
                                q.clear();
                                cancelAll();
                                errorAll(a);
                                return;
                            }
                        }

                        if (e != 0L) {
                            BackpressureHelper.produced(requested, e);
                        }
                    }
                    else if (mode == RIGHT_VALUE) {
                        @SuppressWarnings("unchecked")
                        TRight right = (TRight)val;

                        int idx = rightIndex++;

                        rights.put(idx, right);

                        Publisher<TRightEnd> p;

                        try {
                            p = ObjectHelper.requireNonNull(rightEnd.apply(right), "The rightEnd returned a null Publisher");
                        } catch (Throwable exc) {
                            fail(exc, a, q);
                            return;
                        }

                        LeftRightEndSubscriber end = new LeftRightEndSubscriber(this, false, idx);
                        disposables.add(end);

                        p.subscribe(end);

                        ex = error.get();
                        if (ex != null) {
                            q.clear();
                            cancelAll();
                            errorAll(a);
                            return;
                        }

                        long r = requested.get();
                        long e = 0L;

                        for (TLeft left : lefts.values()) {

                            R w;

                            try {
                                w = ObjectHelper.requireNonNull(resultSelector.apply(left, right), "The resultSelector returned a null value");
                            } catch (Throwable exc) {
                                fail(exc, a, q);
                                return;
                            }

                            if (e != r) {
                                a.onNext(w);

                                e++;
                            } else {
                                ExceptionHelper.addThrowable(error, new MissingBackpressureException("Could not emit value due to lack of requests"));
                                q.clear();
                                cancelAll();
                                errorAll(a);
                                return;
                            }
                        }

                        if (e != 0L) {
                            BackpressureHelper.produced(requested, e);
                        }
                    }
                    else if (mode == LEFT_CLOSE) {
                        LeftRightEndSubscriber end = (LeftRightEndSubscriber)val;

                        lefts.remove(end.index);
                        disposables.remove(end);
                    }
                    else if (mode == RIGHT_CLOSE) {
                        LeftRightEndSubscriber end = (LeftRightEndSubscriber)val;

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
        public void innerComplete(LeftRightSubscriber sender) {
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
        public void innerClose(boolean isLeft, LeftRightEndSubscriber index) {
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
