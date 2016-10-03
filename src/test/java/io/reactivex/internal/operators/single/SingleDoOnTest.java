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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.plugins.RxJavaPlugins;

public class SingleDoOnTest {

    @Test
    public void doOnDispose() {
        final int[] count = { 0 };

        Single.never().doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                count[0]++;
            }
        }).test(true);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnError() {
        final Object[] event = { null };

        Single.error(new TestException()).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                event[0] = e;
            }
        })
        .test();

        assertTrue(event[0].toString(), event[0] instanceof TestException);
    }

    @Test
    public void doOnSubscribe() {
        final int[] count = { 0 };

        Single.never().doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable d) throws Exception {
                count[0]++;
            }
        }).test();

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSuccess() {
        final Object[] event = { null };

        Single.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                event[0] = e;
            }
        })
        .test();

        assertEquals(1, event[0]);
    }

    @Test
    public void doOnSubscribeNormal() {
        final int[] count = { 0 };

        Single.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                count[0]++;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeError() {
        final int[] count = { 0 };

        Single.error(new TestException()).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                count[0]++;
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, count[0]);
    }

    @Test
    public void doOnSubscribeJustCrash() {

        Single.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable s) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSubscribeErrorCrash() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Single.error(new TestException("Outer")).doOnSubscribe(new Consumer<Disposable>() {
                @Override
                public void accept(Disposable s) throws Exception {
                    throw new TestException("Inner");
                }
            })
            .test()
            .assertFailureAndMessage(TestException.class, "Inner");

            TestHelper.assertError(errors, 0, TestException.class, "Outer");
        } finally {
            RxJavaPlugins.reset();
        }

    }
}
