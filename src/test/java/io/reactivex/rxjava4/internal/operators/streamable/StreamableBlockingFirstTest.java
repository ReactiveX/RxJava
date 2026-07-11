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

public class StreamableBlockingFirstTest extends StreamableBaseTest {

    @Test
    public void blockingFirstNormal() throws Throwable {
        assertEquals(1, Streamable.just(1).blockingFirst());
    }

    @Test
    public void blockingFirstMany() throws Throwable {
        assertEquals(1, Streamable.range(1, 5).blockingFirst());
    }

    @Test
    public void blockingFirstEmpty() throws Throwable {
        assertThrows(NoSuchElementException.class, () -> {
            Streamable.empty().blockingFirst();
        });
    }

    @Test
    public void blockingFirstErrorUnchecked() throws Throwable {
        assertThrows(TestException.class, () -> {
            Streamable.error(new TestException()).blockingFirst();
        });
    }

    @Test
    public void blockingFirstErrorChecked() throws Throwable {
        var ex = assertThrows(CompletionException.class, () -> {
            Streamable.error(new IOException()).blockingFirst();
        });

        assertTrue(ex.getCause() instanceof IOException, "Wrong exception? " + ex.getCause());
    }

    @Test
    public void blockingFirstFinishCrash() throws Throwable {
        assertThrows(TestException.class, () -> {
            StreamableFailingFinish.MAIN_COMPLETES.blockingFirst();
        });
    }

    @Test
    public void blockingFirstNextAndFinishCrash() throws Throwable {
        var ex = assertThrows(TestException.class, () -> {
            StreamableFailingFinish.MAIN_FAILS.blockingFirst();
        });

        assertTrue(ex.getSuppressed()[0] instanceof TestException, "Wrong exception? " + ex.getSuppressed()[0]);
    }

}
