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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class ObservableStartWithTest {

    @Test
    public void justCompletableComplete() {
        Observable.just(1).startWith(Completable.complete())
        .test()
        .assertResult(1);
    }

    @Test
    public void emptyCompletableComplete() {
        Observable.empty().startWith(Completable.complete())
        .test()
        .assertResult();
    }

    @Test
    public void runCompletableError() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run).startWith(Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justSingleJust() {
        Observable.just(1).startWith(Single.just(2))
        .test()
        .assertResult(2, 1);
    }

    @Test
    public void emptySingleJust() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run)
        .startWith(Single.just(2))
        .test()
        .assertResult(2);

        verify(run).run();
    }

    @Test
    public void runSingleError() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run).startWith(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justMaybeJust() {
        Observable.just(1).startWith(Maybe.just(2))
        .test()
        .assertResult(2, 1);
    }

    @Test
    public void emptyMaybeJust() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run)
        .startWith(Maybe.just(2))
        .test()
        .assertResult(2);

        verify(run).run();
    }

    @Test
    public void runMaybeError() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run).startWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }

    @Test
    public void justObservableJust() {
        Observable.just(1).startWith(Observable.just(2, 3, 4, 5))
        .test()
        .assertResult(2, 3, 4, 5, 1);
    }

    @Test
    public void emptyObservableJust() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run)
        .startWith(Observable.just(2, 3, 4, 5))
        .test()
        .assertResult(2, 3, 4, 5);

        verify(run).run();
    }

    @Test
    public void emptyObservableEmpty() {
        Runnable run = mock(Runnable.class);
        Runnable run2 = mock(Runnable.class);

        Observable.fromRunnable(run)
        .startWith(Observable.fromRunnable(run2))
        .test()
        .assertResult();

        verify(run).run();
        verify(run2).run();
    }

    @Test
    public void runObservableError() {
        Runnable run = mock(Runnable.class);

        Observable.fromRunnable(run).startWith(Observable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        verify(run, never()).run();
    }
}
