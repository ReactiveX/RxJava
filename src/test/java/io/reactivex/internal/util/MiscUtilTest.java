/**
 * Copyright 2016 Netflix, Inc.
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

import io.reactivex.TestHelper;
import io.reactivex.functions.BiPredicate;

public class MiscUtilTest {
    @Test
    public void pow2UtilityClass() {
        TestHelper.checkUtilityClass(Pow2.class);
    }
    
    @Test
    public void isPowerOf2() {
        for (int i = 1; i > 0; i *= 2) {
            assertTrue(Pow2.isPowerOfTwo(i));
        }
        
        assertFalse(Pow2.isPowerOfTwo(3));
        assertFalse(Pow2.isPowerOfTwo(5));
        assertFalse(Pow2.isPowerOfTwo(6));
        assertFalse(Pow2.isPowerOfTwo(7));
    }
    
    @Test
    public void hashMapSupplier() {
        TestHelper.checkEnum(HashMapSupplier.class);
    }
    
    @Test
    public void arrayListSupplier() {
        TestHelper.checkEnum(ArrayListSupplier.class);
    }
    
    @Test
    public void errorModeEnum() {
        TestHelper.checkEnum(ErrorMode.class);
    }
    
    @Test
    public void linkedArrayList() {
        LinkedArrayList list = new LinkedArrayList(2);
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals("[1, 2, 3]", list.toString());
    }
    
    @Test
    public void appendOnlyLinkedArrayListForEachWhile() throws Exception {
        AppendOnlyLinkedArrayList<Integer> list = new AppendOnlyLinkedArrayList<Integer>(2);
        
        list.add(1);
        list.add(2);
        list.add(3);
        
        final List<Integer> out = new ArrayList<Integer>();
        
        list.forEachWhile(2, new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer t1, Integer t2) throws Exception {
                out.add(t2);
                return t1.equals(t2);
            }
        });
        
        assertEquals(Arrays.asList(1, 2), out);
    }

    @Test
    public void appendOnlyLinkedArrayListForEachWhileAll() throws Exception {
        AppendOnlyLinkedArrayList<Integer> list = new AppendOnlyLinkedArrayList<Integer>(2);
        
        list.add(1);
        list.add(2);
        list.add(3);
        
        final List<Integer> out = new ArrayList<Integer>();
        
        list.forEachWhile(3, new BiPredicate<Integer, Integer>() {
            @Override
            public boolean test(Integer t1, Integer t2) throws Exception {
                out.add(t2);
                return false;
            }
        });
        
        assertEquals(Arrays.asList(1, 2, 3), out);
    }

    @Test
    public void queueDrainHelperUtility() {
        TestHelper.checkUtilityClass(QueueDrainHelper.class);
    }
}
