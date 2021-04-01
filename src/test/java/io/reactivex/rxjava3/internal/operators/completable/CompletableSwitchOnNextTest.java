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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.CompletableSubject;

public class CompletableSwitchOnNextTest extends RxJavaTest {

    @Test
    public void normal() {
        Runnable run = mock(Runnable.class);

        Completable.switchOnNext(
                Flowable.range(1, 10)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Completable.fromRunnable(run);
                    }
                    return Completable.complete();
                })
        )
        .test()
        .assertResult();

        verify(run, times(5)).run();
    }

    @Test
    public void normalDelayError() {
        Runnable run = mock(Runnable.class);

        Completable.switchOnNextDelayError(
                Flowable.range(1, 10)
                .map(v -> {
                    if (v % 2 == 0) {
                        return Completable.fromRunnable(run);
                    }
                    return Completable.complete();
                })
        )
        .test()
        .assertResult();

        verify(run, times(5)).run();
    }

    @Test
    public void noDelaySwitch() {
        PublishProcessor<Completable> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.switchOnNext(pp).test();

        assertTrue(pp.hasSubscribers());

        to.assertEmpty();

        CompletableSubject cs1 = CompletableSubject.create();
        CompletableSubject cs2 = CompletableSubject.create();

        pp.onNext(cs1);

        assertTrue(cs1.hasObservers());

        pp.onNext(cs2);

        assertFalse(cs1.hasObservers());
        assertTrue(cs2.hasObservers());

        pp.onComplete();

        assertTrue(cs2.hasObservers());

        cs2.onComplete();

        to.assertResult();
    }

    @Test
    public void delaySwitch() {
        PublishProcessor<Completable> pp = PublishProcessor.create();

        TestObserver<Void> to = Completable.switchOnNextDelayError(pp).test();

        assertTrue(pp.hasSubscribers());

        to.assertEmpty();

        CompletableSubject cs1 = CompletableSubject.create();
        CompletableSubject cs2 = CompletableSubject.create();

        pp.onNext(cs1);

        assertTrue(cs1.hasObservers());

        pp.onNext(cs2);

        assertFalse(cs1.hasObservers());
        assertTrue(cs2.hasObservers());

        assertTrue(cs2.hasObservers());

        cs2.onError(new TestException());

        assertTrue(pp.hasSubscribers());

        to.assertEmpty();

        pp.onComplete();

        to.assertFailure(TestException.class);
    }
}
