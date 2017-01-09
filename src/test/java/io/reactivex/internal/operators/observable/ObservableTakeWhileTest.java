/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.*;

public class ObservableTakeWhileTest {

    @Test
    public void testTakeWhile1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Integer> take = w.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        Observer<Integer> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileOnSubject1() {
        Subject<Integer> s = PublishSubject.create();
        Observable<Integer> take = s.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        Observer<Integer> observer = TestHelper.mockObserver();
        take.subscribe(observer);

        s.onNext(1);
        s.onNext(2);
        s.onNext(3);
        s.onNext(4);
        s.onNext(5);
        s.onComplete();

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhile2() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> take = w.takeWhile(new Predicate<String>() {
            int index;

            @Override
            public boolean test(String input) {
                return index++ < 2;
            }
        });

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileDoesntLeakErrors() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
            }
        });

        source.takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return false;
            }
        }).blockingLast("");
    }

    @Test
    public void testTakeWhileProtectsPredicateCall() {
        TestObservable source = new TestObservable(mock(Disposable.class), "one");
        final RuntimeException testException = new RuntimeException("test exception");

        Observer<String> observer = TestHelper.mockObserver();
        Observable<String> take = Observable.unsafeCreate(source)
                .takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                throw testException;
            }
        });
        take.subscribe(observer);

        // wait for the Observable to complete
        try {
            source.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        verify(observer, never()).onNext(any(String.class));
        verify(observer, times(1)).onError(testException);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        Disposable s = mock(Disposable.class);
        TestObservable w = new TestObservable(s, "one", "two", "three");

        Observer<String> observer = TestHelper.mockObserver();
        Observable<String> take = Observable.unsafeCreate(w)
                .takeWhile(new Predicate<String>() {
            int index;

            @Override
            public boolean test(String s) {
                return index++ < 1;
            }
        });
        take.subscribe(observer);

        // wait for the Observable to complete
        try {
            w.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(s, times(1)).dispose();
    }

    private static class TestObservable implements ObservableSource<String> {

        final Disposable s;
        final String[] values;
        Thread t;

        TestObservable(Disposable s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onComplete();
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
        Observable<Integer> source = Observable.range(1, 1000).takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });
        TestObserver<Integer> ts = new TestObserver<Integer>();

        source.subscribe(ts);

        ts.assertNoErrors();
        ts.assertValue(1);

        // 2.0.2 - not anymore
//        Assert.assertTrue("Not cancelled!", ts.isCancelled());
    }

    @Test
    public void testErrorCauseIncludesLastValue() {
        TestObserver<String> ts = new TestObserver<String>();
        Observable.just("abc").takeWhile(new Predicate<String>() {
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

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().takeWhile(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.takeWhile(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void badSource() {
        new Observable<Integer>() {
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onComplete();
                observer.onComplete();
            }
        }
        .takeWhile(Functions.alwaysTrue())
        .test()
        .assertResult();
    }
}
