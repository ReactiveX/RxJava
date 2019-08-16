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

package io.reactivex.rxjava3.internal.operators.completable;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableResumeNextTest extends RxJavaTest {

    @Test
    public void resumeWithError() {
        Completable.error(new TestException())
        .onErrorResumeNext(Functions.justFunction(Completable.error(new TestException("second"))))
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "second");
    }

    @Test
    public void disposeInMain() {
        TestHelper.checkDisposedCompletable(new Function<Completable, CompletableSource>() {
            @Override
            public CompletableSource apply(Completable c) throws Exception {
                return c.onErrorResumeNext(Functions.justFunction(Completable.complete()));
            }
        });
    }

    @Test
    public void disposeInResume() {
        TestHelper.checkDisposedCompletable(new Function<Completable, CompletableSource>() {
            @Override
            public CompletableSource apply(Completable c) throws Exception {
                return Completable.error(new TestException()).onErrorResumeNext(Functions.justFunction(c));
            }
        });
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(
                Completable.error(new TestException())
                .onErrorResumeNext(Functions.justFunction(Completable.never()))
        );
    }
}
