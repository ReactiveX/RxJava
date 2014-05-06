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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

public class OperatorOnErrorReturnTest {

    @Test
    public void testResumeNext() {
        TestObservable f = new TestObservable("one");
        Observable<String> w = Observable.create(f);
        final AtomicReference<Throwable> capturedException = new AtomicReference<Throwable>();

        Observable<String> observable = w.onErrorReturn(new Func1<Throwable, String>() {

            @Override
            public String call(Throwable e) {
                capturedException.set(e);
                return "failure";
            }

        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("failure");
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

        Observable<String> observable = w.onErrorReturn(new Func1<Throwable, String>() {

            @Override
            public String call(Throwable e) {
                capturedException.set(e);
                throw new RuntimeException("exception from function");
            }

        });

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        observable.subscribe(observer);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(observer, times(1)).onNext("one");

        // we should have received an onError call on the Observer since the resume function threw an exception
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, times(0)).onCompleted();
        assertNotNull(capturedException.get());
    }

    private static class TestObservable implements Observable.OnSubscribe<String> {

        final String[] values;
        Thread t = null;

        public TestObservable(String... values) {
            this.values = values;
        }

        @Override
        public void call(final Subscriber<? super String> subscriber) {
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            subscriber.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }
}
