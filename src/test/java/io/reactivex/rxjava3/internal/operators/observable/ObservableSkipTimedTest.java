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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableSkipTimedTest extends RxJavaTest {

    @Test
    public void skipTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skip(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        source.onComplete();

        InOrder inOrder = inOrder(o);

        inOrder.verify(o, never()).onNext(1);
        inOrder.verify(o, never()).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onNext(4);
        inOrder.verify(o).onNext(5);
        inOrder.verify(o).onNext(6);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipTimedFinishBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skip(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(o);

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skip(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new TestException());

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(o);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void skipTimedErrorAfterTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skip(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        source.onError(new TestException());

        InOrder inOrder = inOrder(o);

        inOrder.verify(o, never()).onNext(1);
        inOrder.verify(o, never()).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onNext(4);
        inOrder.verify(o).onNext(5);
        inOrder.verify(o).onNext(6);
        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onComplete();

    }

    @Test
    public void skipTimedDefaultScheduler() {
        Observable.just(1).skip(1, TimeUnit.MINUTES)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }
}
