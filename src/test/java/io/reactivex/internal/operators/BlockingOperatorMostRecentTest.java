package io.reactivex.internal.operators;

import static org.junit.Assert.*;
import static io.reactivex.internal.operators.BlockingOperatorMostRecent.mostRecent;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import io.reactivex.Observable;
import io.reactivex.exceptions.TestException;
import io.reactivex.observables.BlockingObservable;
import io.reactivex.schedulers.TestScheduler;
import io.reactivex.subjects.*;

public class BlockingOperatorMostRecentTest {
    @Test
    public void testMostRecentNull() {
        assertEquals(null, Observable.<Void>never().toBlocking().mostRecent(null).iterator().next());
    }

    @Test
    public void testMostRecent() {
        Subject<String, String> s = PublishSubject.create();

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
        Subject<String, String> s = PublishSubject.create();

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
        BlockingObservable<Long> source = Observable.interval(1, TimeUnit.SECONDS, scheduler).take(10).toBlocking();

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