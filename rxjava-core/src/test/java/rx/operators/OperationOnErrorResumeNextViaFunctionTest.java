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
import static rx.operators.OperationOnErrorResumeNextViaFunction.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

public class OperationOnErrorResumeNextViaFunctionTest {

    @Test
    public void testResumeNextWithSynchronousExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Observable<String> w = Observable.create(new Observable.OnSubscribeFunc<String>() {

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onError(new Throwable("injected failure"));
                return Subscriptions.empty();
            }
        });

        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                receivedException.set(t1);
                return Observable.from("twoResume", "threeResume");
            }

        };
        Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(w, resume));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, times(1)).onNext("twoResume");
        verify(aObserver, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    @Test
    public void testResumeNextWithAsyncExecution() {
        final AtomicReference<Throwable> receivedException = new AtomicReference<Throwable>();
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one");
        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                receivedException.set(t1);
                return Observable.from("twoResume", "threeResume");
            }

        };
        Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(Observable.create(w), resume));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, times(1)).onNext("twoResume");
        verify(aObserver, times(1)).onNext("threeResume");
        assertNotNull(receivedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError
     */
    @Test
    public void testFunctionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one");
        Func1<Throwable, Observable<String>> resume = new Func1<Throwable, Observable<String>>() {

            @Override
            public Observable<String> call(Throwable t1) {
                throw new RuntimeException("exception from function");
            }

        };
        Observable<String> observable = Observable.create(onErrorResumeNextViaFunction(Observable.create(w), resume));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            w.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(aObserver, times(1)).onNext("one");

        // we should have received an onError call on the Observer since the resume function threw an exception
        verify(aObserver, times(1)).onError(any(Throwable.class));
        verify(aObserver, times(0)).onCompleted();
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
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
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
