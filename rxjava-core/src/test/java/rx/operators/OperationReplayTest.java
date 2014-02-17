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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.operators.OperationReplay.VirtualBoundedList;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperationReplayTest {
    @Test
    public void testBoundedList() {
        VirtualBoundedList<Integer> list = new VirtualBoundedList<Integer>(3);

        list.add(1); // idx: 0
        list.add(2); // idx: 1
        list.add(3); // idx: 2

        Assert.assertEquals(3, list.size());

        list.add(4); // idx: 3

        Assert.assertEquals(3, list.size());
        Assert.assertEquals(Arrays.asList(2, 3, 4), list.toList());

        Assert.assertEquals(1, list.start());
        Assert.assertEquals(4, list.end());

        list.removeBefore(3);

        Assert.assertEquals(1, list.size());

        Assert.assertEquals(Arrays.asList(4), list.toList());

        Assert.assertEquals(3, list.start());
        Assert.assertEquals(4, list.end());
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadBefore() {
        VirtualBoundedList<Integer> list = new VirtualBoundedList<Integer>(3);

        list.add(1); // idx: 0
        list.add(2); // idx: 1
        list.add(3); // idx: 2
        list.add(4); // idx: 3

        list.get(0);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadAfter() {
        VirtualBoundedList<Integer> list = new VirtualBoundedList<Integer>(3);

        list.add(1); // idx: 0
        list.add(2); // idx: 1
        list.add(3); // idx: 2
        list.add(4); // idx: 3

        list.get(4);
    }

    @Test
    public void testBufferedReplay() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplay() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onCompleted();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testReplaySelector() {
        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector);

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

    }

    @Test
    public void testBufferedReplaySelector() {

        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 3);

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            source.onNext(4);
            source.onCompleted();
            inOrder.verify(observer1, times(1)).onNext(8);
            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testWindowedReplaySelector() {

        final Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1 * 2;
            }

        };

        Func1<Observable<Integer>, Observable<Integer>> selector = new Func1<Observable<Integer>, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Observable<Integer> t1) {
                return t1.map(dbl);
            }

        };

        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> co = source.replay(selector, 100, TimeUnit.MILLISECONDS, scheduler);

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onCompleted();
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onNext(6);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));

        }
        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onError(any(Throwable.class));
        }
    }

    @Test
    public void testBufferedReplayError() {
        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(3);
        co.connect();

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            source.onNext(2);
            source.onNext(3);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            source.onNext(4);
            source.onError(new RuntimeException("Forced failure"));

            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();

        }

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);
            inOrder.verify(observer1, times(1)).onNext(4);
            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();
        }
    }

    @Test
    public void testWindowedReplayError() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        ConnectableObservable<Integer> co = source.replay(100, TimeUnit.MILLISECONDS, scheduler);
        co.connect();

        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);

            source.onNext(1);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(2);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onNext(3);
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);
            source.onError(new RuntimeException("Forced failure"));
            scheduler.advanceTimeBy(60, TimeUnit.MILLISECONDS);

            inOrder.verify(observer1, times(1)).onNext(1);
            inOrder.verify(observer1, times(1)).onNext(2);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();

        }
        {
            Observer<Object> observer1 = mock(Observer.class);
            InOrder inOrder = inOrder(observer1);

            co.subscribe(observer1);
            inOrder.verify(observer1, times(1)).onNext(3);

            inOrder.verify(observer1, times(1)).onError(any(RuntimeException.class));
            inOrder.verifyNoMoreInteractions();
            verify(observer1, never()).onCompleted();
        }
    }
}
