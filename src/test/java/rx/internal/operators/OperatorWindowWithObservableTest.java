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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorWindowWithObservableTest {

    @Test
    public void testWindowViaObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        int n = 30;
        for (int i = 0; i < n; i++) {
            source.onNext(i);
            if (i % 3 == 2 && i < n - 1) {
                boundary.onNext(i / 3);
            }
        }
        source.onCompleted();

        assertEquals(n / 3, values.size());

        int j = 0;
        for (Observer<Object> mo : values) {
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onCompleted();
            verify(mo, never()).onError(any(Throwable.class));
            j += 3;
        }

        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWindowViaObservableBoundaryCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        int n = 30;
        for (int i = 0; i < n; i++) {
            source.onNext(i);
            if (i % 3 == 2 && i < n - 1) {
                boundary.onNext(i / 3);
            }
        }
        boundary.onCompleted();

        assertEquals(n / 3, values.size());

        int j = 0;
        for (Observer<Object> mo : values) {
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onCompleted();
            verify(mo, never()).onError(any(Throwable.class));
            j += 3;
        }

        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWindowViaObservableBoundaryThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);

        boundary.onError(new TestException());

        assertEquals(1, values.size());

        Observer<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowViaObservableSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        @SuppressWarnings("unchecked")
        final Observer<Object> o = mock(Observer.class);

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                @SuppressWarnings("unchecked")
                final Observer<Object> mo = mock(Observer.class);
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onCompleted() {
                o.onCompleted();
            }
        };

        source.window(boundary).subscribe(wo);

        source.onNext(0);
        source.onNext(1);
        source.onNext(2);

        source.onError(new TestException());

        assertEquals(1, values.size());

        Observer<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowNoDuplication() {
        final PublishSubject<Integer> source = PublishSubject.create();
        final TestSubscriber<Integer> tsw = new TestSubscriber<Integer>() {
            boolean once;
            @Override
            public void onNext(Integer t) {
                if (!once) {
                    once = true;
                    source.onNext(2);
                }
                super.onNext(t);
            }
        };
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> t) {
                t.subscribe(tsw);
                super.onNext(t);
            }
        };
        source.window(new Func0<Observable<Object>>() {
            @Override
            public Observable<Object> call() {
                return Observable.never();
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onCompleted();

        assertEquals(1, ts.getOnNextEvents().size());
        assertEquals(Arrays.asList(1, 2), tsw.getOnNextEvents());
    }
    
    @Test
    public void testWindowViaObservableNoUnsubscribe() {
        Observable<Integer> source = Observable.range(1, 10);
        Func0<Observable<String>> boundary = new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return Observable.empty();
            }
        };
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        source.window(boundary).unsafeSubscribe(ts);
        
        assertFalse(ts.isUnsubscribed());
    }
    
    @Test
    public void testBoundaryUnsubscribedOnMainCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Func0<Observable<Integer>> boundaryFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasObservers());
        assertTrue(boundary.hasObservers());
        
        source.onCompleted();

        assertFalse(source.hasObservers());
        assertFalse(boundary.hasObservers());
        
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    @Test
    public void testMainUnsubscribedOnBoundaryCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Func0<Observable<Integer>> boundaryFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasObservers());
        assertTrue(boundary.hasObservers());
        
        boundary.onCompleted();

        assertFalse(source.hasObservers());
        assertFalse(boundary.hasObservers());
        
        ts.assertCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    @Test
    public void testChildUnsubscribed() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Func0<Observable<Integer>> boundaryFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasObservers());
        assertTrue(boundary.hasObservers());

        ts.unsubscribe();

        assertFalse(source.hasObservers());
        assertFalse(boundary.hasObservers());
        
        ts.assertNotCompleted();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    @Test
    public void testInnerBackpressure() {
        Observable<Integer> source = Observable.range(1, 10);
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Func0<Observable<Integer>> boundaryFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return boundary;
            }
        };
        
        final TestSubscriber<Integer> ts = TestSubscriber.create(1);
        final TestSubscriber<Observable<Integer>> ts1 = new TestSubscriber<Observable<Integer>>(1) {
            @Override
            public void onNext(Observable<Integer> t) {
                super.onNext(t);
                t.subscribe(ts);
            }
        };
        source.window(boundaryFunc)
        .subscribe(ts1);
        
        ts1.assertNoErrors();
        ts1.assertCompleted();
        ts1.assertValueCount(1);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValues(1);
        
        ts.requestMore(11);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    @Test
    public void newBoundaryCalledAfterWindowClosed() {
        final AtomicInteger calls = new AtomicInteger();
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Func0<Observable<Integer>> boundaryFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                calls.getAndIncrement();
                return boundary;
            }
        };
        
        TestSubscriber<Observable<Integer>> ts = TestSubscriber.create();
        source.window(boundaryFunc).subscribe(ts);
        
        source.onNext(1);
        boundary.onNext(1);
        assertTrue(boundary.hasObservers());

        source.onNext(2);
        boundary.onNext(2);
        assertTrue(boundary.hasObservers());

        source.onNext(3);
        boundary.onNext(3);
        assertTrue(boundary.hasObservers());
        
        source.onNext(4);
        source.onCompleted();
        
        ts.assertNoErrors();
        ts.assertValueCount(4);
        ts.assertCompleted();

        assertFalse(source.hasObservers());
        assertFalse(boundary.hasObservers());
    }
}