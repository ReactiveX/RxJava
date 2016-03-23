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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorOnErrorResumeNextViaObservableTest {

    @Test
    public void testResumeNext() {
        Disposable s = mock(Disposable.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> NbpObservable = w.onErrorResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
    }

    @Test
    public void testMapResumeAsyncNext() {
        Disposable sr = mock(Disposable.class);
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        // Resume NbpObservable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
        Observable<String> resume = Observable.create(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the NbpObserver is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s))
                    throw new RuntimeException("Forced Failure");
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Observable<String> NbpObservable = w.onErrorResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
    }
    
    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribe() {
        Observable<String> testObservable = Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(Observer<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> NbpObservable = testObservable.onErrorResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("resume");
    }
    
    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribeAsync() {
        Observable<String> testObservable = Observable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(Observer<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        Observable<String> resume = Observable.just("resume");
        Observable<String> NbpObservable = testObservable.subscribeOn(Schedulers.io()).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        NbpObservable.subscribe(ts);

        ts.awaitTerminalEvent();
        
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("resume");
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
        public void accept(final Observer<? super String> NbpObserver) {
            System.out.println("TestObservable subscribed to ...");
            NbpObserver.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("fail".equals(s))
                                throw new RuntimeException("Forced Failure");
                            System.out.println("TestObservable onNext: " + s);
                            NbpObserver.onNext(s);
                        }
                        System.out.println("TestObservable onCompleted");
                        NbpObserver.onComplete();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        NbpObserver.onError(e);
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
                    int c = 0;

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