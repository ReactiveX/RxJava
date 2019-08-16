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

package io.reactivex.rxjava3.internal.operators.mixed;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class SingleFlatMapObservableTest extends RxJavaTest {

    @Test
    public void cancelMain() {
        SingleSubject<Integer> ss = SingleSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ss.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ss.hasObservers());
        assertFalse(ps.hasObservers());

        to.dispose();

        assertFalse(ss.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void cancelOther() {
        SingleSubject<Integer> ss = SingleSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ss.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ss.hasObservers());
        assertFalse(ps.hasObservers());

        ss.onSuccess(1);

        assertFalse(ss.hasObservers());
        assertTrue(ps.hasObservers());

        to.dispose();

        assertFalse(ss.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void errorMain() {
        SingleSubject<Integer> ss = SingleSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ss.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ss.hasObservers());
        assertFalse(ps.hasObservers());

        ss.onError(new TestException());

        assertFalse(ss.hasObservers());
        assertFalse(ps.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void errorOther() {
        SingleSubject<Integer> ss = SingleSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ss.flatMapObservable(Functions.justFunction(ps))
                .test();

        assertTrue(ss.hasObservers());
        assertFalse(ps.hasObservers());

        ss.onSuccess(1);

        assertFalse(ss.hasObservers());
        assertTrue(ps.hasObservers());

        ps.onError(new TestException());

        assertFalse(ss.hasObservers());
        assertFalse(ps.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void mapperCrash() {
        Single.just(1).flatMapObservable(new Function<Integer, ObservableSource<? extends Object>>() {
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
        TestHelper.checkDisposed(Single.never().flatMapObservable(Functions.justFunction(Observable.never())));
    }
}
