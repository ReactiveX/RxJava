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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.schedulers.TestScheduler;

public class OperationTimerTest {
    @Mock
    Observer<Object> observer;
    TestScheduler s;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        s = new TestScheduler();
    }

    @Test
    public void testTimerOnce() {
        Observable.timer(100, TimeUnit.MILLISECONDS, s).subscribe(observer);
        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(observer, times(1)).onNext(0L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testTimerPeriodically() {
        Subscription c = Observable.timer(100, 100, TimeUnit.MILLISECONDS, s).subscribe(observer);
        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(0L);

        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);

        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);

        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(3L);

        c.unsubscribe();
        s.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(any());

        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
}