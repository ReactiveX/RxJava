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
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;

public class OperatorDefaultIfEmptyTest {

    @Test
    public void testDefaultIfEmpty() {
        Observable<Integer> source = Observable.from(1, 2, 3);
        Observable<Integer> observable = source.defaultIfEmpty(10);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(10);
        verify(observer).onNext(1);
        verify(observer).onNext(2);
        verify(observer).onNext(3);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDefaultIfEmptyWithEmpty() {
        Observable<Integer> source = Observable.empty();
        Observable<Integer> observable = source.defaultIfEmpty(10);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        
        verify(observer).onNext(10);
        verify(observer).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testEmptyButClientThrows() {
        @SuppressWarnings("unchecked")
        final Observer<Integer> o = mock(Observer.class);
        
        Observable.<Integer>empty().defaultIfEmpty(1).subscribe(new Subscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        });
        
        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
    }
}
