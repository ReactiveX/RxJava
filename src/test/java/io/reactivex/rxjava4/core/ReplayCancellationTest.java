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

package io.reactivex.rxjava4.core;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.*;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.processors.*;
import io.reactivex.rxjava4.schedulers.TestScheduler;
import io.reactivex.rxjava4.subjects.*;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ReplayCancellationTest extends RxJavaTest {

    @Test
    public void cancelledObservableDoesNotRetainBuffer() throws Exception {
        assertAll(
                () -> assertReclaimed(cancelObservable(0)),
                () -> assertReclaimed(cancelObservable(1)),
                () -> assertReclaimed(cancelObservable(2))
        );
    }

    @Test
    public void cancelledFlowableDoesNotRetainBuffer() throws Exception {
        assertAll(
                () -> assertReclaimed(cancelFlowable(0)),
                () -> assertReclaimed(cancelFlowable(1)),
                () -> assertReclaimed(cancelFlowable(2))
        );
    }

    @Test
    public void cancelledSubjectDoesNotRetainBuffer() throws Exception {
        assertAll(
                () -> assertReclaimed(cancelSubject(0)),
                () -> assertReclaimed(cancelSubject(1)),
                () -> assertReclaimed(cancelSubject(2))
        );
    }

    @Test
    public void cancelledProcessorDoesNotRetainBuffer() throws Exception {
        assertAll(
                () -> assertReclaimed(cancelProcessor(0)),
                () -> assertReclaimed(cancelProcessor(1)),
                () -> assertReclaimed(cancelProcessor(2))
        );
    }

    @Test
    public void flowableRequestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            PublishProcessor<Object> source = PublishProcessor.create();
            ConnectableFlowable<Object> replay = source.replay(1);
            ReplayConsumer consumer = new ReplayConsumer(new Object());
            replay.subscribe(consumer);
            Disposable connection = replay.connect();
            try {
                TestHelper.race(() -> consumer.upstream.request(1), consumer.upstream::cancel);
                source.onNext(1);
                assertEquals(0, consumer.values);
            } finally {
                connection.dispose();
            }
        }
    }

    @Test
    public void processorRequestCancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            ReplayProcessor<Object> source = ReplayProcessor.createWithSize(1);
            ReplayConsumer consumer = new ReplayConsumer(new Object());
            source.subscribe(consumer);
            TestHelper.race(() -> consumer.upstream.request(1), consumer.upstream::cancel);
            source.onNext(1);
            assertEquals(0, consumer.values);
            assertFalse(source.hasSubscribers());
        }
    }

    static ReplayConsumer cancelObservable(int kind) {
        Object value = new Object();
        ReplayConsumer consumer = new ReplayConsumer(value);
        PublishSubject<Object> source = PublishSubject.create();
        ConnectableObservable<Object> replay = switch (kind) {
            case 0 -> source.replay();
            case 1 -> source.replay(1);
            default -> source.replay(1, 1, TimeUnit.DAYS, new TestScheduler());
        };
        replay.subscribe(consumer);
        replay.connect();
        source.onNext(value);
        consumer.disposable.dispose();
        consumer.disposable.dispose();
        assertTrue(consumer.disposable.isDisposed());
        return consumer;
    }

    static ReplayConsumer cancelFlowable(int kind) {
        Object value = new Object();
        ReplayConsumer consumer = new ReplayConsumer(value);
        PublishProcessor<Object> source = PublishProcessor.create();
        ConnectableFlowable<Object> replay = switch (kind) {
            case 0 -> source.replay();
            case 1 -> source.replay(1);
            default -> source.replay(1, 1, TimeUnit.DAYS, new TestScheduler());
        };
        replay.subscribe(consumer);
        replay.connect();
        source.onNext(value);
        consumer.upstream.cancel();
        consumer.upstream.cancel();
        consumer.upstream.request(1);
        return consumer;
    }

    static ReplayConsumer cancelSubject(int kind) {
        Object value = new Object();
        ReplayConsumer consumer = new ReplayConsumer(value);
        ReplaySubject<Object> source = switch (kind) {
            case 0 -> ReplaySubject.create();
            case 1 -> ReplaySubject.createWithSize(1);
            default -> ReplaySubject.createWithTimeAndSize(1, TimeUnit.DAYS, new TestScheduler(), 1);
        };
        source.subscribe(consumer);
        source.onNext(value);
        consumer.disposable.dispose();
        consumer.disposable.dispose();
        assertTrue(consumer.disposable.isDisposed());
        assertFalse(source.hasObservers());
        return consumer;
    }

    static ReplayConsumer cancelProcessor(int kind) {
        Object value = new Object();
        ReplayConsumer consumer = new ReplayConsumer(value);
        ReplayProcessor<Object> source = switch (kind) {
            case 0 -> ReplayProcessor.create();
            case 1 -> ReplayProcessor.createWithSize(1);
            default -> ReplayProcessor.createWithTimeAndSize(1, TimeUnit.DAYS, new TestScheduler(), 1);
        };
        source.subscribe(consumer);
        source.onNext(value);
        consumer.upstream.cancel();
        consumer.upstream.cancel();
        consumer.upstream.request(1);
        assertFalse(source.hasSubscribers());
        return consumer;
    }

    static void assertReclaimed(ReplayConsumer consumer) throws Exception {
        assertEquals(1, consumer.values);
        // Keep the original cancelled handle reachable, as a custom consumer may do.
        try {
            for (int i = 0; i < 20 && consumer.value.get() != null; i++) {
                System.gc();
                Thread.sleep(50);
            }
            assertNull(consumer.value.get(), "A cancelled replay consumer retained its cached value");
        } finally {
            Reference.reachabilityFence(consumer);
        }
    }

    static final class ReplayConsumer implements Observer<Object>, FlowableSubscriber<Object> {
        final WeakReference<Object> value;
        Disposable disposable;
        Subscription upstream;
        int values;

        ReplayConsumer(Object value) {
            this.value = new WeakReference<>(value);
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposable = d;
        }

        @Override
        public void onSubscribe(Subscription s) {
            upstream = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(Object item) {
            values++;
        }

        @Override
        public void onError(Throwable error) {
            throw new AssertionError(error);
        }

        @Override
        public void onComplete() {
            // The cancellation tests use nonterminating sources.
        }
    }
}
