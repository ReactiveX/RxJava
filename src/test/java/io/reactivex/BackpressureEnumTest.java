/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.annotations.BackpressureKind;

public class BackpressureEnumTest {

    @Test
    public void backpressureOverflowStrategy() {
        assertEquals(3, BackpressureOverflowStrategy.values().length);

        assertNotNull(BackpressureOverflowStrategy.valueOf("ERROR"));
    }

    @Test
    public void backpressureStrategy() {
        assertEquals(5, BackpressureStrategy.values().length);

        assertNotNull(BackpressureStrategy.valueOf("BUFFER"));
    }

    @Test
    public void backpressureKind() {
        assertEquals(6, BackpressureKind.values().length);

        assertNotNull(BackpressureKind.valueOf("FULL"));
    }

}
