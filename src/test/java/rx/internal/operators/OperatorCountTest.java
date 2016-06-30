/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import org.junit.*;

import rx.Observable;

public class OperatorCountTest {

    @Test
    public void simple() {
        Assert.assertEquals(0, Observable.empty().count().toBlocking().last().intValue());
        Assert.assertEquals(0L, Observable.empty().countLong().toBlocking().last().intValue());

        Assert.assertEquals(1, Observable.just(1).count().toBlocking().last().intValue());
        Assert.assertEquals(1L, Observable.just(1).countLong().toBlocking().last().intValue());

        Assert.assertEquals(10, Observable.range(1, 10).count().toBlocking().last().intValue());
        Assert.assertEquals(10L, Observable.range(1, 10).countLong().toBlocking().last().intValue());

    }
}
