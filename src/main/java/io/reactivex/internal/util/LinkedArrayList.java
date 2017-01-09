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

/**
 * A list implementation which combines an ArrayList with a LinkedList to
 * avoid copying values when the capacity needs to be increased.
 * <p>
 * The class is non final to allow embedding it directly and thus saving on object allocation.
 */
public class LinkedArrayList {
    /** The capacity of each array segment. */
    final int capacityHint;
    /**
     * Contains the head of the linked array list if not null. The
     * length is always capacityHint + 1 and the last element is an Object[] pointing
     * to the next element of the linked array list.
     */
    Object[] head;
    /** The tail array where new elements will be added. */
    Object[] tail;
    /**
     * The total size of the list; written after elements have been added (release) and
     * and when read, the value indicates how many elements can be safely read (acquire).
     */
    volatile int size;
    /** The next available slot in the current tail. */
    int indexInTail;
    /**
     * Constructor with the capacity hint of each array segment.
     * @param capacityHint the expected number of elements to hold (can grow beyond that)
     */
    public LinkedArrayList(int capacityHint) {
        this.capacityHint = capacityHint;
    }
    /**
     * Adds a new element to this list.
     * @param o the object to add, nulls are accepted
     */
    public void add(Object o) {
        // if no value yet, create the first array
        if (size == 0) {
            head = new Object[capacityHint + 1];
            tail = head;
            head[0] = o;
            indexInTail = 1;
            size = 1;
        } else
        // if the tail is full, create a new tail and link
        if (indexInTail == capacityHint) {
            Object[] t = new Object[capacityHint + 1];
            t[0] = o;
            tail[capacityHint] = t;
            tail = t;
            indexInTail = 1;
            size++;
        } else {
            tail[indexInTail] = o;
            indexInTail++;
            size++;
        }
    }
    /**
     * Returns the head buffer segment or null if the list is empty.
     * @return the head object array
     */
    public Object[] head() {
        return head; // NOPMD
    }

    /**
     * Returns the total size of the list.
     * @return the total size of the list
     */
    public int size() {
        return size;
    }
    @Override
    public String toString() {
        final int cap = capacityHint;
        final int s = size;
        final List<Object> list = new ArrayList<Object>(s + 1);

        Object[] h = head();
        int j = 0;
        int k = 0;
        while (j < s) {
            list.add(h[k]);
            j++;
            if (++k == cap) {
                k = 0;
                h = (Object[])h[cap];
            }
        }

        return list.toString();
    }
}
