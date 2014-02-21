package rx.observers;

import rx.Observer;


/**
 * Observer that does nothing... including swallowing errors.
 */
public class EmptyObserver<T> implements Observer<T> {

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
