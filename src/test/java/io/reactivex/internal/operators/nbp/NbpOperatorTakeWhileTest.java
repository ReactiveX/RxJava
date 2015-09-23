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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Predicate;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.subjects.nbp.*;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorTakeWhileTest {

    @Test
    public void testTakeWhile1() {
        NbpObservable<Integer> w = NbpObservable.just(1, 2, 3);
        NbpObservable<Integer> take = w.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, never()).onNext(3);
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileOnSubject1() {
        NbpSubject<Integer, Integer> s = NbpPublishSubject.create();
        NbpObservable<Integer> take = s.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);

        s.onNext(1);
        s.onNext(2);
        s.onNext(3);
        s.onNext(4);
        s.onNext(5);
        s.onComplete();

        verify(NbpObserver, times(1)).onNext(1);
        verify(NbpObserver, times(1)).onNext(2);
        verify(NbpObserver, never()).onNext(3);
        verify(NbpObserver, never()).onNext(4);
        verify(NbpObserver, never()).onNext(5);
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeWhile2() {
        NbpObservable<String> w = NbpObservable.just("one", "two", "three");
        NbpObservable<String> take = w.takeWhile(new Predicate<String>() {
            int index = 0;

            @Override
            public boolean test(String input) {
                return index++ < 2;
            }
        });

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileDoesntLeakErrors() {
        NbpObservable<String> source = NbpObservable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                NbpObserver.onNext("one");
                NbpObserver.onError(new Throwable("test failed"));
            }
        });

        source.takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return false;
            }
        }).toBlocking().last("");
    }

    @Test
    public void testTakeWhileProtectsPredicateCall() {
        TestObservable source = new TestObservable(mock(Disposable.class), "one");
        final RuntimeException testException = new RuntimeException("test exception");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> take = NbpObservable.create(source)
                .takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                throw testException;
            }
        });
        take.subscribe(NbpObserver);

        // wait for the NbpObservable to complete
        try {
            source.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        verify(NbpObserver, never()).onNext(any(String.class));
        verify(NbpObserver, times(1)).onError(testException);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        Disposable s = mock(Disposable.class);
        TestObservable w = new TestObservable(s, "one", "two", "three");

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable<String> take = NbpObservable.create(w)
                .takeWhile(new Predicate<String>() {
            int index = 0;

            @Override
            public boolean test(String s) {
                return index++ < 1;
            }
        });
        take.subscribe(NbpObserver);

        // wait for the NbpObservable to complete
        try {
            w.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(s, times(1)).dispose();
    }

    private static class TestObservable implements NbpOnSubscribe<String> {

        final Disposable s;
        final String[] values;
        Thread t = null;

        public TestObservable(Disposable s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void accept(final NbpSubscriber<? super String> NbpObserver) {
            System.out.println("TestObservable subscribed to ...");
            NbpObserver.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            NbpObserver.onNext(s);
                        }
                        NbpObserver.onComplete();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }
    
    @Test
    public void testNoUnsubscribeDownstream() {
        NbpObservable<Integer> source = NbpObservable.range(1, 1000).takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.unsafeSubscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValue(1);
        
        Assert.assertFalse("Unsubscribed!", ts.isCancelled());
    }
    
    @Test
    public void testErrorCauseIncludesLastValue() {
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        NbpObservable.just("abc").takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String t1) {
                throw new TestException();
            }
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertError(TestException.class);
        // FIXME last cause value not recorded
//        assertTrue(ts.getOnErrorEvents().get(0).getCause().getMessage().contains("abc"));
    }
    
}