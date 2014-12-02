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

import rx.internal.util.unsafe.Pow2;

/**
 * A simple synchronized, unbounded array queue.
 */
public class SyncArrayQueue {
    Object[] buffer;
    int mask;
    long writeIndex;
    long readIndex;
    
    public SyncArrayQueue() {
        this(8);
    }
    public SyncArrayQueue(int capacityHint) {
        int m = Pow2.roundToPowerOfTwo(capacityHint) - 1;
        mask = m;
        buffer = new Object[m + 1];
    }
    
    Object[] grow() {
        int m = mask;
        Object[] src = buffer;
        long ri = readIndex;

        Object[] b = new Object[m * 2 + 2];
        int readOffset = offset(ri, m);

        int rem = m + 1 - readOffset;
        System.arraycopy(src, readOffset, b, 0, rem);
        System.arraycopy(src, 0, b, rem, readOffset);
        
        readIndex = 0;
        writeIndex = m + 1;
        mask = m * 2 + 1;
        buffer = b;
        return b;
    }
    int offset(long index, int m) {
        return (int)index & m;
    }
    
    public synchronized boolean offer(Object o) {
        if (o == null) {
            throw new NullPointerException("Null's not supported as values.");
        }
        Object[] b = buffer;
        int m = mask;
        long wi = writeIndex;
        
        int writeOffset = offset(wi, m);
        if (b[writeOffset] != null) {
            b = grow();
            writeOffset = m + 1;
            wi = m + 1;
        }
        b[writeOffset] = o;
        writeIndex = wi + 1;
        return true;
    }
    
    public synchronized Object poll() {
        int m = mask;
        Object[] b = buffer;
        long ri = readIndex;
        int readOffset = offset(ri, m);
        Object o = b[readOffset];
        if (o == null) {
            return null;
        }
        b[readOffset] = null;
        readIndex = ri + 1;
        return o;
    }

}
