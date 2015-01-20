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

/**
 * A simplified ArrayList with exposed internal array and size fields.
 */
public class SimpleArrayList {
    public Object[] array;
    public int size;

    public void add(Object o) {
        int s = size;
        Object[] a = array;
        if (a == null) {
            a = new Object[16];
            array = a;
        } else if (s == a.length) {
            Object[] array2 = new Object[s + (s >> 2)];
            System.arraycopy(a, 0, array2, 0, s);
            a = array2;
            array = a;
        }
        a[s] = o;
        size = s + 1;
    }
    /**
     * Adds the elements of the other list to the front of this list.
     * @param other
     */
    public void addFirst(SimpleArrayList other, int start) {
        int s = size;
        int os = other.size - start;
        int news = s + os;
        Object[] a = array;
        if (a == null || news >= a.length) {
            Object[] array2 = new Object[Math.max(16, news + (s >> 2))];
            System.arraycopy(other.array, start, array2, 0, os);
            if (a != null) {
                System.arraycopy(a, 0, array2, os, s);
            }
            array = array2;
            size = news;
        } else {
            System.arraycopy(a, 0, a, os, s);
            System.arraycopy(other.array, start, a, 0, os);
            size = news;
        }
    }
}
