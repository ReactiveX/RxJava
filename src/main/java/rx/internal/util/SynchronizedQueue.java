/**
 * Copyright 2014 Netflix, Inc.
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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Intended for use when the `sun.misc.Unsafe` implementations can't be used.
 *
 * @param <T>
 */
public class SynchronizedQueue<T> implements Queue<T>, Cloneable {

    private final Queue<T> list = new LinkedList<T>();
    private final int size;

    public SynchronizedQueue() {
        this.size = -1;
    }

    public SynchronizedQueue(int size) {
        this.size = size;
    }

    @Override
    public synchronized boolean isEmpty() { // NOPMD
        return list.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) { // NOPMD
        return list.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() { // NOPMD
        return list.iterator();
    }

    @Override
    public synchronized int size() { // NOPMD
        return list.size();
    }

    @Override
    public synchronized boolean add(T e) { // NOPMD
        return list.add(e);
    }

    @Override
    public synchronized boolean remove(Object o) { // NOPMD
        return list.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) { // NOPMD
        return list.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) { // NOPMD
        return list.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) { // NOPMD
        return list.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) { // NOPMD
        return list.retainAll(c);
    }

    @Override
    public synchronized void clear() { // NOPMD
        list.clear();
    }

    @Override
    public synchronized String toString() { // NOPMD
        return list.toString();
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SynchronizedQueue<?> other = (SynchronizedQueue<?>) obj;
        return list.equals(other.list);
    }

    @Override
    public synchronized T peek() { // NOPMD
        return list.peek();
    }

    @Override
    public synchronized T element() { // NOPMD
        return list.element();
    }

    @Override
    public synchronized T poll() { // NOPMD
        return list.poll();
    }

    @Override
    public synchronized T remove() { // NOPMD
        return list.remove();
    }

    @Override
    public synchronized boolean offer(T e) { // NOPMD
        return !(size > -1 && list.size() + 1 > size) && list.offer(e);
    }

    @Override
    public synchronized Object clone() { // NOPMD
        SynchronizedQueue<T> q = new SynchronizedQueue<T>(size);
        q.addAll(list);
        return q;
    }

    @Override
    public synchronized Object[] toArray() { // NOPMD
        return list.toArray();
    }

    @Override
    public synchronized <R> R[] toArray(R[] a) { // NOPMD
        return list.toArray(a);
    }

}
