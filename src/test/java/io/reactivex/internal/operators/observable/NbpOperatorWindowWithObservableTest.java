/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Supplier;
import io.reactivex.observers.*;
import io.reactivex.subjects.PublishSubject;

public class NbpOperatorWindowWithObservableTest {

    @Test
    public void testWindowViaObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        final Observer<Object> o = TestHelper.mockNbpSubscriber();

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new DefaultObserver<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Observer<Object> mo = TestHelper.mockNbpSubscriber();
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
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
        source.onComplete();


        verify(o, never()).onError(any(Throwable.class));

        assertEquals(n / 3, values.size());
        
        int j = 0;
        for (Observer<Object> mo : values) {
            verify(mo, never()).onError(any(Throwable.class));
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onComplete();
            j += 3;
        }

        verify(o).onComplete();
    }

    @Test
    public void testWindowViaObservableBoundaryCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        final Observer<Object> o = TestHelper.mockNbpSubscriber();

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new DefaultObserver<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Observer<Object> mo = TestHelper.mockNbpSubscriber();
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
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
        boundary.onComplete();

        assertEquals(n / 3, values.size());

        int j = 0;
        for (Observer<Object> mo : values) {
            for (int i = 0; i < 3; i++) {
                verify(mo).onNext(j + i);
            }
            verify(mo).onComplete();
            verify(mo, never()).onError(any(Throwable.class));
            j += 3;
        }

        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testWindowViaObservableBoundaryThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        final Observer<Object> o = TestHelper.mockNbpSubscriber();

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new DefaultObserver<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Observer<Object> mo = TestHelper.mockNbpSubscriber();
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
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

        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowViaObservableSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        final Observer<Object> o = TestHelper.mockNbpSubscriber();

        final List<Observer<Object>> values = new ArrayList<Observer<Object>>();

        Observer<Observable<Integer>> wo = new DefaultObserver<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Observer<Object> mo = TestHelper.mockNbpSubscriber();
                values.add(mo);

                args.subscribe(mo);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
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

        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowNoDuplication() {
        final PublishSubject<Integer> source = PublishSubject.create();
        final TestObserver<Integer> tsw = new TestObserver<Integer>() {
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
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> t) {
                t.subscribe(tsw);
                super.onNext(t);
            }
        };
        source.window(new Supplier<Observable<Object>>() {
            @Override
            public Observable<Object> get() {
                return Observable.never();
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onComplete();

        ts.assertValueCount(1);
        tsw.assertValues(1, 2);
    }
    
    @Test
    public void testWindowViaObservableNoUnsubscribe() {
        Observable<Integer> source = Observable.range(1, 10);
        Supplier<Observable<String>> boundary = new Supplier<Observable<String>>() {
            @Override
            public Observable<String> get() {
                return Observable.empty();
            }
        };
        
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>();
        source.window(boundary).unsafeSubscribe(ts);
        
        assertFalse(ts.isCancelled());
    }
    
    @Test
    public void testBoundaryUnsubscribedOnMainCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Supplier<Observable<Integer>> boundaryFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return boundary;
            }
        };
        
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasSubscribers());
        assertTrue(boundary.hasSubscribers());
        
        source.onComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(boundary.hasSubscribers());
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    @Test
    public void testMainUnsubscribedOnBoundaryCompletion() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Supplier<Observable<Integer>> boundaryFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return boundary;
            }
        };
        
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasSubscribers());
        assertTrue(boundary.hasSubscribers());
        
        boundary.onComplete();

        // FIXME source still active because the open window
        assertTrue(source.hasSubscribers());
        assertFalse(boundary.hasSubscribers());
        
        ts.assertComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    
    @Test
    public void testChildUnsubscribed() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Supplier<Observable<Integer>> boundaryFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return boundary;
            }
        };
        
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>();
        source.window(boundaryFunc).subscribe(ts);
        
        assertTrue(source.hasSubscribers());
        assertTrue(boundary.hasSubscribers());

        ts.dispose();

        // FIXME source has subscribers because the open window
        assertTrue(source.hasSubscribers());
        // FIXME boundary has subscribers because the open window
        assertTrue(boundary.hasSubscribers());
        
        ts.assertNotComplete();
        ts.assertNoErrors();
        ts.assertValueCount(1);
    }
    
    @Test
    public void newBoundaryCalledAfterWindowClosed() {
        final AtomicInteger calls = new AtomicInteger();
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Supplier<Observable<Integer>> boundaryFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                calls.getAndIncrement();
                return boundary;
            }
        };
        
        TestObserver<Observable<Integer>> ts = new TestObserver<Observable<Integer>>();
        source.window(boundaryFunc).subscribe(ts);
        
        source.onNext(1);
        boundary.onNext(1);
        assertTrue(boundary.hasSubscribers());

        source.onNext(2);
        boundary.onNext(2);
        assertTrue(boundary.hasSubscribers());

        source.onNext(3);
        boundary.onNext(3);
        assertTrue(boundary.hasSubscribers());
        
        source.onNext(4);
        source.onComplete();
        
        ts.assertNoErrors();
        ts.assertValueCount(4);
        ts.assertComplete();

        assertFalse(source.hasSubscribers());
        assertFalse(boundary.hasSubscribers());
    }
}