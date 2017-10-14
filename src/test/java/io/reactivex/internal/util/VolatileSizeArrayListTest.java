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

import java.util.*;

import org.junit.Test;

public class VolatileSizeArrayListTest {

    @Test
    public void normal() {
        List<Integer> list = new VolatileSizeArrayList<Integer>();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.contains(1));
        assertFalse(list.remove((Integer)1));

        list = new VolatileSizeArrayList<Integer>(16);
        assertTrue(list.add(1));
        assertTrue(list.addAll(Arrays.asList(3, 4, 7)));
        list.add(1, 2);
        assertTrue(list.addAll(4, Arrays.asList(5, 6)));

        assertTrue(list.contains(2));
        assertFalse(list.remove((Integer)10));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), list);
        assertFalse(list.isEmpty());
        assertEquals(7, list.size());

        Iterator<Integer> it = list.iterator();
        for (int i = 1; i < 8; i++) {
            assertEquals(i, it.next().intValue());
        }

        assertArrayEquals(new Object[] { 1, 2, 3, 4, 5, 6, 7 }, list.toArray());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }, list.toArray(new Integer[7]));

        assertTrue(list.containsAll(Arrays.asList(2, 4, 6)));
        assertFalse(list.containsAll(Arrays.asList(2, 4, 6, 10)));

        assertFalse(list.removeAll(Arrays.asList(10, 11, 12)));

        assertFalse(list.retainAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7)));

        assertEquals(7, list.size());

        for (int i = 1; i < 8; i++) {
            assertEquals(i, list.get(i - 1).intValue());
        }

        for (int i = 1; i < 8; i++) {
            assertEquals(i, list.set(i - 1, i).intValue());
        }

        assertEquals(2, list.indexOf(3));

        assertEquals(5, list.lastIndexOf(6));

        ListIterator<Integer> lit = list.listIterator(7);
        for (int i = 7; i > 0; i--) {
            assertEquals(i, lit.previous().intValue());
        }

        assertEquals(Arrays.asList(3, 4, 5), list.subList(2, 5));

        VolatileSizeArrayList<Integer> list2 = new VolatileSizeArrayList<Integer>();
        list2.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));

        assertFalse(list2.equals(list));
        assertFalse(list.equals(list2));

        list2.add(7);
        assertTrue(list2.equals(list));
        assertTrue(list.equals(list2));

        List<Integer> list3 = new ArrayList<Integer>();
        list3.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));

        assertFalse(list3.equals(list));
        assertFalse(list.equals(list3));

        list3.add(7);
        assertTrue(list3.equals(list));
        assertTrue(list.equals(list3));

        assertEquals(list.hashCode(), list3.hashCode());
        assertEquals(list.toString(), list3.toString());

        list.remove(0);
        assertEquals(6, list.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }
}
