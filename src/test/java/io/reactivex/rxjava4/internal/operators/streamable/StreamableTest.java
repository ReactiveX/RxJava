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

package io.reactivex.rxjava4.internal.operators.streamable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;

public class StreamableTest extends StreamableBaseTest {

    @Test
    public void empty() throws Throwable {
        withVirtual(exec -> {

            var ts = new TestSubscriber<Integer>();
            ts.onSubscribe(EmptySubscription.INSTANCE);

            try (var comp = Streamable.empty().forEach(e -> ts.onError(new TestException("Element produced? " + e)), exec)) {

                comp.stage().toCompletableFuture().thenAccept(_ -> ts.onComplete())
                .exceptionally(e -> { ts.onError(e); return null; });

                ts
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult();

                assertFalse(exec.isShutdown(), "Exec::IsShutdown");
                assertFalse(exec.isTerminated(), "Exec::IsTerminated");
            }
        });
    }

    @Test
    public void just() throws Throwable {
        withVirtual(exec -> {

            var ts = new TestSubscriber<Integer>();
            ts.onSubscribe(EmptySubscription.INSTANCE);

            try (var comp = Streamable.just(1).forEach(ts::onNext, exec)) {

                comp.stage().toCompletableFuture().thenAccept(_ -> ts.onComplete())
                .exceptionally(e -> { ts.onError(e); return null; }).join();

                ts
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1);

                assertFalse(exec.isShutdown(), "Exec::IsShutdown");
                assertFalse(exec.isTerminated(), "Exec::IsTerminated");
            }
        });
    }

    @RepeatedTest(100)
    public void fromFlowable() throws Throwable {
        withVirtual(exec -> {
            Flowable.range(1, 10)
            .toStreamable(exec)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            ;

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @RepeatedTest(100)
    public void fromFlowableToStreamableToFlowable() throws Throwable {
        withVirtual(exec -> {
            Flowable.range(1, 10)
            .toStreamable(exec)
            .toFlowable(exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            ;

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @RepeatedTest(100)
    public void createAndTransform() throws Throwable {
        onVirtual(exec -> {
            Streamable.<Integer>create(emitter -> {
                for (int i = 1; i < 11; i++) {
                    emitter.emit(i);
                }
            }, exec)
            .transform((item, emitter, _) -> emitter.emit(-item - 1), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(-2, -3, -4, -5, -6, -7, -8, -9, -10, -11);

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @RepeatedTest(100)
    public void flowableRangeAndTransform() throws Throwable {
        onVirtual(exec -> {
            Flowable.range(1, 10)
            .toStreamable(exec)
            .transform((item, emitter, _) -> emitter.emit(-item - 1), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(-2, -3, -4, -5, -6, -7, -8, -9, -10, -11);

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @Test
    public void flowableRangeAndTransform1() throws Throwable {
        onVirtual(exec -> {
            System.out.println(">> START");
            Flowable.range(1, 10)
            .doOnSubscribe(_ -> System.out.println("Flowable::doOnSubscribe"))
            .doOnRequest(v -> System.out.println("Flowable::doOnRequest " + v))
            .doOnCancel(() -> {
                System.out.println("Flowable::doOnCancel");
                new Exception().printStackTrace();
            })
            .doOnNext(v -> System.out.println("Flowable::doOnNext " + v))
            .toStreamable(exec)
            .transform((item, emitter, _) -> emitter.emit(-item - 1), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(-2, -3, -4, -5, -6, -7, -8, -9, -10, -11);
            System.out.println(">> END");

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @Test
    public void flowableRangeAndTransformFlowable1() throws Throwable {
        onVirtual(exec -> {
            System.out.println(">> START");
            Flowable.range(1, 10)
            .doOnSubscribe(_ -> System.out.println("Flowable::doOnSubscribe"))
            .doOnRequest(v -> System.out.println("Flowable::doOnRequest " + v))
            .doOnCancel(() -> {
                System.out.println("Flowable::doOnCancel");
                new Exception().printStackTrace();
            })
            .doOnNext(v -> System.out.println("Flowable::doOnNext " + v))
            .toStreamable(exec)
            .toFlowable()
            .virtualTransform((item, emitter, _) -> {
                System.out.println("Transform " + item);
                emitter.emit(-item - 1);
            }, exec)
            .toStreamable()
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(-2, -3, -4, -5, -6, -7, -8, -9, -10, -11);
            System.out.println(">> END");

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @Test
    public void flowableRangeAndTransform2() throws Throwable {
        withCachedExecutor(exec -> {
            System.out.println(">> START");
            var ts = Flowable.range(1, 10)
            .doOnSubscribe(_ -> System.out.println("Flowable::doOnSubscribe"))
            .doOnRequest(v -> System.out.println("Flowable::doOnRequest " + v))
            .doOnCancel(() -> {
                System.out.println("Flowable::doOnCancel");
                new Exception().printStackTrace();
            })
            .doOnNext(v -> System.out.println("Flowable::doOnNext " + v))
            .toStreamable(exec)
            .transform((item, emitter, _) -> {
                System.out.println("Transform " + item);
                emitter.emit(-item - 1);
            }, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            ;
            System.out.println(">> CHECK");
            ts.assertResult(-2, -3, -4, -5, -6, -7, -8, -9, -10, -11);
            System.out.println(">> END");

            assertFalse(exec.isShutdown(), "Exec::IsShutdown");
            assertFalse(exec.isTerminated(), "Exec::IsTerminated");
        });
    }

    @Test
    public void rangeTransformFilter() throws Throwable {
        withVirtual(exec -> Flowable.range(1, 10)
        .toStreamable(exec)
        .transform((item, emitter, _) -> {
            if ((item & 1) == 0) {
                emitter.emit(item);
            }
        }, exec)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4, 6, 8, 10));
    }

    @Test
    public void rangeTransformTake() throws Throwable {
        withVirtual(exec -> {
            var cancelled = new AtomicInteger();
            Flowable.range(1, 10)
            .doOnCancel(cancelled::incrementAndGet)
            .toStreamable(exec)
            .transform((item, emitter, stopper) -> {
                if (item == 5) {
                    stopper.dispose();
                }
                emitter.emit(item);
            }, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);

            assertEquals(1, cancelled.get(), "Cancellation count ");
        });
    }

    @Test
    public void concat() throws Throwable {
        withVirtual(exec -> {

            var srcs = Flowable.just(Streamable.just(1), Streamable.empty(), Streamable.just(2))
            .toStreamable();

            Streamable.concat(srcs, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2);

        });
    }

    @Test
    public void fromPublisher() throws Throwable {
        Streamable.fromPublisher(Flowable.range(1, 5))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fromPublisherExec() throws Throwable {
        withVirtual(exec -> {
            Streamable.fromPublisher(Flowable.range(1, 5), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void emptyCurrentThrows() {
        assertThrows(NoSuchElementException.class, () -> {
            StreamableEmpty.EmptyStreamer.INSTANCE.current();
        });
    }

    @Test
    public void neverCurrentThrows() {
        assertThrows(NoSuchElementException.class, () -> {
            StreamableNever.NeverStreamer.INSTANCE.current();
        });
    }

    @Test
    public void never() {
        Streamable.never()
        .test()
        .awaitDone(100, TimeUnit.MILLISECONDS)
        .assertTimeout();
    }

    @Test
    public void never2() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.never()
            .test(exec)
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertTimeout();
        });
    }

    @Test
    public void fromStages() throws Throwable {
        withVirtual(exec -> {
            Streamable.fromStages(List.of(
                    CompletableFuture.completedFuture(1),
                    CompletableFuture.completedFuture(2),
                    CompletableFuture.completedFuture(3)
                ), exec
            )
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(3)
            .assertNoErrors()
            .assertComplete();
        });
    }

    @Test
    public void createPlain() {
        Streamable.create(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void createScheduler() {
        Streamable.create(emitter -> {
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
        }, Schedulers.cached())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void hide() {
        var str = Streamable.empty().hide();

        assertFalse(str instanceof StreamableEmpty, str.getClass().toString());
    }

    @Test
    public void to() {
        Streamable.range(1, 5)
        .to(s -> s.test())
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }
}
