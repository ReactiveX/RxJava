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

package rx.internal.util;

import java.util.concurrent.atomic.AtomicReferenceArray;

import rx.internal.util.unsafe.Pow2;

/**
 * A single-producer single-consumer circular array with resize capability.
 */
public class AtomicArrayQueue {
    static final Object TOMBSTONE = new Object();
    volatile AtomicReferenceArray<Object> buffer;
    long readerIndex;
    long writerIndex;
    final int maxCapacity;
    public AtomicArrayQueue() {
        this(8, Integer.MAX_VALUE);
    }
    public AtomicArrayQueue(int initialCapacity) {
        this(16, Integer.MAX_VALUE);
    }
    public AtomicArrayQueue(int initialCapacity, int maxCapacity) {
        int c0;
        if (initialCapacity >= 1 << 30) {
            c0 = 1 << 30;
        } else {
            c0 = Pow2.roundToPowerOfTwo(initialCapacity);
        }
        int cm;
        if (maxCapacity >= 1 << 30) {
            cm = 1 << 30;
        } else {
            cm = Pow2.roundToPowerOfTwo(maxCapacity);
        }
        this.maxCapacity = cm;
        
        buffer = new AtomicReferenceArray<Object>(c0);
    }
    
    AtomicReferenceArray<Object> grow() {
        long wi = writerIndex;

        AtomicReferenceArray<Object> b = buffer;
        
        int n = b.length() - 1;
        int wo = offset(wi, n);
        
        int n2 = n * 2 + 2;
        AtomicReferenceArray<Object> b2 = new AtomicReferenceArray<Object>(n2);

        // move the front to the back
        boolean caughtUp = false;
        for (int i = wo - 1; i >= 0; i--) {
            Object o = b.getAndSet(i, TOMBSTONE);
            if (o == null) {
                caughtUp = true;
                break;
            }
            b2.lazySet(i + n + 1, o);
        }
        // leave everything beyont the wrap point at its location
        if (!caughtUp) {
            for (int i = n; i >= wo; i--) {
                Object o = b.getAndSet(i, TOMBSTONE);
                if (o == null) {
                    break;
                }
                b2.lazySet(i, o);
            }
        }
        
        buffer = b2;
        
        return b2;
    }
    
    public boolean offer(Object o) {
        AtomicReferenceArray<Object> b = buffer;
        long wi = writerIndex;
        
        int n = b.length() - 1;
        int wo = offset(wi, n);
        if (b.get(wo) != null) {
            if (n + 1 == maxCapacity) {
                return false;
            }
            // grow
            b = grow();
            wo = offset(wi, n * 2 + 1);
        }
        b.lazySet(wo, o);

        writerIndex = wi + 1;

        return true;
    }
    public Object poll() {
        long ri = readerIndex;
        for (;;) {
            AtomicReferenceArray<Object> b = buffer;
            
            int ro = offset(ri, b.length() - 1);
            Object o = b.getAndSet(ro, null);
            if (o == TOMBSTONE) {
                // the offer is in copy, try again with a fresh buffer
                continue;
            } else
            if (o == null) {
                return null;
            }
            readerIndex = ri + 1;
            return o;
        }
    }
    int offset(long index, int mask) {
        return (int)index & mask;
    }
}
