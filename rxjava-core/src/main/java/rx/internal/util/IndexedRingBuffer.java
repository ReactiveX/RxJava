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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import rx.Subscription;
import rx.functions.Action1;

public class IndexedRingBuffer<E> implements Subscription {

    private static final ObjectPool<IndexedRingBuffer> POOL = new ObjectPool<IndexedRingBuffer>() {

        @Override
        protected IndexedRingBuffer createObject() {
            return new IndexedRingBuffer(2000);
        }

    };

    public final static IndexedRingBuffer getInstance() {
        return POOL.borrowObject();
    }

    private final AtomicReferenceArray array;
    private final int size;

    private static final Object REMOVED_SENTINEL = new Object();

    private volatile int indexHint = 0;

    private IndexedRingBuffer(int size) {
        this.size = size;
        array = new AtomicReferenceArray(size);
    }

    /**
     * Add an element and return the index where it was added to allow removal.
     * 
     * @param e
     * @return
     */
    public int add(E e) {
        // start from hint (it's okay if it has race conditions on it)
        int hint = indexHint;
        for (int i = hint; i < size; i++) {
            Object existing = array.get(i);
            if (existing == null || existing == REMOVED_SENTINEL) {
                if (array.compareAndSet(i, null, e)) {
                    hint = i;
                    return i;
                }
            }
        }
        // start from beginning if we didn't return above
        for (int i = 0; i < hint; i++) {
            Object existing = array.get(i);
            if (existing == null || existing == REMOVED_SENTINEL) {
                if (array.compareAndSet(i, null, e)) {
                    hint = i;
                    return i;
                }
            }
        }
        // we didn't find a place so we're full
        throw new IllegalStateException("No space available");
    }

    public E remove(int index) {
        return (E) array.getAndSet(index, REMOVED_SENTINEL);
    }

    @Override
    public void unsubscribe() {
        for (int i = 0; i < size; i++) {
            Object o = array.getAndSet(i, null);
        }

        POOL.returnObject(this);
    }

    @Override
    public boolean isUnsubscribed() {
        return false;
    }

    public List<Throwable> forEach(Action1<? super E> action) {
        List<Throwable> es = null;

        for (int i = 0; i < size; i++) {
            Object element = array.get(i);
            if (element == null) {
                // end of data
                break;
            }
            if (element != REMOVED_SENTINEL) {
                try {
                    action.call((E) element);
                } catch (Throwable e) {
                    if (es == null) {
                        es = new ArrayList<Throwable>();
                    }
                    es.add(e);
                }
            }
        }

        if (es == null) {
            return Collections.emptyList();
        } else {
            return es;
        }
    }
}
