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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.Func2;

public class OperatorSkipWhileTest {

    @SuppressWarnings("unchecked")
    Observer<Integer> w = mock(Observer.class);

    private static final Func1<Integer, Boolean> LESS_THAN_FIVE = new Func1<Integer, Boolean>() {
        @Override
        public Boolean call(Integer v) {
            if (v == 42)
                throw new RuntimeException("that's not the answer to everything!");
            return v < 5;
        }
    };

    private static final Func2<Integer, Integer, Boolean> INDEX_LESS_THAN_THREE = new Func2<Integer, Integer, Boolean>() {
        @Override
        public Boolean call(Integer value, Integer index) {
            return index < 3;
        }
    };

    @Test
    public void testSkipWithIndex() {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 5);
        src.skipWhileWithIndex(INDEX_LESS_THAN_THREE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(4);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipEmpty() {
        Observable<Integer> src = Observable.empty();
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testSkipEverything() {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 3, 2, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testSkipNothing() {
        Observable<Integer> src = Observable.from(5, 3, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onNext(3);
        inOrder.verify(w, times(1)).onNext(1);
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipSome() {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 5, 3, 1, 5);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onNext(3);
        inOrder.verify(w, times(1)).onNext(1);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onCompleted();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipError() {
        Observable<Integer> src = Observable.from(1, 2, 42, 5, 3, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, never()).onNext(anyInt());
        inOrder.verify(w, never()).onCompleted();
        inOrder.verify(w, times(1)).onError(any(RuntimeException.class));
    }
    
    @Test
    public void testSkipManySubscribers() {
        Observable<Integer> src = Observable.range(1, 10).skipWhile(LESS_THAN_FIVE);
        int n = 5;
        for (int i = 0; i < n; i++) {
            @SuppressWarnings("unchecked")
            Observer<Object> o = mock(Observer.class);
            InOrder inOrder = inOrder(o);
            
            src.subscribe(o);
            
            for (int j = 5; j < 10; j++) {
                inOrder.verify(o).onNext(j);
            } 
            inOrder.verify(o).onCompleted();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
}
