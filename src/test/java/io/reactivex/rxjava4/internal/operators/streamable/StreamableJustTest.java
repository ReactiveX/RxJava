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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;

public class StreamableJustTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.just(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void collectIndexer() throws Throwable {
        Streamable.just(1)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1));
    }

    @Test
    public void collectEnumerator() throws Throwable {
        Streamable.just(1)
        .filter(_ -> true)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1));
    }

    @Test
    public void multipleItems() throws Throwable {
        Streamable.just(1, 2).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2);
        Streamable.just(1, 2, 3).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3);
        Streamable.just(1, 2, 3, 4).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4);
        Streamable.just(1, 2, 3, 4, 5).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5);
        Streamable.just(1, 2, 3, 4, 5, 6).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5, 6);
        Streamable.just(1, 2, 3, 4, 5, 6, 7).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5, 6, 7);
        Streamable.just(1, 2, 3, 4, 5, 6, 7, 8).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5, 6, 7, 8);
        Streamable.just(1, 2, 3, 4, 5, 6, 7, 8, 9).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Streamable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).test().awaitDone(5, TimeUnit.SECONDS).assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void multipleItemsRejectNull() {
        assertThrows(NullPointerException.class, () -> Streamable.just(1, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, 5, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, 5, 6, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, 5, 6, 7, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, 5, 6, 7, 8, null));
        assertThrows(NullPointerException.class, () -> Streamable.just(1, 2, 3, 4, 5, 6, 7, 8, 9, null));
    }

}
