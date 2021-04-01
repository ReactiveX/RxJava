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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableOnErrorXTest extends RxJavaTest {

    @Test
    public void normalReturn() {
        Completable.complete()
        .onErrorComplete()
        .test()
        .assertResult();
    }

    @Test
    public void normalResumeNext() {
        final int[] call = { 0 };
        Completable.complete()
        .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
            @Override
            public CompletableSource apply(Throwable e) throws Exception {
                call[0]++;
                return Completable.complete();
            }
        })
        .test()
        .assertResult();

        assertEquals(0, call[0]);
    }

    @Test
    public void onErrorReturnConst() {
        Completable.error(new TestException())
        .onErrorReturnItem(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorReturn() {
        Completable.error(new TestException())
        .onErrorReturn(Functions.justFunction(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void onErrorReturnFunctionThrows() {
        TestHelper.assertCompositeExceptions(Completable.error(new TestException())
        .onErrorReturn(new Function<Throwable, Object>() {
            @Override
            public Object apply(Throwable v) throws Exception {
                throw new IOException();
            }
        })
        .to(TestHelper.testConsumer()), TestException.class, IOException.class);
    }

    @Test
    public void onErrorReturnEmpty() {
        Completable.complete()
        .onErrorReturnItem(2)
        .test()
        .assertResult();
    }

    @Test
    public void onErrorReturnDispose() {
        TestHelper.checkDisposed(CompletableSubject.create().onErrorReturnItem(1));
    }

    @Test
    public void onErrorReturnDoubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletableToMaybe(new Function<Completable, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Completable v) throws Exception {
                return v.onErrorReturnItem(1);
            }
        });
    }
}
