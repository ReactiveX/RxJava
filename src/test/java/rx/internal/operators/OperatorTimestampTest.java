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
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.*;

import rx.*;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;
import rx.schedulers.*;
import rx.subjects.PublishSubject;

public class OperatorTimestampTest {
    @Mock
    Observer<Object> observer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void timestampWithScheduler() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Timestamped<Integer>> m = source.timestamp(scheduler);
        m.subscribe(observer);

        source.onNext(1);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(0, 1));
        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(100, 2));
        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(200, 3));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void timestampWithScheduler2() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Timestamped<Integer>> m = source.timestamp(scheduler);
        m.subscribe(observer);

        source.onNext(1);
        source.onNext(2);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        source.onNext(3);

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(0, 1));
        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(0, 2));
        inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(200, 3));

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void withDefaultScheduler() {
        final TestScheduler scheduler = new TestScheduler();

        RxJavaHooks.setOnComputationScheduler(new Func1<Scheduler, Scheduler>() {
            @Override
            public Scheduler call(Scheduler t) {
                return scheduler;
            }
        });

        try {
            PublishSubject<Integer> source = PublishSubject.create();
            Observable<Timestamped<Integer>> m = source.timestamp();
            m.subscribe(observer);

            source.onNext(1);
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            source.onNext(3);

            InOrder inOrder = inOrder(observer);

            inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(0, 1));
            inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(100, 2));
            inOrder.verify(observer, times(1)).onNext(new Timestamped<Integer>(200, 3));

            verify(observer, never()).onError(any(Throwable.class));
            verify(observer, never()).onCompleted();
        } finally {
            RxJavaHooks.reset();
        }
    }
}
