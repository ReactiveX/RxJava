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
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.TestScheduler;

public class BlockingFlowableLatestTest {
    @Test(timeout = 1000)
    public void testSimple() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onComplete when receiving the 10th item
        // which onComplete will overwrite the previous value
        for (int i = 0; i < 9; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertEquals(true, it.hasNext());

            Assert.assertEquals(Long.valueOf(i), it.next());
        }

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        Assert.assertEquals(false, it.hasNext());
    }

    @Test(timeout = 1000)
    public void testSameSourceMultipleIterators() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        for (int j = 0; j < 3; j++) {
            Iterator<Long> it = iter.iterator();

            // only 9 because take(10) will immediately call onComplete when receiving the 10th item
            // which onComplete will overwrite the previous value
            for (int i = 0; i < 9; i++) {
                scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

                Assert.assertEquals(true, it.hasNext());

                Assert.assertEquals(Long.valueOf(i), it.next());
            }

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            Assert.assertEquals(false, it.hasNext());
        }
    }

    @Test(timeout = 1000, expected = NoSuchElementException.class)
    public void testEmpty() {
        Flowable<Long> source = Flowable.<Long> empty();

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        Assert.assertEquals(false, it.hasNext());

        it.next();
    }

    @Test(timeout = 1000, expected = NoSuchElementException.class)
    public void testSimpleJustNext() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onComplete when receiving the 10th item
        // which onComplete will overwrite the previous value
        for (int i = 0; i < 10; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertEquals(Long.valueOf(i), it.next());
        }
    }

    @Test(/* timeout = 1000, */expected = RuntimeException.class)
    public void testHasNextThrows() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.hasNext();
    }

    @Test(timeout = 1000, expected = RuntimeException.class)
    public void testNextThrows() {
        TestScheduler scheduler = new TestScheduler();

        Flowable<Long> source = Flowable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

        Iterable<Long> iter = source.blockingLatest();
        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.next();
    }

    @Test(timeout = 1000)
    public void testFasterSource() {
        PublishProcessor<Integer> source = PublishProcessor.create();
        Flowable<Integer> blocker = source;

        Iterable<Integer> iter = blocker.blockingLatest();
        Iterator<Integer> it = iter.iterator();

        source.onNext(1);

        Assert.assertEquals(Integer.valueOf(1), it.next());

        source.onNext(2);
        source.onNext(3);

        Assert.assertEquals(Integer.valueOf(3), it.next());

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        Assert.assertEquals(Integer.valueOf(6), it.next());

        source.onNext(7);
        source.onComplete();

        Assert.assertEquals(false, it.hasNext());
    }

    @Ignore("THe target is an enum")
    @Test
    public void constructorshouldbeprivate() {
        TestHelper.checkUtilityClass(BlockingFlowableLatest.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void remove() {
        Flowable.never().blockingLatest().iterator().remove();
    }

    @Test
    public void interrupted() {
        Iterator<Object> it = Flowable.never().blockingLatest().iterator();

        Thread.currentThread().interrupt();

        try {
            it.hasNext();
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
        Thread.interrupted();
    }

    @Test(expected = NoSuchElementException.class)
    public void empty() {
        Flowable.empty().blockingLatest().iterator().next();
    }

    @Test(expected = TestException.class)
    public void error() {
        Flowable.error(new TestException()).blockingLatest().iterator().next();
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
