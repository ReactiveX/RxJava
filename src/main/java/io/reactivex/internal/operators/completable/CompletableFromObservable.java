package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class CompletableFromObservable<T> extends Completable {

    final ObservableConsumable<T> observable;

    public CompletableFromObservable(ObservableConsumable<T> observable) {
        this.observable = observable;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        observable.subscribe(new Observer<T>() {

            @Override
            public void onComplete() {
                s.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }

            @Override
            public void onNext(T value) {
                // ignored
            }

            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }
            
        });
    }
}
