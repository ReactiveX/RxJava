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

import java.lang.ref.Cleaner;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StreamableInterceptConfig;
import io.reactivex.rxjava4.exceptions.CompositeException;
import io.reactivex.rxjava4.functions.Consumer;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;

public abstract class StreamableBaseTest extends RxJavaTest {

    protected java.util.function.Consumer<Cleaner.Cleanable> stageTrackingState;

    protected Consumer<? super Throwable> oldHandler;

    protected List<Throwable> errors;

    protected List<Cleaner.Cleanable> cleaners;

    protected volatile boolean undeliverablesExpected;

    @BeforeEach
    protected final void beforeTest() {
        errors = Collections.synchronizedList(new ArrayList<>());
        cleaners = Collections.synchronizedList(new ArrayList<>());
        undeliverablesExpected = false;

        stageTrackingState = CompletionStageDisposable.getAllocationTrace();
        CompletionStageDisposable.setAllocationTrace(cleaners::add);

        oldHandler = RxJavaPlugins.getErrorHandler();
        RxJavaPlugins.setErrorHandler(e -> {
            if (!undeliverablesExpected) {
                errors.add(e);
            }
            if (oldHandler != null) {
                oldHandler.accept(e);
            }
        });
    }

    @AfterEach
    protected final void afterTest(TestInfo testInfo) {
        CompletionStageDisposable.setAllocationTrace(stageTrackingState);
        for (var c : cleaners) {
            c.clean();
        }
        if (!errors.isEmpty()) {
            throw new AssertionError("Undeliverable exceptions during test detected: " + testInfo.getDisplayName(),
                    new CompositeException(errors));
        }
    }

    protected final void setUndeliverablesExpected(boolean isExpected) {
        undeliverablesExpected = isExpected;
    }

    static final BiConsumer<Object, Throwable> DEBUG_WHEN_COMPLETE_NEXT = (v, e) -> {
        if (e != null) {
            IO.println("OnError  : " + e.toString());
            e.printStackTrace(System.out);
        } else {
            IO.println("OnNext   : " + v);
        }
    };
    static final BiConsumer<Object, Throwable> DEBUG_WHEN_COMPLETE_FINISH = (v, e) -> {
        if (e != null) {
            IO.println("OnFinish : " + e.toString());
            e.printStackTrace(System.out);
        } else {
            IO.println("OnFinish : " + v);
        }
    };

    static final StreamableInterceptConfig<Object> DEBUG_INTERCEPT = new StreamableInterceptConfig<>(
            (_, v) -> { IO.println("OnStream"); return v; },
            (_, v) -> { IO.println("OnNext"); return v.whenComplete(DEBUG_WHEN_COMPLETE_NEXT); },
            (v)    -> { IO.println("OnCurrent: " + v); return v; },
            (_, v) -> { IO.println("OnFinish"); return v.whenComplete(DEBUG_WHEN_COMPLETE_FINISH); }
    );

    /**
     * An intercept config that prints out the lifecycle events, values and completion signals.
     * @return the type-appropriate {@link StreamableInterceptConfig}
     */
    @SuppressWarnings("unchecked")
    public static <T> StreamableInterceptConfig<T> debugIntercept() {
        return (StreamableInterceptConfig<T>)DEBUG_INTERCEPT;
    }

    /**
     * Awaits the given {@link StreamProcessor#hasStreamers()} to register an
     * incoming consumer.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to arrive
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void awaitStreamers(StreamProcessor<?, ?> sp, long timeoutMillis)
    throws InterruptedException, TimeoutException
    {
        awaitCondition(true, sp::hasStreamers, timeoutMillis, "hasStreamers still false");
    }

    /**
     * Awaits the given {@link StreamProcessor#hasStreamers()} to register an
     * incoming consumer.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to arrive
     * @param atLeast the minimum number of streamers expected
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void awaitStreamers(StreamProcessor<?, ?> sp, long timeoutMillis, int atLeast)
    throws InterruptedException, TimeoutException
    {
        awaitCondition(true, () -> sp.streamerCount() >= atLeast, timeoutMillis,
                "streamerCount still below " + atLeast + " (was " + sp.streamerCount() + ")");
    }

    /**
     * Awaits the given {@link StreamProcessor#hasStreamers()} to lose
     * all of its streamers.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to leave
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void awaitNoStreamers(StreamProcessor<?, ?> sp, long timeoutMillis)
    throws InterruptedException, TimeoutException
    {
        awaitCondition(false, sp::hasStreamers, timeoutMillis, "hasStreamers still true");
    }

    /**
     * Awaits until the processor has no streamers and asserts that state.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to leave
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void assertNoStreamers(StreamProcessor<?, ?> sp, long timeoutMillis)
    throws InterruptedException, TimeoutException
    {
        awaitNoStreamers(sp, timeoutMillis);
        if (sp.hasStreamers()) {
            throw new AssertionError("Processor still has streamers: " + sp.streamerCount());
        }
    }

    /**
     * Awaits until the processor has at least one streamer and asserts that state.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to arrive
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void assertHasStreamers(StreamProcessor<?, ?> sp, long timeoutMillis)
    throws InterruptedException, TimeoutException
    {
        awaitStreamers(sp, timeoutMillis);
        if (!sp.hasStreamers()) {
            throw new AssertionError("Processor has no streamers");
        }
    }

    /**
     * Awaits until the processor has at least {@code atLeast} streamers and asserts that state.
     * @param sp the processor
     * @param timeoutMillis how long to wait for the streamer(s) to arrive
     * @param atLeast the minimum number of streamers expected
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void assertHasStreamers(StreamProcessor<?, ?> sp, long timeoutMillis, int atLeast)
    throws InterruptedException, TimeoutException
    {
        awaitStreamers(sp, timeoutMillis, atLeast);
        if (sp.streamerCount() < atLeast) {
            throw new AssertionError("Processor streamerCount below " + atLeast + ": " + sp.streamerCount());
        }
    }

    /**
     * Awaits a given {@link BooleanSupplier} to return the expected {@code value}
     * within the given time period by sleeping in 1 microsecond increments until
     * the timeout happens.
     * @param value the expected value within the timeout period
     * @param condition the condition to repeatedly call to assess the state
     * @param timeoutMillis how long to wait for the condition to become as expected
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void awaitCondition(boolean value, @NonNull BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException, TimeoutException {
        awaitCondition(value, condition, timeoutMillis, "condition still " + (!value));
    }

    /**
     * Awaits a given {@link BooleanSupplier} to return the expected {@code value}
     * within the given wall-clock time period.
     * @param value the expected value within the timeout period
     * @param condition the condition to repeatedly call to assess the state
     * @param timeoutMillis how long to wait for the condition to become as expected
     * @param timeoutMessage the message used when the wait times out
     * @throws InterruptedException if the sleep is interrupted
     * @throws TimeoutException if the wait times out
     */
    public static void awaitCondition(boolean value, @NonNull BooleanSupplier condition, long timeoutMillis,
            @NonNull String timeoutMessage)
            throws InterruptedException, TimeoutException {
        long end = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (condition.getAsBoolean() != value) {
            if (System.nanoTime() >= end) {
                throw new TimeoutException(timeoutMessage);
            }
            Thread.sleep(0, 1000);
        }
    }
}
