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

package io.reactivex.rxjava4.core.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.functions.Consumer;

public class SampleConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        Consumer<Object> consumer = _ -> { };

        assertFalse(new SampleConfig<>(false).emitLast(), "emitLast - false");
        assertTrue(new SampleConfig<>(true).emitLast(), "emitLast - true");

        assertEquals(consumer, new SampleConfig<>(consumer).onDropped(), "onDropped");
        assertFalse(new SampleConfig<>(consumer).emitLast(), "emitLast");

        assertEquals(consumer, new SampleConfig<>(false, consumer).onDropped(), "onDropped");
        assertFalse(new SampleConfig<>(false, consumer).emitLast(), "emitLast");

        assertEquals(consumer, new SampleConfig<>(true, consumer).onDropped(), "onDropped");
        assertTrue(new SampleConfig<>(true, consumer).emitLast(), "emitLast");
    }
}