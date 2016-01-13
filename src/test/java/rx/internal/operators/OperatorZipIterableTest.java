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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class OperatorZipIterableTest {
    Func2<String, String, String> concat2Strings;
    PublishSubject<String> s1;
    PublishSubject<String> s2;
    Observable<String> zipped;

    Observer<String> observer;
    InOrder inOrder;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        concat2Strings = new Func2<String, String, String>() {
            @Override
            public String call(String t1, String t2) {
                return t1 + "-" + t2;
            }
        };

        s1 = PublishSubject.create();
        s2 = PublishSubject.create();
        zipped = Observable.zip(s1, s2, concat2Strings);

        observer = mock(Observer.class);
        inOrder = inOrder(observer);

        zipped.subscribe(observer);
    }

    Func2<Object, Object, String> zipr2 = new Func2<Object, Object, String>() {

        @Override
        public String call(Object t1, Object t2) {
            return "" + t1 + t2;
        }

    };
    Func3<Object, Object, Object, String> zipr3 = new Func3<Object, Object, Object, String>() {

        @Override
        public String call(Object t1, Object t2, Object t3) {
            return "" + t1 + t2 + t3;
        }

    };

    @Test
    public void testZipIterableSameSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onNext("three-3");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptyFirstSize() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onCompleted();

        io.verify(o).onCompleted();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableEmptySecond() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList();

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onCompleted();

        verify(o, never()).onNext(any(String.class));
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testZipIterableFirstShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableSecondShorter() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onNext("three-");
        r1.onCompleted();

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testZipIterableFirstThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
        InOrder io = inOrder(o);

        Iterable<String> r2 = Arrays.asList("1", "2", "3");

        r1.zipWith(r2, zipr2).subscribe(o);

        r1.onNext("one-");
        r1.onNext("two-");
        r1.onError(new TestException());

        io.verify(o).onNext("one-1");
        io.verify(o).onNext("two-2");
        io.verify(o).onError(any(TestException.class));

        verify(o, never()).onCompleted();

    }

    @Test
    public void testZipIterableIteratorThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
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

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any(String.class));

    }

    @Test
    public void testZipIterableHasNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
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

        verify(o, never()).onCompleted();

    }

    @Test
    public void testZipIterableNextThrows() {
        PublishSubject<String> r1 = PublishSubject.create();
        /* define a Observer to receive aggregated events */
        @SuppressWarnings("unchecked")
        Observer<String> o = mock(Observer.class);
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
        verify(o, never()).onCompleted();

    }
    
    Action1<String> printer = new Action1<String>() {
        @Override
        public void call(String t1) {
            System.out.println(t1);
        }
    };

    static final class SquareStr implements Func1<Integer, String> {
        final AtomicInteger counter = new AtomicInteger();
        @Override
        public String call(Integer t1) {
            counter.incrementAndGet();
            System.out.println("Omg I'm calculating so hard: " + t1 + "*" + t1 + "=" + (t1*t1));
            return " " + (t1*t1);
        }
    }

    @Test public void testTake2() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4, 5);
        Iterable<String> it = Arrays.asList("a", "b", "c", "d", "e");
        
        SquareStr squareStr = new SquareStr();
        
        o.map(squareStr).zipWith(it, concat2Strings).take(2).subscribe(printer);
        
        assertEquals(2, squareStr.counter.get());
    }

    @Test
    public void testZipIterableWithDelay() {
        TestScheduler scheduler = new TestScheduler();
        Observable<Integer> o = Observable.just(1, 2).zipWith(Arrays.asList(1), new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer v1, Integer v2) {
                return v1;
            }
        }).delay(500, TimeUnit.MILLISECONDS, scheduler);

        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        o.subscribe(subscriber);
        scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        subscriber.assertValue(1);
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
    }
}
