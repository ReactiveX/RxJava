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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;

public class StreamableForEachTest extends StreamableBaseTest {

    @Test
    public void forEachCheckedCrash() {
        var ex = assertThrows(CompletionException.class, () -> {
            Streamable.just(1)
            .forEach(_ -> {
                throw new Exception("test");
            })
            .await()
            ;
        });

        assertEquals("test", ex.getCause().getMessage());
    }

    @Test
    public void forEachUncheckedCrash() {
        var ex = assertThrows(TestException.class, () -> {
            Streamable.just(1)
            .forEach(_ -> {
                throw new TestException("test");
            })
            .await()
            ;
        });

        assertEquals("test", ex.getMessage());
    }

    @Test
    public void forEachExecCheckedCrash() throws Throwable {
        withCachedExecutor(exec -> {
            var ex = assertThrows(CompletionException.class, () -> {
                Streamable.just(1)
                .forEach(_ -> {
                    throw new Exception("test");
                }, exec)
                .await()
                ;
            });

            assertEquals("test", ex.getCause().getMessage());
        });
    }

    @Test
    public void forEachExecUncheckedCrash() throws Throwable {
        withCachedExecutor(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.just(1)
                .forEach(_ -> {
                    throw new TestException("test");
                }, exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachBiCheckedCrash() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(CompletionException.class, () -> {
                Streamable.just(1)
                .forEach((_, _) -> {
                    throw new Exception("test");
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getCause().getMessage());
        });
    }

    @Test
    public void forEachBiUncheckedCrash() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.just(1)
                .forEach((_, _) -> {
                    throw new TestException("test");
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachBiUncheckedPropagation() throws Throwable {
        withVirtual(exec -> {
            var ex = assertThrows(TestException.class, () -> {
                Streamable.error(new TestException("test"))
                .forEach((_, _) -> {
                }, new CompositeDisposable(), exec)
                .await()
                ;
            });

            assertEquals("test", ex.getMessage());
        });
    }

    @Test
    public void forEachOutsideCancel() {
        var cd = new CompositeDisposable();
        var counter = new AtomicInteger();

        assertThrows(CancellationException.class, () -> {
            Streamable.range(1, 5)
            .forEach(_ -> {
                counter.getAndIncrement();
                cd.dispose();
                Thread.sleep(10); // The body may fall off faster than the cancel can propagate out, so sleep
            }, cd)
            .await();
        });

        assertTrue(cd.isDisposed(), "cd was not disposed");
        assertEquals(1, counter.get());
    }

    @Test
    public void forEachBiOutsideCancel() throws Throwable {
        withVirtual(exec -> {
            var cd = new CompositeDisposable();
            var counter = new AtomicInteger();

            assertThrows(CancellationException.class, () -> {
                Streamable.range(1, 5)
                .forEach((_, _) -> {
                    counter.getAndIncrement();
                    cd.dispose();
                    Thread.sleep(10); // The body may fall off faster than the cancel can propagate out, so sleep
                }, cd, exec)
                .await();
            });

            assertTrue(cd.isDisposed(), "cd was not disposed");
            assertEquals(1, counter.get());
        });
    }

    @Test
    public void forEachBiInsideCancel() throws Throwable {
        withVirtual(exec -> {
            var cd = new CompositeDisposable();
            var counter = new AtomicInteger();

            Streamable.range(1, 5)
            .forEach((_, s) -> {
                counter.getAndIncrement();
                s.dispose();
            }, cd, exec)
            .await();

            assertFalse(cd.isDisposed(), "cd was disposed");
            assertEquals(1, counter.get());
        });
    }

    @Test
    public void forEachInput() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();
        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        Streamable.range(1, 5)
        .subscribe(dsp);

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void forEachInputDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var dsp = new DispatchStreamProcessor<>();
            var ts = dsp.test(exec);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            awaitStreamers(dsp, 1000);

            Streamable.range(1, 5)
            .subscribe(dsp);

            ts.awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void forEachInputError() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();
        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        Streamable.error(new TestException())
        .subscribe(dsp);

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void forEachInputCancelUpfront() throws Throwable {
        var dsp0 = new DispatchStreamProcessor<>();

        var dsp = new DispatchStreamProcessor<>();
        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        var cd = new CompositeDisposable();
        cd.dispose();
        dsp0.subscribe(dsp.withCancellation(cd));

        awaitNoStreamers(dsp0, 1000);

        awaitNoStreamers(dsp, 1000);

        assertTrue(dsp.hasComplete(), "dsp completes: error = " + dsp.hasThrowable());
    }

    @Test
    public void forEachInputWithCancellationOverride() throws Throwable {
        var dspMain = new DispatchStreamProcessor<>();

        var dspSecondary = new DispatchStreamProcessor<>();
        var ts = dspSecondary.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dspSecondary, 1000);

        var cd = new CompositeDisposable();
        dspMain.subscribe(dspSecondary.withCancellation(cd));

        awaitStreamers(dspMain, 1000);

        dspMain.next(1).toCompletableFuture().join();
        dspMain.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);

        assertTrue(dspSecondary.hasComplete(), "dsp completes: error = " + dspSecondary.hasThrowable());
    }

    @Test
    public void forEachInputSendNull() throws Throwable {
        IO.println("forEachInputSendNull");
        var error = new AtomicReference<Throwable>();
        var dsp = new DispatchStreamProcessor<>();
        var si = StreamSink.create(_ -> null, e -> { error.set(e); return Streamer.FINISHED; });
        var f = dsp.subscribe(si);

        IO.println("    hasStreamers()");

        awaitStreamers(dsp, 1000);

        IO.println("    next(1)");

        dsp.next(1).toCompletableFuture().join();

        IO.println("    f.toCompletableFuture.join");

        f.toCompletableFuture().join();

        assertTrue(error.get() instanceof NullPointerException, "" + error.get());

        IO.println("    .");
    }

    @Test
    public void forEachInputSendCrash() throws Throwable {
        IO.println("forEachInputSendCrash");
        var error = new AtomicReference<Throwable>();
        var dsp = new DispatchStreamProcessor<>();
        var si = StreamSink.create(_ -> { throw new TestException(); }, e -> { error.set(e); return Streamer.FINISHED; });
        var f = dsp.subscribe(si);

        IO.println("    hasStreamers()");

        awaitStreamers(dsp, 1000);

        IO.println("    next(1)");
        dsp.next(1).toCompletableFuture().join();

        IO.println("    f.toCompletableFuture.join");

        f.toCompletableFuture().join();

        assertTrue(error.get() instanceof TestException, "" + error.get());

        IO.println("    .");
    }

    @Test
    public void forEachInputTerminateNull() throws Throwable {
        IO.println("forEachInputTerminateNull");
        var dsp = new DispatchStreamProcessor<>();
        var si = StreamSink.create(_ -> Streamer.NEXT_TRUE, _ -> { return null; });
        var f = dsp.subscribe(si);

        IO.println("    hasStreamers()");

        awaitStreamers(dsp, 1000);

        IO.println("    next(1)");
        dsp.next(1).toCompletableFuture().join();

        IO.println("    finish()");
        dsp.finish(null).toCompletableFuture().join();

        IO.println("    f.toCompletableFuture.join");

        var ex = assertThrows(CompletionException.class, () -> {
            f.toCompletableFuture().join();
        });

        assertTrue(ex.getCause() instanceof NullPointerException, ex.getCause().toString());

        IO.println("    .");
    }

    @Test
    public void forEachInputTerminateCrash() throws Throwable {
        IO.println("forEachInputTerminateNull");
        var dsp = new DispatchStreamProcessor<>();
        var si = StreamSink.create(_ -> Streamer.NEXT_TRUE, _ -> { throw new TestException(); });
        var f = dsp.subscribe(si);

        IO.println("    hasStreamers()");

        awaitStreamers(dsp, 1000);

        IO.println("    next(1)");
        dsp.next(1).toCompletableFuture().join();

        IO.println("    finish()");
        dsp.finish(null).toCompletableFuture().join();

        IO.println("    f.toCompletableFuture.join");

        var ex = assertThrows(CompletionException.class, () -> {
            f.toCompletableFuture().join();
        });

        assertTrue(ex.getCause() instanceof TestException, ex.getCause().toString());

        IO.println("    .");
    }

    @Test
    public void forEachInputTerminateBothCrash() throws Throwable {
        IO.println("forEachInputTerminateNull");
        var dsp = new DispatchStreamProcessor<>();
        var si = StreamSink.create(_ -> null, _ -> { throw new TestException(); });
        var f = dsp.subscribe(si);

        IO.println("    hasStreamers()");

        awaitStreamers(dsp, 1000);

        IO.println("    next(1)");
        dsp.next(1).toCompletableFuture().join();

        IO.println("    finish()");
        dsp.finish(null).toCompletableFuture().join();

        IO.println("    f.toCompletableFuture.join");

        var ex = assertThrows(CompletionException.class, () -> {
            f.toCompletableFuture().join();
        });

        assertTrue(ex.getCause() instanceof TestException, ex.getCause().toString());
        assertTrue(ex.getCause().getSuppressed()[0] instanceof NullPointerException,
                ex.getCause().getSuppressed()[0].toString());

        IO.println("    .");
    }

    @Test
    public void forEachUpstreamFinishCrash() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();
        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        awaitStreamers(dsp, 1000);

        StreamableFailingFinish.MAIN_COMPLETES
        .subscribe(dsp);

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void forEachBiUpstreamFinishCrash() throws Throwable {
        var fs = StreamableFailingFinish.MAIN_COMPLETES
        .forEach((_, _) -> {
        }, new CompositeDisposable(), Executors.newVirtualThreadPerTaskExecutor());

        assertThrows(TestException.class, () -> {
            fs.await();
        });
    }

    @Test
    public void forEachBiUpstreamFinishCrashDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var fs = StreamableFailingFinish.MAIN_COMPLETES
                    .forEach((_, _) -> {
                    }, new CompositeDisposable(), exec);

                    assertThrows(TestException.class, () -> {
                        fs.await();
                    });
        });
    }
}
