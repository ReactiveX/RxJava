/**
 * Copyright 2013 Netflix, Inc.
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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Functions;

public class OperationAggregateTest {
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
        
        Observable<Integer> result = Observable.from(1, 2, 3, 4, 5).aggregate(0, sum, Functions.<Integer>identity());
        
        result.subscribe(observer);
        
        verify(observer).onNext(1 + 2 + 3 + 4 + 5);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
    }
    
    @Test
    public void testAggregateIndexedAsAverage() {
        Func3<Integer, Integer, Integer, Integer> sumIndex = new Func3<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer acc, Integer value, Integer index) {
                return acc + (index + 1) + value;
            }
        };
        Func2<Integer, Integer, Integer> selectIndex = new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer count) {
                return t1 + count;
            }
            
        };
        
        Observable<Integer> result = Observable.from(1, 2, 3, 4, 5)
            .aggregateIndexed(0, sumIndex, selectIndex);
        
        result.subscribe(observer);
        
        verify(observer).onNext(2 + 4 + 6 + 8 + 10 + 5);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
    }
    
    static class CustomException extends RuntimeException { }
    
    @Test
    public void testAggregateAsIntSumSourceThrows() {
        Observable<Integer> result = Observable.concat(Observable.from(1, 2, 3, 4, 5),
            Observable.<Integer>error(new CustomException()))
            .aggregate(0, sum, Functions.<Integer>identity());
        
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
            .aggregate(0, sumErr, Functions.<Integer>identity());
        
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
            .aggregate(0, sum, error);
        
        result.subscribe(observer);
        
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        verify(observer, times(1)).onError(any(CustomException.class));
    }

}
