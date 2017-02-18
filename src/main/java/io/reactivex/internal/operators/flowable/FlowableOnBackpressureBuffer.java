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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Action;
import io.reactivex.internal.fuseable.SimplePlainQueue;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableOnBackpressureBuffer<T> extends AbstractFlowableWithUpstream<T, T> {
    final int bufferSize;
    final boolean unbounded;
    final boolean delayError;
    final Action onOverflow;

    public FlowableOnBackpressureBuffer(Flowable<T> source, int bufferSize, boolean unbounded,
            boolean delayError, Action onOverflow) {
        super(source);
        this.bufferSize = bufferSize;
        this.unbounded = unbounded;
        this.delayError = delayError;
        this.onOverflow = onOverflow;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new BackpressureBufferSubscriber<T>(s, bufferSize, unbounded, delayError, onOverflow));
    }

    static final class BackpressureBufferSubscriber<T> extends BasicIntQueueSubscription<T> implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -2514538129242366402L;

        final Subscriber<? super T> actual;
        final SimplePlainQueue<T> queue;
        final boolean delayError;
        final Action onOverflow;

        Subscription s;

        volatile boolean cancelled;

        volatile boolean done;
        Throwable error;

        final AtomicLong requested = new AtomicLong();

        boolean outputFused;

        BackpressureBufferSubscriber(Subscriber<? super T> actual, int bufferSize,
                boolean unbounded, boolean delayError, Action onOverflow) {
            this.actual = actual;
            this.onOverflow = onOverflow;
            this.delayError = delayError;

            SimplePlainQueue<T> q;

            if (unbounded) {
                q = new SpscLinkedArrayQueue<T>(bufferSize);
            } else {
                q = new SpscArrayQueue<T>(bufferSize);
            }

            this.queue = q;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                s.cancel();
                MissingBackpressureException ex = new MissingBackpressureException("Buffer is full");
                try {
                    onOverflow.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    ex.initCause(e);
                }
                onError(ex);
                return;
            }
            if (outputFused) {
                actual.onNext(null);
            } else {
                drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            if (outputFused) {
                actual.onError(t);
            } else {
                drain();
            }
        }

        @Override
        public void onComplete() {
            done = true;
            if (outputFused) {
                actual.onComplete();
            } else {
                drain();
            }
        }

        @Override
        public void request(long n) {
            if (!outputFused) {
                if (SubscriptionHelper.validate(n)) {
                    BackpressureHelper.add(requested, n);
                    drain();
                }
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                s.cancel();

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        void drain() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                final SimplePlainQueue<T> q = queue;
                final Subscriber<? super T> a = actual;
                for (;;) {

                    if (checkTerminated(done, q.isEmpty(), a)) {
                        return;
                    }

                    long r = requested.get();

                    long e = 0L;

                    while (e != r) {
                        boolean d = done;
                        T v = q.poll();
                        boolean empty = v == null;

                        if (checkTerminated(d, empty, a)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(v);

                        e++;
                    }

                    if (e == r) {
                        boolean d = done;
                        boolean empty = q.isEmpty();

                        if (checkTerminated(d, empty, a)) {
                            return;
                        }
                    }

                    if (e != 0L) {
                        if (r != Long.MAX_VALUE) {
                            requested.addAndGet(-e);
                        }
                    }

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a) {
            if (cancelled) {
                queue.clear();
                return true;
            }
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        a.onComplete();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            return queue.poll();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}
