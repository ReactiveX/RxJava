package rx.operators;

import static org.mockito.Mockito.*;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public class OperationToObservableFuture {
    private static class ToObservableFuture<T> implements Func1<Observer<T>, Subscription> {
        private final Future<T> that;
        private final Long time;
        private final TimeUnit unit;

        public ToObservableFuture(Future<T> that) {
            this.that = that;
            this.time = null;
            this.unit = null;
        }

        public ToObservableFuture(Future<T> that, long time, TimeUnit unit) {
            this.that = that;
            this.time = time;
            this.unit = unit;
        }

        @Override
        public Subscription call(Observer<T> observer) {
            try {
                T value = (time == null) ? that.get() : that.get(time, unit);

                if (!that.isCancelled()) {
                    observer.onNext(value);
                }
                observer.onCompleted();
            } catch (Exception e) {
                observer.onError(e);
            }

            // the get() has already completed so there is no point in
            // giving the user a way to cancel.
            return Observable.noOpSubscription();
        }
    }

    public static <T> Func1<Observer<T>, Subscription> toObservableFuture(final Future<T> that) {
        return new ToObservableFuture<T>(that);
    }

    public static <T> Func1<Observer<T>, Subscription> toObservableFuture(final Future<T> that, long time, TimeUnit unit) {
        return new ToObservableFuture<T>(that, time, unit);
    }

    @SuppressWarnings("unchecked")
    public static class UnitTest {
        @Test
        public void testSuccess() throws Exception {
            Future<Object> future = mock(Future.class);
            Object value = new Object();
            when(future.get()).thenReturn(value);
            ToObservableFuture ob = new ToObservableFuture(future);
            Observer<Object> o = mock(Observer.class);

            Subscription sub = ob.call(o);
            sub.unsubscribe();

            verify(o, times(1)).onNext(value);
            verify(o, times(1)).onCompleted();
            verify(o, never()).onError(null);
            verify(future, never()).cancel(true);
        }

        @Test
        public void testFailure() throws Exception {
            Future<Object> future = mock(Future.class);
            RuntimeException e = new RuntimeException();
            when(future.get()).thenThrow(e);
            ToObservableFuture ob = new ToObservableFuture(future);
            Observer<Object> o = mock(Observer.class);

            Subscription sub = ob.call(o);
            sub.unsubscribe();

            verify(o, never()).onNext(null);
            verify(o, never()).onCompleted();
            verify(o, times(1)).onError(e);
            verify(future, never()).cancel(true);
        }
    }
}
