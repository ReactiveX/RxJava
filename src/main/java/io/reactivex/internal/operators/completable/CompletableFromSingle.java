package io.reactivex.internal.operators.completable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class CompletableFromSingle<T> extends Completable {

    final SingleConsumable<T> single;
    
    public CompletableFromSingle(SingleConsumable<T> single) {
        this.single = single;
    }

    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        single.subscribe(new SingleSubscriber<T>() {

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }

            @Override
            public void onSubscribe(Disposable d) {
                s.onSubscribe(d);
            }

            @Override
            public void onSuccess(T value) {
                s.onComplete();
            }
            
        });
    }

}
