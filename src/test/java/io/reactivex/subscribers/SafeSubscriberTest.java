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

package io.reactivex.subscribers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.*;

import io.reactivex.*;

public class SafeSubscriberTest {

    /**
     * Ensure onNext can not be called after onError
     */
    @Test
    public void testOnNextAfterOnError() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<String>(new TestSubscriber<String>(w)));

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
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        
        st.subscribe(new SafeSubscriber<String>(new TestSubscriber<String>(w)));

        t.sendOnNext("one");
        t.sendOnError(new RuntimeException("bad"));
        t.sendOnCompleted();

        verify(w, times(1)).onNext("one");
        verify(w, times(1)).onError(any(Throwable.class));
        verify(w, Mockito.never()).onComplete();
    }

    /**
     * Ensure onNext can not be called after onCompleted
     */
    @Test
    public void testOnNextAfterOnCompleted() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<String>(new TestSubscriber<String>(w)));

        t.sendOnNext("one");
        t.sendOnCompleted();
        t.sendOnNext("two");

        verify(w, times(1)).onNext("one");
        verify(w, Mockito.never()).onNext("two");
        verify(w, times(1)).onComplete();
        verify(w, Mockito.never()).onError(any(Throwable.class));
    }

    /**
     * Ensure onError can not be called after onCompleted
     */
    @Test
    public void testOnErrorAfterOnCompleted() {
        TestObservable t = new TestObservable();
        Flowable<String> st = Flowable.unsafeCreate(t);

        Subscriber<String> w = TestHelper.mockSubscriber();
        st.subscribe(new SafeSubscriber<String>(new TestSubscriber<String>(w)));

        t.sendOnNext("one");
        t.sendOnCompleted();
        t.sendOnError(new RuntimeException("bad"));

        verify(w, times(1)).onNext("one");
        verify(w, times(1)).onComplete();
        verify(w, Mockito.never()).onError(any(Throwable.class));
    }

    /**
     * A Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
     */
    private static class TestObservable implements Publisher<String> {

        Subscriber<? super String> observer = null;

        /* used to simulate subscription */
        public void sendOnCompleted() {
            observer.onComplete();
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
        public void subscribe(Subscriber<? super String> observer) {
            this.observer = observer;
            observer.onSubscribe(new Subscription() {

                @Override
                public void cancel() {
                    // going to do nothing to pretend I'm a bad Observable that keeps allowing events to be sent
                    System.out.println("==> SynchronizeTest unsubscribe that does nothing!");
                }

                @Override
                public void request(long n) {
                    
                }

            });
        }

    }
}