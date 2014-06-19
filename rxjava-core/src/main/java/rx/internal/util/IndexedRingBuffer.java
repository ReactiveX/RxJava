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
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicInteger index = new AtomicInteger();
    private final int size;
    
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
        int i = index.getAndIncrement();
        if (i < size) {
            array.set(i, e);
            return i;
        } else {
            // we didn't find a place so we're full
            throw new IllegalStateException("No space available");
            // TODO resize/defrag the array rather than throwing an error
        }
    }

    public E remove(int index) {
        return (E) array.getAndSet(index, null);
    }

    @Override
    public void unsubscribe() {
        index.set(0);
        POOL.returnObject(this);
    }

    @Override
    public boolean isUnsubscribed() {
        return false;
    }

    public List<Throwable> forEach(Action1<? super E> action) {
        List<Throwable> es = null;

        for (int i = 0; i < index.get(); i++) {
            Object element = array.get(i);
            if (element == null) {
                continue;
            }
            try {
                action.call((E) element);
            } catch (Throwable e) {
                if (es == null) {
                    es = new ArrayList<Throwable>();
                }
                es.add(e);
            }
        }

        if (es == null) {
            return Collections.emptyList();
        } else {
            return es;
        }
    }
}
