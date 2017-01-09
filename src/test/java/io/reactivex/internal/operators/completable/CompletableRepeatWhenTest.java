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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.*;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.functions.*;

public class CompletableRepeatWhenTest {
    @Test
    public void whenCounted() {

        final int[] counter = { 0 };

        Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                counter[0]++;
            }
        })
        .repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> f) throws Exception {
                final int[] j = { 3 };
                return f.takeWhile(new Predicate<Object>() {
                    @Override
                    public boolean test(Object v) throws Exception {
                        return j[0]-- != 0;
                    }
                });
            }
        })
        .subscribe();

        assertEquals(4, counter[0]);
    }
}
