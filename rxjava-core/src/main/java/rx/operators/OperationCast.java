package rx.operators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.util.functions.Func1;

/**
 * Converts the elements of an observable sequence to the specified type.
 */
public class OperationCast {

    public static <T, R> OnSubscribeFunc<R> cast(
            Observable<? extends T> source, final Class<R> klass) {
        return OperationMap.map(source, new Func1<T, R>() {
            public R call(T t) {
                return klass.cast(t);
            }
        });
    }

    public static class UnitTest {

        @Test
        public void testCast() {
            Observable<?> source = Observable.from(1, 2);
            Observable<Integer> observable = Observable.create(cast(source,
                    Integer.class));

            @SuppressWarnings("unchecked")
            Observer<Integer> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(1);
            verify(aObserver, times(1)).onNext(1);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testCastWithWrongType() {
            Observable<?> source = Observable.from(1, 2);
            Observable<Boolean> observable = Observable.create(cast(source,
                    Boolean.class));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onError(
                    org.mockito.Matchers.any(ClassCastException.class));
        }
    }

}
