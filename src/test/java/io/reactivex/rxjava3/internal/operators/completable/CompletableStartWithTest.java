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

import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class CompletableStartWithTest {

    @Test
    public void singleNormal() {
        Completable.complete().startWith(Single.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void singleError() {
        Runnable run = mock(Runnable.class);

        Completable.fromRunnable(run).startWith(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void maybeNormal() {
        Completable.complete().startWith(Maybe.just(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void maybeEmptyNormal() {
        Completable.complete().startWith(Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void maybeError() {
        Runnable run = mock(Runnable.class);

        Completable.fromRunnable(run).startWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }
}
