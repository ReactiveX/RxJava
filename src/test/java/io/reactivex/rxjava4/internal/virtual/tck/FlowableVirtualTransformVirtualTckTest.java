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

package io.reactivex.rxjava4.internal.virtual.tck;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Publisher;

import org.testng.annotations.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.tck.BaseTck;
import io.reactivex.rxjava4.testsupport.TestHelper;

@Test
public class FlowableVirtualTransformVirtualTckTest extends BaseTck<Long> {

    @Override
    public Publisher<Long> createFlowPublisher(final long elements) {
        var half = elements >> 1;
        var rest = elements - half;
        return Flowable.rangeLong(0, rest)
                .virtualTransform((v, emitter) -> {
                    emitter.emit(v);
                    if (v < rest - 1 || half == rest) {
                        emitter.emit(v);
                    }
                }, Schedulers.virtual(), Flowable.bufferSize());
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return Flowable.just(1)
                .virtualTransform((_, _) -> {
                    throw new IOException();
                }, Schedulers.virtual(), Flowable.bufferSize());
    }

    @Test
    public void slowProducer() {
        var log = new ConcurrentLinkedQueue<String>();

        var ts = Flowable.range(1, 10)
        .doOnNext(v -> log.offer("Range: " + v))
        .doOnRequest(v -> log.offer("SubscribeOn requested: " + v))
        .doOnSubscribe(_ -> log.offer("Range subscribed to"))
        .subscribeOn(Schedulers.computation())
        .doOnRequest(v -> log.offer("Map requested: " + v))
        .doOnSubscribe(_ -> log.offer("subscribeOn subscribed to"))
        .map(v -> {
            log.offer("Map: " + v);
            log.offer("Map interrupted? " + Thread.interrupted());
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                log.offer("Map sleep interrupted");
            }
            return v;
        })
        .doOnRequest(v -> log.offer("Transform requested: " + v))
        .virtualTransform((v, emitter) -> {
            log.offer("Tansform before emit: " + v);
            emitter.emit(v);
            log.offer("Tansform after emit: " + v);
        }, Schedulers.virtual(), Flowable.bufferSize())
        .doOnRequest(v -> log.offer("Test requested: " + v))
        .doOnNext(v -> log.offer("Test received: " + v))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        ;

        try {
            ts.assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        } catch (AssertionError ex) {
            var sb = new StringBuilder();
            log.forEach(v -> sb.append("\r\n").append(v));
            var exc = new AssertionError(sb.append("\r\n").toString(), ex);
            throw exc;
        }
    }

    @Test
    public void slowProducerService() {
        TestHelper.checkObstruction();

        Flowable.range(1, 10)
        .subscribeOn(Schedulers.computation())
        .map(v -> {
            Thread.interrupted();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                // ignored
            }
            return v;
        })
        .virtualTransform((v, emitter) -> {
            emitter.emit(v);
        }, Schedulers.virtual(), Flowable.bufferSize())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
}
