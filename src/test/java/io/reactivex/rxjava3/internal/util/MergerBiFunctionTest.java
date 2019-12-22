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

package io.reactivex.rxjava3.internal.util;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;

public class MergerBiFunctionTest extends RxJavaTest {

    @Test
    public void firstEmpty() throws Exception {
        MergerBiFunction<Integer> merger = new MergerBiFunction<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> list = merger.apply(Collections.<Integer>emptyList(), Arrays.asList(3, 5));

        assertEquals(Arrays.asList(3, 5), list);
    }

    @Test
    public void bothEmpty() throws Exception {
        MergerBiFunction<Integer> merger = new MergerBiFunction<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> list = merger.apply(Collections.<Integer>emptyList(), Collections.<Integer>emptyList());

        assertEquals(Collections.<Integer>emptyList(), list);
    }

    @Test
    public void secondEmpty() throws Exception {
        MergerBiFunction<Integer> merger = new MergerBiFunction<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> list = merger.apply(Arrays.asList(2, 4), Collections.<Integer>emptyList());

        assertEquals(Arrays.asList(2, 4), list);
    }

    @Test
    public void sameSize() throws Exception {
        MergerBiFunction<Integer> merger = new MergerBiFunction<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> list = merger.apply(Arrays.asList(2, 4), Arrays.asList(3, 5));

        assertEquals(Arrays.asList(2, 3, 4, 5), list);
    }

    @Test
    public void sameSizeReverse() throws Exception {
        MergerBiFunction<Integer> merger = new MergerBiFunction<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        List<Integer> list = merger.apply(Arrays.asList(3, 5), Arrays.asList(2, 4));

        assertEquals(Arrays.asList(2, 3, 4, 5), list);
    }
}
