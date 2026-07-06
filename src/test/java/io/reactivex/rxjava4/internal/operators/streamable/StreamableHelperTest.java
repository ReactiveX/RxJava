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

import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamer;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class StreamableHelperTest extends StreamableBaseTest {

    @Test
    public void utilityClass() throws Throwable {
        TestHelper.checkEnum(StreamableHelper.class);
    }

    @Test
    public void andThenSupplyOtherFails() {
        var e = assertThrows(CompletionException.class, () -> {
            StreamableHelper.andThenSupply(CompletableFuture.completedStage(1),
                    () -> CompletableFuture.failedFuture(new TestException()))
            .join();
        });

        assertTrue(e.getCause() instanceof TestException, e.getCause().toString());
    }

    @Test
    public void isCancellationUnwraps() {
        assertTrue(StreamableHelper.isCancellation(new CompletionException(new CancellationException())), "complete-cancel");
        assertTrue(StreamableHelper.isCancellation(new CancellationException()), "cancel");
        assertFalse(StreamableHelper.isCancellation(new CompletionException(new TestException())), "complete-test");
    }

    @Test
    public void awaitAllVoidEmpty() {
        assertSame(Streamer.FINISHED, StreamableHelper.awaitAllVoid(List.of()));
    }

    @Test
    public void awaitAllBooleanEmpty() {
        assertSame(Streamer.NEXT_FALSE, StreamableHelper.awaitAllBoolean(List.of()));
    }

    @Test
    public void awaitAllVoidError() {
        assertThrows(TestException.class, () -> {
            try {
                StreamableHelper.awaitAllVoid(List.of(CompletableFuture.failedFuture(new TestException())))
                .toCompletableFuture().join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
        });
    }

    @Test
    public void awaitAllVoidErrorCancel() {
        StreamableHelper.awaitAllVoid(List.of(CompletableFuture.failedFuture(new CancellationException())))
        .toCompletableFuture().join();
    }

    @Test
    public void awaitAllBooleanError() {
        assertThrows(TestException.class, () -> {
            try {
                StreamableHelper.awaitAllBoolean(List.of(CompletableFuture.failedFuture(new TestException())))
                .toCompletableFuture().join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
        });
    }

    @Test
    public void awaitAllBooleanErrorCancel() {
        StreamableHelper.awaitAllBoolean(List.of(CompletableFuture.failedFuture(new CancellationException())))
        .toCompletableFuture().join();
    }

    @Test
    public void awaitAllBooleanAllTrue() {
        assertTrue(StreamableHelper.awaitAllBoolean(List.of(Streamer.NEXT_TRUE, Streamer.NEXT_TRUE)).join(), "should have been true");
    }

    @Test
    public void awaitAllBooleanAllFalse() {
        assertFalse(StreamableHelper.awaitAllBoolean(List.of(Streamer.NEXT_FALSE, Streamer.NEXT_FALSE)).join(), "should have been false");
    }

    @Test
    public void awaitAllBooleanMixed() {
        assertFalse(StreamableHelper.awaitAllBoolean(List.of(Streamer.NEXT_TRUE, Streamer.NEXT_FALSE)).join(),
                "should have been (true, false) -> false");
        assertFalse(StreamableHelper.awaitAllBoolean(List.of(Streamer.NEXT_FALSE, Streamer.NEXT_TRUE)).join(),
                "should have been (false, true) -> false");
    }
}
