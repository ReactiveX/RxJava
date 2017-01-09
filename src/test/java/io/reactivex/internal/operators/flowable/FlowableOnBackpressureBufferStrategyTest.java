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

import static io.reactivex.BackpressureOverflowStrategy.*;
import static io.reactivex.internal.functions.Functions.EMPTY_ACTION;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.subscribers.*;

public class FlowableOnBackpressureBufferStrategyTest {

    @Test(timeout = 2000)
    public void backpressureWithBufferDropOldest() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        Action incrementOnDrop = new Action() {
            @Override
            public void run() throws Exception {
                droppedCount.incrementAndGet();
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, incrementOnDrop, DROP_OLDEST))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitTerminalEvent();
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(497, ts.values().get(0).intValue());
        assertEquals(498, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        assertEquals(droppedCount.get(), 500 - bufferSize);
    }

    private TestSubscriber<Long> createTestSubscriber() {
        return new TestSubscriber<Long>(new DefaultSubscriber<Long>() {

            @Override
            protected void onStart() {
            }

            @Override
            public void onComplete() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Long t) {
            }

        }, 0L);
    }

    @Test(timeout = 2000)
    public void backpressureWithBufferDropLatest() throws InterruptedException {
        int bufferSize = 3;
        final AtomicInteger droppedCount = new AtomicInteger(0);
        Action incrementOnDrop = new Action() {
            @Override
            public void run() throws Exception {
                droppedCount.incrementAndGet();
            }
        };
        TestSubscriber<Long> ts = createTestSubscriber();
        Flowable.fromPublisher(send500ValuesAndComplete.onBackpressureBuffer(bufferSize, incrementOnDrop, DROP_LATEST))
                .subscribe(ts);
        // we request 10 but only 3 should come from the buffer
        ts.request(10);
        ts.awaitTerminalEvent();
        assertEquals(bufferSize, ts.values().size());
        ts.assertNoErrors();
        assertEquals(0, ts.values().get(0).intValue());
        assertEquals(1, ts.values().get(1).intValue());
        assertEquals(499, ts.values().get(2).intValue());
        assertEquals(droppedCount.get(), 500 - bufferSize);
    }

    private static final Flowable<Long> send500ValuesAndComplete = Flowable.unsafeCreate(new Publisher<Long>() {
        @Override
        public void subscribe(Subscriber<? super Long> s) {
            BooleanSubscription bs = new BooleanSubscription();
            s.onSubscribe(bs);
            long i = 0;
            while (!bs.isCancelled() && i < 500) {
                s.onNext(i++);
            }
            if (!bs.isCancelled()) {
                s.onComplete();
            }
        }
    });


    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferNegativeCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(-1, EMPTY_ACTION , DROP_OLDEST);
    }

    @Test(expected = IllegalArgumentException.class)
    public void backpressureBufferZeroCapacity() throws InterruptedException {
        Flowable.empty().onBackpressureBuffer(0, EMPTY_ACTION , DROP_OLDEST);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1)
                .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void error() {
        Flowable
        .error(new TestException())
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void overflowError() {
        Flowable.range(1, 20)
        .onBackpressureBuffer(8, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertFailure(MissingBackpressureException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Object>, Object>() {
            @Override
            public Object apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR);
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Object>>() {
            @Override
            public Flowable<Object> apply(Flowable<Object> f) throws Exception {
                return f.onBackpressureBuffer(8, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR);
            }
        });
    }

    @Test
    public void overflowCrashes() {
        Flowable.range(1, 20)
        .onBackpressureBuffer(8, new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        }, BackpressureOverflowStrategy.DROP_OLDEST)
        .test(0L)
        .assertFailure(TestException.class);
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(Flowable.just(1)
                .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR));
    }

    @Test
    public void empty() {
        Flowable.empty()
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .test(0L)
        .assertResult();
    }

    @Test
    public void justTake() {
        Flowable.just(1)
        .onBackpressureBuffer(16, Functions.EMPTY_ACTION, BackpressureOverflowStrategy.ERROR)
        .take(1)
        .test()
        .assertResult(1);
    }
}
