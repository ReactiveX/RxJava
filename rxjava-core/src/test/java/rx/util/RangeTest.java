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
package rx.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RangeTest {

    @Test
    public void testSimpleRange() {
        assertEquals(Arrays.asList(1, 2, 3, 4), toList(Range.create(1, 5)));
    }

    @Test
    public void testRangeWithStep() {
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), toList(Range.createWithStep(1, 10, 2)));
    }

    @Test
    public void testRangeWithCount() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), toList(Range.createWithCount(1, 5)));
    }

    @Test
    public void testRangeWithCount2() {
        assertEquals(Arrays.asList(2, 3, 4, 5), toList(Range.createWithCount(2, 4)));
    }

    @Test
    public void testRangeWithCount3() {
        assertEquals(Arrays.asList(0, 1, 2, 3), toList(Range.createWithCount(0, 4)));
    }

    @Test
    public void testRangeWithCount4() {
        assertEquals(Arrays.asList(10, 11, 12, 13, 14), toList(Range.createWithCount(10, 5)));
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        List<T> result = new ArrayList<T>();
        for (T element : iterable) {
            result.add(element);
        }
        return result;
    }
}
