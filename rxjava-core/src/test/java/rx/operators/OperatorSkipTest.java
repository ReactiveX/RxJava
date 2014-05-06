/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observer;

public class OperatorSkipTest {

    @Test
    public void testSkipNegativeElements() {

        Observable<String> skip = Observable.from("one", "two", "three").lift(new OperatorSkip<String>(-99));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipZeroElements() {

        Observable<String> skip = Observable.from("one", "two", "three").lift(new OperatorSkip<String>(0));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipOneElement() {

        Observable<String> skip = Observable.from("one", "two", "three").lift(new OperatorSkip<String>(1));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipTwoElements() {

        Observable<String> skip = Observable.from("one", "two", "three").lift(new OperatorSkip<String>(2));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipEmptyStream() {

        Observable<String> w = Observable.empty();
        Observable<String> skip = w.lift(new OperatorSkip<String>(1));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSkipMultipleObservers() {

        Observable<String> skip = Observable.from("one", "two", "three").lift(new OperatorSkip<String>(2));

        @SuppressWarnings("unchecked")
        Observer<String> observer1 = mock(Observer.class);
        skip.subscribe(observer1);

        @SuppressWarnings("unchecked")
        Observer<String> observer2 = mock(Observer.class);
        skip.subscribe(observer2);

        verify(observer1, times(1)).onNext(any(String.class));
        verify(observer1, never()).onError(any(Throwable.class));
        verify(observer1, times(1)).onCompleted();

        verify(observer2, times(1)).onNext(any(String.class));
        verify(observer2, never()).onError(any(Throwable.class));
        verify(observer2, times(1)).onCompleted();
    }

    @Test
    public void testSkipError() {

        Exception e = new Exception();

        Observable<String> ok = Observable.from("one");
        Observable<String> error = Observable.error(e);

        Observable<String> skip = Observable.concat(ok, error).lift(new OperatorSkip<String>(100));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        skip.subscribe(observer);

        verify(observer, never()).onNext(any(String.class));
        verify(observer, times(1)).onError(e);
        verify(observer, never()).onCompleted();

    }
}
