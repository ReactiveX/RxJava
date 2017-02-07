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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class SingleDoFinallyTest implements Action {

    int calls;

    @Override
    public void run() throws Exception {
        calls++;
    }

    @Test
    public void normalJust() {
        Single.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalError() {
        Single.error(new TestException())
        .doFinally(this)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingle(new Function<Single<Object>, Single<Object>>() {
            @Override
            public Single<Object> apply(Single<Object> f) throws Exception {
                return f.doFinally(SingleDoFinallyTest.this);
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void nullAction() {
        Single.just(1).doFinally(null);
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Single.just(1)
            .doFinally(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertResult(1)
            .cancel();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(PublishSubject.create().singleOrError().doFinally(this));
    }
}
