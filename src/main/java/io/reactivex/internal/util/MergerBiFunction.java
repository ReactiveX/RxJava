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

import io.reactivex.functions.BiFunction;

/**
 * A BiFunction that merges two Lists into a new list.
 * @param <T> the value type
 */
public final class MergerBiFunction<T> implements BiFunction<List<T>, List<T>, List<T>> {

    final Comparator<? super T> comparator;

    public MergerBiFunction(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    @Override
    public List<T> apply(List<T> a, List<T> b) throws Exception {
        int n = a.size() + b.size();
        if (n == 0) {
            return new ArrayList<T>();
        }
        List<T> both = new ArrayList<T>(n);

        Iterator<T> at = a.iterator();
        Iterator<T> bt = b.iterator();

        T s1 = at.hasNext() ? at.next() : null;
        T s2 = bt.hasNext() ? bt.next() : null;

        while (s1 != null && s2 != null) {
            if (comparator.compare(s1, s2) < 0) { // s1 comes before s2
                both.add(s1);
                s1 = at.hasNext() ? at.next() : null;
            } else {
                both.add(s2);
                s2 = bt.hasNext() ? bt.next() : null;
            }
        }

        if (s1 != null) {
            both.add(s1);
            while (at.hasNext()) {
                both.add(at.next());
            }
        } else {
            both.add(s2);
            while (bt.hasNext()) {
                both.add(bt.next());
            }
        }

        return both;
    }
}
