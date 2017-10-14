/**
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

package io.reactivex.internal.functions;

import org.junit.Test;

import static org.junit.Assert.*;
import io.reactivex.TestHelper;

public class ObjectHelperTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(ObjectHelper.class);
    }

    @Test
    public void hashCodeOf() {
        assertEquals(0, ObjectHelper.hashCode(null));

        assertEquals(((Integer)1).hashCode(), ObjectHelper.hashCode(1));
    }

    @Test
    public void verifyPositiveInt() throws Exception {
        assertEquals(1, ObjectHelper.verifyPositive(1, "param"));
    }

    @Test
    public void verifyPositiveLong() throws Exception {
        assertEquals(1L, ObjectHelper.verifyPositive(1L, "param"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyPositiveIntFail() throws Exception {
        assertEquals(-1, ObjectHelper.verifyPositive(-1, "param"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyPositiveLongFail() throws Exception {
        assertEquals(-1L, ObjectHelper.verifyPositive(-1L, "param"));
    }

    @Test
    public void compare() {
        assertEquals(-1, ObjectHelper.compare(0, 2));
        assertEquals(0, ObjectHelper.compare(0, 0));
        assertEquals(1, ObjectHelper.compare(2, 0));
    }
}
