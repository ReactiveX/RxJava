package rx.internal.operators;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.internal.producers.SingleDelayedProducer;

import java.util.concurrent.Callable;

/**
 * Do not invoke the function until an Observer subscribes; Invokes function on each
 * subscription.
 * <p>
 * Pass {@code fromCallable} a function, and {@code fromCallable} will call this function to emit result of invocation
 * afresh each time a new Observer subscribes.
 */
public final class OnSubscribeFromCallable<T> implements Observable.OnSubscribe<T> {

    private final Callable<? extends T> resultFactory;

    public OnSubscribeFromCallable(Callable<? extends T> resultFactory) {
        this.resultFactory = resultFactory;
    }

    @Override
    public void call(Subscriber<? super T> subscriber) {
        final SingleDelayedProducer<T> singleDelayedProducer = new SingleDelayedProducer<T>(subscriber);

        subscriber.setProducer(singleDelayedProducer);

        try {
            singleDelayedProducer.setValue(resultFactory.call());
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            subscriber.onError(t);
        }
    }
}
