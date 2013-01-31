/**
 * Copyright 2013 Netflix, Inc.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.SynchronizedObserver;
import rx.util.functions.Func1;

/**
 * An observable that wraps an observable of the same type and then enforces the semantics
 * expected of a well-behaved observable.
 * <p>
 * An observable that ensures onNext, onCompleted, or onError calls on its subscribers are
 * not interleaved, onCompleted and onError are only called once respectively, and no
 * onNext calls follow onCompleted and onError calls.
 * <p>
 * NOTE: {@link Observable#create} already wraps Observables so this is generally redundant.
 * 
 * @param <T>
 *            The type of the observable sequence.
 */
public final class OperationSynchronize<T> {

    /**
     * Accepts an observable and wraps it in another observable which ensures that the resulting observable is well-behaved.
     * 
     * A well-behaved observable ensures onNext, onCompleted, or onError calls to its subscribers are
     * not interleaved, onCompleted and onError are only called once respectively, and no
     * onNext calls follow onCompleted and onError calls.
     * 
     * @param observable
     * @param <T>
     * @return
     */
    public static <T> Func1<Observer<T>, Subscription> synchronize(Observable<T> observable) {
        return new Synchronize<T>(observable);
    }

    private static class Synchronize<T> implements Func1<Observer<T>, Subscription> {

        public Synchronize(Observable<T> innerObservable) {
            this.innerObservable = innerObservable;
        }

        private Observable<T> innerObservable;
        private SynchronizedObserver<T> atomicObserver;

        public Subscription call(Observer<T> observer) {
            AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            atomicObserver = new SynchronizedObserver<T>(observer, subscription);
            return subscription.wrap(innerObservable.subscribe(atomicObserver));
        }

    }

    public static class UnitTest {

        /**
         * Ensure onCompleted can not be called after an Unsubscribe
         */
        @Test
        public void testOnCompletedAfterUnSubscribe() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            ws.unsubscribe();
            t.sendOnCompleted();

            verify(w, times(1)).onNext("one");
            verify(w, Mockito.never()).onCompleted();
        }

        /**
         * Ensure onNext can not be called after an Unsubscribe
         */
        @Test
        public void testOnNextAfterUnSubscribe() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            ws.unsubscribe();
            t.sendOnNext("two");

            verify(w, times(1)).onNext("one");
            verify(w, Mockito.never()).onNext("two");
        }

        /**
         * Ensure onError can not be called after an Unsubscribe
         */
        @Test
        public void testOnErrorAfterUnSubscribe() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            ws.unsubscribe();
            t.sendOnError(new RuntimeException("bad"));

            verify(w, times(1)).onNext("one");
            verify(w, Mockito.never()).onError(any(Exception.class));
        }

        /**
         * Ensure onNext can not be called after onError
         */
        @Test
        public void testOnNextAfterOnError() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnError(new RuntimeException("bad"));
            t.sendOnNext("two");

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onError(any(Exception.class));
            verify(w, Mockito.never()).onNext("two");
        }

        /**
         * Ensure onCompleted can not be called after onError
         */
        @Test
        public void testOnCompletedAfterOnError() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnError(new RuntimeException("bad"));
            t.sendOnCompleted();

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onError(any(Exception.class));
            verify(w, Mockito.never()).onCompleted();
        }

        /**
         * Ensure onNext can not be called after onCompleted
         */
        @Test
        public void testOnNextAfterOnCompleted() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnCompleted();
            t.sendOnNext("two");

            verify(w, times(1)).onNext("one");
            verify(w, Mockito.never()).onNext("two");
            verify(w, times(1)).onCompleted();
            verify(w, Mockito.never()).onError(any(Exception.class));
        }

        /**
         * Ensure onError can not be called after onCompleted
         */
        @Test
        public void testOnErrorAfterOnCompleted() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(t));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnCompleted();
            t.sendOnError(new RuntimeException("bad"));

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onCompleted();
            verify(w, Mockito.never()).onError(any(Exception.class));
        }

        /**
         * A Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
         */
        private static class TestObservable extends Observable<String> {

            Observer<String> observer = null;

            public TestObservable(Subscription s) {
            }

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // going to do nothing to pretend I'm a bad Observable that keeps allowing events to be sent
                    }

                };
            }

        }
    }

}