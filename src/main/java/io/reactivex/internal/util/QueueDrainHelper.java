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
package io.reactivex.internal.util;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.*;

/**
 * Utility class to help with the queue-drain serialization idiom.
 */
public final class QueueDrainHelper {
    /** Utility class. */
    private QueueDrainHelper() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Drain the queue but give up with an error if there aren't enough requests.
     * @param <T> the queue value type
     * @param <U> the emission value type
     * @param q the queue
     * @param a the subscriber
     * @param delayError true if errors should be delayed after all normal items
     * @param dispose the disposable to call when termination happens and cleanup is necessary
     * @param qd the QueueDrain instance that gives status information to the drain logic
     */
    public static <T, U> void drainMaxLoop(SimplePlainQueue<T> q, Subscriber<? super U> a, boolean delayError,
            Disposable dispose, QueueDrain<T, U> qd) {
        int missed = 1;

        for (;;) {
            for (;;) {
                boolean d = qd.done();

                T v = q.poll();

                boolean empty = v == null;

                if (checkTerminated(d, empty, a, delayError, q, qd)) {
                    if (dispose != null) {
                        dispose.dispose();
                    }
                    return;
                }

                if (empty) {
                    break;
                }

                long r = qd.requested();
                if (r != 0L) {
                    if (qd.accept(a, v)) {
                        if (r != Long.MAX_VALUE) {
                            qd.produced(1);
                        }
                    }
                } else {
                    q.clear();
                    if (dispose != null) {
                        dispose.dispose();
                    }
                    a.onError(new MissingBackpressureException("Could not emit value due to lack of requests."));
                    return;
                }
            }

            missed = qd.leave(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    public static <T, U> boolean checkTerminated(boolean d, boolean empty,
            Subscriber<?> s, boolean delayError, SimpleQueue<?> q, QueueDrain<T, U> qd) {
        if (qd.cancelled()) {
            q.clear();
            return true;
        }

        if (d) {
            if (delayError) {
                if (empty) {
                    Throwable err = qd.error();
                    if (err != null) {
                        s.onError(err);
                    } else {
                        s.onComplete();
                    }
                    return true;
                }
            } else {
                Throwable err = qd.error();
                if (err != null) {
                    q.clear();
                    s.onError(err);
                    return true;
                } else
                if (empty) {
                    s.onComplete();
                    return true;
                }
            }
        }

        return false;
    }

    public static <T, U> void drainLoop(SimplePlainQueue<T> q, Observer<? super U> a, boolean delayError, Disposable dispose, ObservableQueueDrain<T, U> qd) {

        int missed = 1;

        for (;;) {
            if (checkTerminated(qd.done(), q.isEmpty(), a, delayError, q, dispose, qd)) {
                return;
            }

            for (;;) {
                boolean d = qd.done();
                T v = q.poll();
                boolean empty = v == null;

                if (checkTerminated(d, empty, a, delayError, q, dispose, qd)) {
                    return;
                }

                if (empty) {
                    break;
                }

                qd.accept(a, v);
            }

            missed = qd.leave(-missed);
            if (missed == 0) {
                break;
            }
        }
    }

    public static <T, U> boolean checkTerminated(boolean d, boolean empty,
            Observer<?> s, boolean delayError, SimpleQueue<?> q, Disposable disposable, ObservableQueueDrain<T, U> qd) {
        if (qd.cancelled()) {
            q.clear();
            disposable.dispose();
            return true;
        }

        if (d) {
            if (delayError) {
                if (empty) {
                    if (disposable != null) {
                        disposable.dispose();
                    }
                    Throwable err = qd.error();
                    if (err != null) {
                        s.onError(err);
                    } else {
                        s.onComplete();
                    }
                    return true;
                }
            } else {
                Throwable err = qd.error();
                if (err != null) {
                    q.clear();
                    if (disposable != null) {
                        disposable.dispose();
                    }
                    s.onError(err);
                    return true;
                } else
                if (empty) {
                    if (disposable != null) {
                        disposable.dispose();
                    }
                    s.onComplete();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a queue: spsc-array if capacityHint is positive and
     * spsc-linked-array if capacityHint is negative; in both cases, the
     * capacity is the absolute value of prefetch.
     * @param <T> the value type of the queue
     * @param capacityHint the capacity hint, negative value will create an array-based SPSC queue
     * @return the queue instance
     */
    public static <T> SimpleQueue<T> createQueue(int capacityHint) {
        if (capacityHint < 0) {
            return new SpscLinkedArrayQueue<T>(-capacityHint);
        }
        return new SpscArrayQueue<T>(capacityHint);
    }

    /**
     * Requests Long.MAX_VALUE if prefetch is negative or the exact
     * amount if prefetch is positive.
     * @param s the Subscription to request from
     * @param prefetch the prefetch value
     */
    public static void request(Subscription s, int prefetch) {
        s.request(prefetch < 0 ? Long.MAX_VALUE : prefetch);
    }

    static final long COMPLETED_MASK = 0x8000000000000000L;
    static final long REQUESTED_MASK = 0x7FFFFFFFFFFFFFFFL;

    /**
     * Accumulates requests (not validated) and handles the completed mode draining of the queue based on the requests.
     *
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onComplete() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     *
     * @param <T> the value type emitted
     * @param n the request amount, positive (not validated)
     * @param actual the target Subscriber to send events to
     * @param queue the queue to drain if in the post-complete state
     * @param state holds the request amount and the post-completed flag
     * @param isCancelled a supplier that returns true if the drain has been cancelled
     * @return true if the state indicates a completion state.
     */
    public static <T> boolean postCompleteRequest(long n,
                                                  Subscriber<? super T> actual,
                                                  Queue<T> queue,
                                                  AtomicLong state,
                                                  BooleanSupplier isCancelled) {
        for (; ; ) {
            long r = state.get();

            // extract the current request amount
            long r0 = r & REQUESTED_MASK;

            // preserve COMPLETED_MASK and calculate new requested amount
            long u = (r & COMPLETED_MASK) | BackpressureHelper.addCap(r0, n);

            if (state.compareAndSet(r, u)) {
                // (complete, 0) -> (complete, n) transition then replay
                if (r == COMPLETED_MASK) {

                    postCompleteDrain(n | COMPLETED_MASK, actual, queue, state, isCancelled);

                    return true;
                }
                // (active, r) -> (active, r + n) transition then continue with requesting from upstream
                return false;
            }
        }

    }

    static boolean isCancelled(BooleanSupplier cancelled) {
        try {
            return cancelled.getAsBoolean();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            return true;
        }
    }

    /**
     * Drains the queue based on the outstanding requests in post-completed mode (only!).
     *
     * @param n the current request amount
     * @param actual the target Subscriber to send events to
     * @param queue the queue to drain if in the post-complete state
     * @param state holds the request amount and the post-completed flag
     * @param isCancelled a supplier that returns true if the drain has been cancelled
     * @return true if the queue was completely drained or the drain process was cancelled
     */
    static <T> boolean postCompleteDrain(long n,
                                         Subscriber<? super T> actual,
                                         Queue<T> queue,
                                         AtomicLong state,
                                         BooleanSupplier isCancelled) {

// TODO enable fast-path
//        if (n == -1 || n == Long.MAX_VALUE) {
//            for (;;) {
//                if (isCancelled.getAsBoolean()) {
//                    break;
//                }
//
//                T v = queue.poll();
//
//                if (v == null) {
//                    actual.onComplete();
//                    break;
//                }
//
//                actual.onNext(v);
//            }
//
//            return true;
//        }

        long e = n & COMPLETED_MASK;

        for (; ; ) {

            while (e != n) {
                if (isCancelled(isCancelled)) {
                    return true;
                }

                T t = queue.poll();

                if (t == null) {
                    actual.onComplete();
                    return true;
                }

                actual.onNext(t);
                e++;
            }

            if (isCancelled(isCancelled)) {
                return true;
            }

            if (queue.isEmpty()) {
                actual.onComplete();
                return true;
            }

            n = state.get();

            if (n == e) {

                n = state.addAndGet(-(e & REQUESTED_MASK));

                if ((n & REQUESTED_MASK) == 0L) {
                    return false;
                }

                e = n & COMPLETED_MASK;
            }
        }

    }

    /**
     * Signals the completion of the main sequence and switches to post-completion replay mode.
     *
     * <p>
     * Don't modify the queue after calling this method!
     *
     * <p>
     * Post-completion backpressure handles the case when a source produces values based on
     * requests when it is active but more values are available even after its completion.
     * In this case, the onComplete() can't just emit the contents of the queue but has to
     * coordinate with the requested amounts. This requires two distinct modes: active and
     * completed. In active mode, requests flow through and the queue is not accessed but
     * in completed mode, requests no-longer reach the upstream but help in draining the queue.
     * <p>
     * The algorithm utilizes the most significant bit (bit 63) of a long value (AtomicLong) since
     * request amount only goes up to Long.MAX_VALUE (bits 0-62) and negative values aren't
     * allowed.
     *
     * @param <T> the value type emitted
     * @param actual the target Subscriber to send events to
     * @param queue the queue to drain if in the post-complete state
     * @param state holds the request amount and the post-completed flag
     * @param isCancelled a supplier that returns true if the drain has been cancelled
     */
    public static <T> void postComplete(Subscriber<? super T> actual,
                                        Queue<T> queue,
                                        AtomicLong state,
                                        BooleanSupplier isCancelled) {

        if (queue.isEmpty()) {
            actual.onComplete();
            return;
        }

        if (postCompleteDrain(state.get(), actual, queue, state, isCancelled)) {
            return;
        }

        for (; ; ) {
            long r = state.get();

            if ((r & COMPLETED_MASK) != 0L) {
                return;
            }

            long u = r | COMPLETED_MASK;
            // (active, r) -> (complete, r) transition
            if (state.compareAndSet(r, u)) {
                // if the requested amount was non-zero, drain the queue
                if (r != 0L) {
                    postCompleteDrain(u, actual, queue, state, isCancelled);
                }

                return;
            }
        }

    }
}
