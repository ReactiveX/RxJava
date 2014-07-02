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

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized boolean contains(Object o) {
        return list.contains(o);
    }

    public synchronized Iterator<T> iterator() {
        return list.iterator();
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized boolean add(T e) {
        return list.add(e);
    }

    public synchronized boolean remove(Object o) {
        return list.remove(o);
    }

    public synchronized boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    public synchronized boolean addAll(Collection<? extends T> c) {
        return list.addAll(c);
    }

    public synchronized boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    public synchronized boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    public synchronized void clear() {
        list.clear();
    }

    public synchronized String toString() {
        return list.toString();
    }

    public synchronized boolean equals(Object o) {
        return list.equals(o);
    }

    public synchronized int hashCode() {
        return list.hashCode();
    }

    public synchronized T peek() {
        return list.peek();
    }

    public synchronized T element() {
        return list.element();
    }

    public synchronized T poll() {
        return list.poll();
    }

    public synchronized T remove() {
        return list.remove();
    }

    public synchronized boolean offer(T e) {
        if (size > -1 && list.size() + 1 > size) {
            return false;
        }
        return list.offer(e);
    }

    public synchronized Object clone() {
        return list.clone();
    }

    public synchronized Object[] toArray() {
        return list.toArray();
    }

    public synchronized <R> R[] toArray(R[] a) {
        return list.toArray(a);
    }

}
