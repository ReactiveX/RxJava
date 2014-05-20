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
package rx.operators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.Test;

import rx.Observable;
import rx.observables.BlockingObservable;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class BlockingOperatorLatestTest {
    @Test(timeout = 1000)
    public void testSimple() {
        TestScheduler scheduler = new TestScheduler();

        BlockingObservable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10).toBlocking();

        Iterable<Long> iter = source.latest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onCompleted when receiving the 10th item
        // which onCompleted will overwrite the previous value
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

        BlockingObservable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10).toBlocking();

        Iterable<Long> iter = source.latest();

        for (int j = 0; j < 3; j++) {
            Iterator<Long> it = iter.iterator();

            // only 9 because take(10) will immediately call onCompleted when receiving the 10th item
            // which onCompleted will overwrite the previous value
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
        BlockingObservable<Long> source = Observable.<Long> empty().toBlocking();

        Iterable<Long> iter = source.latest();

        Iterator<Long> it = iter.iterator();

        Assert.assertEquals(false, it.hasNext());

        it.next();
    }

    @Test(timeout = 1000, expected = NoSuchElementException.class)
    public void testSimpleJustNext() {
        TestScheduler scheduler = new TestScheduler();

        BlockingObservable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10).toBlocking();

        Iterable<Long> iter = source.latest();

        Iterator<Long> it = iter.iterator();

        // only 9 because take(10) will immediately call onCompleted when receiving the 10th item
        // which onCompleted will overwrite the previous value
        for (int i = 0; i < 10; i++) {
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            Assert.assertEquals(Long.valueOf(i), it.next());
        }
    }

    @Test(/* timeout = 1000, */expected = RuntimeException.class)
    public void testHasNextThrows() {
        TestScheduler scheduler = new TestScheduler();

        BlockingObservable<Long> source = Observable.<Long> error(new RuntimeException("Forced failure!"), scheduler).toBlocking();

        Iterable<Long> iter = source.latest();

        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.hasNext();
    }

    @Test(timeout = 1000, expected = RuntimeException.class)
    public void testNextThrows() {
        TestScheduler scheduler = new TestScheduler();

        BlockingObservable<Long> source = Observable.<Long> error(new RuntimeException("Forced failure!"), scheduler).toBlocking();

        Iterable<Long> iter = source.latest();
        Iterator<Long> it = iter.iterator();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        it.next();
    }

    @Test(timeout = 1000)
    public void testFasterSource() {
        PublishSubject<Integer> source = PublishSubject.create();
        BlockingObservable<Integer> blocker = source.toBlocking();

        Iterable<Integer> iter = blocker.latest();
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
        source.onCompleted();

        Assert.assertEquals(false, it.hasNext());
    }
}
