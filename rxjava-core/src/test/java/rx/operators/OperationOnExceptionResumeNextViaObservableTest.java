/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import static rx.operators.OperationOnExceptionResumeNextViaObservable.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.IObservable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public class OperationOnExceptionResumeNextViaObservableTest {

    @Test
    public void testResumeNextWithException() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "EXCEPTION", "two", "three");
        Observable<String> resume = Observable.from("twoResume", "threeResume");
        IObservable<String> observable = onExceptionResumeNextViaObservable(f, resume);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, times(1)).onNext("twoResume");
        verify(aObserver, times(1)).onNext("threeResume");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testResumeNextWithRuntimeException() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "RUNTIMEEXCEPTION", "two", "three");
        Observable<String> resume = Observable.from("twoResume", "threeResume");
        IObservable<String> observable = onExceptionResumeNextViaObservable(f, resume);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, Mockito.never()).onNext("two");
        verify(aObserver, Mockito.never()).onNext("three");
        verify(aObserver, times(1)).onNext("twoResume");
        verify(aObserver, times(1)).onNext("threeResume");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testThrowablePassesThru() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "THROWABLE", "two", "three");
        Observable<String> resume = Observable.from("twoResume", "threeResume");
        IObservable<String> observable = onExceptionResumeNextViaObservable(f, resume);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, never()).onNext("twoResume");
        verify(aObserver, never()).onNext("threeResume");
        verify(aObserver, times(1)).onError(any(Throwable.class));
        verify(aObserver, never()).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testErrorPassesThru() {
        Subscription s = mock(Subscription.class);
        // Trigger failure on second element
        TestObservable f = new TestObservable(s, "one", "ERROR", "two", "three");
        Observable<String> resume = Observable.from("twoResume", "threeResume");
        IObservable<String> observable = onExceptionResumeNextViaObservable(f, resume);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, never()).onNext("twoResume");
        verify(aObserver, never()).onNext("threeResume");
        verify(aObserver, times(1)).onError(any(Throwable.class));
        verify(aObserver, never()).onCompleted();
        verifyNoMoreInteractions(aObserver);
    }

    @Test
    public void testMapResumeAsyncNext() {
        Subscription sr = mock(Subscription.class);
        // Trigger multiple failures
        Observable<String> w = Observable.from("one", "fail", "two", "three", "fail");
        // Resume Observable is async
        TestObservable resume = new TestObservable(sr, "twoResume", "threeResume");

        // Introduce map function that fails intermittently (Map does not prevent this when the observer is a
        //  rx.operator incl onErrorResumeNextViaObservable)
        w = w.map(new Func1<String, String>() {
            @Override
            public String call(String s) {
                if ("fail".equals(s))
                    throw new RuntimeException("Forced Failure");
                System.out.println("BadMapper:" + s);
                return s;
            }
        });

        IObservable<String> observable = onExceptionResumeNextViaObservable(w, resume);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            // if the thread gets started (which it shouldn't if it's working correctly)
            if (resume.t != null) {
                resume.t.join();
            }
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, never()).onNext("two");
        verify(aObserver, never()).onNext("three");
        verify(aObserver, times(1)).onNext("twoResume");
        verify(aObserver, times(1)).onNext("threeResume");
        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
    }

    private static class TestObservable implements IObservable<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public Subscription subscribe(final Observer<? super String> observer) {
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
