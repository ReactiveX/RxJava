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

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.mockito.Mockito.*;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class OperationFlatMapTest {
    @Test
    public void testNormal() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);
        
        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };
        
        List<Integer> source = Arrays.asList(16, 32, 64);
        
        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }
    @Test
    public void testCollectionFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                throw new OperationReduceTest.CustomException();
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };
        
        List<Integer> source = Arrays.asList(16, 32, 64);
        
        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);
        
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(OperationReduceTest.CustomException.class));
    }
    
    @Test
    public void testResultFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);
        
        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new OperationReduceTest.CustomException();
            }
        };
        
        List<Integer> source = Arrays.asList(16, 32, 64);
        
        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);
        
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(OperationReduceTest.CustomException.class));
    }
    @Test
    public void testMergeError() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, Observable<Integer>> func = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.error(new OperationReduceTest.CustomException());
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };
        
        List<Integer> source = Arrays.asList(16, 32, 64);
        
        Observable.from(source).mergeMap(func, resFunc).subscribe(o);
        
        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(OperationReduceTest.CustomException.class));
    }
}
