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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.ref.WeakReference;
import java.util.concurrent.Flow.Subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.processors.ReplayProcessor;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Isolated
public class ReplayCancellationIsolatedTest extends RxJavaTest {

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
