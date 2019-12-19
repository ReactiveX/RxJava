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

package io.reactivex.rxjava3.internal.jdk8;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableStageSubscriberOrDefaultTest extends RxJavaTest {

    @Test
    public void firstJust() throws Exception {
        Integer v = Flowable.just(1)
                .firstStage(null)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)1, v);
    }

    @Test
    public void firstEmpty() throws Exception {
        Integer v = Flowable.<Integer>empty()
                .firstStage(2)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)2, v);
    }

    @Test
    public void firstCancels() throws Exception {
        BehaviorProcessor<Integer> source = BehaviorProcessor.createDefault(1);

        Integer v = source
                .firstStage(null)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)1, v);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void firstCompletableFutureCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .firstStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.cancel(true);

        assertTrue(cf.isCancelled());

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void firstCompletableManualCompleteCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .firstStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.complete(1);

        assertTrue(cf.isDone());
        assertFalse(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        assertEquals((Integer)1, cf.get());
    }

    @Test
    public void firstCompletableManualCompleteExceptionallyCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .firstStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.completeExceptionally(new TestException());

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void firstError() throws Exception {
        CompletableFuture<Integer> cf = Flowable.<Integer>error(new TestException())
                .firstStage(null)
                .toCompletableFuture();

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void firstSourceIgnoresCancel() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onError(new TestException());
                    s.onComplete();
                }
            }
            .firstStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void firstDoubleOnSubscribe() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                }
            }
            .firstStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        });
    }

    @Test
    public void singleJust() throws Exception {
        Integer v = Flowable.just(1)
                .singleStage(null)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)1, v);
    }

    @Test
    public void singleEmpty() throws Exception {
        Integer v = Flowable.<Integer>empty()
                .singleStage(2)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)2, v);
    }

    @Test
    public void singleTooManyCancels() throws Exception {
        ReplayProcessor<Integer> source = ReplayProcessor.create();
        source.onNext(1);
        source.onNext(2);

        TestHelper.assertError(source
                .singleStage(null)
                .toCompletableFuture(), IllegalArgumentException.class);

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void singleCompletableFutureCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .singleStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.cancel(true);

        assertTrue(cf.isCancelled());

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void singleCompletableManualCompleteCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .singleStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.complete(1);

        assertTrue(cf.isDone());
        assertFalse(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        assertEquals((Integer)1, cf.get());
    }

    @Test
    public void singleCompletableManualCompleteExceptionallyCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .singleStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.completeExceptionally(new TestException());

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void singleError() throws Exception {
        CompletableFuture<Integer> cf = Flowable.<Integer>error(new TestException())
                .singleStage(null)
                .toCompletableFuture();

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void singleSourceIgnoresCancel() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onComplete();
                    s.onError(new TestException());
                    s.onComplete();
                }
            }
            .singleStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void singleDoubleOnSubscribe() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onComplete();
                }
            }
            .singleStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        });
    }

    @Test
    public void lastJust() throws Exception {
        Integer v = Flowable.just(1)
                .lastStage(null)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)1, v);
    }

    @Test
    public void lastRange() throws Exception {
        Integer v = Flowable.range(1, 5)
                .lastStage(null)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)5, v);
    }

    @Test
    public void lastEmpty() throws Exception {
        Integer v = Flowable.<Integer>empty()
                .lastStage(2)
                .toCompletableFuture()
                .get();

        assertEquals((Integer)2, v);
    }

    @Test
    public void lastCompletableFutureCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .lastStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.cancel(true);

        assertTrue(cf.isCancelled());

        assertFalse(source.hasSubscribers());
    }

    @Test
    public void lastCompletableManualCompleteCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .lastStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.complete(1);

        assertTrue(cf.isDone());
        assertFalse(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        assertEquals((Integer)1, cf.get());
    }

    @Test
    public void lastCompletableManualCompleteExceptionallyCancels() throws Exception {
        PublishProcessor<Integer> source = PublishProcessor.create();

        CompletableFuture<Integer> cf = source
                .lastStage(null)
                .toCompletableFuture();

        assertTrue(source.hasSubscribers());

        cf.completeExceptionally(new TestException());

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        assertFalse(source.hasSubscribers());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void lastError() throws Exception {
        CompletableFuture<Integer> cf = Flowable.<Integer>error(new TestException())
                .lastStage(null)
                .toCompletableFuture();

        assertTrue(cf.isDone());
        assertTrue(cf.isCompletedExceptionally());
        assertFalse(cf.isCancelled());

        TestHelper.assertError(cf, TestException.class);
    }

    @Test
    public void lastSourceIgnoresCancel() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onComplete();
                    s.onError(new TestException());
                    s.onComplete();
                }
            }
            .lastStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }

    @Test
    public void lastDoubleOnSubscribe() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            Integer v = new Flowable<Integer>() {
                @Override
                protected void subscribeActual(Subscriber<? super Integer> s) {
                    s.onSubscribe(new BooleanSubscription());
                    s.onSubscribe(new BooleanSubscription());
                    s.onNext(1);
                    s.onComplete();
                }
            }
            .lastStage(null)
            .toCompletableFuture()
            .get();

            assertEquals((Integer)1, v);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        });
    }
}
