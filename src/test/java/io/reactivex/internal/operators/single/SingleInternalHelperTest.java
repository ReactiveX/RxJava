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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.*;


public class SingleInternalHelperTest {

    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(SingleInternalHelper.class);
    }

    @Test
    public void noSuchElementCallableEnum() {
        assertEquals(1, SingleInternalHelper.NoSuchElementCallable.values().length);
        assertNotNull(SingleInternalHelper.NoSuchElementCallable.valueOf("INSTANCE"));
    }

    @Test
    public void toFlowableEnum() {
        assertEquals(1, SingleInternalHelper.ToFlowable.values().length);
        assertNotNull(SingleInternalHelper.ToFlowable.valueOf("INSTANCE"));
    }

    @Test
    public void toObservableEnum() {
        assertEquals(1, SingleInternalHelper.ToObservable.values().length);
        assertNotNull(SingleInternalHelper.ToObservable.valueOf("INSTANCE"));
    }

    @Test
    public void singleIterableToFlowableIterable() {
        Iterable<? extends Flowable<Integer>> it = SingleInternalHelper.iterableToFlowable(
                Collections.singletonList(Single.just(1)));

        Iterator<? extends Flowable<Integer>> iter = it.iterator();

        if (iter.hasNext()) {
            iter.next().test().assertResult(1);
            if (iter.hasNext()) {
                fail("Iterator reports an additional element");
            }
        } else {
            fail("Iterator was empty");
        }
    }
}
