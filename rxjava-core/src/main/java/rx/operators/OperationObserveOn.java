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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.ImmediateScheduler;
import rx.concurrency.Schedulers;

/**
 * Asynchronously notify Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperationObserveOn {

    public static <T> OnSubscribeFunc<T> observeOn(Observable<? extends T> source, Scheduler scheduler) {
        return new ObserveOn<T>(source, scheduler);
    }

    private static class ObserveOn<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Scheduler scheduler;

        public ObserveOn(Observable<? extends T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            if (scheduler instanceof ImmediateScheduler) {
                // do nothing if we request ImmediateScheduler so we don't invoke overhead
                return source.subscribe(observer);
            } else {
                return source.subscribe(new ScheduledObserver<T>(observer, scheduler));
            }
        }
    }

    public static class UnitTest {

        /**
         * This is testing a no-op path since it uses Schedulers.immediate() which will not do scheduling.
         */
        @Test
        @SuppressWarnings("unchecked")
        public void testObserveOn() {
            Observer<Integer> observer = mock(Observer.class);
            Observable.create(observeOn(Observable.from(1, 2, 3), Schedulers.immediate())).subscribe(observer);

            verify(observer, times(1)).onNext(1);
            verify(observer, times(1)).onNext(2);
            verify(observer, times(1)).onNext(3);
            verify(observer, times(1)).onCompleted();
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testOrdering() throws InterruptedException {
            Observable<String> obs = Observable.from("one", null, "two", "three", "four");

            Observer<String> observer = mock(Observer.class);

            InOrder inOrder = inOrder(observer);

            final CountDownLatch completedLatch = new CountDownLatch(1);
            doAnswer(new Answer<Void>() {

                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    completedLatch.countDown();
                    return null;
                }
            }).when(observer).onCompleted();

            obs.observeOn(Schedulers.threadPoolForComputation()).subscribe(observer);

            if (!completedLatch.await(1000, TimeUnit.MILLISECONDS)) {
                fail("timed out waiting");
            }

            inOrder.verify(observer, times(1)).onNext("one");
            inOrder.verify(observer, times(1)).onNext(null);
            inOrder.verify(observer, times(1)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            inOrder.verify(observer, times(1)).onNext("four");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

    }

}
