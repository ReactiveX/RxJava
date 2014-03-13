package rx.observers;

import rx.Observer;
import rx.Subscriber;

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
public class SerializedSubscriber<T> extends Subscriber<T> {

    private final Observer<T> s;

    public SerializedSubscriber(Subscriber<? super T> s) {
        this.s = new SerializedObserver<T>(s);
    }

    @Override
    public void onCompleted() {
        s.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        s.onError(e);
    }

    @Override
    public void onNext(T t) {
        s.onNext(t);
    }
}
