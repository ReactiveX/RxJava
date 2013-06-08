package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Returns an Observable that emits a Boolean that indicates whether all items emitted by an
 * Observable satisfy a condition.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
 */
public class OperationAll {

    public static <T> Func1<Observer<Boolean>, Subscription> all(Observable<T> sequence, Func1<T, Boolean> predicate) {
        return new AllObservable<T>(sequence, predicate);
    }

    private static class AllObservable<T> implements Func1<Observer<Boolean>, Subscription> {
        private final Observable<T> sequence;
        private final Func1<T, Boolean> predicate;

        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();


        private AllObservable(Observable<T> sequence, Func1<T, Boolean> predicate) {
            this.sequence = sequence;
            this.predicate = predicate;
        }


        @Override
        public Subscription call(final Observer<Boolean> observer) {
            return subscription.wrap(sequence.subscribe(new AllObserver(observer)));

        }

        private class AllObserver implements Observer<T> {
            private final Observer<Boolean> underlying;

            private final AtomicBoolean status = new AtomicBoolean(true);

            public AllObserver(Observer<Boolean> underlying) {
                this.underlying = underlying;
            }

            @Override
            public void onCompleted() {
                if (status.get()) {
                    underlying.onNext(true);
                    underlying.onCompleted();
                }
            }

            @Override
            public void onError(Exception e) {
                underlying.onError(e);
            }

            @Override
            public void onNext(T args) {
                boolean result = predicate.call(args);
                boolean changed = status.compareAndSet(true, result);

                if (changed && !result) {
                    underlying.onNext(false);
                    underlying.onCompleted();
                    subscription.unsubscribe();
                }
            }
        }

    }

    public static class UnitTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testAll() {
            Observable<String> obs = Observable.from("one", "two", "six");

            Observer<Boolean> observer = mock(Observer.class);
            Observable.create(all(obs, new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            })).subscribe(observer);

            verify(observer).onNext(true);
            verify(observer).onCompleted();
            verifyNoMoreInteractions(observer);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testNotAll() {
            Observable<String> obs = Observable.from("one", "two", "three", "six");

            Observer<Boolean> observer = mock(Observer.class);
            Observable.create(all(obs, new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            })).subscribe(observer);

            verify(observer).onNext(false);
            verify(observer).onCompleted();
            verifyNoMoreInteractions(observer);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testEmpty() {
            Observable<String> obs = Observable.empty();

            Observer<Boolean> observer = mock(Observer.class);
            Observable.create(all(obs, new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            })).subscribe(observer);

            verify(observer).onNext(true);
            verify(observer).onCompleted();
            verifyNoMoreInteractions(observer);
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testError() {
            Exception error = new Exception();
            Observable<String> obs = Observable.error(error);

            Observer<Boolean> observer = mock(Observer.class);
            Observable.create(all(obs, new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            })).subscribe(observer);

            verify(observer).onError(error);
            verifyNoMoreInteractions(observer);
        }
    }
}
