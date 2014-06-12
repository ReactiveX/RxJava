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
package rx.internal.util.jctools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpscArrayQueueTest {

    @Test
    public void testOfferLimit() {
        int size = 1024;
        SpscArrayQueue<Integer> q = new SpscArrayQueue<Integer>(size);
        for (int i = 0; i < size; i++) {
            assertTrue(q.offer(i));
        }
        // this should fail
        assertFalse(q.offer(0));
    }

    @Test
    public void testOfferLimitAfterPoll() {
        int size = 1024;
        SpscArrayQueue<Integer> q = new SpscArrayQueue<Integer>(size);
        for (int i = 0; i < size; i++) {
            assertTrue(q.offer(i));
        }
        //         this should fail
        assertFalse(q.offer(0));
        assertEquals(0, q.poll().intValue());
        assertEquals(1, q.poll().intValue());
        assertTrue(q.offer(1024));
        assertTrue(q.offer(1025));
        // full again so next should fail
        assertFalse(q.offer(1026));
    }
}
