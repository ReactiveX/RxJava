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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class FlowableStartWithTest {

    @Test
    public void justCompletableComplete() {
        Flowable.just(1).startWith(Completable.complete())
        .test()
        .assertResult(1);
    }

    @Test
    public void emptyCompletableComplete() {
        Flowable.empty().startWith(Completable.complete())
        .test()
        .assertResult();
    }

    @Test
    public void runCompletableError() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run).startWith(Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justSingleJust() {
        Flowable.just(1).startWith(Single.just(2))
        .test()
        .assertResult(2, 1);
    }

    @Test
    public void emptySingleJust() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run)
        .startWith(Single.just(2))
        .test()
        .assertResult(2);

        verify(run).run();
    }

    @Test
    public void runSingleError() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run).startWith(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justMaybeJust() {
        Flowable.just(1).startWith(Maybe.just(2))
        .test()
        .assertResult(2, 1);
    }

    @Test
    public void emptyMaybeJust() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run)
        .startWith(Maybe.just(2))
        .test()
        .assertResult(2);

        verify(run).run();
    }

    @Test
    public void runMaybeError() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run).startWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justFlowableJust() {
        Flowable.just(1).startWith(Flowable.just(2, 3, 4, 5))
        .test()
        .assertResult(2, 3, 4, 5, 1);
    }

    @Test
    public void emptyFlowableJust() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run)
        .startWith(Flowable.just(2, 3, 4, 5))
        .test()
        .assertResult(2, 3, 4, 5);

        verify(run).run();
    }

    @Test
    public void emptyFlowableEmpty() {
        Runnable run = mock(Runnable.class);
        Runnable run2 = mock(Runnable.class);

        Flowable.fromRunnable(run)
        .startWith(Flowable.fromRunnable(run2))
        .test()
        .assertResult();

        verify(run).run();
        verify(run2).run();
    }

    @Test
    public void runFlowableError() {
        Runnable run = mock(Runnable.class);

        Flowable.fromRunnable(run).startWith(Flowable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }
}
