package rx.observers;

import rx.Subscriber;

/**
 * Observer that does nothing... including swallowing errors.
 */
public class EmptyObserver<T> extends Subscriber<T> {

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(T args) {

    }

}
