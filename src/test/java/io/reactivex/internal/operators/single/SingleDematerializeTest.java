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

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.subjects.SingleSubject;

public class SingleDematerializeTest {

    @Test
    public void success() {
        Single.just(Notification.createOnNext(1))
        .<Integer>dematerialize()
        .test()
        .assertResult(1);
    }

    @Test
    public void empty() {
        Single.just(Notification.createOnComplete())
        .<Integer>dematerialize()
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Single.error(new TestException())
        .<Integer>dematerialize()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorNotification() {
        Single.just(Notification.createOnError(new TestException()))
        .<Integer>dematerialize()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(new Function<Single<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Single<Object> v) throws Exception {
                return v.dematerialize();
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(SingleSubject.create().dematerialize());
    }

    @Test
    public void wrongType() {
        Single.just(1)
        .<String>dematerialize()
        .test()
        .assertFailure(ClassCastException.class);
    }
}
