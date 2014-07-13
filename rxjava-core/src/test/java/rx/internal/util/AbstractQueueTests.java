/*
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
 *
 * Original License: https://github.com/JCTools/JCTools/blob/master/LICENSE
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/test/java/org/jctools/queues/QueueSanityTest.java
 */
package rx.internal.util;

import org.junit.Test;

import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractQueueTests {
    protected static final int SIZE = 8192 * 2;

    /**
     * The queue to test
     */
    protected abstract Queue<Integer> getQueue();

    @Test
    public void sanity() {
        final Queue<Integer> q = getQueue();

        for (int i = 0; i < SIZE; i++) {
            assertNull(q.poll());
            assertEquals(0, q.size());
        }
        int i = 0;
        while (i < SIZE && q.offer(i))
            i++;
        int size = i;
        assertEquals(size, q.size());

        // expect FIFO
        i = 0;
        Integer p;
        Integer e;
        while ((p = q.peek()) != null) {
            e = q.poll();
            assertEquals(p, e);
            assertEquals(size - (i + 1), q.size());
            assertEquals(e.intValue(), i++);
        }
        assertEquals(size, i);
    }

    @Test
    public void shortSanity() {
        final Queue<Integer> q = getQueue();

        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
        Integer e = 1;
        q.offer(e);
        assertEquals(1, q.size());
        assertTrue(!q.isEmpty());
        Integer oh = q.poll();
        assertEquals(e, oh);
        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
    }
}
