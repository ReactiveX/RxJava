package rx.operators;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

/**
 * Convert an Observable into a Future.
 */
public class OperationToFuture {

    /**
     * Returns a Future that expects a single item from the observable.
     * 
     * @param that
     *            an observable sequence to get a Future for.
     * @param <T>
     *            the type of source.
     * @return the Future to retrieve a single elements from an Observable
     */
    public static <T> Future<T> toFuture(Observable<T> that) {

        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Exception> error = new AtomicReference<Exception>();

        final Subscription s = that.subscribe(new Observer<T>() {

            @Override
            public void onCompleted() {
                finished.countDown();
            }

            @Override
            public void onError(Exception e) {
                error.compareAndSet(null, e);
                finished.countDown();
            }

            @Override
            public void onNext(T v) {
                if (!value.compareAndSet(null, v)) {
                    // this means we received more than one value and must fail as a Future can handle only a single value
                    error.compareAndSet(null, new IllegalStateException("Observable.toFuture() only supports sequences with a single value. Use .toList().toFuture() if multiple values are expected."));
                    finished.countDown();
                }
            }
        });

        return new Future<T>() {

            private volatile boolean cancelled = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (finished.getCount() > 0) {
                    cancelled = true;
                    s.unsubscribe();
                    // release the latch (a race condition may have already released it by now)
                    finished.countDown();
                    return true;
                } else {
                    // can't cancel
                    return false;
                }
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                return finished.getCount() == 0;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                finished.await();
                return getValue();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                if (finished.await(timeout, unit)) {
                    return getValue();
                } else {
                    throw new TimeoutException("Timed out after " + unit.toMillis(timeout) + "ms waiting for underlying Observable.");
                }
            }

            private T getValue() throws ExecutionException {
                if (error.get() != null) {
                    throw new ExecutionException("Observable onError", error.get());
                } else {
                    return value.get();
                }
            }

        };

    }

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
        Observable<String> obs = Observable.create(new Func1<Observer<String>, Subscription>() {

            @Override
            public Subscription call(Observer<String> observer) {
                observer.onNext("one");
                observer.onError(new TestException());
                return Subscriptions.empty();
            }
        });

        Future<String> f = toFuture(obs);
        try {
            f.get();
            fail("expected exception");
        } catch (Exception e) {
            assertEquals(TestException.class, e.getCause().getClass());
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
