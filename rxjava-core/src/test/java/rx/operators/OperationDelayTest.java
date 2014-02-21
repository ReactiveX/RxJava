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
import static org.mockito.MockitoAnnotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperationDelayTest {
    @Mock
    private Observer<Long> observer;
    @Mock
    private Observer<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        initMocks(this);
        scheduler = new TestScheduler();
    }

    @Test
    public void testDelay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testLongDelay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithError() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).map(new Func1<Long, Long>() {
            @Override
            public Long call(Long value) {
                if (value == 1L) {
                    throw new RuntimeException("error!");
                }
                return value;
            }
        });
        Observable<Long> delayed = source.delay(1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);

        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();

        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }

    // TODO activate this test once https://github.com/Netflix/RxJava/issues/552 is fixed
    @Ignore
    @Test
    public void testDelayWithMultipleSubscriptions() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        delayed.subscribe(observer2);

        InOrder inOrder = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);

        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(observer, never()).onCompleted();
        verify(observer2, never()).onCompleted();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder2.verify(observer2, times(1)).onCompleted();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscription() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Integer> result = Observable.from(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onCompleted();

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(1)).onNext(1);
        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscriptionCancelBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Integer> result = Observable.from(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = mock(Observer.class);

        Subscription s = result.subscribe(o);
        s.unsubscribe();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final List<PublishSubject<Integer>> delays = new ArrayList<PublishSubject<Integer>>();
        final int n = 10;
        for (int i = 0; i < n; i++) {
            PublishSubject<Integer> delay = PublishSubject.create();
            delays.add(delay);
        }

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return delays.get(t1);
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
            delays.get(i).onNext(i);
            inOrder.verify(o).onNext(i);
        }
        source.onCompleted();

        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSingleSend1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);
        delay.onNext(2);

        inOrder.verify(o).onNext(1);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        source.onError(new OperationReduceTest.CustomException());
        delay.onNext(1);

        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableDelayFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                throw new OperationReduceTest.CustomException();
            }
        };
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);

        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableDelayThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        delay.onError(new OperationReduceTest.CustomException());

        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableSubscriptionNormal() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Func0<Observable<Integer>> subFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return delay;
            }
        };
        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableSubscriptionFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Func0<Observable<Integer>> subFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                throw new OperationReduceTest.CustomException();
            }
        };
        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);

        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableSubscriptionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Func0<Observable<Integer>> subFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return delay;
            }
        };
        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onError(new OperationReduceTest.CustomException());

        source.onNext(2);

        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testDelayWithObservableEmptyDelayer() {
        PublishSubject<Integer> source = PublishSubject.create();

        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.empty();
            }
        };
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        source.onCompleted();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSubscriptionRunCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> sdelay = PublishSubject.create();
        final PublishSubject<Integer> delay = PublishSubject.create();
        Func0<Observable<Integer>> subFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return sdelay;
            }
        };
        Func1<Integer, Observable<Integer>> delayFunc = new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer t1) {
                return delay;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        sdelay.onCompleted();

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onCompleted();
    }
}
