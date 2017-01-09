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

package io.reactivex.internal.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class OpenHashSetTest {

    static class Value {

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @Test
    public void addRemoveCollision() {
        Value v1 = new Value();
        Value v2 = new Value();

        OpenHashSet<Value> set = new OpenHashSet<Value>();

        assertTrue(set.add(v1));

        assertFalse(set.add(v1));

        assertFalse(set.remove(v2));

        assertTrue(set.add(v2));

        assertFalse(set.add(v2));

        assertTrue(set.remove(v2));

        assertFalse(set.remove(v2));
    }
}
