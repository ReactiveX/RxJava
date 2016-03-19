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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.Observable;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.exceptions.*;
import io.reactivex.flowable.TestHelper;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.*;

public class NbpOperatorMergeDelayErrorTest {

    Observer<String> stringObserver;

    @Before
    public void before() {
        stringObserver = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testErrorDelayed1() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", "three"));

        Observable<String> m = Observable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source NbpObservable keeps sending after onError
        // inner NbpObservable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
        // inner NbpObservable errors are considered terminal for that source
    }

    @Test
    public void testErrorDelayed2() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
        final Observable<String> o2 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));
        final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));

        Observable<String> m = Observable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source NbpObservable keeps sending after onError
        // inner NbpObservable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed3() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
        final Observable<String> o2 = Observable.create(new TestErrorObservable("four", "five", "six"));
        final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));
        final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));

        Observable<String> m = Observable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed4() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
        final Observable<String> o2 = Observable.create(new TestErrorObservable("four", "five", "six"));
        final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight"));
        final Observable<String> o4 = Observable.create(new TestErrorObservable("nine", null));

        Observable<String> m = Observable.mergeDelayError(o1, o2, o3, o4);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
    }

    @Test
    public void testErrorDelayed4WithThreading() {
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");
        final TestAsyncErrorObservable o2 = new TestAsyncErrorObservable("four", "five", "six");
        final TestAsyncErrorObservable o3 = new TestAsyncErrorObservable("seven", "eight");
        // throw the error at the very end so no onComplete will be called after it
        final TestAsyncErrorObservable o4 = new TestAsyncErrorObservable("nine", null);

        Observable<String> m = Observable.mergeDelayError(Observable.create(o1), Observable.create(o2), Observable.create(o3), Observable.create(o4));
        m.subscribe(stringObserver);

        try {
            o1.t.join();
            o2.t.join();
            o3.t.join();
            o4.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(1)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(1)).onNext("five");
        verify(stringObserver, times(1)).onNext("six");
        verify(stringObserver, times(1)).onNext("seven");
        verify(stringObserver, times(1)).onNext("eight");
        verify(stringObserver, times(1)).onNext("nine");
        verify(stringObserver, times(1)).onError(any(NullPointerException.class));
        verify(stringObserver, never()).onComplete();
    }

    @Test
    public void testCompositeErrorDelayed1() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", null));

        Observable<String> m = Observable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, times(1)).onNext("two");
        verify(stringObserver, times(0)).onNext("three");
        verify(stringObserver, times(1)).onNext("four");
        verify(stringObserver, times(0)).onNext("five");
        // despite not expecting it ... we don't do anything to prevent it if the source NbpObservable keeps sending after onError
        // inner NbpObservable errors are considered terminal for that source
//        verify(stringObserver, times(1)).onNext("six");
    }

    @Test
    public void testCompositeErrorDelayed2() {
        final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
        final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", null));

        Observable<String> m = Observable.mergeDelayError(o1, o2);
        CaptureObserver w = new CaptureObserver();
        m.subscribe(w);

        assertNotNull(w.e);
        
        assertEquals(2, ((CompositeException)w.e).size());
        
//        if (w.e instanceof CompositeException) {
//            assertEquals(2, ((CompositeException) w.e).getExceptions().size());
//            w.e.printStackTrace();
//        } else {
//            fail("Expecting CompositeException");
//        }

    }

    /**
     * The unit tests below are from OperationMerge and should ensure the normal merge functionality is correct.
     */

    @Test
    public void testMergeObservableOfObservables() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

        Observable<Observable<String>> observableOfObservables = Observable.create(new NbpOnSubscribe<Observable<String>>() {

            @Override
            public void accept(Observer<? super Observable<String>> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                // simulate what would happen in an NbpObservable
                NbpObserver.onNext(o1);
                NbpObserver.onNext(o2);
                NbpObserver.onComplete();
            }

        });
        Observable<String> m = Observable.mergeDelayError(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArray() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

        Observable<String> m = Observable.mergeDelayError(o1, o2);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMergeList() {
        final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
        final Observable<String> o2 = Observable.create(new TestSynchronousObservable());
        List<Observable<String>> listOfObservables = new ArrayList<Observable<String>>();
        listOfObservables.add(o1);
        listOfObservables.add(o2);

        Observable<String> m = Observable.mergeDelayError(Observable.fromIterable(listOfObservables));
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onComplete();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeArrayWithThreading() {
        final TestASynchronousObservable o1 = new TestASynchronousObservable();
        final TestASynchronousObservable o2 = new TestASynchronousObservable();

        Observable<String> m = Observable.mergeDelayError(Observable.create(o1), Observable.create(o2));
        m.subscribe(stringObserver);

        try {
            o1.t.join();
            o2.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(2)).onNext("hello");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test(timeout = 1000L)
    public void testSynchronousError() {
        final Observable<Observable<String>> o1 = Observable.error(new RuntimeException("unit test"));

        final CountDownLatch latch = new CountDownLatch(1);
        Observable.mergeDelayError(o1).subscribe(new DefaultObserver<String>() {
            @Override
            public void onComplete() {
                fail("Expected onError path");
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }

            @Override
            public void onNext(String s) {
                fail("Expected onError path");
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            fail("interrupted");
        }
    }

    private static class TestSynchronousObservable implements NbpOnSubscribe<String> {

        @Override
        public void accept(Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            NbpObserver.onNext("hello");
            NbpObserver.onComplete();
        }
    }

    private static class TestASynchronousObservable implements NbpOnSubscribe<String> {
        Thread t;

        @Override
        public void accept(final Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    NbpObserver.onNext("hello");
                    NbpObserver.onComplete();
                }

            });
            t.start();
        }
    }

    private static class TestErrorObservable implements NbpOnSubscribe<String> {

        String[] valuesToReturn;

        TestErrorObservable(String... values) {
            valuesToReturn = values;
        }

        @Override
        public void accept(Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            boolean errorThrown = false;
            for (String s : valuesToReturn) {
                if (s == null) {
                    System.out.println("throwing exception");
                    NbpObserver.onError(new NullPointerException());
                    errorThrown = true;
                    // purposefully not returning here so it will continue calling onNext
                    // so that we also test that we handle bad sequences like this
                } else {
                    NbpObserver.onNext(s);
                }
            }
            if (!errorThrown) {
                NbpObserver.onComplete();
            }
        }
    }

    private static class TestAsyncErrorObservable implements NbpOnSubscribe<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        Thread t;

        @Override
        public void accept(final Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {

                            }
                            NbpObserver.onError(new NullPointerException());
                            return;
                        } else {
                            NbpObserver.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    NbpObserver.onComplete();
                }

            });
            t.start();
        }
    }

    private static class CaptureObserver extends DefaultObserver<String> {
        volatile Throwable e;

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
            this.e = e;
        }

        @Override
        public void onNext(String args) {

        }

    }
    @Test
    @Ignore("Subscribers should not throw")
    public void testMergeSourceWhichDoesntPropagateExceptionBack() {
        Observable<Integer> source = Observable.create(new NbpOnSubscribe<Integer>() {
            @Override
            public void accept(Observer<? super Integer> t1) {
                t1.onSubscribe(EmptyDisposable.INSTANCE);
                try {
                    t1.onNext(0);
                } catch (Throwable swallow) {
                    
                }
                t1.onNext(1);
                t1.onComplete();
            }
        });
        
        Observable<Integer> result = Observable.mergeDelayError(source, Observable.just(2));
        
        final Observer<Integer> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);
        
        result.unsafeSubscribe(new DefaultObserver<Integer>() {
            int calls;
            @Override
            public void onNext(Integer t) {
                if (calls++ == 0) {
                    throw new TestException();
                }
                o.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onComplete() {
                o.onComplete();
            }
            
        });
        
        /*
         * If the child onNext throws, why would we keep accepting values from
         * other sources?
         */
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(0);
        inOrder.verify(o, never()).onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testErrorInParentObservable() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.mergeDelayError(
                Observable.just(Observable.just(1), Observable.just(2))
                        .startWith(Observable.<Integer> error(new RuntimeException()))
                ).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertTerminated();
        ts.assertValues(1, 2);
        assertEquals(1, ts.errorCount());

    }

    @Test
    public void testErrorInParentObservableDelayed() throws Exception {
        for (int i = 0; i < 50; i++) {
            final TestASynchronous1sDelayedObservable o1 = new TestASynchronous1sDelayedObservable();
            final TestASynchronous1sDelayedObservable o2 = new TestASynchronous1sDelayedObservable();
            Observable<Observable<String>> parentObservable = Observable.create(new NbpOnSubscribe<Observable<String>>() {
                @Override
                public void accept(Observer<? super Observable<String>> op) {
                    op.onSubscribe(EmptyDisposable.INSTANCE);
                    op.onNext(Observable.create(o1));
                    op.onNext(Observable.create(o2));
                    op.onError(new NullPointerException("throwing exception in parent"));
                }
            });
    
            Observer<String> stringObserver = TestHelper.mockNbpSubscriber();
            
            TestObserver<String> ts = new TestObserver<String>(stringObserver);
            Observable<String> m = Observable.mergeDelayError(parentObservable);
            m.subscribe(ts);
            System.out.println("testErrorInParentObservableDelayed | " + i);
            ts.awaitTerminalEvent(2000, TimeUnit.MILLISECONDS);
            ts.assertTerminated();
    
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onComplete();
        }
    }

    private static class TestASynchronous1sDelayedObservable implements NbpOnSubscribe<String> {
        Thread t;

        @Override
        public void accept(final Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        NbpObserver.onError(e);
                    }
                    NbpObserver.onNext("hello");
                    NbpObserver.onComplete();
                }

            });
            t.start();
        }
    }
}