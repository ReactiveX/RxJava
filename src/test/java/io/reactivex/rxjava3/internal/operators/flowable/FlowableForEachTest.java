/*
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableForEachTest extends RxJavaTest {

    @Test
    public void forEachWile() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5)
        .doOnNext(list::add)
        .forEachWhile(v -> v < 3);

        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void forEachWileWithError() {
        final List<Object> list = new ArrayList<>();

        Flowable.range(1, 5).concatWith(Flowable.<Integer>error(new TestException()))
        .doOnNext(list::add)
        .forEachWhile(v -> true, e -> list.add(100));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 100), list);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(
                Flowable.never()
                .forEachWhile(v -> true)
        );
    }
}
