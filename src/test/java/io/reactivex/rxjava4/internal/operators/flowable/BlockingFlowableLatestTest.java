/*
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

package io.reactivex.rxjava4.internal.operators.flowable;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.processors.PublishProcessor;
import io.reactivex.rxjava4.schedulers.TestScheduler;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class BlockingFlowableLatestTest extends RxJavaTest {
    @Test
    public void simple() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onComplete when receiving the 10th item
        // which onComplete will overwrite the previous value
        for (int i = 0; i < 9; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            assertTrue(it.hasNext());

            assertEquals(Long.valueOf(i), it.next());
        }

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        assertFalse(it.hasNext());
    }

    @Test
    public void sameSourceMultipleIterators() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        for (int j = 0; j < 3; j++) {
            Iterator<Long> it = iter.iterator();

            // only 9 because take(10) will immediately call onComplete when receiving the 10th item
            // which onComplete will overwrite the previous value
            for (int i = 0; i < 9; i++) {
                scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

                assertTrue(it.hasNext());

                assertEquals(Long.valueOf(i), it.next());
            }

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void empty() {
        assertThrows(NoSuchElementException.class, () -> {
            Flowable<Long> source = Flowable.<Long> empty();

            Iterable<Long> iter = source.blockingLatest();

            Iterator<Long> it = iter.iterator();

            assertFalse(it.hasNext());

            it.next();
        });
    }

    @Test
    public void simpleJustNext() {
        assertThrows(NoSuchElementException.class, () -> {
            TestScheduler scheduler = new TestScheduler();

            Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

            Iterable<Long> iter = source.blockingLatest();

            Iterator<Long> it = iter.iterator();

            // only 9 because take(10) will immediately call onComplete when receiving the 10th item
            // which onComplete will overwrite the previous value
            for (int i = 0; i < 10; i++) {
                scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

                assertEquals(Long.valueOf(i), it.next());
            }
        });

    }

    @Test
    public void hasNextThrows() {
        assertThrows(RuntimeException.class, () -> {
            TestScheduler scheduler = new TestScheduler();

            Flowable<Long> source = Flowable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

            Iterable<Long> iter = source.blockingLatest();

            Iterator<Long> it = iter.iterator();

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            it.hasNext();
        });
    }

    @Test
    public void nextThrows() {
        assertThrows(RuntimeException.class, () -> {
            TestScheduler scheduler = new TestScheduler();

            Flowable<Long> source = Flowable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

            Iterable<Long> iter = source.blockingLatest();
            Iterator<Long> it = iter.iterator();

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            it.next();
        });
    }

    @Test
    public void fasterSource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        Flowable<Integer> blocker = source;

        Iterable<Integer> iter = blocker.blockingLatest();
        Iterator<Integer> it = iter.iterator();

        source.onNext(1);

        assertEquals(Integer.valueOf(1), it.next());

        source.onNext(2);
        source.onNext(3);

        assertEquals(Integer.valueOf(3), it.next());

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        assertEquals(Integer.valueOf(6), it.next());

        source.onNext(7);
        source.onComplete();

        assertFalse(it.hasNext());
    }

    @Test
    public void remove() {
        assertThrows(UnsupportedOperationException.class, () -> {
            Flowable.never().blockingLatest().iterator().remove();
        });
    }

    @Test
    public void interrupted() {
        Iterator<Object> it = Flowable.never().blockingLatest().iterator();

        Thread.currentThread().interrupt();

        try {
            it.hasNext();
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof InterruptedException, ex.toString());
        }
        Thread.interrupted();
    }

    @Test
    public void empty2() {
        assertThrows(NoSuchElementException.class, () -> {
            Flowable.empty().blockingLatest().iterator().next();
        });
    }

    @Test
    public void error() {
        assertThrows(TestException.class, () -> {
            Flowable.error(new TestException()).blockingLatest().iterator().next();
        });
    }

    @Test
    public void error2() {
        Iterator<Object> it = Flowable.error(new TestException()).blockingLatest().iterator();

        for (int i = 0; i < 3; i++) {
            try {
                it.hasNext();
                fail("Should have thrown");
            } catch (TestException ex) {
                // expected
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onError() {
        Iterator<Object> it = Flowable.never().blockingLatest().iterator();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ((Subscriber<Object>)it).onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
