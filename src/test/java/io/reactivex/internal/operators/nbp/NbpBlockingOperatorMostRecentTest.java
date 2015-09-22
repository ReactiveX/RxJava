/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static io.reactivex.internal.operators.nbp.NbpBlockingOperatorMostRecent.mostRecent;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import io.reactivex.NbpObservable;
import io.reactivex.exceptions.TestException;
import io.reactivex.observables.nbp.NbpBlockingObservable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.nbp.*;

public class NbpBlockingOperatorMostRecentTest {
    @Test
    public void testMostRecentNull() {
        assertEquals(null, NbpObservable.<Void>never().toBlocking().mostRecent(null).iterator().next());
    }

    @Test
    public void testMostRecent() {
        NbpSubject<String, String> s = NbpPublishSubject.create();

        Iterator<String> it = mostRecent(s, "default").iterator();

        assertTrue(it.hasNext());
        assertEquals("default", it.next());
        assertEquals("default", it.next());

        s.onNext("one");
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertEquals("one", it.next());

        s.onNext("two");
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertEquals("two", it.next());

        s.onComplete();
        assertFalse(it.hasNext());

    }

    @Test(expected = TestException.class)
    public void testMostRecentWithException() {
        NbpSubject<String, String> s = NbpPublishSubject.create();

        Iterator<String> it = mostRecent(s, "default").iterator();

        assertTrue(it.hasNext());
        assertEquals("default", it.next());
        assertEquals("default", it.next());

        s.onError(new TestException());
        assertTrue(it.hasNext());

        it.next();
    }

    @Test(timeout = 1000)
    public void testSingleSourceManyIterators() {
        TestScheduler scheduler = new TestScheduler();
        NbpBlockingObservable<Long> source = NbpObservable.interval(1, TimeUnit.SECONDS, scheduler).take(10).toBlocking();

        Iterable<Long> iter = source.mostRecent(-1L);

        for (int j = 0; j < 3; j++) {
            Iterator<Long> it = iter.iterator();

            Assert.assertEquals(Long.valueOf(-1), it.next());

            for (int i = 0; i < 9; i++) {
                scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

                Assert.assertEquals(true, it.hasNext());
                Assert.assertEquals(Long.valueOf(i), it.next());
            }
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertEquals(false, it.hasNext());
        }

    }
}