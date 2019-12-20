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

package io.reactivex.rxjava3.internal.jdk8;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleMapOptionalTest extends RxJavaTest {

    @Test
    public void successSuccess() {
        Single.just(1)
        .mapOptional(Optional::of)
        .test()
        .assertResult(1);
    }

    @Test
    public void successEmpty() {
        Single.just(1)
        .mapOptional(v -> Optional.empty())
        .test()
        .assertResult();
    }

    @Test
    public void error() throws Throwable {
        @SuppressWarnings("unchecked")
        Function<? super Integer, Optional<? extends Integer>> f = mock(Function.class);

        Single.<Integer>error(new TestException())
        .mapOptional(f)
        .test()
        .assertFailure(TestException.class);

        verify(f, never()).apply(any());
    }

    @Test
    public void mapperCrash() {
        Single.just(1)
        .mapOptional(v -> { throw new TestException(); })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.never().mapOptional(Optional::of));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToMaybe(m -> m.mapOptional(Optional::of));
    }
}
