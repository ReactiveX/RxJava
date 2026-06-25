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

import io.reactivex.rxjava4.core.RxJavaTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObservableMergeConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertTrue(new ObservableMergeConfig(true).delayErrors(), "delayErrors - true");
        assertFalse(new ObservableMergeConfig(false).delayErrors(), "delayErrors - false");
        assertEquals(5, new ObservableMergeConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new ObservableMergeConfig(true, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new ObservableMergeConfig(false, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertTrue(new ObservableMergeConfig(true, 5).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new ObservableMergeConfig(false, 5).delayErrors(), "delayErrors both - false, 5");

        assertTrue(new ObservableMergeConfig(true, 5, 10).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new ObservableMergeConfig(false, 5, 10).delayErrors(), "delayErrors both - false, 5");
        assertEquals(5, new ObservableMergeConfig(true, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(10, new ObservableMergeConfig(false, 5, 10).bufferSize(), "bufferSize both - false, 5");
    }
}
