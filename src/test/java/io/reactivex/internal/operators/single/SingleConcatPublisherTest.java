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

import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.*;

public class SingleConcatPublisherTest {

    @Test
    public void scalar() {
        Single.concat(Flowable.just(Single.just(1)))
        .test()
        .assertResult(1);
    }

    @Test
    public void callable() {
        Single.concat(Flowable.fromCallable(new Callable<Single<Integer>>() {
            @Override
            public Single<Integer> call() throws Exception {
                return Single.just(1);
            }
        }))
        .test()
        .assertResult(1);
    }
}
