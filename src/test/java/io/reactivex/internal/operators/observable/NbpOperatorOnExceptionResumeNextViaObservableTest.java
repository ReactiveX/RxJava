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

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorOnExceptionResumeNextViaObservableTest {

    @Test
    public void testResumeNextWithException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "EXCEPTION", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> NbpObservable = w.onExceptionResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testResumeNextWithRuntimeException() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "RUNTIMEEXCEPTION", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> NbpObservable = w.onExceptionResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testThrowablePassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "THROWABLE", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> NbpObservable = w.onExceptionResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onNext("twoResume");
        verify(NbpObserver, never()).onNext("threeResume");
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testErrorPassesThru() {
        // Trigger failure on second element
        TestObservable f = new TestObservable("one", "ERROR", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.just("twoResume", "threeResume");
        Observable<String> NbpObservable = w.onExceptionResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onNext("twoResume");
        verify(NbpObserver, never()).onNext("threeResume");
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testMapResumeAsyncNext() {
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        // Resume NbpObservable is async
        TestObservable f = new TestObservable("twoResume", "threeResume");
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

        Observable<String> NbpObservable = w.onExceptionResumeNext(resume);

        Observer<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        try {
            // if the thread gets started (which it shouldn't if it's working correctly)
            if (f.t != null) {
                f.t.join();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("twoResume");
        verify(NbpObserver, times(1)).onNext("threeResume");
        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }
    
    
    @Test
    public void testBackpressure() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(0, 100000)
                .onExceptionResumeNext(Observable.just(1))
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


    private static class TestObservable implements NbpOnSubscribe<String> {

        final String[] values;
        Thread t = null;

        public TestObservable(String... values) {
            this.values = values;
        }

        @Override
        public void accept(final Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("EXCEPTION".equals(s))
                                throw new Exception("Forced Exception");
                            else if ("RUNTIMEEXCEPTION".equals(s))
                                throw new RuntimeException("Forced RuntimeException");
                            else if ("ERROR".equals(s))
                                throw new Error("Forced Error");
                            else if ("THROWABLE".equals(s))
                                throw new Throwable("Forced Throwable");
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
}