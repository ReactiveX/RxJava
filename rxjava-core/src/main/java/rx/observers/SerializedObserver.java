package rx.observers;

import rx.Observer;

/**
 * Enforce single-threaded, serialized, ordered execution of onNext, onCompleted, onError.
 * <p>
 * When multiple threads are notifying they will be serialized by:
 * <p>
 * <li>Allowing only one thread at a time to emit</li>
 * <li>Adding notifications to a queue if another thread is already emitting</li>
 * <li>Not holding any locks or blocking any threads while emitting</li>
 * <p>
 * 
 * @param <T>
 */
public class SerializedObserver<T> implements Observer<T> {
    /*
     * Facade to actual implementation until final decision is made
     * on the implementation.
     */
    private final SerializedObserverViaQueueAndLock<T> actual;

    public SerializedObserver(Observer<? super T> observer) {
        this.actual = new SerializedObserverViaQueueAndLock<T>(observer);
    }

    @Override
    public void onCompleted() {
        actual.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        actual.onError(e);
    }

    @Override
    public void onNext(T t) {
        actual.onNext(t);
    }
}
