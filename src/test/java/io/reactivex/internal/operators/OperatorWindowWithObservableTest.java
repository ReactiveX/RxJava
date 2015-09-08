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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorWindowWithObservableTest {

    @Test
    public void testWindowViaObservableNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> boundary = PublishSubject.create();

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<>();

        Subscriber<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Subscriber<Object> mo = TestHelper.mockSubscriber();
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
        for (Subscriber<Object> mo : values) {
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

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<>();

        Subscriber<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Subscriber<Object> mo = TestHelper.mockSubscriber();
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
        for (Subscriber<Object> mo : values) {
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

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<>();

        Subscriber<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Subscriber<Object> mo = TestHelper.mockSubscriber();
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

        Subscriber<Object> mo = values.get(0);

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

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<>();

        Subscriber<Observable<Integer>> wo = new Observer<Observable<Integer>>() {
            @Override
            public void onNext(Observable<Integer> args) {
                final Subscriber<Object> mo = TestHelper.mockSubscriber();
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

        Subscriber<Object> mo = values.get(0);

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
        
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<>();
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
        
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<>();
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
        
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<>();
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
        
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<>();
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
    public void testInnerBackpressure() {
        Observable<Integer> source = Observable.range(1, 10);
        final PublishSubject<Integer> boundary = PublishSubject.create();
        Supplier<Observable<Integer>> boundaryFunc = new Supplier<Observable<Integer>>() {
            @Override
            public Observable<Integer> get() {
                return boundary;
            }
        };
        
        final TestSubscriber<Integer> ts = new TestSubscriber<>(1L);
        final TestSubscriber<Observable<Integer>> ts1 = new TestSubscriber<Observable<Integer>>(1L) {
            @Override
            public void onNext(Observable<Integer> t) {
                super.onNext(t);
                t.subscribe(ts);
            }
        };
        source.window(boundaryFunc)
        .subscribe(ts1);
        
        ts1.assertNoErrors();
        ts1.assertComplete();
        ts1.assertValueCount(1);
        
        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValues(1);
        
        ts.request(11);
        
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ts.assertNoErrors();
        ts.assertComplete();
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
        
        TestSubscriber<Observable<Integer>> ts = new TestSubscriber<>();
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