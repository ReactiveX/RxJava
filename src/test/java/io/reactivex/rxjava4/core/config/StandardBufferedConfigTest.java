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

public class StandardBufferedConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertTrue(new StandardBufferedConfig(true).delayErrors(), "delayError - true");
        assertFalse(new StandardBufferedConfig(false).delayErrors(), "delayError - false");
        assertEquals(5, new StandardBufferedConfig(5).bufferSize(), "prefetch - 5");
        assertEquals(5, new StandardBufferedConfig(true, 5).bufferSize(), "prefetch both - true, 5");
        assertEquals(5, new StandardBufferedConfig(false, 5).bufferSize(), "prefetch both - false, 5");
        assertTrue(new StandardBufferedConfig(true, 5).delayErrors(), "delayError both - true, 5");
        assertFalse(new StandardBufferedConfig(false, 5).delayErrors(), "delayError both - false, 5");

        assertEquals(ErrorMode.IMMEDIATE, new StandardBufferedConfig(ErrorMode.IMMEDIATE).errorMode(), "errorMode - IMMEDIATE");
        assertEquals(ErrorMode.BOUNDARY, new StandardBufferedConfig(ErrorMode.BOUNDARY).errorMode(), "errorMode - BOUNDARY");
        assertEquals(ErrorMode.END, new StandardBufferedConfig(ErrorMode.END).errorMode(), "errorMode - END");
        assertEquals(5, new StandardBufferedConfig(5).bufferSize(), "bufferSize - 5");
        assertEquals(5, new StandardBufferedConfig(ErrorMode.IMMEDIATE, 5).bufferSize(), "bufferSize both - IMMEDIATE, 5");
        assertEquals(5, new StandardBufferedConfig(ErrorMode.BOUNDARY, 5).bufferSize(), "bufferSize both - BOUNDARY, 5");
        assertEquals(5, new StandardBufferedConfig(ErrorMode.END, 5).bufferSize(), "bufferSize both - END, 5");
        assertEquals(ErrorMode.IMMEDIATE, new StandardBufferedConfig(ErrorMode.IMMEDIATE, 5).errorMode(), "errorMode both - IMMEDIATE, 5");
        assertEquals(ErrorMode.BOUNDARY, new StandardBufferedConfig(ErrorMode.BOUNDARY, 5).errorMode(), "errorMode both - BOUNDARY, 5");
        assertEquals(ErrorMode.END, new StandardBufferedConfig(ErrorMode.END, 5).errorMode(), "errorMode both - END, 5");
}
}
