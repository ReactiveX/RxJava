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

/*
 * The code was inspired by the similarly named JCTools class:
 * https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic
 */

package io.reactivex.internal.queue;

import static org.junit.Assert.*;
import org.junit.Test;

public class SimpleQueueTest {

    @Test(expected = NullPointerException.class)
    public void spscArrayQueueNull() {
        SpscArrayQueue<Object> q = new SpscArrayQueue<Object>(16);
        q.offer(null);
    }

    @Test(expected = NullPointerException.class)
    public void spscLinkedArrayQueueNull() {
        SpscLinkedArrayQueue<Object> q = new SpscLinkedArrayQueue<Object>(16);
        q.offer(null);
    }

    @Test(expected = NullPointerException.class)
    public void mpscLinkedQueueNull() {
        MpscLinkedQueue<Object> q = new MpscLinkedQueue<Object>();
        q.offer(null);
    }

    @Test
    public void spscArrayQueueBiOffer() {
        SpscArrayQueue<Object> q = new SpscArrayQueue<Object>(16);
        q.offer(1, 2);

        assertEquals(1, q.poll());
        assertEquals(2, q.poll());
        assertNull(q.poll());
    }

    @Test
    public void spscLinkedArrayQueueBiOffer() {
        SpscLinkedArrayQueue<Object> q = new SpscLinkedArrayQueue<Object>(16);
        q.offer(1, 2);

        assertEquals(1, q.poll());
        assertEquals(2, q.poll());
        assertNull(q.poll());
    }

    @Test
    public void mpscLinkedQueueBiOffer() {
        MpscLinkedQueue<Object> q = new MpscLinkedQueue<Object>();
        q.offer(1, 2);

        assertEquals(1, q.poll());
        assertEquals(2, q.poll());
        assertNull(q.poll());
    }

}
