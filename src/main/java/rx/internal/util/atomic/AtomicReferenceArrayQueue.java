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
 * Original location: https://github.com/JCTools/JCTools/blob/master/jctools-core/src/main/java/org/jctools/queues/atomic/AtomicReferenceArrayQueue.java
 */
package rx.internal.util.atomic;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

import rx.internal.util.unsafe.Pow2;

abstract class AtomicReferenceArrayQueue<E> extends AbstractQueue<E> {
    protected final AtomicReferenceArray<E> buffer;
    protected final int mask;
    public AtomicReferenceArrayQueue(int capacity) {
        int actualCapacity = Pow2.roundToPowerOfTwo(capacity);
        this.mask = actualCapacity - 1;
        this.buffer = new AtomicReferenceArray<E>(actualCapacity);
    }
    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void clear() {
        // we have to test isEmpty because of the weaker poll() guarantee
        while (poll() != null || !isEmpty())
            ;
    }
    protected final int calcElementOffset(long index, int mask) {
        return (int)index & mask;
    }
    protected final int calcElementOffset(long index) {
        return (int)index & mask;
    }
    protected final E lvElement(AtomicReferenceArray<E> buffer, int offset) {
        return buffer.get(offset);
    }
    protected final E lpElement(AtomicReferenceArray<E> buffer, int offset) {
        return buffer.get(offset); // no weaker form available
    }
    protected final E lpElement(int offset) {
        return buffer.get(offset); // no weaker form available
    }
    protected final void spElement(AtomicReferenceArray<E> buffer, int offset, E value) {
        buffer.lazySet(offset, value);  // no weaker form available
    }
    protected final void spElement(int offset, E value) {
        buffer.lazySet(offset, value);  // no weaker form available
    }
    protected final void soElement(AtomicReferenceArray<E> buffer, int offset, E value) {
        buffer.lazySet(offset, value);
    }
    protected final void soElement(int offset, E value) {
        buffer.lazySet(offset, value);
    }
    protected final void svElement(AtomicReferenceArray<E> buffer, int offset, E value) {
        buffer.set(offset, value);
    }
    protected final E lvElement(int offset) {
        return lvElement(buffer, offset);
    }
}
