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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.Observable.NbpOnSubscribe;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorOnErrorReturnTest {

    @Test
    public void testResumeNext() {
        TestObservable f = new TestObservable("one");
        Observable<String> w = Observable.create(f);
        final AtomicReference<Throwable> capturedException = new AtomicReference<Throwable>();

        Observable<String> NbpObservable = w.onErrorReturn(new Function<Throwable, String>() {

            @Override
            public String apply(Throwable e) {
                capturedException.set(e);
                return "failure";
            }

        });

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("failure");
        assertNotNull(capturedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError
     */
    @Test
    public void testFunctionThrowsError() {
        TestObservable f = new TestObservable("one");
        Observable<String> w = Observable.create(f);
        final AtomicReference<Throwable> capturedException = new AtomicReference<Throwable>();

        Observable<String> NbpObservable = w.onErrorReturn(new Function<Throwable, String>() {

            @Override
            public String apply(Throwable e) {
                capturedException.set(e);
                throw new RuntimeException("exception from function");
            }

        });

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        NbpObservable.subscribe(NbpObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(NbpObserver, times(1)).onNext("one");

        // we should have received an onError call on the NbpObserver since the resume function threw an exception
        verify(NbpObserver, times(1)).onError(any(Throwable.class));
        verify(NbpObserver, times(0)).onComplete();
        assertNotNull(capturedException.get());
    }
    
    @Test
    public void testMapResumeAsyncNext() {
        // Trigger multiple failures
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");

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

        Observable<String> NbpObservable = w.onErrorReturn(new Function<Throwable, String>() {

            @Override
            public String apply(Throwable t1) {
                return "resume";
            }
            
        });

        @SuppressWarnings("unchecked")
        DefaultObserver<String> NbpObserver = mock(DefaultObserver.class);
        TestObserver<String> ts = new TestObserver<String>(NbpObserver);
        NbpObservable.subscribe(ts);
        ts.awaitTerminalEvent();

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, Mockito.never()).onNext("two");
        verify(NbpObserver, Mockito.never()).onNext("three");
        verify(NbpObserver, times(1)).onNext("resume");
    }
    
    @Test
    public void testBackpressure() {
        TestObserver<Integer> ts = new TestObserver<Integer>();
        Observable.range(0, 100000)
                .onErrorReturn(new Function<Throwable, Integer>() {

                    @Override
                    public Integer apply(Throwable t1) {
                        return 1;
                    }
                    
                })
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
        public void accept(final Observer<? super String> NbpSubscriber) {
            NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            NbpSubscriber.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        NbpSubscriber.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }
    
    
    
}