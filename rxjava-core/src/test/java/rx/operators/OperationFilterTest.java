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
import static rx.operators.OperationFilter.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.IObservable;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class OperationFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.from("one", "two", "three");
        IObservable<String> observable = filter(w, new Func1<String, Boolean>() {

            @Override
            public Boolean call(String t1) {
                return t1.equals("two");
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);
        verify(aObserver, Mockito.never()).onNext("one");
        verify(aObserver, times(1)).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }
}
