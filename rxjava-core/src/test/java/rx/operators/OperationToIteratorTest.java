package rx.operators;

import static org.junit.Assert.*;
import static rx.operators.OperationToIterator.*;

import java.util.Iterator;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class OperationToIteratorTest {

    @Test
    public void testToIterator() {
        Observable<String> obs = Observable.from("one", "two", "three");

        Iterator<String> it = toIterator(obs);

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
        Observable<String> obs = Observable.create(new OnSubscribeFunc<String>() {

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
                return Subscriptions.empty();
            }
        });

        Iterator<String> it = toIterator(obs);

        assertEquals(true, it.hasNext());
        assertEquals("one", it.next());

        assertEquals(true, it.hasNext());
        it.next();
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

}
