/**
 * Copyright 2016 Netflix, Inc.
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class ObservableReduceTest {
    @Mock
    SingleObserver<Object> NbpSingleObserver;
    @Mock
    MaybeObserver<Object> NbpMaybeObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    BiFunction<Integer, Integer, Integer> sum = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    @Test
    public void testAggregateAsIntSum() {

        Single<Integer> result = Observable.just(1, 2, 3, 4, 5).reduce(0, sum)
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(NbpSingleObserver);

        verify(NbpSingleObserver).onSuccess(1 + 2 + 3 + 4 + 5);
        verify(NbpSingleObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAggregateAsIntSumSourceThrows() {
        Single<Integer> result = Observable.concat(Observable.just(1, 2, 3, 4, 5),
                Observable.<Integer> error(new TestException()))
                .reduce(0, sum).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(NbpSingleObserver);

        verify(NbpSingleObserver, never()).onSuccess(any());
        verify(NbpSingleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumAccumulatorThrows() {
        BiFunction<Integer, Integer, Integer> sumErr = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        Single<Integer> result = Observable.just(1, 2, 3, 4, 5)
                .reduce(0, sumErr).map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer v) {
                        return v;
                    }
                });

        result.subscribe(NbpSingleObserver);

        verify(NbpSingleObserver, never()).onSuccess(any());
        verify(NbpSingleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testAggregateAsIntSumResultSelectorThrows() {

        Function<Integer, Integer> error = new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer t1) {
                throw new TestException();
            }
        };

        Single<Integer> result = Observable.just(1, 2, 3, 4, 5)
                .reduce(0, sum).map(error);

        result.subscribe(NbpSingleObserver);

        verify(NbpSingleObserver, never()).onSuccess(any());
        verify(NbpSingleObserver, times(1)).onError(any(TestException.class));
    }

    @Test
    public void testBackpressureWithNoInitialValue() throws InterruptedException {
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = source.reduce(sum);

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void testBackpressureWithInitialValue() throws InterruptedException {
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Single<Integer> reduced = source.reduce(0, sum);

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }



}