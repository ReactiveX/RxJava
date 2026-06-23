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

public class MaybeMergeConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertTrue(new MaybeMergeConfig(true).delayErrors(), "delayErrors - true");
        assertFalse(new MaybeMergeConfig(false).delayErrors(), "delayErrors - false");
        assertEquals(5, new MaybeMergeConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new MaybeMergeConfig(true, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new MaybeMergeConfig(false, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertTrue(new MaybeMergeConfig(true, 5).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new MaybeMergeConfig(false, 5).delayErrors(), "delayErrors both - false, 5");
    }
}
