/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationScan.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func2;

public class OperationScanTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testScanIntegersWithInitialValue() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        Observable<Integer> observable = Observable.from(1, 2, 3);

        Observable<String> m = Observable.create(scan(observable, "", new Func2<String, Integer, String>() {

            @Override
            public String call(String s, Integer n) {
                return s + n.toString();
            }

        }));
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext("");
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("12");
        verify(observer, times(1)).onNext("123");
        verify(observer, times(4)).onNext(anyString());
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValue() {
        @SuppressWarnings("unchecked")
        Observer<Integer> Observer = mock(Observer.class);

        Observable<Integer> observable = Observable.from(1, 2, 3);

        Observable<Integer> m = Observable.create(scan(observable, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }));
        m.subscribe(Observer);

        verify(Observer, never()).onError(any(Throwable.class));
        verify(Observer, never()).onNext(0);
        verify(Observer, times(1)).onNext(1);
        verify(Observer, times(1)).onNext(3);
        verify(Observer, times(1)).onNext(6);
        verify(Observer, times(3)).onNext(anyInt());
        verify(Observer, times(1)).onCompleted();
        verify(Observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testScanIntegersWithoutInitialValueAndOnlyOneValue() {
        @SuppressWarnings("unchecked")
        Observer<Integer> Observer = mock(Observer.class);

        Observable<Integer> observable = Observable.from(1);

        Observable<Integer> m = Observable.create(scan(observable, new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 + t2;
            }

        }));
        m.subscribe(Observer);

        verify(Observer, never()).onError(any(Throwable.class));
        verify(Observer, never()).onNext(0);
        verify(Observer, times(1)).onNext(1);
        verify(Observer, times(1)).onNext(anyInt());
        verify(Observer, times(1)).onCompleted();
        verify(Observer, never()).onError(any(Throwable.class));
    }
}
