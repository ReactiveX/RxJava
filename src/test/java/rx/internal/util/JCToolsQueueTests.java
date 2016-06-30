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

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.internal.util.atomic.*;
import rx.internal.util.unsafe.*;

public class JCToolsQueueTests {
    static final class IntField {
        int value;
    }
    static void await(CyclicBarrier cb) {
        try {
            cb.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (BrokenBarrierException ex) {
            throw new RuntimeException(ex);
        }
    }
    @Test
    public void casBasedUnsafe() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        long offset = UnsafeAccess.addressOf(IntField.class, "value");
        IntField f = new IntField();
        
        assertTrue(UnsafeAccess.compareAndSwapInt(f, offset, 0, 1));
        assertFalse(UnsafeAccess.compareAndSwapInt(f, offset, 0, 2));
        
        assertEquals(1, UnsafeAccess.getAndAddInt(f, offset, 2));
        
        assertEquals(3, UnsafeAccess.getAndIncrementInt(f, offset));
        
        assertEquals(4, UnsafeAccess.getAndSetInt(f, offset, 0));
    }
    
    @Test
    public void powerOfTwo() {
        assertTrue(Pow2.isPowerOfTwo(1));
        assertTrue(Pow2.isPowerOfTwo(2));
        assertFalse(Pow2.isPowerOfTwo(3));
        assertTrue(Pow2.isPowerOfTwo(4));
        assertFalse(Pow2.isPowerOfTwo(5));
        assertTrue(Pow2.isPowerOfTwo(8));
        assertFalse(Pow2.isPowerOfTwo(13));
        assertTrue(Pow2.isPowerOfTwo(16));
        assertFalse(Pow2.isPowerOfTwo(25));
        assertFalse(Pow2.isPowerOfTwo(31));
        assertTrue(Pow2.isPowerOfTwo(32));
    }
    
    @Test(expected = NullPointerException.class)
    public void testMpmcArrayQueueNull() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        MpmcArrayQueue<Integer> q = new MpmcArrayQueue<Integer>(16);
        q.offer(null);
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testMpmcArrayQueueIterator() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        MpmcArrayQueue<Integer> q = new MpmcArrayQueue<Integer>(16);
        q.iterator();
    }
    
    @Test
    public void testMpmcArrayQueueOfferPoll() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        Queue<Integer> q = new MpmcArrayQueue<Integer>(128);
        
        testOfferPoll(q);
    }
    
    @Test
    public void testMpmcOfferUpToCapacity() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        int n = 128;
        MpmcArrayQueue<Integer> queue = new MpmcArrayQueue<Integer>(n);
        for (int i = 0; i < n; i++) {
            assertTrue(queue.offer(i));
        }
        assertFalse(queue.offer(n));
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testMpscLinkedAtomicQueueIterator() {
        MpscLinkedAtomicQueue<Integer> q = new MpscLinkedAtomicQueue<Integer>();
        q.iterator();
    }
    
    @Test(expected = NullPointerException.class)
    public void testMpscLinkedAtomicQueueNull() {
        MpscLinkedAtomicQueue<Integer> q = new MpscLinkedAtomicQueue<Integer>();
        q.offer(null);
    }
    
    @Test
    public void testMpscLinkedAtomicQueueOfferPoll() {
        MpscLinkedAtomicQueue<Integer> q = new MpscLinkedAtomicQueue<Integer>();
        
        testOfferPoll(q);
    }
    
    @Test(timeout = 2000)
    public void testMpscLinkedAtomicQueuePipelined() throws InterruptedException {
        final MpscLinkedAtomicQueue<Integer> q = new MpscLinkedAtomicQueue<Integer>();
        
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < 1000 * 1000; i++) {
            set.add(i);
        }
        
        final CyclicBarrier cb = new CyclicBarrier(3);
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                await(cb);
                for (int i = 0; i < 500 * 1000; i++) {
                    q.offer(i);
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                await(cb);
                for (int i = 500 * 1000; i < 1000 * 1000; i++) {
                    q.offer(i);
                }
            }
        });
        
        t1.start();
        t2.start();
        
        await(cb);
        
        Integer j;
        for (int i = 0; i < 1000 * 1000; i++) {
            while ((j = q.poll()) == null);
            assertTrue("Value " + j + " already removed", set.remove(j));
        }
        assertTrue("Set is not empty", set.isEmpty());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testMpscLinkedQueueIterator() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        MpscLinkedQueue<Integer> q = new MpscLinkedQueue<Integer>();
        q.iterator();
    }
    
    @Test(expected = NullPointerException.class)
    public void testMpscLinkedQueueNull() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        MpscLinkedQueue<Integer> q = new MpscLinkedQueue<Integer>();
        q.offer(null);
    }
    
    @Test
    public void testMpscLinkedQueueOfferPoll() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        MpscLinkedQueue<Integer> q = new MpscLinkedQueue<Integer>();
        
        testOfferPoll(q);
    }
    @Test(timeout = 2000)
    public void testMpscLinkedQueuePipelined() throws InterruptedException {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        final MpscLinkedQueue<Integer> q = new MpscLinkedQueue<Integer>();
        
        Set<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < 1000 * 1000; i++) {
            set.add(i);
        }
        
        final CyclicBarrier cb = new CyclicBarrier(3);
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                await(cb);
                for (int i = 0; i < 500 * 1000; i++) {
                    q.offer(i);
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                await(cb);
                for (int i = 500 * 1000; i < 1000 * 1000; i++) {
                    q.offer(i);
                }
            }
        });
        
        t1.start();
        t2.start();
        
        await(cb);
        
        Integer j;
        for (int i = 0; i < 1000 * 1000; i++) {
            while ((j = q.poll()) == null);
            assertTrue("Value " + j + " already removed", set.remove(j));
        }
        assertTrue("Set is not empty", set.isEmpty());
    }
    
    protected void testOfferPoll(Queue<Integer> q) {
        for (int i = 0; i < 64; i++) {
            assertTrue(q.offer(i));
        }
        assertFalse(q.isEmpty());
        for (int i = 0; i < 64; i++) {
            assertEquals((Integer)i, q.peek());
            
            assertEquals(64 - i, q.size());
            
            assertEquals((Integer)i, q.poll());
        }
        assertTrue(q.isEmpty());
        
        for (int i = 0; i < 64; i++) {
            assertTrue(q.offer(i));
            assertEquals((Integer)i, q.poll());
        }
        
        assertTrue(q.isEmpty());
        assertNull(q.peek());
        assertNull(q.poll());
    }
    
    @Test(expected = NullPointerException.class)
    public void testSpmcArrayQueueNull() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpmcArrayQueue<Integer> q = new SpmcArrayQueue<Integer>(16);
        q.offer(null);
    }
    
    @Test
    public void testSpmcArrayQueueOfferPoll() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        Queue<Integer> q = new SpmcArrayQueue<Integer>(128);
        
        testOfferPoll(q);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpmcArrayQueueIterator() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpmcArrayQueue<Integer> q = new SpmcArrayQueue<Integer>(16);
        q.iterator();
    }
    
    @Test
    public void testSpmcOfferUpToCapacity() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        int n = 128;
        SpmcArrayQueue<Integer> queue = new SpmcArrayQueue<Integer>(n);
        for (int i = 0; i < n; i++) {
            assertTrue(queue.offer(i));
        }
        assertFalse(queue.offer(n));
    }
    
    @Test(expected = NullPointerException.class)
    public void testSpscArrayQueueNull() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpscArrayQueue<Integer> q = new SpscArrayQueue<Integer>(16);
        q.offer(null);
    }
    
    @Test
    public void testSpscArrayQueueOfferPoll() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        Queue<Integer> q = new SpscArrayQueue<Integer>(128);
        
        testOfferPoll(q);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscArrayQueueIterator() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpscArrayQueue<Integer> q = new SpscArrayQueue<Integer>(16);
        q.iterator();
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscLinkedAtomicQueueIterator() {
        SpscLinkedAtomicQueue<Integer> q = new SpscLinkedAtomicQueue<Integer>();
        q.iterator();
    }
    @Test(expected = NullPointerException.class)
    public void testSpscLinkedAtomicQueueNull() {
        SpscLinkedAtomicQueue<Integer> q = new SpscLinkedAtomicQueue<Integer>();
        q.offer(null);
    }
    
    @Test
    public void testSpscLinkedAtomicQueueOfferPoll() {
        SpscLinkedAtomicQueue<Integer> q = new SpscLinkedAtomicQueue<Integer>();
        
        testOfferPoll(q);
    }
    
    @Test(timeout = 2000)
    public void testSpscLinkedAtomicQueuePipelined() throws InterruptedException {
        final SpscLinkedAtomicQueue<Integer> q = new SpscLinkedAtomicQueue<Integer>();
        final AtomicInteger count = new AtomicInteger();
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Integer j;
                for (int i = 0; i < 1000 * 1000; i++) {
                    while ((j = q.poll()) == null);
                    if (j == i) {
                        count.getAndIncrement();
                    }
                }
            }
        });
        t.start();
        
        for (int i = 0; i < 1000 * 1000; i++) {
            assertTrue(q.offer(i));
        }
        t.join();
        
        assertEquals(1000 * 1000, count.get());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscLinkedQueueIterator() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpscLinkedQueue<Integer> q = new SpscLinkedQueue<Integer>();
        q.iterator();
    }
    
    @Test(expected = NullPointerException.class)
    public void testSpscLinkedQueueNull() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpscLinkedQueue<Integer> q = new SpscLinkedQueue<Integer>();
        q.offer(null);
    }
    
    @Test
    public void testSpscLinkedQueueOfferPoll() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        SpscLinkedQueue<Integer> q = new SpscLinkedQueue<Integer>();
        
        testOfferPoll(q);
    }
    
    @Test(timeout = 2000)
    public void testSpscLinkedQueuePipelined() throws InterruptedException {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        final SpscLinkedQueue<Integer> q = new SpscLinkedQueue<Integer>();
        final AtomicInteger count = new AtomicInteger();
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Integer j;
                for (int i = 0; i < 1000 * 1000; i++) {
                    while ((j = q.poll()) == null);
                    if (j == i) {
                        count.getAndIncrement();
                    }
                }
            }
        });
        t.start();
        
        for (int i = 0; i < 1000 * 1000; i++) {
            assertTrue(q.offer(i));
        }
        t.join();
        
        assertEquals(1000 * 1000, count.get());
    }

    @Test
    public void testSpscOfferUpToCapacity() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        int n = 128;
        SpscArrayQueue<Integer> queue = new SpscArrayQueue<Integer>(n);
        for (int i = 0; i < n; i++) {
            assertTrue(queue.offer(i));
        }
        assertFalse(queue.offer(n));
    }

    @Test(expected = InternalError.class)
    public void testUnsafeAccessAddressOf() {
        if (!UnsafeAccess.isUnsafeAvailable()) {
            return;
        }
        UnsafeAccess.addressOf(Object.class, "field");
    }
    
    @Test
    public void testSpscExactAtomicArrayQueue() {
        for (int i = 1; i <= RxRingBuffer.SIZE * 2; i++) {
            SpscExactAtomicArrayQueue<Integer> q = new SpscExactAtomicArrayQueue<Integer>(i);
            
            for (int j = 0; j < i; j++) {
                assertTrue(q.offer(j));
            }
            
            assertFalse(q.offer(i));
            
            for (int j = 0; j < i; j++) {
                assertEquals((Integer)j, q.peek());
                assertEquals((Integer)j, q.poll());
            }
            
            for (int j = 0; j < RxRingBuffer.SIZE * 4; j++) {
                assertTrue(q.offer(j));
                assertEquals((Integer)j, q.peek());
                assertEquals((Integer)j, q.poll());
            }
        }
    }
    
    @Test
    public void testUnboundedAtomicArrayQueue() {
        for (int i = 1; i <= RxRingBuffer.SIZE * 2; i *= 2) {
            SpscUnboundedAtomicArrayQueue<Integer> q = new SpscUnboundedAtomicArrayQueue<Integer>(i);
            
            for (int j = 0; j < i; j++) {
                assertTrue(q.offer(j));
            }
            
            assertTrue(q.offer(i));
            
            for (int j = 0; j < i; j++) {
                assertEquals((Integer)j, q.peek());
                assertEquals((Integer)j, q.poll());
            }
            
            assertEquals((Integer)i, q.peek());
            assertEquals((Integer)i, q.poll());
            
            for (int j = 0; j < RxRingBuffer.SIZE * 4; j++) {
                assertTrue(q.offer(j));
                assertEquals((Integer)j, q.peek());
                assertEquals((Integer)j, q.poll());
            }
        }
        
    }

    
    @Test(expected = NullPointerException.class)
    public void testSpscAtomicArrayQueueNull() {
        SpscAtomicArrayQueue<Integer> q = new SpscAtomicArrayQueue<Integer>(16);
        q.offer(null);
    }
    
    @Test
    public void testSpscAtomicArrayQueueOfferPoll() {
        Queue<Integer> q = new SpscAtomicArrayQueue<Integer>(128);
        
        testOfferPoll(q);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscAtomicArrayQueueIterator() {
        SpscAtomicArrayQueue<Integer> q = new SpscAtomicArrayQueue<Integer>(16);
        q.iterator();
    }

    @Test(expected = NullPointerException.class)
    public void testSpscExactAtomicArrayQueueNull() {
        SpscExactAtomicArrayQueue<Integer> q = new SpscExactAtomicArrayQueue<Integer>(10);
        q.offer(null);
    }
    
    @Test
    public void testSpscExactAtomicArrayQueueOfferPoll() {
        Queue<Integer> q = new SpscAtomicArrayQueue<Integer>(120);
        
        testOfferPoll(q);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscExactAtomicArrayQueueIterator() {
        SpscAtomicArrayQueue<Integer> q = new SpscAtomicArrayQueue<Integer>(10);
        q.iterator();
    }

    @Test(expected = NullPointerException.class)
    public void testSpscUnboundedAtomicArrayQueueNull() {
        SpscUnboundedAtomicArrayQueue<Integer> q = new SpscUnboundedAtomicArrayQueue<Integer>(16);
        q.offer(null);
    }
    
    @Test
    public void testSpscUnboundedAtomicArrayQueueOfferPoll() {
        Queue<Integer> q = new SpscUnboundedAtomicArrayQueue<Integer>(128);
        
        testOfferPoll(q);
    }
    @Test(expected = UnsupportedOperationException.class)
    public void testSpscUnboundedAtomicArrayQueueIterator() {
        SpscUnboundedAtomicArrayQueue<Integer> q = new SpscUnboundedAtomicArrayQueue<Integer>(16);
        q.iterator();
    }

}
