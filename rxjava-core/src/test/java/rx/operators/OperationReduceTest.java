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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Functions;

public class OperationReduceTest {
    @Mock
    Observer<Object> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    Func2<Integer, Integer, Integer> sum = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    @Test
    public void testAggregateAsIntSum() {

        Observable<Integer> result = Observable.from(1, 2, 3, 4, 5).reduce(0, sum).map(Functions.<Integer> identity());

        result.subscribe(observer);

        verify(observer).onNext(1 + 2 + 3 + 4 + 5);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    static class CustomException extends RuntimeException {
    }

    @Test
    public void testAggregateAsIntSumSourceThrows() {
        Observable<Integer> result = Observable.concat(Observable.from(1, 2, 3, 4, 5),
                Observable.<Integer> error(new CustomException()))
                .reduce(0, sum).map(Functions.<Integer> identity());

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(CustomException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrows() {
        Func2<Integer, Integer, Integer> sumErr = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new CustomException();
            }
        };

        Observable<Integer> result = Observable.from(1, 2, 3, 4, 5)
                .reduce(0, sumErr).map(Functions.<Integer> identity());

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(CustomException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrows() {

        Func1<Integer, Integer> error = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                throw new CustomException();
            }
        };

        Observable<Integer> result = Observable.from(1, 2, 3, 4, 5)
                .reduce(0, sum).map(error);

        result.subscribe(observer);

        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(CustomException.class));
    }

}
