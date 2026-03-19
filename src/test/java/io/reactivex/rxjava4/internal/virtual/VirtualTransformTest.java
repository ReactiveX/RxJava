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

package io.reactivex.rxjava4.internal.virtual;

import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class VirtualTransformTest {

    @Test
    public void checkIsInsideVirtualThread() {
        try (var scope = Executors.newVirtualThreadPerTaskExecutor()) {
            var cancelled = new AtomicBoolean();
            Flowable.range(1, 5)
            .doOnCancel(() -> cancelled.set(true))
            .virtualTransform((v, emitter) -> emitter.emit(v), scope)
            .take(1)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertTrue(cancelled.get());
        }
    }

    @Test
    public void errorUpstream() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.error(new IOException())
            .virtualTransform((v, e) -> e.emit(v), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(IOException.class)
            ;
        });
    }

    @Test
    public void errorTransform() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.range(1, 5)
            .virtualTransform((_, _) -> { throw new IOException(); }, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertError(IOException.class)
            ;
        });
    }

    @Test
    public void take() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.range(1, 5)
            .virtualTransform((v, e) -> e.emit(v), exec)
            .take(2)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2)
            ;
        });
    }

    @Test
    public void observeOn() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.range(1, 10000)
            .virtualTransform((v, e) -> e.emit(v), exec)
            .observeOn(Schedulers.single(), false, 2)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertNoErrors()
            .assertValueCount(10000);
        });
    }

    @Test
    public void empty() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.empty()
            .virtualTransform((v, e) -> e.emit(v), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult()
            ;
        });
    }

    @Test
    public void emptyNever() throws Throwable {
        TestHelper.withVirtual(exec -> {
            Flowable.just(1).concatWith(Flowable.never())
            .virtualTransform((v, e) -> e.emit(v), exec)
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValues(1)
            ;
        });
    }
}
