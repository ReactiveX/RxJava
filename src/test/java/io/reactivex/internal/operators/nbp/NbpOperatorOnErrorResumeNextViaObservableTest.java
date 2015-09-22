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

import java.util.function.Function;

import org.junit.*;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorOnErrorResumeNextViaObservableTest {

    @Test
    public void testResumeNext() {
        Disposable s = mock(Disposable.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        NbpObservable<String> w = NbpObservable.create(f);
        NbpObservable<String> resume = NbpObservable.just("twoResume", "threeResume");
        NbpObservable<String> NbpObservable = w.onErrorResumeNext(resume);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
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
        NbpObservable<String> w = NbpObservable.just("one", "fail", "two", "three", "fail");
        // Resume NbpObservable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
        NbpObservable<String> resume = NbpObservable.create(f);

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

        NbpObservable<String> NbpObservable = w.onErrorResumeNext(resume);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
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
        NbpObservable<String> testObservable = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        NbpObservable<String> resume = NbpObservable.just("resume");
        NbpObservable<String> NbpObservable = testObservable.onErrorResumeNext(resume);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        NbpObservable.subscribe(NbpObserver);

        verify(NbpObserver, Mockito.never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, times(1)).onNext("resume");
    }
    
    @Test
    @Ignore("Publishers should not throw")
    public void testResumeNextWithFailureOnSubscribeAsync() {
        NbpObservable<String> testObservable = NbpObservable.create(new NbpOnSubscribe<String>() {

            @Override
            public void accept(NbpSubscriber<? super String> t1) {
                throw new RuntimeException("force failure");
            }
            
        });
        NbpObservable<String> resume = NbpObservable.just("resume");
        NbpObservable<String> NbpObservable = testObservable.subscribeOn(Schedulers.io()).onErrorResumeNext(resume);

        @SuppressWarnings("unchecked")
        NbpObserver<String> NbpObserver = mock(NbpObserver.class);
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>(NbpObserver);
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
        public void accept(final NbpSubscriber<? super String> NbpObserver) {
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
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(0, 100000)
                .onErrorResumeNext(NbpObservable.just(1))
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