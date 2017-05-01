/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the current underlying array size in a volatile field.
 *
 * @param <T> the element type
 * @since 2.0.7
 */
public final class VolatileSizeArrayList<T> extends AtomicInteger implements List<T>, RandomAccess {

    private static final long serialVersionUID = 3972397474470203923L;

    final ArrayList<T> list;

    public VolatileSizeArrayList() {
        list = new ArrayList<T>();
    }

    public VolatileSizeArrayList(int initialCapacity) {
        list = new ArrayList<T>(initialCapacity);
    }

    @Override
    public int size() {
        return get();
    }

    @Override
    public boolean isEmpty() {
        return get() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T e) {
        boolean b = list.add(e);
        lazySet(list.size());
        return b;
    }

    @Override
    public boolean remove(Object o) {
        boolean b = list.remove(o);
        lazySet(list.size());
        return b;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean b = list.addAll(c);
        lazySet(list.size());
        return b;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean b = list.addAll(index, c);
        lazySet(list.size());
        return b;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean b = list.removeAll(c);
        lazySet(list.size());
        return b;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean b = list.retainAll(c);
        lazySet(list.size());
        return b;
    }

    @Override
    public void clear() {
        list.clear();
        lazySet(0);
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        return list.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        list.add(index, element);
        lazySet(list.size());
    }

    @Override
    public T remove(int index) {
        T v = list.remove(index);
        lazySet(list.size());
        return v;
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VolatileSizeArrayList) {
            return list.equals(((VolatileSizeArrayList<?>)obj).list);
        }
        return list.equals(obj);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
