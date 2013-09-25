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
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Wraps an Observable in another Observable that ensures that the resulting Observable is
 * chronologically well-behaved.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/synchronize.png">
 * <p>
 * A well-behaved Observable does not interleave its invocations of the <code>onNext</code>,
 * <code>onCompleted</code>, and <code>onError</code> methods of its Observers; it invokes
 * <code>onCompleted</code> or <code>onError</code> only once; and it never invokes
 * <code>onNext</code> after invoking either <code>onCompleted</code> or <code>onError</code>. The
 * synchronize operation enforces this, and the Observable it returns invokes <code>onNext</code>
 * and <code>onCompleted</code> or <code>onError</code> synchronously.
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
     * @return the wrapped synchronized observable sequence
     */
    public static <T> OnSubscribeFunc<T> synchronize(Observable<? extends T> observable) {
        return new Synchronize<T>(observable, null);
    }

    /**
     * Accepts an observable and wraps it in another observable which ensures that the resulting observable is well-behaved.
     * This is accomplished by acquiring a mutual-exclusion lock for the object provided as the lock parameter.
     *
     * A well-behaved observable ensures onNext, onCompleted, or onError calls to its subscribers are
     * not interleaved, onCompleted and onError are only called once respectively, and no
     * onNext calls follow onCompleted and onError calls.
     *
     * @param observable
     * @param lock
     *            The lock object to synchronize each observer call on
     * @param <T>
     * @return the wrapped synchronized observable sequence
     */
    public static <T> OnSubscribeFunc<T> synchronize(Observable<? extends T> observable, Object lock) {
        return new Synchronize<T>(observable, lock);
    }

    private static class Synchronize<T> implements OnSubscribeFunc<T> {

        public Synchronize(Observable<? extends T> innerObservable, Object lock) {
            this.innerObservable = innerObservable;
            this.lock = lock;
        }

        private Observable<? extends T> innerObservable;
        private SynchronizedObserver<T> atomicObserver;
        private Object lock;

        public Subscription onSubscribe(Observer<? super T> observer) {
            SafeObservableSubscription subscription = new SafeObservableSubscription();
            if(lock == null) {
                atomicObserver = new SynchronizedObserver<T>(observer, subscription);
            }
            else {
                atomicObserver = new SynchronizedObserver<T>(observer, subscription, lock);
            }
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
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

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
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

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
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            ws.unsubscribe();
            t.sendOnError(new RuntimeException("bad"));

            verify(w, times(1)).onNext("one");
            verify(w, Mockito.never()).onError(any(Throwable.class));
        }

        /**
         * Ensure onNext can not be called after onError
         */
        @Test
        public void testOnNextAfterOnError() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnError(new RuntimeException("bad"));
            t.sendOnNext("two");

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onError(any(Throwable.class));
            verify(w, Mockito.never()).onNext("two");
        }

        /**
         * Ensure onCompleted can not be called after onError
         */
        @Test
        public void testOnCompletedAfterOnError() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnError(new RuntimeException("bad"));
            t.sendOnCompleted();

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onError(any(Throwable.class));
            verify(w, Mockito.never()).onCompleted();
        }

        /**
         * Ensure onNext can not be called after onCompleted
         */
        @Test
        public void testOnNextAfterOnCompleted() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

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
            verify(w, Mockito.never()).onError(any(Throwable.class));
        }

        /**
         * Ensure onError can not be called after onCompleted
         */
        @Test
        public void testOnErrorAfterOnCompleted() {
            TestObservable t = new TestObservable(null);
            Observable<String> st = Observable.create(synchronize(Observable.create(t)));

            @SuppressWarnings("unchecked")
            Observer<String> w = mock(Observer.class);
            @SuppressWarnings("unused")
            Subscription ws = st.subscribe(w);

            t.sendOnNext("one");
            t.sendOnCompleted();
            t.sendOnError(new RuntimeException("bad"));

            verify(w, times(1)).onNext("one");
            verify(w, times(1)).onCompleted();
            verify(w, Mockito.never()).onError(any(Throwable.class));
        }

        /**
         * A Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
         */
        private static class TestObservable implements OnSubscribeFunc<String> {

            Observer<? super String> observer = null;

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
            public void sendOnError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
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
