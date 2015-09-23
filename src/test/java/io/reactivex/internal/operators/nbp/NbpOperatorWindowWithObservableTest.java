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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorWindowWithObservableTest {

    @Test
    public void testWindowViaObservableNormal1() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();

        final NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        final List<NbpSubscriber<Object>> values = new ArrayList<>();

        NbpSubscriber<NbpObservable<Integer>> wo = new NbpObserver<NbpObservable<Integer>>() {
            @Override
            public void onNext(NbpObservable<Integer> args) {
                final NbpSubscriber<Object> mo = TestHelper.mockNbpSubscriber();
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
        for (NbpSubscriber<Object> mo : values) {
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();

        final NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        final List<NbpSubscriber<Object>> values = new ArrayList<>();

        NbpSubscriber<NbpObservable<Integer>> wo = new NbpObserver<NbpObservable<Integer>>() {
            @Override
            public void onNext(NbpObservable<Integer> args) {
                final NbpSubscriber<Object> mo = TestHelper.mockNbpSubscriber();
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
        for (NbpSubscriber<Object> mo : values) {
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();

        final NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        final List<NbpSubscriber<Object>> values = new ArrayList<>();

        NbpSubscriber<NbpObservable<Integer>> wo = new NbpObserver<NbpObservable<Integer>>() {
            @Override
            public void onNext(NbpObservable<Integer> args) {
                final NbpSubscriber<Object> mo = TestHelper.mockNbpSubscriber();
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

        NbpSubscriber<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowViaObservableSourceThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();

        final NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        final List<NbpSubscriber<Object>> values = new ArrayList<>();

        NbpSubscriber<NbpObservable<Integer>> wo = new NbpObserver<NbpObservable<Integer>>() {
            @Override
            public void onNext(NbpObservable<Integer> args) {
                final NbpSubscriber<Object> mo = TestHelper.mockNbpSubscriber();
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

        NbpSubscriber<Object> mo = values.get(0);

        verify(mo).onNext(0);
        verify(mo).onNext(1);
        verify(mo).onNext(2);
        verify(mo).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testWindowNoDuplication() {
        final NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpTestSubscriber<Integer> tsw = new NbpTestSubscriber<Integer>() {
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
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<NbpObservable<Integer>>() {
            @Override
            public void onNext(NbpObservable<Integer> t) {
                t.subscribe(tsw);
                super.onNext(t);
            }
        };
        source.window(new Supplier<NbpObservable<Object>>() {
            @Override
            public NbpObservable<Object> get() {
                return NbpObservable.never();
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onComplete();

        ts.assertValueCount(1);
        tsw.assertValues(1, 2);
    }
    
    @Test
    public void testWindowViaObservableNoUnsubscribe() {
        NbpObservable<Integer> source = NbpObservable.range(1, 10);
        Supplier<NbpObservable<String>> boundary = new Supplier<NbpObservable<String>>() {
            @Override
            public NbpObservable<String> get() {
                return NbpObservable.empty();
            }
        };
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
        source.window(boundary).unsafeSubscribe(ts);
        
        assertFalse(ts.isCancelled());
    }
    
    @Test
    public void testBoundaryUnsubscribedOnMainCompletion() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> boundaryFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return boundary;
            }
        };
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> boundaryFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return boundary;
            }
        };
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> boundaryFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return boundary;
            }
        };
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> boundary = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> boundaryFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                calls.getAndIncrement();
                return boundary;
            }
        };
        
        NbpTestSubscriber<NbpObservable<Integer>> ts = new NbpTestSubscriber<>();
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