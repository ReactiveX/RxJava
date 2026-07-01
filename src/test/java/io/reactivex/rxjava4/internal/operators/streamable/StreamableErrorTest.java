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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.operators.streamable.StreamableError.ErrorStreamer;

@Isolated
public class StreamableErrorTest extends StreamableBaseTest {

    @Test
    public void normalSingleExecutor() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.error(new TestException())
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void normal() throws Throwable {
        Streamable.error(new TestException())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("resource")
    @Test
    public void currentThrows() {
        assertThrows(IllegalStateException.class, () -> {
            new ErrorStreamer<>(new TestException()).current();
        });
    }
}
