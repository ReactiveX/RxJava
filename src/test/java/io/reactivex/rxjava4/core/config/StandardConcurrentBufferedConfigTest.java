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

import io.reactivex.rxjava4.core.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StandardConcurrentBufferedConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertEquals(ErrorMode.IMMEDIATE, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE).errorMode(), "errorMode - true");
        assertEquals(ErrorMode.BOUNDARY, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY).errorMode(), "errorMode - false");
        assertEquals(ErrorMode.END, new StandardConcurrentBufferedConfig(ErrorMode.END).errorMode(), "errorMode - false");
        assertEquals(5, new StandardConcurrentBufferedConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.END, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertEquals(ErrorMode.IMMEDIATE, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE, 5).errorMode(), "errorMode both - true, 5");
        assertEquals(ErrorMode.BOUNDARY, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, 5).errorMode(), "errorMode both - false, 5");
        assertEquals(ErrorMode.END, new StandardConcurrentBufferedConfig(ErrorMode.END, 5).errorMode(), "errorMode both - false, 5");

        assertEquals(ErrorMode.IMMEDIATE, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE, 5, 10).errorMode(), "delayErrors both - true, 5");
        assertEquals(ErrorMode.BOUNDARY, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, 5, 10).errorMode(), "delayErrors both - false, 5");
        assertEquals(ErrorMode.END, new StandardConcurrentBufferedConfig(ErrorMode.END, 5, 10).errorMode(), "delayErrors both - false, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(ErrorMode.END, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");

        assertEquals(10, new StandardConcurrentBufferedConfig(ErrorMode.IMMEDIATE, 5, 10).bufferSize(), "bufferSize both - false, 5");
        assertEquals(10, new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, 5, 10).bufferSize(), "bufferSize both - false, 5");
        assertEquals(10, new StandardConcurrentBufferedConfig(ErrorMode.END, 5, 10).bufferSize(), "bufferSize both - false, 5");

        assertTrue(new StandardConcurrentBufferedConfig(true).delayErrors(), "delayErrors - true");
        assertFalse(new StandardConcurrentBufferedConfig(false).delayErrors(), "delayErrors - false");
        assertEquals(5, new StandardConcurrentBufferedConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(true, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(false, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertTrue(new StandardConcurrentBufferedConfig(true, 5).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new StandardConcurrentBufferedConfig(false, 5).delayErrors(), "delayErrors both - false, 5");

        assertTrue(new StandardConcurrentBufferedConfig(true, 5, 10).delayErrors(), "delayErrors both - true, 5");
        assertFalse(new StandardConcurrentBufferedConfig(false, 5, 10).delayErrors(), "delayErrors both - false, 5");
        assertEquals(5, new StandardConcurrentBufferedConfig(true, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(10, new StandardConcurrentBufferedConfig(false, 5, 10).bufferSize(), "bufferSize both - false, 5");
}
}
