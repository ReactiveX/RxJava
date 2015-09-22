/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorSkipTest {

    @Test
    public void testSkipNegativeElements() {

        NbpObservable<String> skip = NbpObservable.just("one", "two", "three").skip(-99);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipZeroElements() {

        NbpObservable<String> skip = NbpObservable.just("one", "two", "three").skip(0);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipOneElement() {

        NbpObservable<String> skip = NbpObservable.just("one", "two", "three").skip(1);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipTwoElements() {

        NbpObservable<String> skip = NbpObservable.just("one", "two", "three").skip(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, times(1)).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipEmptyStream() {

        NbpObservable<String> w = NbpObservable.empty();
        NbpObservable<String> skip = w.skip(1);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);
        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testSkipMultipleObservers() {

        NbpObservable<String> skip = NbpObservable.just("one", "two", "three")
                .skip(2);

        NbpSubscriber<String> observer1 = TestHelper.mockNbpSubscriber();
        skip.subscribe(observer1);

        NbpSubscriber<String> observer2 = TestHelper.mockNbpSubscriber();
        skip.subscribe(observer2);

        verify(observer1, times(1)).onNext(any(String.class));
        verify(observer1, never()).onError(any(Throwable.class));
        verify(observer1, times(1)).onComplete();

        verify(observer2, times(1)).onNext(any(String.class));
        verify(observer2, never()).onError(any(Throwable.class));
        verify(observer2, times(1)).onComplete();
    }

    @Test
    public void testSkipError() {

        Exception e = new Exception();

        NbpObservable<String> ok = NbpObservable.just("one");
        NbpObservable<String> error = NbpObservable.error(e);

        NbpObservable<String> skip = NbpObservable.concat(ok, error).skip(100);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        skip.subscribe(NbpObserver);

        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, times(1)).onError(e);
        verify(NbpObserver, never()).onComplete();

    }
    
    @Test
    public void testRequestOverflowDoesNotOccur() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, 10).skip(5).subscribe(ts);
        ts.assertTerminated();
        ts.assertComplete();
        ts.assertNoErrors();
        assertEquals(Arrays.asList(6,7,8,9,10), ts.values());
    }
    
}