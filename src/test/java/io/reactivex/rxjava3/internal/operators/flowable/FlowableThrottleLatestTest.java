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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableThrottleLatestTest extends RxJavaTest {

    @Test
    public void just() {
        Flowable.just(1)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Flowable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeEmitLatest() {
        Flowable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES, true)
        .test()
        .assertResult(1, 5);
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                return f.throttleLatest(1, TimeUnit.MINUTES);
            }
        });
    }

    @Test
    public void badRequest() {
        TestHelper.assertBadRequestReported(
                Flowable.never()
                .throttleLatest(1, TimeUnit.MINUTES)
        );
    }

    @Test
    public void normal() {
        TestScheduler sch = new TestScheduler();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch).test();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3);

        pp.onNext(4);

        ts.assertValuesOnly(1, 3);

        pp.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        pp.onNext(6);

        ts.assertValuesOnly(1, 3, 5, 6);

        pp.onNext(7);
        pp.onComplete();

        ts.assertResult(1, 3, 5, 6);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 3, 5, 6);
    }

    @Test
    public void normalEmitLast() {
        TestScheduler sch = new TestScheduler();
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, true).test();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3);

        pp.onNext(4);

        ts.assertValuesOnly(1, 3);

        pp.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3, 5);

        pp.onNext(6);

        ts.assertValuesOnly(1, 3, 5, 6);

        pp.onNext(7);
        pp.onComplete();

        ts.assertResult(1, 3, 5, 6, 7);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 3, 5, 6, 7);
    }

    @Test
    public void missingBackpressureExceptionFirst() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        Flowable.just(1, 2)
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES, sch)
        .test(0)
        .assertFailure(MissingBackpressureException.class);

        verify(onCancel).run();
    }

    @Test
    public void missingBackpressureExceptionLatest() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        TestSubscriber<Integer> ts = Flowable.just(1, 2)
        .concatWith(Flowable.<Integer>never())
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true)
        .test(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertFailure(MissingBackpressureException.class, 1);

        verify(onCancel).run();
    }

    @Test
    public void missingBackpressureExceptionLatestComplete() throws Throwable {
        TestScheduler sch = new TestScheduler();
        Action onCancel = mock(Action.class);

        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true)
        .test(1);

        pp.onNext(1);
        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onComplete();

        ts.assertFailure(MissingBackpressureException.class, 1);

        verify(onCancel, never()).run();
    }

    @Test
    public void take() throws Throwable {
        Action onCancel = mock(Action.class);

        Flowable.range(1, 5)
        .doOnCancel(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES)
        .take(1)
        .test()
        .assertResult(1);

        verify(onCancel).run();
    }

    @Test
    public void reentrantComplete() {
        TestScheduler sch = new TestScheduler();
        final PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    pp.onNext(2);
                }
                if (t == 2) {
                    pp.onComplete();
                }
            }
        };

        pp.throttleLatest(1, TimeUnit.SECONDS, sch).subscribe(ts);

        pp.onNext(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertResult(1, 2);
    }

    /** Emit 1, 2, 3, then advance time by a second; 1 and 3 should end up in downstream, 2 should be dropped. */
    @Test
    public void onDroppedBasicNoEmitLast() {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(2);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(3);

        ts.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertValuesOnly(1, 3);
        drops.assertValuesOnly(2);

        pp.onComplete();

        ts.assertResult(1, 3);

        drops.assertValuesOnly(2);
    }

    /** Emit 1, 2, 3; 1 should end up in downstream, 2, 3 should be dropped. */
    @Test
    public void onDroppedBasicNoEmitLastDropLast() {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(2);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(3);

        ts.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        pp.onComplete();

        ts.assertResult(1);

        drops.assertValuesOnly(2, 3);
    }

    /** Emit 1, 2, 3; 1 and 3 should end up in downstream, 2 should be dropped. */
    @Test
    public void onDroppedBasicEmitLast() {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, true, drops::onNext)
        .test();

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(2);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onNext(3);

        ts.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        pp.onComplete();

        ts.assertResult(1, 3);

        drops.assertValuesOnly(2);
    }

    /** Emit 1, 2, 3; 3 should trigger an error to the downstream because 2 is dropped and the callback crashes. */
    @Test
    public void onDroppedBasicNoEmitLastFirstDropCrash() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriber<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> {
            if (d == 2) {
                throw new TestException("forced");
            }
        })
        .test();

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertFailure(TestException.class, 1);

        verify(whenDisposed).run();
    }

    /**
     * Emit 1, 2, Error; the error should trigger the drop callback and crash it too,
     * downstream gets 1, composite(source, drop-crash).
     */
    @Test
    public void onDroppedBasicNoEmitLastOnErrorDropCrash() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestSubscriberEx<>());

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onError(new TestException("source"));

        ts.assertFailure(CompositeException.class, 1);

        TestHelper.assertCompositeExceptions(ts, TestException.class, "source", TestException.class, "forced 2");

        verify(whenDisposed, never()).run();
    }

    /**
     * Emit 1, 2, 3; 3 should trigger a drop-crash for 2, which then would trigger the error path and drop-crash for 3,
     * the last item not delivered, downstream gets 1, composite(drop-crash 2, drop-crash 3).
     */
    @Test
    public void onDroppedBasicEmitLastOnErrorDropCrash() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, true, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestSubscriberEx<>());

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onNext(3);

        ts.assertFailure(CompositeException.class, 1);

        TestHelper.assertCompositeExceptions(ts, TestException.class, "forced 2", TestException.class, "forced 3");

        verify(whenDisposed).run();
    }

    /** Emit 1, complete; Downstream gets 1, complete, no drops. */
    @Test
    public void onDroppedBasicNoEmitLastNoLastToDrop() {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onComplete();

        ts.assertResult(1);
        drops.assertEmpty();
    }

    /** Emit 1, error; Downstream gets 1, error, no drops. */
    @Test
    public void onDroppedErrorNoEmitLastNoLastToDrop() {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriber<Integer> ts = pp.throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test();

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        pp.onError(new TestException());

        ts.assertFailure(TestException.class, 1);
        drops.assertEmpty();
    }

    /**
     * Emit 1, 2, complete; complete should crash drop, downstream gets 1, drop-crash 2.
     */
    @Test
    public void onDroppedHasLastNoEmitLastDropCrash() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestSubscriberEx<>());

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        pp.onComplete();

        ts.assertFailureAndMessage(TestException.class, "forced 2", 1);

        verify(whenDisposed, never()).run();
    }

    /**
     * Emit 1, 2 then dispose the sequence; downstream gets 1, drop should get for 2.
     */
    @Test
    public void onDroppedDisposeDrops() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .subscribeWith(new TestSubscriberEx<>());

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        pp.onNext(2);

        ts.assertValuesOnly(1);

        ts.cancel();

        ts.assertValuesOnly(1);
        drops.assertValuesOnly(2);

        verify(whenDisposed).run();
    }

    /**
     * Emit 1 then dispose the sequence; downstream gets 1, drop should not get called.
     */
    @Test
    public void onDroppedDisposeNoDrops() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .subscribeWith(new TestSubscriberEx<>());

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertValuesOnly(1);

        ts.cancel();

        ts.assertValuesOnly(1);
        drops.assertEmpty();

        verify(whenDisposed).run();
    }

    /**
     * Emit 1, 2 then dispose the sequence; downstream gets 1, global error handler should get drop-crash 2.
     */
    @Test
    public void onDroppedDisposeCrashesDrop() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            PublishProcessor<Integer> pp =PublishProcessor.create();

            TestScheduler sch = new TestScheduler();

            Action whenDisposed = mock(Action.class);

            TestSubscriberEx<Integer> ts = pp
            .doOnCancel(whenDisposed)
            .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
            .subscribeWith(new TestSubscriberEx<>());

            ts.assertEmpty();

            pp.onNext(1);

            ts.assertValuesOnly(1);

            pp.onNext(2);

            ts.assertValuesOnly(1);

            ts.cancel();

            ts.assertValuesOnly(1);

            verify(whenDisposed).run();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "forced 2");
        });
    }

    /** Emit 1 but downstream is backpressured; downstream gets MBE, drops gets 1. */
    @Test
    public void onDroppedBackpressured() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        TestSubscriber<Object> drops = new TestSubscriber<>();
        drops.onSubscribe(EmptySubscription.INSTANCE);

        Action whenDisposed = mock(Action.class);

        TestSubscriber<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, drops::onNext)
        .test(0L);

        ts.assertEmpty();
        drops.assertEmpty();

        pp.onNext(1);

        ts.assertFailure(MissingBackpressureException.class);

        drops.assertValuesOnly(1);

        verify(whenDisposed).run();
    }

    /** Emit 1 but downstream is backpressured; drop crashes, downstream gets composite(MBE, drop-crash 1). */
    @Test
    public void onDroppedBackpressuredDropCrash() throws Throwable {
        PublishProcessor<Integer> pp =PublishProcessor.create();

        TestScheduler sch = new TestScheduler();

        Action whenDisposed = mock(Action.class);

        TestSubscriberEx<Integer> ts = pp
        .doOnCancel(whenDisposed)
        .throttleLatest(1, TimeUnit.SECONDS, sch, false, d -> { throw new TestException("forced " + d); })
        .subscribeWith(new TestSubscriberEx<>(0L));

        ts.assertEmpty();

        pp.onNext(1);

        ts.assertFailure(CompositeException.class);

        TestHelper.assertCompositeExceptions(ts,
                MissingBackpressureException.class, "Could not emit value due to lack of requests",
                TestException.class, "forced 1");

        verify(whenDisposed).run();
    }
}
