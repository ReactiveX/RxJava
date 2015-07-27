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

import java.util.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class LinkedArrayListTest {
    @Test
    public void testAdd() {
        LinkedArrayList list = new LinkedArrayList(16);
        
        List<Integer> expected = new ArrayList<Integer>(32);
        for (int i = 0; i < 32; i++) {
            list.add(i);
            expected.add(i);
        }
        
        assertEquals(expected, list.toList());
        assertEquals(32, list.size());
    }
}