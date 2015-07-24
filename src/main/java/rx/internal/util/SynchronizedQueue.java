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
public class SynchronizedQueue<T> implements Queue<T> {

    private final LinkedList<T> list = new LinkedList<T>();
    private final int size;

    public SynchronizedQueue() {
        this.size = -1;
    }

    public SynchronizedQueue(int size) {
        this.size = size;
    }

    @Override
    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public synchronized Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public synchronized int size() {
        return list.size();
    }

    @Override
    public synchronized boolean add(T e) {
        return list.add(e);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        return list.addAll(c);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public synchronized void clear() {
        list.clear();
    }

    @Override
    public synchronized String toString() {
        return list.toString();
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SynchronizedQueue<?> other = (SynchronizedQueue<?>) obj;
        if (list == null) {
            if (other.list != null)
                return false;
        } else if (!list.equals(other.list))
            return false;
        return true;
    }

    @Override
    public synchronized T peek() {
        return list.peek();
    }

    @Override
    public synchronized T element() {
        return list.element();
    }

    @Override
    public synchronized T poll() {
        return list.poll();
    }

    @Override
    public synchronized T remove() {
        return list.remove();
    }

    @Override
    public synchronized boolean offer(T e) {
        if (size > -1 && list.size() + 1 > size) {
            return false;
        }
        return list.offer(e);
    }

    @Override
    public synchronized Object clone() {
        SynchronizedQueue<T> q = new SynchronizedQueue<T>(size);
        q.addAll(list);
        return q;
    }

    @Override
    public synchronized Object[] toArray() {
        return list.toArray();
    }

    @Override
    public synchronized <R> R[] toArray(R[] a) {
        return list.toArray(a);
    }

}
