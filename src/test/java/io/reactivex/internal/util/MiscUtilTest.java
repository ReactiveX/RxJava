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
import org.junit.Test;

import io.reactivex.TestHelper;

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
}
