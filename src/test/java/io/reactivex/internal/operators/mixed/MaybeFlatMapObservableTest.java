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

package io.reactivex.internal.operators.mixed;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.*;

public class MaybeFlatMapObservableTest {

    @Test
    public void cancelMain() {
        MaybeSubject<Integer> ms = MaybeSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ms.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ms.hasObservers());
        assertFalse(ps.hasObservers());

        to.cancel();

        assertFalse(ms.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void cancelOther() {
        MaybeSubject<Integer> ms = MaybeSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ms.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ms.hasObservers());
        assertFalse(ps.hasObservers());

        ms.onSuccess(1);

        assertFalse(ms.hasObservers());
        assertTrue(ps.hasObservers());

        to.cancel();

        assertFalse(ms.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void mapperCrash() {
        Maybe.just(1).flatMapObservable(new Function<Integer, ObservableSource<? extends Object>>() {
            @Override
            public ObservableSource<? extends Object> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(Maybe.never().flatMapObservable(Functions.justFunction(Observable.never())));
    }
}
