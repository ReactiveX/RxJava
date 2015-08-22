/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackpressureUtilsTest {
    @Test
    public void testAddCap() {
        assertEquals(2L, BackpressureUtils.addCap(1, 1));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.addCap(1, Long.MAX_VALUE - 1));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.addCap(1, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.addCap(Long.MAX_VALUE - 1, Long.MAX_VALUE - 1));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.addCap(Long.MAX_VALUE, Long.MAX_VALUE));
    }
    
    @Test
    public void testMultiplyCap() {
        assertEquals(6, BackpressureUtils.multiplyCap(2, 3));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.multiplyCap(2, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.multiplyCap(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, BackpressureUtils.multiplyCap(1L << 32, 1L << 32));

    }
}
