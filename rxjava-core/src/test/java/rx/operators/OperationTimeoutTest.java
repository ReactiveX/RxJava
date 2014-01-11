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
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.*;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperationTimeoutTest {
    @Test
    public void testTimeoutSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        timeout.onNext(1);
        
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        
    }
    
    @Test
    public void testTimeoutSelectorTimeoutFirst() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        timeout.onNext(1);
        
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
        
    }
    
    @Test
    public void testTimeoutSelectorFirstThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                throw new OperationReduceTest.CustomException();
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        
    }
    @Test
    public void testTimeoutSelectorSubsequentThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new OperationReduceTest.CustomException();
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onCompleted();
        
    }
    
    @Test
    public void testTimeoutSelectorFirstObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.<Integer>error(new OperationReduceTest.CustomException());
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        
    }
    @Test
    public void testTimeoutSelectorSubsequentObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();
        
        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.<Integer>error(new OperationReduceTest.CustomException());
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };
        
        Observable<Integer> other = Observable.from(Arrays.asList(100));
        
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);
        
        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onCompleted();
        
    }
}
