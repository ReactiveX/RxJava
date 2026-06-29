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

package io.reactivex.rxjava4.internal.operators.completable;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.Function;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.subjects.CompletableSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class CompletableHideTest extends RxJavaTest {

    @Test
    public void never() {
        Completable.never()
        .hide()
        .test()
        .assertNotComplete()
        .assertNoErrors();
    }

    @Test
    public void complete() {
        Completable.complete()
        .hide()
        .test()
        .assertResult();
    }

    @Test
    public void error() {
        Completable.error(new TestException())
        .hide()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void hidden() {
        assertFalse(CompletableSubject.create().hide() instanceof CompletableSubject);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposedCompletable(Completable::hide);
    }

    @Test
    public void isDisposed() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestHelper.checkDisposed(pp.ignoreElements().hide());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable((Function<Completable, Completable>) Completable::hide);
    }
}
