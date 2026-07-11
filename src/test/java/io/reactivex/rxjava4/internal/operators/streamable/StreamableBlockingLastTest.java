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

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableBlockingLastTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        assertEquals(1, Streamable.just(1).blockingLast());
    }

    @Test
    public void many() throws Throwable {
        assertEquals(5, Streamable.range(1, 5).blockingLast());
    }

    @Test
    public void empty() throws Throwable {
        assertThrows(NoSuchElementException.class, () -> {
            Streamable.empty().blockingLast();
        });
    }

    @Test
    public void errorUnchecked() throws Throwable {
        assertThrows(TestException.class, () -> {
            Streamable.error(new TestException()).blockingLast();
        });
    }

    @Test
    public void errorChecked() throws Throwable {
        var ex = assertThrows(CompletionException.class, () -> {
            Streamable.error(new IOException()).blockingLast();
        });

        assertTrue(ex.getCause() instanceof IOException, "Wrong exception? " + ex.getCause());
    }

    @Test
    public void finishCrash() throws Throwable {
        assertThrows(TestException.class, () -> {
            StreamableFailingFinish.MAIN_COMPLETES.blockingLast();
        });
    }

    @Test
    public void nextAndFinishCrash() throws Throwable {
        var ex = assertThrows(TestException.class, () -> {
            StreamableFailingFinish.MAIN_FAILS.blockingLast();
        });

        assertTrue(ex.getSuppressed()[0] instanceof TestException, "Wrong exception? " + ex.getSuppressed()[0]);
    }

}
