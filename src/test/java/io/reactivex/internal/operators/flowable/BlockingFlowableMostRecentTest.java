/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.processors.*;
import io.reactivex.schedulers.TestScheduler;

public class BlockingFlowableMostRecentTest {
    @Test
    public void testMostRecentNull() {
        assertEquals(null, Flowable.<Void>never().blockingMostRecent(null).iterator().next());
    }

    @Test
    public void testMostRecent() {
        FlowableProcessor<String> s = PublishProcessor.create();

        Iterator<String> it = s.blockingMostRecent("default").iterator();

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
        FlowableProcessor<String> s = PublishProcessor.create();

        Iterator<String> it = s.blockingMostRecent("default").iterator();

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
        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingMostRecent(-1L);

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

    @Ignore("The target is an enum")
    @Test
    public void constructorshouldbeprivate() {
        TestHelper.checkUtilityClass(BlockingFlowableMostRecent.class);
    }


    @Test
    public void empty() {
        Iterator<Integer> it = Flowable.<Integer>empty()
        .blockingMostRecent(1)
        .iterator();

        try {
            it.next();
            fail("Should have thrown");
        } catch (NoSuchElementException ex) {
            // expected
        }

        try {
            it.remove();
            fail("Should have thrown");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }
}
