package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.Observable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.EmptySubscription;

public class BlockingOperatorToIteratorTest {

    @Test
    public void testToIterator() {
        Observable<String> obs = Observable.just("one", "two", "three");

        Iterator<String> it = obs.toBlocking().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("two", it.next());

        assertEquals(true, it.hasNext());
        assertEquals("three", it.next());

        assertEquals(false, it.hasNext());

    }

    @Test(expected = TestException.class)
    public void testToIteratorWithException() {
        Observable<String> obs = Observable.create(new Publisher<String>() {

            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(EmptySubscription.INSTANCE);
                observer.onNext("one");
                observer.onError(new TestException());
            }
        });

        Iterator<String> it = obs.toBlocking().iterator();

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();
    }

    @Ignore("subscribe() should not throw")
    @Test(expected = TestException.class)
    public void testExceptionThrownFromOnSubscribe() {
        Iterable<String> strings = Observable.create(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                throw new TestException("intentional");
            }
        }).toBlocking();
        for (String string : strings) {
            // never reaches here
            System.out.println(string);
        }
    }
}