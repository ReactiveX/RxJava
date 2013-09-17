package rx.operators;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static rx.util.functions.Functions.alwaysTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Returns an {@link Observable} that emits <code>true</code> if any element of
 * an observable sequence satisfies a condition, otherwise <code>false</code>.
 */
public final class OperationAny {

    /**
     * Returns an {@link Observable} that emits <code>true</code> if the source
     * {@link Observable} is not empty, otherwise <code>false</code>.
     * 
     * @param source
     *            The source {@link Observable} to check if not empty.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<Boolean> any(
            Observable<? extends T> source) {
        return new Any<T>(source, alwaysTrue());
    }

    /**
     * Returns an {@link Observable} that emits <code>true</code> if any element
     * of the source {@link Observable} satisfies the given condition, otherwise
     * <code>false</code>. Note: always emit <code>false</code> if the source
     * {@link Observable} is empty.
     * 
     * @param source
     *            The source {@link Observable} to check if any element
     *            satisfies the given condition.
     * @param predicate
     *            The condition to test every element.
     * @return A subscription function for creating the target Observable.
     */
    public static <T> OnSubscribeFunc<Boolean> any(
            Observable<? extends T> source, Func1<? super T, Boolean> predicate) {
        return new Any<T>(source, predicate);
    }

    private static class Any<T> implements OnSubscribeFunc<Boolean> {

        private final Observable<? extends T> source;
        private final Func1<? super T, Boolean> predicate;

        private Any(Observable<? extends T> source,
                Func1<? super T, Boolean> predicate) {
            this.source = source;
            this.predicate = predicate;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Boolean> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(source.subscribe(new Observer<T>() {

                private final AtomicBoolean hasEmitted = new AtomicBoolean(
                        false);

                @Override
                public void onNext(T value) {
                    try {
                        if (hasEmitted.get() == false) {
                            if (predicate.call(value) == true
                                    && hasEmitted.getAndSet(true) == false) {
                                observer.onNext(true);
                                observer.onCompleted();
                                // this will work if the sequence is
                                // asynchronous, it
                                // will have no effect on a synchronous
                                // observable
                                subscription.unsubscribe();
                            }
                        }
                    } catch (Throwable ex) {
                        observer.onError(ex);
                        // this will work if the sequence is asynchronous, it
                        // will have no effect on a synchronous observable
                        subscription.unsubscribe();
                    }

                }

                @Override
                public void onError(Throwable ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    if (!hasEmitted.get()) {
                        observer.onNext(false);
                        observer.onCompleted();
                    }
                }
            }));
        }

    }

    public static class UnitTest {

        @Test
        public void testAnyWithTwoItems() {
            Observable<Integer> w = Observable.from(1, 2);
            Observable<Boolean> observable = Observable.create(any(w));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, never()).onNext(false);
            verify(aObserver, times(1)).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testAnyWithOneItem() {
            Observable<Integer> w = Observable.from(1);
            Observable<Boolean> observable = Observable.create(any(w));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, never()).onNext(false);
            verify(aObserver, times(1)).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testAnyWithEmpty() {
            Observable<Integer> w = Observable.empty();
            Observable<Boolean> observable = Observable.create(any(w));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(false);
            verify(aObserver, never()).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testAnyWithPredicate1() {
            Observable<Integer> w = Observable.from(1, 2, 3);
            Observable<Boolean> observable = Observable.create(any(w,
                    new Func1<Integer, Boolean>() {

                        @Override
                        public Boolean call(Integer t1) {
                            return t1 < 2;
                        }
                    }));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, never()).onNext(false);
            verify(aObserver, times(1)).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testAnyWithPredicate2() {
            Observable<Integer> w = Observable.from(1, 2, 3);
            Observable<Boolean> observable = Observable.create(any(w,
                    new Func1<Integer, Boolean>() {

                        @Override
                        public Boolean call(Integer t1) {
                            return t1 < 1;
                        }
                    }));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(false);
            verify(aObserver, never()).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testAnyWithEmptyAndPredicate() {
            // If the source is empty, always output false.
            Observable<Integer> w = Observable.empty();
            Observable<Boolean> observable = Observable.create(any(w,
                    new Func1<Integer, Boolean>() {

                        @Override
                        public Boolean call(Integer t1) {
                            return true;
                        }
                    }));

            @SuppressWarnings("unchecked")
            Observer<Boolean> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext(false);
            verify(aObserver, never()).onNext(true);
            verify(aObserver, never()).onError(
                    org.mockito.Matchers.any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}
