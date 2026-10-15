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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableCollectTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .hide()
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void normalOptimized() throws Throwable {
        Streamable.range(1, 5)
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of()));
    }

    @Test
    public void crash() throws Throwable {
        Streamable.error(new TestException())
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrash() throws Throwable {
        StreamableFailingFinish.MAIN_COMPLETES
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, new ArrayList<>(List.of()));
    }

    @Test
    public void indexerDisposed() throws Throwable {
        assertThrows(CancellationException.class, () -> {
            var cd = new CompositeDisposable();
            cd.dispose();

            Streamable.range(1, 5)
            .collect(ArrayList::new, ArrayList::add)
            .stream(cd)
            .awaitNext();
        });
    }

    @Test
    public void intelvalRange() throws Throwable {
        Streamable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .hide()
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L)));
    }

    @Test
    public void viaDispatch() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp
        .collect(ArrayList::new, ArrayList::add)
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.next(2).toCompletableFuture().join();
        dsp.next(3).toCompletableFuture().join();
        dsp.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3)));
    }

    @Test
    public void viaDispatchError() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp
        .collect(ArrayList::new, ArrayList::add)
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.next(2).toCompletableFuture().join();
        dsp.next(3).toCompletableFuture().join();
        dsp.finish(new TestException()).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void initialItemSupplierNull() {
        Streamable.range(1, 5)
        .collect(() -> null, (_, _) -> { })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void initialItemSupplierCrash() {
        Streamable.range(1, 5)
        .collect(() -> { throw new IOException(); }, (_, _) -> { })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void indexedSourceCollectorCrash() {
        Streamable.range(1, 5)
        .collect(ArrayList::new, (_, _) -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void enumerableSourceCollectorNormal() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void enumerableSourceCollectorCrash() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, (_, _) -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void genericSourceCollectorCrash() {
        Streamable.range(1, 5)
        .hide()
        .collect(ArrayList::new, (_, _) -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void deferredEnumerableSourceCollectorNormal() {
        Single.just(1)
        .flattenAsStreamable(_ -> List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void deferredEnumerableSourceCollectorEmpty() {
        Single.just(1)
        .flattenAsStreamable(_ -> List.of())
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of()));
    }

    @Test
    public void enumerableSourceCancel() {
        var cd = new CompositeDisposable();
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .map(v -> {
            if (v == 3) {
                cd.dispose();
            }
            return v;
        })
        .collect(ArrayList::new, ArrayList::add)
        .withCancellation(cd)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CancellationException.class)
        ;
    }

    @Test
    public void deferredEnumerableSourceCrash() {
        Single.error(new TestException())
        .flattenAsStreamable(_ -> List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, ArrayList::add)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void deferredEnumerableSourceCrashDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Single.error(new TestException())
            .flattenAsStreamable(_ -> List.of(1, 2, 3, 4, 5))
            .collect(ArrayList::new, ArrayList::add)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void deferredEnumerableCollectorCrash() {
        Single.just(1)
        .flattenAsStreamable(_ -> List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, (_, _) -> {
            throw new TestException();
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void deferredEnumerableCollectorCancels() {
        var cd = new CompositeDisposable();
        Single.just(1)
        .flattenAsStreamable(_ -> List.of(1, 2, 3, 4, 5))
        .collect(ArrayList::new, (_, _) -> {
            cd.dispose();
        })
        .withCancellation(cd)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CancellationException.class);
    }

    @Test
    public void viaDispatchCrash() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp
        .collect(ArrayList::new, (_, _) -> {
            throw new TestException();
        })
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
