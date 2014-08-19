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
package rx.subjects;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;

public class AsyncSubjectTest {

    private final Throwable testException = new Throwable();

    @Test
    public void testNeverCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(testException);
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testNull() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext(null);
        subject.onCompleted();

        verify(observer, times(1)).onNext(null);
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeAfterCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onCompleted();

        subject.subscribe(observer);

        verify(observer, times(1)).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSubscribeAfterError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");

        RuntimeException re = new RuntimeException("failed");
        subject.onError(re);

        subject.subscribe(observer);

        verify(observer, times(1)).onError(re);
        verify(observer, Mockito.never()).onNext(any(String.class));
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testError() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");
        subject.onNext("three");
        subject.onError(testException);
        subject.onNext("four");
        subject.onError(new Throwable());
        subject.onCompleted();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, times(1)).onError(testException);
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testUnsubscribeBeforeCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Subscription subscription = subject.subscribe(observer);

        subject.onNext("one");
        subject.onNext("two");

        subscription.unsubscribe();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onCompleted();

        subject.onNext("three");
        subject.onCompleted();

        verify(observer, Mockito.never()).onNext(anyString());
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, Mockito.never()).onCompleted();
    }

    @Test
    public void testEmptySubjectCompleted() {
        AsyncSubject<String> subject = AsyncSubject.create();

        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onCompleted();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, never()).onNext(null);
        inOrder.verify(observer, never()).onNext(any(String.class));
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Can receive timeout if subscribe never receives an onError/onCompleted ... which reveals a race condition.
     */
    @Test(timeout = 10000)
    public void testSubscribeCompletionRaceCondition() {
        /*
         * With non-threadsafe code this fails most of the time on my dev laptop and is non-deterministic enough
         * to act as a unit test to the race conditions.
         * 
         * With the synchronization code in place I can not get this to fail on my laptop.
         */
        for (int i = 0; i < 50; i++) {
            final AsyncSubject<String> subject = AsyncSubject.create();
            final AtomicReference<String> value1 = new AtomicReference<String>();

            subject.subscribe(new Action1<String>() {

                @Override
                public void call(String t1) {
                    try {
                        // simulate a slow observer
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    value1.set(t1);
                }

            });

            Thread t1 = new Thread(new Runnable() {

                @Override
                public void run() {
                    subject.onNext("value");
                    subject.onCompleted();
                }
            });

            SubjectObserverThread t2 = new SubjectObserverThread(subject);
            SubjectObserverThread t3 = new SubjectObserverThread(subject);
            SubjectObserverThread t4 = new SubjectObserverThread(subject);
            SubjectObserverThread t5 = new SubjectObserverThread(subject);

            t2.start();
            t3.start();
            t1.start();
            t4.start();
            t5.start();
            try {
                t1.join();
                t2.join();
                t3.join();
                t4.join();
                t5.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals("value", value1.get());
            assertEquals("value", t2.value.get());
            assertEquals("value", t3.value.get());
            assertEquals("value", t4.value.get());
            assertEquals("value", t5.value.get());
        }

    }

    private static class SubjectObserverThread extends Thread {

        private final AsyncSubject<String> subject;
        private final AtomicReference<String> value = new AtomicReference<String>();

        public SubjectObserverThread(AsyncSubject<String> subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state 
                String v = subject.timeout(2000, TimeUnit.MILLISECONDS).toBlocking().single();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
