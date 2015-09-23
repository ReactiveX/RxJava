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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function3;
import io.reactivex.subjects.nbp.NbpPublishSubject;

public class NbpOperatorZipIterableTest {
    BiFunction<String, String, String> concat2Strings;
    NbpPublishSubject<String> s1;
    NbpPublishSubject<String> s2;
    NbpObservable<String> zipped;

    NbpSubscriber<String> NbpObserver;
    InOrder inOrder;

    @Before
    public void setUp() {
        concat2Strings = new BiFunction<String, String, String>() {
            @Override
            public String apply(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = NbpPublishSubject.create();
        s2 = NbpPublishSubject.create();
        zipped = NbpObservable.zip(s1, s2, concat2Strings);

        NbpObserver = TestHelper.mockNbpSubscriber();
        inOrder = inOrder(NbpObserver);

        zipped.subscribe(NbpObserver);
    }

    BiFunction<Object, Object, String> zipr2 = new BiFunction<Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Function3<Object, Object, Object, String> zipr3 = new Function3<Object, Object, Object, String>() {

        @Override
        public String apply(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    @Test
    public void testZipIterableSameSize() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onNext("three-3");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptyFirstSize() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptySecond() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList();

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onComplete();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZipIterableFirstShorter() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableSecondShorter() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onComplete();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableFirstThrows() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void testZipIterableIteratorThrows() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                throw new TestException();
            }
        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any(String.class));

    }

    @Test
    public void testZipIterableHasNextThrows() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int count;

                    @Override
                    public boolean hasNext() {
                        if (count == 0) {
                            return true;
                        }
                        throw new TestException();
                    }

                    @Override
                    public String next() {
                        count++;
                        return "1";
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();

    }

    @Test
    public void testZipIterableNextThrows() {
        NbpPublishSubject<String> r1 = NbpPublishSubject.create();
        /* define a NbpSubscriber to receive aggregated events */
        NbpSubscriber<String> o = TestHelper.mockNbpSubscriber();
        InOrder io = inOrder(o);

        Iterable<String> r2 = new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        throw new TestException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                };
            }

        };

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onError(new TestException());

        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onComplete();

    }
    
    Consumer<String> printer = System.out::println;

    static final class SquareStr implements Function<Integer, String> {
        final AtomicInteger counter = new AtomicInteger();
        @Override
        public String apply(Integer t1) {
            counter.incrementAndGet();
            System.out.println("Omg I'm calculating so hard: " + t1 + "*" + t1 + "=" + (t1*t1));
            return " " + (t1*t1);
        }
    }

    @Test 
    public void testTake2() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");
        
        SquareStr squareStr = new SquareStr();
        
        o.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);
        
        assertEquals(2, squareStr.counter.get());
    }
}