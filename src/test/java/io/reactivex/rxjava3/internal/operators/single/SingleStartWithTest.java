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

package io.reactivex.rxjava3.internal.operators.single;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class SingleStartWithTest {

    @Test
    public void justCompletableComplete() {
        Single.just(1)
        .startWith(Completable.complete())
        .test()
        .assertResult(1);
    }

    @Test
    public void justCompletableError() {
        Single.just(1)
        .startWith(Completable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void justSingleJust() {
        Single.just(1)
        .startWith(Single.just(0))
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void justSingleError() {
        Single.just(1)
        .startWith(Single.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void justMaybeJust() {
        Single.just(1)
        .startWith(Maybe.just(0))
        .test()
        .assertResult(0, 1);
    }

    @Test
    public void justMaybeEmpty() {
        Single.just(1)
        .startWith(Maybe.empty())
        .test()
        .assertResult(1);
    }

    @Test
    public void justMaybeError() {
        Single.just(1)
        .startWith(Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void justObservableJust() {
        Single.just(1)
        .startWith(Observable.just(-1, 0))
        .test()
        .assertResult(-1, 0, 1);
    }

    @Test
    public void justObservableEmpty() {
        Single.just(1)
        .startWith(Observable.empty())
        .test()
        .assertResult(1);
    }

    @Test
    public void justObservableError() {
        Single.just(1)
        .startWith(Observable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void justFlowableJust() {
        Single.just(1)
        .startWith(Flowable.just(-1, 0))
        .test()
        .assertResult(-1, 0, 1);
    }

    @Test
    public void justFlowableEmpty() {
        Single.just(1)
        .startWith(Observable.empty())
        .test()
        .assertResult(1);
    }

    @Test
    public void justFlowableError() {
        Single.just(1)
        .startWith(Flowable.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }
}
