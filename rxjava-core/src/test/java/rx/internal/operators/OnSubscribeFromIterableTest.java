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
package rx.internal.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.observers.TestSubscriber;

public class OnSubscribeFromIterableTest {

    @Test
    public void testIterable() {
        Observable<String> observable = Observable.create(new OnSubscribeFromIterable<String>(Arrays.<String> asList("one", "two", "three")));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testObservableFromIterable() {
        Observable<String> observable = Observable.from(Arrays.<String> asList("one", "two", "three"));

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testBackpressureViaRequest() {
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(Arrays.asList(1, 2, 3, 4, 5));
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.request(1);
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1));
        ts.request(2);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
        ts.request(3);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertTerminalEvent();
    }

    @Test
    public void testNoBackpressure() {
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(Arrays.asList(1, 2, 3, 4, 5));
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.assertReceivedOnNext(Collections.<Integer> emptyList());
        ts.request(-1); // infinite
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3, 4, 5));
        ts.assertTerminalEvent();
    }
    
    @Test
    public void testSubscribeMultipleTimes() {
        OnSubscribeFromIterable<Integer> o = new OnSubscribeFromIterable<Integer>(Arrays.asList(1, 2, 3));
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
        
        ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
        
        ts = new TestSubscriber<Integer>();
        o.call(ts);
        ts.assertReceivedOnNext(Arrays.asList(1, 2, 3));
    }

}
