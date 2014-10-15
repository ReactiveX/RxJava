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

import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestSubjectTest {

    private TestScheduler testScheduler;

    @Before
    public void setUp() {
        testScheduler = Schedulers.test();
    }

    @Test
    public void testOnNext() {
        TestSubject<String> testSubject = TestSubject.create(testScheduler);
        TestSubscriber<String> observer = new TestSubscriber<String>();
        testSubject.subscribe(observer);

        testScheduler.advanceTimeBy(1, TimeUnit.HOURS);

        testSubject.onNext("a");
        testSubject.onCompleted();

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        observer.assertReceivedOnNext(Arrays.asList("a"));
        observer.assertNoErrors();
        observer.assertTerminalEvent();
    }

    @Test
    public void testOnError() {
        TestSubject<String> testSubject = TestSubject.create(testScheduler);
        TestSubscriber<String> observer = new TestSubscriber<String>();
        testSubject.subscribe(observer);

        Throwable e = new RuntimeException("test");

        testScheduler.advanceTimeBy(1, TimeUnit.HOURS);

        testSubject.onError(e);

        testScheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        observer.assertReceivedOnNext(new ArrayList<String>());
        assertEquals(Arrays.asList(e), observer.getOnErrorEvents());
    }
}
