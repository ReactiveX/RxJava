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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class BlockingObservableLatestTest extends RxJavaTest {
    @Test
    public void simple() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onComplete when receiving the 10th item
        // which onComplete will overwrite the previous value
        for (int i = 0; i < 9; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertTrue(it.hasNext());

            Assert.assertEquals(Long.valueOf(i), it.next());
        }

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void sameSourceMultipleIterators() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        for (int j = 0; j < 3; j++) {
            Iterator<Long> it = iter.iterator();

            // only 9 because take(10) will immediately call onComplete when receiving the 10th item
            // which onComplete will overwrite the previous value
            for (int i = 0; i < 9; i++) {
                scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

                Assert.assertTrue(it.hasNext());

                Assert.assertEquals(Long.valueOf(i), it.next());
            }

            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
            Assert.assertFalse(it.hasNext());
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void empty() {
        Observable<Long> source = Observable.<Long> empty();

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        Assert.assertFalse(it.hasNext());

        it.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void simpleJustNext() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onComplete when receiving the 10th item
        // which onComplete will overwrite the previous value
        for (int i = 0; i < 10; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertEquals(Long.valueOf(i), it.next());
        }
    }

    @Test(expected = RuntimeException.class)
    public void hasNextThrows() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Long> source = Observable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

        Iterable<Long> iter = source.blockingLatest();

        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.hasNext();
    }

    @Test(expected = RuntimeException.class)
    public void nextThrows() {
        TestScheduler scheduler = new TestScheduler();

        Observable<Long> source = Observable.<Long> error(new RuntimeException("Forced failure!")).subscribeOn(scheduler);

        Iterable<Long> iter = source.blockingLatest();
        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.next();
    }

    @Test
    public void fasterSource() {
        PublishSubject<Integer> source = PublishSubject.create();
        Observable<Integer> blocker = source;

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

        Assert.assertFalse(it.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void remove() {
        Observable.never().blockingLatest().iterator().remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void empty2() {
        Observable.empty().blockingLatest().iterator().next();
    }

    @Test(expected = TestException.class)
    public void error() {
        Observable.error(new TestException()).blockingLatest().iterator().next();
    }

    @Test
    public void error2() {
        Iterator<Object> it = Observable.error(new TestException()).blockingLatest().iterator();

        for (int i = 0; i < 3; i++) {
            try {
                it.hasNext();
                fail("Should have thrown");
            } catch (TestException ex) {
                // expected
            }
        }
    }

    @Test
    public void interrupted() {
        Iterator<Object> it = Observable.never().blockingLatest().iterator();

        Thread.currentThread().interrupt();

        try {
            it.hasNext();
        } catch (RuntimeException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof InterruptedException);
        }
        Thread.interrupted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onError() {
        Iterator<Object> it = Observable.never().blockingLatest().iterator();

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            ((Observer<Object>)it).onError(new TestException());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
