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

import org.junit.Test;
import rx.Observer;
import rx.schedulers.TestScheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class TestSubjectTest {

    @Test
    public void testObserverPropagateValueAfterTriggeringActions() {
        final TestScheduler scheduler = new TestScheduler();

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext(1);
        scheduler.triggerActions();

        verify(observer, times(1)).onNext(1);
    }

    @Test
    public void testObserverPropagateValueInFutureTimeAfterTriggeringActions() {
        final TestScheduler scheduler = new TestScheduler();
        scheduler.advanceTimeTo(100, TimeUnit.SECONDS);

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onNext(1);
        scheduler.triggerActions();

        verify(observer, times(1)).onNext(1);
    }



    @Test
    public void testObserverPropagateErrorAfterTriggeringActions() {
        final IOException e = new IOException();
        final TestScheduler scheduler = new TestScheduler();

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onError(e);
        scheduler.triggerActions();

        verify(observer, times(1)).onError(e);
    }

    @Test
    public void testObserverPropagateErrorInFutureTimeAfterTriggeringActions() {
        final IOException e = new IOException();
        final TestScheduler scheduler = new TestScheduler();
        scheduler.advanceTimeTo(100, TimeUnit.SECONDS);

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onError(e);
        scheduler.triggerActions();

        verify(observer, times(1)).onError(e);
    }



    @Test
    public void testObserverPropagateCompletedAfterTriggeringActions() {
        final TestScheduler scheduler = new TestScheduler();

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onCompleted();
        scheduler.triggerActions();

        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testObserverPropagateCompletedInFutureTimeAfterTriggeringActions() {
        final TestScheduler scheduler = new TestScheduler();
        scheduler.advanceTimeTo(100, TimeUnit.SECONDS);

        final TestSubject<Integer> subject = TestSubject.create(scheduler);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        subject.subscribe(observer);

        subject.onCompleted();
        scheduler.triggerActions();

        verify(observer, times(1)).onCompleted();
    }
}
