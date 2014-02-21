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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationOnErrorResumeNextViaObservable.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Func1;

public class OperationOnErrorResumeNextViaObservableTest {

    @Test
    public void testResumeNext() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "fail", "two", "three");
        Observable<String> w = Observable.create(f);
        Observable<String> resume = Observable.from("twoResume", "threeResume");
        Observable<String> observable = Observable.create(onErrorResumeNextViaObservable(w, resume));

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
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    @Test
    public void testMapResumeAsyncNext() {
        Subscription sr = mock(Subscription.class);
        // Trigger multiple failures
        Observable<String> w = Observable.from("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable f = new TestObservable(sr, "twoResume", "threeResume");
        Observable<String> resume = Observable.create(f);

        // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Func1<String, String>() {
            public String call(String s) {
                if ("fail".equals(s))
                    throw new RuntimeException("Forced Failure");
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        Observable<String> observable = Observable.create(onErrorResumeNextViaObservable(w, resume));

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
        verify(observer, Mockito.never()).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, times(1)).onNext("twoResume");
        verify(observer, times(1)).onNext("threeResume");
    }

    private static class TestObservable implements Observable.OnSubscribeFunc<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            if ("fail".equals(s))
                                throw new RuntimeException("Forced Failure");
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        System.out.println("TestObservable onCompleted");
                        observer.onCompleted();
                    } catch (Throwable e) {
                        System.out.println("TestObservable onError: " + e);
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
            return s;
        }
    }
}
