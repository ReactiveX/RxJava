/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.util;

import static org.junit.Assert.assertTrue;

import java.util.*;

import org.junit.*;

public class SynchronizedQueueTest {
    SynchronizedQueue<Object> q = new SynchronizedQueue<Object>();

    @Test
    public void testEquals() {

         assertTrue(q.equals(q));
    }

    @Test
    public void contains() {
        q.offer(1);
        Assert.assertTrue(q.add(2));

        Assert.assertEquals(2, q.size());
        Assert.assertTrue(q.contains(1));
        Assert.assertTrue(q.contains(2));
        Assert.assertFalse(q.contains(3));
    }

    @Test
    public void iterator() {
        q.offer(1);

        Assert.assertEquals(1, q.iterator().next());
    }

    @Test
    public void remove() {
        q.offer(1);
        q.offer(2);
        q.offer(3);

        Assert.assertTrue(q.remove(2));
        Assert.assertFalse(q.remove(2));
    }

    @Test
    public void addAllContainsAll() {
        q.addAll(Arrays.asList(1, 2, 3, 4));

        q.removeAll(Arrays.asList(2, 3));

        Assert.assertEquals(2, q.size());
        Assert.assertTrue(q.contains(1));
        Assert.assertFalse(q.contains(2));
        Assert.assertFalse(q.contains(3));
        Assert.assertTrue(q.contains(4));

        Assert.assertTrue(q.containsAll(Arrays.asList(1, 4)));
        Assert.assertFalse(q.containsAll(Arrays.asList(2, 3)));
    }

    @Test
    public void retainAll() {
        q.addAll(Arrays.asList(1, 2, 3, 4));

        q.retainAll(Arrays.asList(2, 3));

        Assert.assertEquals(2, q.size());
        Assert.assertFalse(q.contains(1));
        Assert.assertTrue(q.contains(2));
        Assert.assertTrue(q.contains(3));
        Assert.assertFalse(q.contains(4));
    }

    @Test
    public void clear() {
        q.addAll(Arrays.asList(1, 2, 3, 4));

        q.clear();

        Assert.assertEquals(0, q.size());
        Assert.assertTrue(q.isEmpty());
    }

    @Test
    public void toStringValue() {
        q.offer(1);

        Assert.assertEquals("[1]", q.toString());
    }

    @Test
    public void equalsTo() {
        q.offer(1);

        SynchronizedQueue<Integer> q2 = new SynchronizedQueue<Integer>();
        q2.offer(1);

        SynchronizedQueue<Integer> q3 = new SynchronizedQueue<Integer>();
        q3.offer(2);

        Assert.assertEquals(q, q2);
        Assert.assertEquals(q.hashCode(), q2.hashCode());
        Assert.assertNotEquals(q, q3);

        Assert.assertFalse(q.equals(null));
        Assert.assertFalse(q.equals(1));

        Assert.assertEquals(q, q.clone());

    }

    @Test
    public void toArray() {
        q.offer(1);

        Object[] a = q.toArray();

        Object[] b = q.toArray(new Integer[1]);

        Assert.assertEquals(1, a[0]);
        Assert.assertEquals(1, b[0]);
    }

    @Test
    public void peekElement() {
        q.offer(1);

        Assert.assertEquals(1, q.peek());
        Assert.assertEquals(1, q.element());
        Assert.assertEquals(1, q.remove());
        try {
            q.element();
            Assert.fail("Failed to throw on empty queue");
        } catch (NoSuchElementException ex) {
            // expected
        }
    }
}
