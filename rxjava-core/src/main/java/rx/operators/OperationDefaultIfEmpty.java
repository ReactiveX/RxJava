package rx.operators;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Returns the elements of the specified sequence or the specified default value
 * in a singleton sequence if the sequence is empty.
 */
public class OperationDefaultIfEmpty {

    /**
     * Returns the elements of the specified sequence or the specified default
     * value in a singleton sequence if the sequence is empty.
     * 
     * @param source
     *            The sequence to return the specified value for if it is empty.
     * @param defaultValue
     *            The value to return if the sequence is empty.
     * @return An observable sequence that contains the specified default value
     *         if the source is empty; otherwise, the elements of the source
     *         itself.
     */
    public static <T> OnSubscribeFunc<T> defaultIfEmpty(
            Observable<? extends T> source, T defaultValue) {
        return new DefaultIfEmpty<T>(source, defaultValue);
    }

    private static class DefaultIfEmpty<T> implements OnSubscribeFunc<T> {

        private final Observable<? extends T> source;
        private final T defaultValue;

        private DefaultIfEmpty(Observable<? extends T> source, T defaultValue) {
            this.source = source;
            this.defaultValue = defaultValue;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(source.subscribe(new Observer<T>() {

                private volatile boolean hasEmitted = false;

                @Override
                public void onNext(T value) {
                    try {
                        hasEmitted = true;
                        observer.onNext(value);
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
                    if (hasEmitted) {
                        observer.onCompleted();
                    } else {
                        observer.onNext(defaultValue);
                        observer.onCompleted();
                    }
                }
            }));
        }
    }
}
