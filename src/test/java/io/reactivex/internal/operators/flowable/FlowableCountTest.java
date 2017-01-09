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

package io.reactivex.internal.operators.flowable;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.functions.Function;

public class FlowableCountTest {
    @Test
    public void simpleFlowable() {
        Assert.assertEquals(0, Flowable.empty().count().toFlowable().blockingLast().intValue());

        Assert.assertEquals(1, Flowable.just(1).count().toFlowable().blockingLast().intValue());

        Assert.assertEquals(10, Flowable.range(1, 10).count().toFlowable().blockingLast().intValue());

    }

    @Test
    public void simple() {
        Assert.assertEquals(0, Flowable.empty().count().blockingGet().intValue());

        Assert.assertEquals(1, Flowable.just(1).count().blockingGet().intValue());

        Assert.assertEquals(10, Flowable.range(1, 10).count().blockingGet().intValue());

    }


    @Test
    public void dispose() {
        TestHelper.checkDisposed(Flowable.just(1).count());

        TestHelper.checkDisposed(Flowable.just(1).count().toFlowable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeFlowable(new Function<Flowable<Object>, Flowable<Long>>() {
            @Override
            public Flowable<Long> apply(Flowable<Object> o) throws Exception {
                return o.count().toFlowable();
            }
        });

        TestHelper.checkDoubleOnSubscribeFlowableToSingle(new Function<Flowable<Object>, SingleSource<Long>>() {
            @Override
            public SingleSource<Long> apply(Flowable<Object> o) throws Exception {
                return o.count();
            }
        });
    }

}
