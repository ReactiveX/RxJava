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
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableAndThenObservableTest extends RxJavaTest {

    @Test
    public void cancelMain() {
        CompletableSubject cs = CompletableSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = cs.andThen(ps)
                .test();

        assertTrue(cs.hasObservers());
        assertFalse(ps.hasObservers());

        to.dispose();

        assertFalse(cs.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void cancelOther() {
        CompletableSubject cs = CompletableSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = cs.andThen(ps)
                .test();

        assertTrue(cs.hasObservers());
        assertFalse(ps.hasObservers());

        cs.onComplete();

        assertFalse(cs.hasObservers());
        assertTrue(ps.hasObservers());

        to.dispose();

        assertFalse(cs.hasObservers());
        assertFalse(ps.hasObservers());
    }

    @Test
    public void errorMain() {
        CompletableSubject cs = CompletableSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = cs.andThen(ps)
                .test();

        assertTrue(cs.hasObservers());
        assertFalse(ps.hasObservers());

        cs.onError(new TestException());

        assertFalse(cs.hasObservers());
        assertFalse(ps.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void errorOther() {
        CompletableSubject cs = CompletableSubject.create();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = cs.andThen(ps)
                .test();

        assertTrue(cs.hasObservers());
        assertFalse(ps.hasObservers());

        cs.onComplete();

        assertFalse(cs.hasObservers());
        assertTrue(ps.hasObservers());

        ps.onError(new TestException());

        assertFalse(cs.hasObservers());
        assertFalse(ps.hasObservers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void isDisposed() {
        TestHelper.checkDisposed(Completable.never().andThen(Observable.never()));
    }

}
