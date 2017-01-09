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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.observers.TestObserver;

public class ObservableSkipTest {

    @Test
    public void testSkipNegativeElements() {

        Observable<String> skip = Observable.just("one", "two", "three").skip(-99);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipZeroElements() {

        Observable<String> skip = Observable.just("one", "two", "three").skip(0);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipOneElement() {

        Observable<String> skip = Observable.just("one", "two", "three").skip(1);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipTwoElements() {

        Observable<String> skip = Observable.just("one", "two", "three").skip(2);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipEmptyStream() {

        Observable<String> w = Observable.empty();
        Observable<String> skip = w.skip(1);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testSkipMultipleObservers() {

        Observable<String> skip = Observable.just("one", "two", "three")
                .skip(2);

        Observer<String> observer1 = TestHelper.mockObserver();
        skip.subscribe(observer1);

        Observer<String> observer2 = TestHelper.mockObserver();
        skip.subscribe(observer2);

        verify(observer1, times(1)).onNext(any(String.class));
        verify(observer1, never()).onError(any(Throwable.class));
        verify(observer1, times(1)).onComplete();

        verify(observer2, times(1)).onNext(any(String.class));
        verify(observer2, never()).onError(any(Throwable.class));
        verify(observer2, times(1)).onComplete();
    }

    @Test
    public void testSkipError() {

        Exception e = new Exception();

        Observable<String> ok = Observable.just("one");
        Observable<String> error = Observable.error(e);

        Observable<String> skip = Observable.concat(ok, error).skip(100);

        Observer<String> observer = TestHelper.mockObserver();
        skip.subscribe(observer);

        verify(observer, never()).onNext(any(String.class));
        verify(observer, times(1)).onError(e);
        verify(observer, never()).onComplete();

    }

    @Test
    public void testRequestOverflowDoesNotOccur() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(1, 10).skip(5).subscribe(ts);
        ts.assertTerminated();
        ts.assertComplete();
        ts.assertNoErrors();
        assertEquals(Arrays.asList(6,7,8,9,10), ts.values());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).skip(2));
    }
}
