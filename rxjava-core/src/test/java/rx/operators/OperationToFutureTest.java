package rx.operators;

import static org.junit.Assert.*;
import static rx.operators.OperationToFuture.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class OperationToFutureTest {

    @Test
    public void testToFuture() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one");
        Future<String> f = toFuture(obs);
        assertEquals("one", f.get());
    }

    @Test
    public void testToFutureList() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one", "two", "three");
        Future<List<String>> f = toFuture(obs.toList());
        assertEquals("one", f.get().get(0));
        assertEquals("two", f.get().get(1));
        assertEquals("three", f.get().get(2));
    }

    @Test(expected = ExecutionException.class)
    public void testExceptionWithMoreThanOneElement() throws InterruptedException, ExecutionException {
        Observable<String> obs = Observable.from("one", "two");
        Future<String> f = toFuture(obs);
        assertEquals("one", f.get());
        // we expect an exception since there are more than 1 element
    }

    @Test
    public void testToFutureWithException() {
        Observable<String> obs = Observable.create(new OnSubscribeFunc<String>() {

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
                return Subscriptions.empty();
            }
        });

        Future<String> f = toFuture(obs);
        try {
            f.get();
            fail("expected exception");
        } catch (Throwable e) {
            assertEquals(TestException.class, e.getCause().getClass());
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
