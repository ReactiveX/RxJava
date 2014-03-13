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

import static org.mockito.Mockito.*;
import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

import java.util.Arrays;

public class OperatorAllTest {

    final Func1<String, Boolean> hasLength3 = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String s) {
            return s.length() == 3;
        }
    };

    @Test
    @SuppressWarnings("unchecked")
    public void testAll() {
        Observable<Boolean> obs = Observable.from(Arrays.asList("one", "two", "six")).all(hasLength3);

        Observer<Boolean> observer = mock(Observer.class);
        obs.subscribe(new TestSubscriber<Boolean>(observer));

        verify(observer).onNext(true);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotAll() {
        Observable<Boolean> obs = Observable.from(Arrays.asList("one", "two", "three", "six")).all(hasLength3);

        Observer<Boolean> observer = mock(Observer.class);
        obs.subscribe(new TestSubscriber<Boolean>(observer));

        verify(observer).onNext(false);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmpty() {
        Observable<Boolean> obs = Observable.<String>empty().all(hasLength3);

        Observer<Boolean> observer = mock(Observer.class);
        obs.subscribe(new TestSubscriber<Boolean>(observer));

        verify(observer).onNext(true);
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testError() {
        Throwable error = new Throwable();
        Observable<Boolean> obs = Observable.<String>error(error).all(hasLength3);

        Observer<Boolean> observer = mock(Observer.class);
        obs.subscribe(new TestSubscriber<Boolean>(observer));

        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }
}
