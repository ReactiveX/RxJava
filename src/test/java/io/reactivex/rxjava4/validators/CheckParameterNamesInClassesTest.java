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

package io.reactivex.rxjava4.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;

public class CheckParameterNamesInClassesTest extends RxJavaTest {

    void method(int paramName) {
        // deliberately empty
        assertEquals(1, paramName);
    }

    @Test
    public void javacParametersEnabled() throws Exception {
        assertEquals(
                "paramName",
                getClass()
                .getDeclaredMethod("method", Integer.TYPE)
                .getParameters()[0].getName(),
                "Please enable saving parameter names via the -parameters javac argument");
    }
}
