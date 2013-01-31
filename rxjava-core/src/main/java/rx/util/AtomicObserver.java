package rx.util;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observer;

/**
 * Wrapper around Observer to ensure compliance with Rx contract.
 * <p>
 * The following is taken from the Rx Design Guidelines document: http://go.microsoft.com/fwlink/?LinkID=205219
 * <pre>
 * Messages sent to instances of the IObserver interface follow the following grammar:
 * 
 * OnNext* (OnCompleted | OnError)?
 * 
 * This grammar allows observable sequences to send any amount (0 or more) of OnNext messages to the subscribed
 * observer instance, optionally followed by a single success (OnCompleted) or failure (OnError) message.
 * 
 * The single message indicating that an observable sequence has finished ensures that consumers of the observable
 * sequence can deterministically establish that it is safe to perform cleanup operations.
 * 
 * A single failure further ensures that abort semantics can be maintained for operators that work on
 * multiple observable sequences (see paragraph 6.6).
 * </pre>
 * 
 * <p>
 * This wrapper will do the following:
 * <ul>
 * <li>Allow only single execution of either onError or onCompleted.</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * <li>When onError or onComplete occur it will unsubscribe from the Observable (if executing asynchronously).</li>
 * </ul>
 * <p>
 * It will not synchronized onNext execution. Use the {@link SynchronizedObserver} to do that.
 * 
 * @param <T>
 */
public class AtomicObserver<T> implements Observer<T> {

    private final Observer<T> actual;
    private final AtomicBoolean isFinished = new AtomicBoolean(false);
    private final AtomicObservableSubscription subscription;

    public AtomicObserver(AtomicObservableSubscription subscription, Observer<T> actual) {
        this.subscription = subscription;
        this.actual = actual;
    }

    @Override
    public void onCompleted() {
        if (isFinished.compareAndSet(false, true)) {
            actual.onCompleted();
            // auto-unsubscribe
            subscription.unsubscribe();
        }
    }

    @Override
    public void onError(Exception e) {
        if (isFinished.compareAndSet(false, true)) {
            actual.onError(e);
            // auto-unsubscribe
            subscription.unsubscribe();
        }
    }

    @Override
    public void onNext(T args) {
        if (!isFinished.get()) {
            actual.onNext(args);
        }
    }

}
