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
package rx.operators;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

public class OperatorTakeWhileTest {

    @Test
    public void testTakeWhile1() {
        Observable<Integer> w = Observable.from(1, 2, 3);
        Observable<Integer> take = w.takeWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer input) {
                return input < 3;
            }
        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeWhileOnSubject1() {
        Subject<Integer, Integer> s = PublishSubject.create();
        Observable<Integer> take = s.takeWhile(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer input) {
                return input < 3;
            }
        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        take.subscribe(observer);

        s.onNext(1);
        s.onNext(2);
        s.onNext(3);
        s.onNext(4);
        s.onNext(5);
        s.onCompleted();

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeWhile2() {
        Observable<String> w = Observable.from("one", "two", "three");
        Observable<String> take = w.takeWhileWithIndex(new Func2<String, Integer, Boolean>() {
            @Override
            public Boolean call(String input, Integer index) {
                return index < 2;
            }
        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testTakeWhileDoesntLeakErrors() {
        Observable<String> source = Observable.create(new OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> observer) {
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
            }
        });

        source.takeWhile(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
                return false;
            }
        }).toBlocking().lastOrDefault("");
    }

    @Test
    public void testTakeWhileProtectsPredicateCall() {
        TestObservable source = new TestObservable(mock(Subscription.class), "one");
        final RuntimeException testException = new RuntimeException("test exception");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> take = Observable.create(source).takeWhile(new Func1<String, Boolean>() {
            @Override
            public Boolean call(String s) {
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
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one", "two", "three");

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> take = Observable.create(w).takeWhileWithIndex(new Func2<String, Integer, Boolean>() {
            @Override
            public Boolean call(String s, Integer index) {
                return index < 1;
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
        verify(s, times(1)).unsubscribe();
    }

    private static class TestObservable implements Observable.OnSubscribe<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void call(final Subscriber<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.add(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onCompleted();
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
}
