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
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class ObservableOnErrorResumeNextViaObservableTest {

    @Test
    public void testResumeNext() {
        Disposable s = mock(Disposable.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onErrorResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    @Test
    public void testMapResumeAsyncNext() {
        Disposable sr = mock(Disposable.class);
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
        Observable<String> resume = Observable.unsafeCreate(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the Observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Observable<String> observable = w.onErrorResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("one");
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribe() {
        Observable<String> testObservable = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(Observer<? super String> t1) {
                throw new RuntimeException("force failure");
            }

        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> observable = testObservable.onErrorResumeNext(resume);

        Observer<String> observer = TestHelper.mockObserver();
        observable.subscribe(observer);

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("resume");
    }

    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribeAsync() {
        Observable<String> testObservable = Observable.unsafeCreate(new ObservableSource<String>() {

            @Override
            public void subscribe(Observer<? super String> t1) {
                throw new RuntimeException("force failure");
            }

        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> observable = testObservable.subscribeOn(Schedulers.io()).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        DefaultObserver<String> observer = mock(DefaultObserver.class);
        TestObserver<String> ts = new TestObserver<String>(observer);
        observable.subscribe(ts);

        ts.awaitTerminalEvent();

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(1)).onNext("resume");
    }

    static class TestObservable implements ObservableSource<String> {

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
                            if ("fail".equals(s)) {
                                throw new RuntimeException("Forced Failure");
                            }
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        System.out.println("TestObservable onComplete");
                        observer.onComplete();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }

    @Test
    public void testBackpressure() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(0, 100000)
                .onErrorResumeNext(Observable.just(1))
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    int c;

                    @Override
                    public Integer apply(Integer t1) {
                        if (c++ <= 1) {
                            // slow
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        return t1;
                    }

                })
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }
}
