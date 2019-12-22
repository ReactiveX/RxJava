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

package io.reactivex.rxjava3.internal.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.BooleanSupplier;
import io.reactivex.rxjava3.internal.queue.SpscArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class QueueDrainHelperTest extends RxJavaTest {

    @Test
    public void isCancelled() {
        assertTrue(QueueDrainHelper.isCancelled(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                throw new IOException();
            }
        }));
    }

    @Test
    public void requestMaxInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Integer.MAX_VALUE, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MAX_VALUE);
    }

    @Test
    public void requestMinInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Long.MAX_VALUE, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MIN_VALUE);
    }

    @Test
    public void requestAlmostMaxInt() {
        QueueDrainHelper.request(new Subscription() {
            @Override
            public void request(long n) {
                assertEquals(Integer.MAX_VALUE - 1, n);
            }

            @Override
            public void cancel() {
            }
        }, Integer.MAX_VALUE - 1);
    }

    @Test
    public void postCompleteEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        };

        ts.onSubscribe(new BooleanSubscription());

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertResult();
    }

    @Test
    public void postCompleteWithRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertResult(1);
    }

    @Test
    public void completeRequestRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Integer> ts = new TestSubscriber<>();
            final ArrayDeque<Integer> queue = new ArrayDeque<>();
            final AtomicLong state = new AtomicLong();
            final BooleanSupplier isCancelled = new BooleanSupplier() {
                @Override
                public boolean getAsBoolean() throws Exception {
                    return false;
                }
            };

            ts.onSubscribe(new BooleanSubscription());
            queue.offer(1);

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    QueueDrainHelper.postCompleteRequest(1, ts, queue, state, isCancelled);
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    QueueDrainHelper.postComplete(ts, queue, state, isCancelled);
                }
            };

            TestHelper.race(r1, r2);

            ts.assertResult(1);
        }
    }

    @Test
    public void postCompleteCancelled() {
        final TestSubscriber<Integer> ts = new TestSubscriber<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return ts.isCancelled();
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();
        ts.cancel();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertEmpty();
    }

    @Test
    public void postCompleteCancelledAfterOne() {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                cancel();
            }
        };
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        AtomicLong state = new AtomicLong();
        BooleanSupplier isCancelled = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return ts.isCancelled();
            }
        };

        ts.onSubscribe(new BooleanSubscription());
        queue.offer(1);
        state.getAndIncrement();

        QueueDrainHelper.postComplete(ts, queue, state, isCancelled);

        ts.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void drainMaxLoopMissingBackpressure() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);
        q.offer(1);

        QueueDrainHelper.drainMaxLoop(q, ts, false, null, qd);

        ts.assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void drainMaxLoopMissingBackpressureWithResource() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);
        q.offer(1);

        Disposable d = Disposable.empty();

        QueueDrainHelper.drainMaxLoop(q, ts, false, d, qd);

        ts.assertFailure(MissingBackpressureException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void drainMaxLoopDontAccept() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 1;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);
        q.offer(1);

        QueueDrainHelper.drainMaxLoop(q, ts, false, null, qd);

        ts.assertEmpty();
    }

    @Test
    public void checkTerminatedDelayErrorEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, true, ts, true, q, qd);

        ts.assertResult();
    }

    @Test
    public void checkTerminatedDelayErrorNonEmpty() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, false, ts, true, q, qd);

        ts.assertEmpty();
    }

    @Test
    public void checkTerminatedDelayErrorEmptyError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return new TestException();
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, true, ts, true, q, qd);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void checkTerminatedNonDelayErrorError() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.onSubscribe(new BooleanSubscription());

        QueueDrain<Integer, Integer> qd = new QueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return new TestException();
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public long requested() {
                return 0;
            }

            @Override
            public long produced(long n) {
                return 0;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public boolean accept(Subscriber<? super Integer> a, Integer v) {
                return false;
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, false, ts, false, q, qd);

        ts.assertFailure(TestException.class);
    }

    @Test
    public void observerCheckTerminatedDelayErrorEmpty() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, true, to, true, q, null, qd);

        to.assertResult();
    }

    @Test
    public void observerCheckTerminatedDelayErrorEmptyResource() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        Disposable d = Disposable.empty();

        QueueDrainHelper.checkTerminated(true, true, to, true, q, d, qd);

        to.assertResult();

        assertTrue(d.isDisposed());
    }

    @Test
    public void observerCheckTerminatedDelayErrorNonEmpty() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return null;
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, false, to, true, q, null, qd);

        to.assertEmpty();
    }

    @Test
    public void observerCheckTerminatedDelayErrorEmptyError() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return new TestException();
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, true, to, true, q, null, qd);

        to.assertFailure(TestException.class);
    }

    @Test
    public void observerCheckTerminatedNonDelayErrorError() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return new TestException();
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        QueueDrainHelper.checkTerminated(true, false, to, false, q, null, qd);

        to.assertFailure(TestException.class);
    }

    @Test
    public void observerCheckTerminatedNonDelayErrorErrorResource() {
        TestObserver<Integer> to = new TestObserver<>();
        to.onSubscribe(Disposable.empty());

        ObservableQueueDrain<Integer, Integer> qd = new ObservableQueueDrain<Integer, Integer>() {
            @Override
            public boolean cancelled() {
                return false;
            }

            @Override
            public boolean done() {
                return false;
            }

            @Override
            public Throwable error() {
                return new TestException();
            }

            @Override
            public boolean enter() {
                return true;
            }

            @Override
            public int leave(int m) {
                return 0;
            }

            @Override
            public void accept(Observer<? super Integer> a, Integer v) {
            }
        };

        SpscArrayQueue<Integer> q = new SpscArrayQueue<>(32);

        Disposable d = Disposable.empty();

        QueueDrainHelper.checkTerminated(true, false, to, false, q, d, qd);

        to.assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void postCompleteAlreadyComplete() {

        TestSubscriber<Integer> ts = new TestSubscriber<>();

        Queue<Integer> q = new ArrayDeque<>();
        q.offer(1);

        AtomicLong state = new AtomicLong(QueueDrainHelper.COMPLETED_MASK);

        QueueDrainHelper.postComplete(ts, q, state, new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        });
    }
}
