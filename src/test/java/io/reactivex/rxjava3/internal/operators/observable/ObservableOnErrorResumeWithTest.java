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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableOnErrorResumeWithTest extends RxJavaTest {

    @Test
    public void resumeNext() {
        Disposable upstream = mock(Disposable.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(upstream, "one", "fail", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> observable = w.onErrorResumeWith(resume);

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
    public void mapResumeAsyncNext() {
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

        Observable<String> observable = w.onErrorResumeWith(resume);

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

    static class TestObservable implements ObservableSource<String> {

        final Disposable upstream;
        final String[] values;
        Thread t;

        TestObservable(Disposable upstream, String... values) {
            this.upstream = upstream;
            this.values = values;
        }

        @Override
        public void subscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.onSubscribe(upstream);
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
    public void backpressure() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(0, 100000)
                .onErrorResumeWith(Observable.just(1))
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
                .subscribe(to);
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
    }
}
