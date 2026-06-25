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

public class ObservableConcatEagerConfigTest extends RxJavaTest {

    @Test
    public void validation() {
        assertEquals(ErrorMode.IMMEDIATE, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE).errorMode(), "errorMode - true");
        assertEquals(ErrorMode.BOUNDARY, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY).errorMode(), "errorMode - false");
        assertEquals(ErrorMode.END, new ObservableConcatEagerConfig(ErrorMode.END).errorMode(), "errorMode - false");
        assertEquals(5, new ObservableConcatEagerConfig(5).maxConcurrency(), "maxConcurrency - 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, 5).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.END, 5).maxConcurrency(), "maxConcurrency both - false, 5");
        assertEquals(ErrorMode.IMMEDIATE, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, 5).errorMode(), "errorMode both - true, 5");
        assertEquals(ErrorMode.BOUNDARY, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY, 5).errorMode(), "errorMode both - false, 5");
        assertEquals(ErrorMode.END, new ObservableConcatEagerConfig(ErrorMode.END, 5).errorMode(), "errorMode both - false, 5");

        assertEquals(ErrorMode.IMMEDIATE, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, 5, 10).errorMode(), "delayErrors both - true, 5");
        assertEquals(ErrorMode.BOUNDARY, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY, 5, 10).errorMode(), "delayErrors both - false, 5");
        assertEquals(ErrorMode.END, new ObservableConcatEagerConfig(ErrorMode.END, 5, 10).errorMode(), "delayErrors both - false, 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");
        assertEquals(5, new ObservableConcatEagerConfig(ErrorMode.END, 5, 10).maxConcurrency(), "maxConcurrency both - true, 5");

        assertEquals(10, new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, 5, 10).bufferSize(), "bufferSize both - false, 5");
        assertEquals(10, new ObservableConcatEagerConfig(ErrorMode.BOUNDARY, 5, 10).bufferSize(), "bufferSize both - false, 5");
        assertEquals(10, new ObservableConcatEagerConfig(ErrorMode.END, 5, 10).bufferSize(), "bufferSize both - false, 5");
    }
}
