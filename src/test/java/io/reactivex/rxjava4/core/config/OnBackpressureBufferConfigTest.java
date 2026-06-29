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

public class OnBackpressureBufferConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        Consumer<Object> consumer = _ -> { };

        assertEquals(5, new OnBackpressureBufferConfig<>(5).capacity(), "capacity - 5");
        assertFalse(new OnBackpressureBufferConfig<>(false).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(true).delayError(), "delayError - 5");

        assertFalse(new OnBackpressureBufferConfig<>(5, false).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(5, true).delayError(), "delayError - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, false).capacity(), "delayError - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, true).capacity(), "delayError - 5");

        assertFalse(new OnBackpressureBufferConfig<>(false,false).delayError(), "delayError - 5");
        assertFalse(new OnBackpressureBufferConfig<>(false, false).unbounded(), "unbounded - 5");
        assertTrue(new OnBackpressureBufferConfig<>(true, true).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(true, true).unbounded(), "unbounded - 5");

        assertFalse(new OnBackpressureBufferConfig<>(false, true).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(false, true).unbounded(), "unbounded - 5");
        assertTrue(new OnBackpressureBufferConfig<>(true, false).delayError(), "delayError - 5");
        assertFalse(new OnBackpressureBufferConfig<>(true, false).unbounded(), "unbounded - 5");

        assertEquals(consumer, new OnBackpressureBufferConfig<>(consumer).onDropped(), "capacity - 5");

        assertEquals(5, new OnBackpressureBufferConfig<>(5, consumer).capacity(), "capacity - 5");
        assertEquals(consumer, new OnBackpressureBufferConfig<>(5, consumer).onDropped(), "onDropped - 5");

        assertFalse(new OnBackpressureBufferConfig<>(5, false,false).delayError(), "delayError - 5");
        assertFalse(new OnBackpressureBufferConfig<>(5, false, false).unbounded(), "unbounded - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, false, false).capacity(), "capacity - 5");
        assertTrue(new OnBackpressureBufferConfig<>(5, true, true).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(5, true, true).unbounded(), "unbounded - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, false, true).capacity(), "capacity - 5");

        assertFalse(new OnBackpressureBufferConfig<>(5, false, true).delayError(), "delayError - 5");
        assertTrue(new OnBackpressureBufferConfig<>(5, false, true).unbounded(), "unbounded - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, false, true).capacity(), "capacity - 5");
        assertTrue(new OnBackpressureBufferConfig<>(5, true, false).delayError(), "delayError - 5");
        assertFalse(new OnBackpressureBufferConfig<>(5, true, false).unbounded(), "unbounded - 5");
        assertEquals(5, new OnBackpressureBufferConfig<>(5, true, false).capacity(), "capacity - 5");
    }
}