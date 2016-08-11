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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.*;

public class FlowableWindowWithObservableTest {

    @Test
    public void testWindowViaObservableNormal1() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<Subscriber<Object>>();

        Subscriber<Flowable<Integer>> wo = new DefaultSubscriber<Flowable<Integer>>() {
            @Override
            public void onNext(Flowable<Integer> args) {
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<Subscriber<Object>>();

        Subscriber<Flowable<Integer>> wo = new DefaultSubscriber<Flowable<Integer>>() {
            @Override
            public void onNext(Flowable<Integer> args) {
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<Subscriber<Object>>();

        Subscriber<Flowable<Integer>> wo = new DefaultSubscriber<Flowable<Integer>>() {
            @Override
            public void onNext(Flowable<Integer> args) {
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        PublishProcessor<Integer> boundary = PublishProcessor.create();

        final Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Subscriber<Object>> values = new ArrayList<Subscriber<Object>>();

        Subscriber<Flowable<Integer>> wo = new DefaultSubscriber<Flowable<Integer>>() {
            @Override
            public void onNext(Flowable<Integer> args) {
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
        final PublishProcessor<Integer> source = PublishProcessor.create();
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
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>() {
            @Override
            public void onNext(Flowable<Integer> t) {
                t.subscribe(tsw);
                super.onNext(t);
            }
        };
        source.window(new Callable<Flowable<Object>>() {
            @Override
            public Flowable<Object> call() {
                return Flowable.never();
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onComplete();

        ts.assertValueCount(1);
        tsw.assertValues(1, 2);
    }
    
    @Test
    public void testWindowViaObservableNoUnsubscribe() {
        Flowable<Integer> source = Flowable.range(1, 10);
        Callable<Flowable<String>> boundary = new Callable<Flowable<String>>() {
            @Override
            public Flowable<String> call() {
                return Flowable.empty();
            }
        };
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
        source.window(boundary).subscribe(ts);
        
        assertFalse(ts.isCancelled());
    }
    
    @Test
    public void testBoundaryUnsubscribedOnMainCompletion() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> boundary = PublishProcessor.create();
        Callable<Flowable<Integer>> boundaryFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> boundary = PublishProcessor.create();
        Callable<Flowable<Integer>> boundaryFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> boundary = PublishProcessor.create();
        Callable<Flowable<Integer>> boundaryFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return boundary;
            }
        };
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
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
        Flowable<Integer> source = Flowable.range(1, 10);
        final PublishProcessor<Integer> boundary = PublishProcessor.create();
        Callable<Flowable<Integer>> boundaryFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                return boundary;
            }
        };
        
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1L);
        final TestSubscriber<Flowable<Integer>> ts1 = new TestSubscriber<Flowable<Integer>>(1L) {
            @Override
            public void onNext(Flowable<Integer> t) {
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
        PublishProcessor<Integer> source = PublishProcessor.create();
        final PublishProcessor<Integer> boundary = PublishProcessor.create();
        Callable<Flowable<Integer>> boundaryFunc = new Callable<Flowable<Integer>>() {
            @Override
            public Flowable<Integer> call() {
                calls.getAndIncrement();
                return boundary;
            }
        };
        
        TestSubscriber<Flowable<Integer>> ts = new TestSubscriber<Flowable<Integer>>();
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