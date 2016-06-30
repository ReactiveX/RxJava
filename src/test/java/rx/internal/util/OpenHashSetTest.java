/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class OpenHashSetTest {
    @Test
    public void addRemove() {
        OpenHashSet<Integer> set = new OpenHashSet<Integer>();
        
        for (int i = 0; i < 1000; i++) {
            assertTrue(set.add(i));
            assertFalse(set.add(i));
            assertTrue(set.remove(i));
            assertFalse(set.remove(i));
        }
        
        Object[] values = set.values();
        for (Object i : values) {
            assertNull(i);
        }
    }
    
    @Test
    public void addAllRemoveAll() {
        for (int i = 16; i < 128 * 1024; i *= 2) {
            OpenHashSet<Integer> set = new OpenHashSet<Integer>(i);
            for (int j = 0; j < i * 2; j++) {
                assertTrue(set.add(j));
                assertFalse(set.add(j));
            }
            for (int j = i * 2 - 1; j >= 0; j--) {
                assertTrue(set.remove(j));
                assertFalse(set.remove(j));
            }
            Object[] values = set.values();
            for (Object j : values) {
                assertNull(j);
            }
        }
    }
}
