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

public class SingleConcatConfigEagerTest extends RxJavaTest {

    @Test
    public void validation() {
        assertTrue(new SingleConcatEagerConfig(true).delayError(), "delayError - true");
        assertFalse(new SingleConcatEagerConfig(false).delayError(), "delayError - false");
        assertEquals(5, new SingleConcatEagerConfig(5).prefetch(), "prefetch - 5");
        assertEquals(5, new SingleConcatEagerConfig(true, 5).prefetch(), "prefetch both - true, 5");
        assertEquals(5, new SingleConcatEagerConfig(false, 5).prefetch(), "prefetch both - false, 5");
        assertEquals(5, new SingleConcatEagerConfig(true, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new SingleConcatEagerConfig(false, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertTrue(new SingleConcatEagerConfig(true, 5).delayError(), "delayError both - true, 5");
        assertFalse(new SingleConcatEagerConfig(false, 5).delayError(), "delayError both - false, 5");

        assertEquals(5, new SingleConcatEagerConfig(true, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5, 10");
        assertEquals(10, new SingleConcatEagerConfig(false, 5, 10).prefetch(), "prefetch both - false, 5, 10");
        assertTrue(new SingleConcatEagerConfig(true, 5, 10).delayError(), "delayError both - true, 5, 10");
        assertFalse(new SingleConcatEagerConfig(false, 5, 10).delayError(), "delayError both - true, 5, 10");
    }
}
