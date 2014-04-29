package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Observable.OnSubscribe;
import rx.functions.Func0;
import rx.subscriptions.SerialSubscription;

/**
 * Repeatedly subscribes to the source observable if the pre- or
 * postcondition is true.
 * <p>
 * This combines the While and DoWhile into a single operation through
 * the conditions.
 * 
 * @param <T>
 *            the result value type
 */
public final class OperatorWhileDoWhile<T> implements OnSubscribe<T> {
    final Func0<Boolean> preCondition;
    final Func0<Boolean> postCondition;
    final Observable<? extends T> source;

    public OperatorWhileDoWhile(Observable<? extends T> source,
            Func0<Boolean> preCondition, Func0<Boolean> postCondition) {
        this.source = source;
        this.preCondition = preCondition;
        this.postCondition = postCondition;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        boolean first;
        try {
            first = preCondition.call();
        } catch (Throwable t) {
            child.onError(t);
            return;
        }

        if (first) {
            SerialSubscription cancel = new SerialSubscription();
            child.add(cancel);
            final SourceObserver sourceObserver = new SourceObserver(child, cancel);

            Subscriber<T> firstSubscription = new Subscriber<T>() {

                @Override
                public void onCompleted() {
                    sourceObserver.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    sourceObserver.onError(e);
                }

                @Override
                public void onNext(T t) {
                    sourceObserver.onNext(t);
                }

            };
            cancel.set(firstSubscription);
            source.unsafeSubscribe(firstSubscription);
        } else {
            child.onCompleted();
        }
    }

    /** Observe the source. */
    final class SourceObserver implements Observer<T> {
        final Subscriber<? super T> actual;
        final SerialSubscription cancel;

        public SourceObserver(Subscriber<? super T> actual, SerialSubscription cancel) {
            this.actual = actual;
            this.cancel = cancel;
        }

        @Override
        public void onNext(T args) {
            actual.onNext(args);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onCompleted() {
            boolean next;
            try {
                next = postCondition.call();
            } catch (Throwable t) {
                actual.onError(t);
                return;
            }
            if (next) {
                Subscriber<T> newSubscription = new Subscriber<T>() {

                    @Override
                    public void onCompleted() {
                        SourceObserver.this.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        SourceObserver.this.onError(e);
                    }

                    @Override
                    public void onNext(T t) {
                        SourceObserver.this.onNext(t);
                    }

                };
                cancel.set(newSubscription);
                source.unsafeSubscribe(newSubscription);

            } else {
                actual.onCompleted();
            }
        }

    }
}