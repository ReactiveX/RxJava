/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.nbp;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.functions.*;

public class NbpObservableWindowTests {

    @Test
    public void testWindow() {
        final ArrayList<List<Integer>> lists = new ArrayList<>();

        NbpObservable.concat(
            NbpObservable.just(1, 2, 3, 4, 5, 6)
            .window(3)
            .map(new Function<NbpObservable<Integer>, NbpObservable<List<Integer>>>() {
                @Override
                public NbpObservable<List<Integer>> apply(NbpObservable<Integer> xs) {
                    return xs.toList();
                }
            })
        )
        .toBlocking().forEach(new Consumer<List<Integer>>() {
            @Override
            public void accept(List<Integer> xs) {
                lists.add(xs);
            }
        });

        assertArrayEquals(lists.get(0).toArray(new Integer[3]), new Integer[] { 1, 2, 3 });
        assertArrayEquals(lists.get(1).toArray(new Integer[3]), new Integer[] { 4, 5, 6 });
        assertEquals(2, lists.size());

    }
}