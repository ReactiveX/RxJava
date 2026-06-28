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

import io.reactivex.rxjava4.core.*;

public class StandardConcurrentConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertTrue(new StandardConcurrentConfig(true).delayErrors(), "delayErrors - true");
        assertFalse(new StandardConcurrentConfig(false).delayErrors(), "delayErrors - false");
        assertEquals(5, new StandardConcurrentConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new StandardConcurrentConfig(true, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new StandardConcurrentConfig(false, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertTrue(new StandardConcurrentConfig(true, 5).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new StandardConcurrentConfig(false, 5).delayErrors(), "delayErrors both - false, 5");

        assertEquals(ErrorMode.IMMEDIATE, new StandardConcurrentConfig(ErrorMode.IMMEDIATE).errorMode(), "errorMode - IMMEDIATE");
        assertEquals(ErrorMode.BOUNDARY, new StandardConcurrentConfig(ErrorMode.BOUNDARY).errorMode(), "errorMode - BOUNDARY");
        assertEquals(ErrorMode.END, new StandardConcurrentConfig(ErrorMode.END).errorMode(), "errorMode - END");
        assertEquals(5, new StandardConcurrentConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new StandardConcurrentConfig(ErrorMode.IMMEDIATE, 5).maxConcurrency(), "maxConcurrency both - IMMEDIATE, 5");
        assertEquals(5, new StandardConcurrentConfig(ErrorMode.BOUNDARY, 5).maxConcurrency(), "maxConcurrency both - BOUNDARY, 5");
        assertEquals(5, new StandardConcurrentConfig(ErrorMode.END, 5).maxConcurrency(), "maxConcurrency both - END, 5");
        assertEquals(ErrorMode.IMMEDIATE, new StandardConcurrentConfig(ErrorMode.IMMEDIATE, 5).errorMode(), "errorMode both - IMMEDIATE, 5");
        assertEquals(ErrorMode.BOUNDARY, new StandardConcurrentConfig(ErrorMode.BOUNDARY, 5).errorMode(), "errorMode both - BOUNDARY, 5");
        assertEquals(ErrorMode.END, new StandardConcurrentConfig(ErrorMode.END, 5).errorMode(), "errorMode both - END, 5");
}
}
