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
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.*;
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
}
